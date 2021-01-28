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
package org.neo4j.gds.ml.linkmodels;

import org.neo4j.gds.ml.MLTrainResult;
import org.neo4j.gds.ml.linkmodels.logisticregression.LinkLogisticRegressionData;
import org.neo4j.gds.ml.splitting.EdgeSplitter;
import org.neo4j.graphalgo.AlgorithmFactory;
import org.neo4j.graphalgo.TrainProc;
import org.neo4j.graphalgo.api.Graph;
import org.neo4j.graphalgo.api.GraphStore;
import org.neo4j.graphalgo.config.GraphCreateConfig;
import org.neo4j.graphalgo.core.CypherMapWrapper;
import org.neo4j.graphalgo.core.model.ModelCatalog;
import org.neo4j.graphalgo.core.utils.mem.AllocationTracker;
import org.neo4j.graphalgo.core.utils.mem.MemoryEstimation;
import org.neo4j.graphalgo.core.utils.progress.ProgressEventTracker;
import org.neo4j.graphalgo.exceptions.MemoryEstimationNotImplementedException;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class LinkPredictionTrainProc extends
    TrainProc<LinkPredictionTrain, LinkLogisticRegressionData, LinkPredictionTrainConfig> {

    @Procedure(name = "gds.alpha.ml.linkPrediction.train", mode = Mode.READ)
    @Description("Trains a link prediction model")
    public Stream<MLTrainResult> train(
        @Name(value = "graphName") Object graphNameOrConfig,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        var result = compute(
            graphNameOrConfig,
            configuration
        );
        ModelCatalog.set(result.result());
        return Stream.of(new MLTrainResult(result.result(), result.computeMillis()));
    }

    @Override
    protected LinkPredictionTrainConfig newConfig(
        String username,
        Optional<String> graphName,
        Optional<GraphCreateConfig> maybeImplicitCreate,
        CypherMapWrapper config
    ) {
        return LinkPredictionTrainConfig.of(username, graphName, maybeImplicitCreate, config);
    }

    @Override
    protected AlgorithmFactory<LinkPredictionTrain, LinkPredictionTrainConfig> algorithmFactory() {
        return new AlgorithmFactory<>() {
            @Override
            public GraphAndAlgorithm<LinkPredictionTrain> build(
                GraphStore graphStore,
                LinkPredictionTrainConfig configuration,
                AllocationTracker tracker,
                Log log,
                ProgressEventTracker eventTracker
            ) {
                var trainGraph = graphStore.getGraph(
                    configuration.nodeLabelIdentifiers(graphStore),
                    List.of(configuration.trainRelationshipType()),
                    Optional.of(EdgeSplitter.RELATIONSHIP_PROPERTY)
                );
                var testGraph = graphStore.getGraph(
                    configuration.nodeLabelIdentifiers(graphStore),
                    List.of(configuration.testRelationshipType()),
                    Optional.of(EdgeSplitter.RELATIONSHIP_PROPERTY)
                );

                var algo = new LinkPredictionTrain(trainGraph, testGraph, configuration, log);
                return GraphAndAlgorithm.of(trainGraph, algo);
            }

            @Override
            public LinkPredictionTrain build(
                Graph graph,
                LinkPredictionTrainConfig configuration,
                AllocationTracker tracker,
                Log log,
                ProgressEventTracker eventTracker
            ) {
                throw new UnsupportedOperationException("Link Prediction requires two graphs as input.");
            }

            @Override
            public MemoryEstimation memoryEstimation(LinkPredictionTrainConfig configuration) {
                throw new MemoryEstimationNotImplementedException();
            }
        };
    }
}
