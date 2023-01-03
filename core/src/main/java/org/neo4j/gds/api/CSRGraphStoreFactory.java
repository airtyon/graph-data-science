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
package org.neo4j.gds.api;

import org.neo4j.gds.api.schema.GraphSchema;
import org.neo4j.gds.config.GraphProjectConfig;
import org.neo4j.gds.core.GraphDimensions;
import org.neo4j.gds.core.loading.CSRGraphStore;
import org.neo4j.gds.core.loading.Capabilities;
import org.neo4j.gds.core.loading.GraphStoreBuilder;
import org.neo4j.gds.core.loading.NodeImportResult;
import org.neo4j.gds.core.loading.RelationshipImportResult;
import org.neo4j.gds.mem.MemoryUsage;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

public abstract class CSRGraphStoreFactory<CONFIG extends GraphProjectConfig> extends GraphStoreFactory<CSRGraphStore, CONFIG> {

    public CSRGraphStoreFactory(
        CONFIG graphProjectConfig,
        Capabilities capabilities,
        GraphLoaderContext loadingContext,
        GraphDimensions dimensions
    ) {
        super(graphProjectConfig, capabilities, loadingContext, dimensions);
    }

    protected CSRGraphStore createGraphStore(
        NodeImportResult nodeImportResult,
        RelationshipImportResult relationshipImportResult
    ) {
        return new GraphStoreBuilder()
            .databaseId(DatabaseId.of(loadingContext.graphDatabaseService()))
            .capabilities(capabilities)
            .schema(computeGraphSchema(nodeImportResult, relationshipImportResult))
            .nodeImportResult(NodeImportResult.of(nodeImportResult.idMap(), nodeImportResult.properties()))
            .relationshipImportResult(relationshipImportResult)
            .concurrency(graphProjectConfig.readConcurrency())
            .build();
    }

    protected void logLoadingSummary(GraphStore graphStore) {
        var sizeInBytes = MemoryUsage.sizeOf(graphStore);
        var memoryUsage = MemoryUsage.humanReadable(sizeInBytes);
        progressTracker.logInfo(formatWithLocale("Actual memory usage of the loaded graph: %s", memoryUsage));
    }

    protected abstract GraphSchema computeGraphSchema(
        NodeImportResult nodeImportResult,
        RelationshipImportResult relationshipImportResult
    );
}
