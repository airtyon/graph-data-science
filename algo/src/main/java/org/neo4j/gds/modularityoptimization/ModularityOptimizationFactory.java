/*
 * Copyright (c) "Neo4j"
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
package org.neo4j.gds.modularityoptimization;

import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.config.BaseConfig;
import org.neo4j.gds.config.IterationsConfig;
import org.neo4j.gds.core.concurrency.DefaultPool;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;
import org.neo4j.gds.k1coloring.ImmutableK1ColoringStreamConfig;
import org.neo4j.gds.k1coloring.K1ColoringAlgorithmFactory;
import org.neo4j.gds.k1coloring.K1ColoringBaseConfig;

import java.util.List;

import static org.neo4j.gds.modularityoptimization.ModularityOptimization.K1COLORING_MAX_ITERATIONS;

public class ModularityOptimizationFactory<CONFIG extends ModularityOptimizationBaseConfig> extends
    GraphAlgorithmFactory<ModularityOptimization, CONFIG> {

    private static final String MODULARITY_OPTIMIZATION_TASK_NAME = "ModularityOptimization";


    @Override
    public MemoryEstimation memoryEstimation(CONFIG configuration) {

        return new ModularityOptimizationMemoryEstimateDefinition().memoryEstimation(configuration);
    }

    @Override
    public String taskName() {
        return MODULARITY_OPTIMIZATION_TASK_NAME;
    }

    @Override
    public ModularityOptimization build(
        Graph graph,
        CONFIG configuration,
        ProgressTracker progressTracker
    ) {
        var seedProperty = configuration.seedProperty() != null ? graph.nodeProperties(configuration.seedProperty()) : null;
        return build(graph, configuration, seedProperty, progressTracker);
    }

    public ModularityOptimization build(
        Graph graph,
        CONFIG configuration,
        NodePropertyValues seedProperty,
        ProgressTracker progressTracker
    ) {
        return new ModularityOptimization(
            graph,
            configuration.maxIterations(),
            configuration.tolerance(),
            seedProperty,
            configuration.concurrency(),
            configuration.batchSize(),
            DefaultPool.INSTANCE,
            progressTracker
        );
    }

    @Override
    public Task progressTask(Graph graph, CONFIG config) {
        return modularityOptimizationProgressTask(graph, config);
    }

    public static <T extends BaseConfig & IterationsConfig> Task modularityOptimizationProgressTask(Graph graph, T config) {
        return Tasks.task(
            MODULARITY_OPTIMIZATION_TASK_NAME,
            Tasks.task(
                "initialization",
                K1ColoringAlgorithmFactory.k1ColoringProgressTask(graph, createModularityConfig())
            ),
            Tasks.iterativeDynamic(
                "compute modularity",
                () -> List.of(Tasks.leaf("optimizeForColor", graph.relationshipCount())),
                config.maxIterations()
            )
        );
    }

    private static K1ColoringBaseConfig createModularityConfig() {
        return ImmutableK1ColoringStreamConfig
            .builder()
            .maxIterations(K1COLORING_MAX_ITERATIONS)
            .build();

    }
}
