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
package org.neo4j.gds.catalog;

import org.jetbrains.annotations.Nullable;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.config.GraphProjectConfig;
import org.neo4j.gds.core.loading.GraphStoreCatalog;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public class GraphInfoWithHistogram extends GraphInfo {

    public final Map<String, Object> degreeDistribution;

    public GraphInfoWithHistogram(
        GraphInfo graphInfo,
        @Nullable Map<String, Object> degreeDistribution
    ) {
        super(
            graphInfo.graphName,
            graphInfo.database,
            graphInfo.memoryUsage,
            graphInfo.sizeInBytes,
            graphInfo.nodeProjection,
            graphInfo.relationshipProjection,
            graphInfo.nodeQuery,
            graphInfo.relationshipQuery,
            graphInfo.nodeFilter,
            graphInfo.relationshipFilter,
            graphInfo.nodeCount,
            graphInfo.relationshipCount,
            graphInfo.creationTime,
            graphInfo.modificationTime,
            graphInfo.schema,
            graphInfo.configuration
        );
        this.degreeDistribution = degreeDistribution;
    }

    static GraphInfoWithHistogram of(GraphProjectConfig graphProjectConfig, GraphStore graphStore, boolean computeHistogram) {
        var graphInfo = GraphInfo.withMemoryUsage(graphProjectConfig, graphStore);

        Optional<Map<String, Object>> maybeDegreeDistribution = GraphStoreCatalog.getDegreeDistribution(
            graphProjectConfig.username(),
            graphStore.databaseId(),
            graphProjectConfig.graphName()
        );

        var degreeDistribution = maybeDegreeDistribution.orElseGet(() -> {
            if (computeHistogram) {
                var newHistogram = GraphInfoHelper.degreeDistribution(graphStore.getUnion());
                // Cache the computed degree distribution in the Catalog
                GraphStoreCatalog.setDegreeDistribution(
                    graphProjectConfig.username(),
                    graphStore.databaseId(),
                    graphProjectConfig.graphName(),
                    newHistogram
                );
                return newHistogram;
            } else {
                return null;
            }
        });

        return new GraphInfoWithHistogram(graphInfo, degreeDistribution);
    }
}
