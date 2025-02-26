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
package org.neo4j.gds.algorithms.estimation;

import org.neo4j.gds.Algorithm;
import org.neo4j.gds.AlgorithmFactory;
import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.api.GraphName;
import org.neo4j.gds.api.User;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.core.GraphDimensions;
import org.neo4j.gds.core.loading.GraphStoreCatalogService;
import org.neo4j.gds.core.utils.mem.MemoryEstimations;
import org.neo4j.gds.core.utils.mem.MemoryTreeWithDimensions;
import org.neo4j.gds.memest.DatabaseGraphStoreEstimationService;
import org.neo4j.gds.memest.FictitiousGraphStoreEstimationService;
import org.neo4j.gds.memest.MemoryEstimationGraphConfigParser;
import org.neo4j.gds.results.MemoryEstimateResult;

import java.util.Map;
import java.util.Optional;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

public class AlgorithmEstimator {
    private final GraphStoreCatalogService graphStoreCatalogService;

    private final FictitiousGraphStoreEstimationService fictitiousGraphStoreEstimationService;
    private final DatabaseGraphStoreEstimationService databaseGraphStoreEstimationService;
    private final DatabaseId databaseId;
    private final User user;

    public AlgorithmEstimator(
        GraphStoreCatalogService graphStoreCatalogService,
        FictitiousGraphStoreEstimationService fictitiousGraphStoreEstimationService,
        DatabaseGraphStoreEstimationService databaseGraphStoreEstimationService,
        DatabaseId databaseId,
        User user
    ) {
        this.graphStoreCatalogService = graphStoreCatalogService;
        this.fictitiousGraphStoreEstimationService = fictitiousGraphStoreEstimationService;
        this.databaseGraphStoreEstimationService = databaseGraphStoreEstimationService;
        this.databaseId = databaseId;
        this.user = user;
    }

    public <G, A extends Algorithm<?>, C extends AlgoBaseConfig> MemoryEstimateResult estimate(
        Object graphNameOrConfiguration,
        C config,
        Optional<String> maybeRelationshipProperty,
        AlgorithmFactory<G, A, C> algorithmFactory
    ) {
        GraphDimensions dimensions;

        var estimationBuilder = MemoryEstimations.builder("Memory Estimation");
        if (graphNameOrConfiguration instanceof Map) {
            var memoryEstimationGraphConfigParser = new MemoryEstimationGraphConfigParser(user.getUsername());
            var graphProjectConfig = memoryEstimationGraphConfigParser.parse(graphNameOrConfiguration);

            var graphMemoryEstimation = graphProjectConfig.isFictitiousLoading()
                ? fictitiousGraphStoreEstimationService.estimate(graphProjectConfig)
                : databaseGraphStoreEstimationService.estimate(graphProjectConfig);

            dimensions = graphMemoryEstimation.dimensions();
            estimationBuilder.add("graph", graphMemoryEstimation.estimateMemoryUsageAfterLoading());
        } else if (graphNameOrConfiguration instanceof String) {
            var graphStore = graphStoreCatalogService.getGraphWithGraphStore(
                GraphName.parse(
                    (String) graphNameOrConfiguration),
                config,
                maybeRelationshipProperty,
                user,
                databaseId
            ).getRight();
            dimensions = GraphDimensionsComputer.of(graphStore, config);
        } else {
            throw new IllegalArgumentException(formatWithLocale(
                "Expected `graphNameOrConfiguration` to be of type String or Map, but got `%s`",
                graphNameOrConfiguration.getClass().getSimpleName()
            ));
        }

        var memoryTree = estimationBuilder
            .add("algorithm", algorithmFactory.memoryEstimation(config))
            .build()
            .estimate(dimensions, config.concurrency());

        return new MemoryEstimateResult(new MemoryTreeWithDimensions(memoryTree, dimensions));
    }

}
