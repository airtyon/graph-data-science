/*
 * Copyright (c) 2017-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.embeddings.graphsage.proc;

import org.jetbrains.annotations.TestOnly;
import org.neo4j.gds.embeddings.graphsage.Aggregator;
import org.neo4j.gds.embeddings.graphsage.GraphSageHelper;
import org.neo4j.gds.embeddings.graphsage.algo.GraphSage;
import org.neo4j.gds.embeddings.graphsage.algo.GraphSageTrain;
import org.neo4j.gds.embeddings.graphsage.algo.GraphSageTrainConfig;
import org.neo4j.graphalgo.AbstractAlgorithmFactory;
import org.neo4j.graphalgo.api.Graph;
import org.neo4j.graphalgo.core.utils.ProgressLogger;
import org.neo4j.graphalgo.core.utils.mem.AllocationTracker;
import org.neo4j.graphalgo.core.utils.mem.MemoryEstimation;
import org.neo4j.graphalgo.core.utils.mem.MemoryEstimations;
import org.neo4j.graphalgo.core.utils.paged.HugeObjectArray;

import static org.neo4j.graphalgo.core.utils.mem.MemoryEstimations.PERSISTENT;
import static org.neo4j.graphalgo.core.utils.mem.MemoryEstimations.TEMPORARY;
import static org.neo4j.graphalgo.core.utils.mem.MemoryUsage.sizeOfDoubleArray;

public final class GraphSageTrainAlgorithmFactory extends AbstractAlgorithmFactory<GraphSageTrain, GraphSageTrainConfig> {

    public GraphSageTrainAlgorithmFactory() {
        super();
    }

    @Override
    protected long taskVolume(Graph graph, GraphSageTrainConfig configuration) {
        return 1;
    }

    @Override
    protected String taskName() {
        return GraphSageTrain.class.getSimpleName();
    }

    @Override
    protected GraphSageTrain build(
        Graph graph,
        GraphSageTrainConfig configuration,
        AllocationTracker tracker,
        ProgressLogger progressLogger
    ) {
        return new GraphSageTrain(graph, configuration, tracker, progressLogger);
    }

    @Override
    public MemoryEstimation memoryEstimation(GraphSageTrainConfig configuration) {
        return MemoryEstimations.setup(
            "",
            graphDimensions -> estimate(configuration, graphDimensions.nodeCount())
        );
    }

    private MemoryEstimation estimate(GraphSageTrainConfig config, long nodeCount) {
        var layerConfigs = config.layerConfigs();
        var numberOfLayers = layerConfigs.size();

        var layerBuilder = MemoryEstimations.builder("GraphSageTrain")
            .startField(PERSISTENT)
            .startField("weights");

        long initialAdamOptimizer = 0L;
        long updateAdamOptimizer = 0L;
        for (int i = 0; i < numberOfLayers; i++) {
            var layerConfig = layerConfigs.get(i);
            var weightDimensions = layerConfig.rows() * layerConfig.cols();
            var weightsMemory = sizeOfDoubleArray(weightDimensions);
            if (layerConfig.aggregatorType() == Aggregator.AggregatorType.POOL) {
                // selfWeights
                weightsMemory += sizeOfDoubleArray(layerConfig.rows() * layerConfig.rows());
                // neighborsWeights
                weightsMemory += sizeOfDoubleArray(layerConfig.rows() * layerConfig.rows());
                // bias
                weightsMemory += sizeOfDoubleArray(layerConfig.rows());
            }
            layerBuilder.fixed("layer " + (i + 1), weightsMemory);

            initialAdamOptimizer += 2 * sizeOfDoubleArray(weightDimensions);
            updateAdamOptimizer += 5 * weightDimensions;
        }

        return layerBuilder
            .endField()
            .endField()
            .startField(TEMPORARY)
            .field("this.instance", GraphSage.class)
            .add("initialFeatures", HugeObjectArray.memoryEstimation(sizeOfDoubleArray(config.featuresSize())))
            .startField("trainOnEpoch")
            .fixed("initialAdamOptimizer", initialAdamOptimizer)
            .perThread("concurrentBatches", MemoryEstimations
                .builder()
                .startField("trainOnBatch")
                .add(GraphSageHelper.embeddingsEstimation(config, 3 * config.batchSize(), nodeCount, true))
                .fixed("updateAdamOptimizer", updateAdamOptimizer)
                .endField()
                .build())
            .endField()
            .endField()
            .build();
    }

    @TestOnly
    public GraphSageTrainAlgorithmFactory(ProgressLogger.ProgressLoggerFactory factory) {
        super(factory);
    }
}
