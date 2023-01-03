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
package org.neo4j.gds.core.io;

import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.api.IdMap;
import org.neo4j.gds.api.RelationshipPropertyStore;
import org.neo4j.gds.api.Relationships;
import org.neo4j.gds.api.schema.RelationshipSchema;
import org.neo4j.gds.core.io.file.RelationshipBuilderFromVisitor;
import org.neo4j.gds.core.io.file.RelationshipVisitor;
import org.neo4j.gds.core.loading.construction.GraphFactory;
import org.neo4j.gds.core.loading.construction.RelationshipsBuilder;
import org.neo4j.gds.core.loading.construction.RelationshipsBuilderBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GraphStoreRelationshipVisitor extends RelationshipVisitor {

    private final Supplier<RelationshipsBuilderBuilder> relationshipBuilderSupplier;
    private final Map<String, RelationshipsBuilder> relationshipBuilders;
    private final Map<String, RelationshipBuilderFromVisitor> relationshipFromVisitorBuilders;

    public GraphStoreRelationshipVisitor(
        RelationshipSchema relationshipSchema,
        Supplier<RelationshipsBuilderBuilder> relationshipBuilderSupplier,
        Map<String, RelationshipsBuilder> relationshipBuilders
    ) {
        super(relationshipSchema);
        this.relationshipBuilderSupplier = relationshipBuilderSupplier;
        this.relationshipBuilders = relationshipBuilders;
        relationshipFromVisitorBuilders = new HashMap<>();
    }

    @Override
    protected void exportElement() {
        // TODO: this logic should move to the RelationshipsBuilder
        var relationshipsBuilder = relationshipFromVisitorBuilders.computeIfAbsent(
                relationshipType(),
                (relationshipType) -> {
                    var propertyConfigs = getPropertySchema()
                        .stream()
                        .map(schema -> GraphFactory.PropertyConfig.of(schema.key(), schema.aggregation(), schema.defaultValue()))
                        .collect(Collectors.toList());
                    var relBuilder = relationshipBuilderSupplier.get()
                        .propertyConfigs(propertyConfigs)
                        .build();
                    relationshipBuilders.put(relationshipType, relBuilder);
                    return RelationshipBuilderFromVisitor.of(
                        propertyConfigs.size(),
                        relBuilder,
                        GraphStoreRelationshipVisitor.this
                    );
                }
            );
        relationshipsBuilder.addFromVisitor();
    }

    public static final class Builder extends RelationshipVisitor.Builder<Builder, GraphStoreRelationshipVisitor> {

        Map<String, RelationshipsBuilder> relationshipBuildersByType;
        int concurrency;
        IdMap nodes;

        public Builder withRelationshipBuildersToTypeResultMap(Map<String, RelationshipsBuilder> relationshipBuildersByType) {
            this.relationshipBuildersByType = relationshipBuildersByType;
            return this;
        }

        public Builder withConcurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        public Builder withNodes(IdMap nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder withAllocationTracker() {
            return this;
        }

        @Override
        public Builder me() {
            return this;
        }

        @Override
        public GraphStoreRelationshipVisitor build() {
            Supplier<RelationshipsBuilderBuilder> relationshipBuilderSupplier = () -> GraphFactory
                .initRelationshipsBuilder()
                .concurrency(concurrency)
                .nodes(nodes);
            return new GraphStoreRelationshipVisitor(
                relationshipSchema,
                relationshipBuilderSupplier,
                relationshipBuildersByType
            );
        }
    }

    @ValueClass
    interface RelationshipVisitorResult {
        Map<RelationshipType, Relationships.Topology> relationshipTypesWithTopology();
        Map<RelationshipType, RelationshipPropertyStore> propertyStores();
        long relationshipCount();
    }
}
