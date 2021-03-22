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
package org.neo4j.graphalgo.beta.filter;

import org.neo4j.graphalgo.RelationshipType;
import org.neo4j.graphalgo.annotation.ValueClass;
import org.neo4j.graphalgo.api.CompositeRelationshipIterator;
import org.neo4j.graphalgo.api.DefaultValue;
import org.neo4j.graphalgo.api.GraphStore;
import org.neo4j.graphalgo.api.NodeMapping;
import org.neo4j.graphalgo.api.RelationshipProperty;
import org.neo4j.graphalgo.api.RelationshipPropertyStore;
import org.neo4j.graphalgo.api.Relationships;
import org.neo4j.graphalgo.beta.filter.expression.EvaluationContext;
import org.neo4j.graphalgo.beta.filter.expression.Expression;
import org.neo4j.graphalgo.core.Aggregation;
import org.neo4j.graphalgo.core.concurrency.ParallelUtil;
import org.neo4j.graphalgo.core.loading.construction.GraphFactory;
import org.neo4j.graphalgo.core.loading.construction.RelationshipsBuilder;
import org.neo4j.graphalgo.core.utils.BitUtil;
import org.neo4j.graphalgo.core.utils.collection.primitive.PrimitiveLongIterable;
import org.neo4j.graphalgo.core.utils.collection.primitive.PrimitiveLongIterator;
import org.neo4j.graphalgo.core.utils.mem.AllocationTracker;
import org.neo4j.values.storable.NumberType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.neo4j.graphalgo.api.AdjacencyCursor.NOT_FOUND;

final class RelationshipsFilter {

    @ValueClass
    interface FilteredRelationships {

        Map<RelationshipType, Relationships.Topology> topology();

        Map<RelationshipType, RelationshipPropertyStore> propertyStores();
    }

    static FilteredRelationships filterRelationships(
        GraphStore graphStore,
        Expression expression,
        NodeMapping inputNodes,
        NodeMapping outputNodes,
        int concurrency,
        ExecutorService executorService,
        AllocationTracker allocationTracker
    ) {
        Map<RelationshipType, Relationships.Topology> topologies = new HashMap<>();
        Map<RelationshipType, RelationshipPropertyStore> relPropertyStores = new HashMap<>();

        for (RelationshipType relType : graphStore.relationshipTypes()) {
            var outputRelationships = filterRelationshipType(
                graphStore,
                expression,
                inputNodes,
                outputNodes,
                relType,
                concurrency,
                executorService,
                allocationTracker
            );

            // Drop relationship types that have been completely filtered out.
            if (outputRelationships.topology().elementCount() == 0) {
                continue;
            }

            topologies.put(relType, outputRelationships.topology());

            var propertyStoreBuilder = RelationshipPropertyStore.builder();
            outputRelationships.properties().forEach((propertyKey, properties) -> {
                propertyStoreBuilder.putIfAbsent(
                    propertyKey,
                    RelationshipProperty.of(
                        propertyKey,
                        NumberType.FLOATING_POINT,
                        GraphStore.PropertyState.PERSISTENT,
                        properties,
                        DefaultValue.forDouble(),
                        Aggregation.NONE
                    )
                );
            });

            relPropertyStores.put(relType, propertyStoreBuilder.build());
        }

        return ImmutableFilteredRelationships.builder()
            .topology(topologies)
            .propertyStores(relPropertyStores)
            .build();
    }

    @ValueClass
    interface FilteredRelationship {
        RelationshipType relationshipType();

        Relationships.Topology topology();

        Map<String, Relationships.Properties> properties();
    }

    private static FilteredRelationship filterRelationshipType(
        GraphStore graphStore,
        Expression relationshipExpr,
        NodeMapping inputNodes,
        NodeMapping outputNodes,
        RelationshipType relType,
        int concurrency,
        ExecutorService executorService,
        AllocationTracker allocationTracker
    ) {
        var propertyKeys = new ArrayList<>(graphStore.relationshipPropertyKeys(relType));

        var propertyConfigs = propertyKeys
            .stream()
            .map(key -> GraphFactory.PropertyConfig.of(Aggregation.NONE, DefaultValue.forDouble()))
            .collect(Collectors.toList());

        var relationshipsBuilder = GraphFactory.initRelationshipsBuilder()
            .nodes(outputNodes)
            .concurrency(concurrency)
            .tracker(allocationTracker)
            .addAllPropertyConfigs(propertyConfigs)
            .build();

        var compositeIterator = graphStore.getCompositeRelationshipIterator(relType, propertyKeys);

        var propertyIndices = IntStream
            .range(0, propertyKeys.size())
            .boxed()
            .collect(Collectors.toMap(propertyKeys::get, idx -> idx));

        var batchSize = BitUtil.ceilDiv(outputNodes.nodeCount(), concurrency);
        var relationshipFilterTasks = outputNodes
            .batchIterables(batchSize)
            .stream()
            .map(nodeIterator -> new RelationshipFilterTask(
                nodeIterator,
                relationshipExpr,
                compositeIterator.concurrentCopy(),
                inputNodes,
                outputNodes,
                relationshipsBuilder,
                relType,
                propertyIndices
            ))
            .collect(Collectors.toList());

        ParallelUtil.runWithConcurrency(concurrency, relationshipFilterTasks, executorService);

        var relationships = relationshipsBuilder.buildAll();
        var topology = relationships.get(0).topology();
        var properties = IntStream.range(0, propertyKeys.size())
            .boxed()
            .collect(Collectors.toMap(
                propertyKeys::get,
                idx -> relationships.get(idx).properties().orElseThrow(IllegalStateException::new)
            ));

        return ImmutableFilteredRelationship.builder()
            .relationshipType(relType)
            .topology(topology)
            .properties(properties)
            .build();
    }

    private RelationshipsFilter() {}

    private static final class RelationshipFilterTask implements Runnable {
        private final PrimitiveLongIterator nodeIterator;
        private final Expression expression;
        private final EvaluationContext.RelationshipEvaluationContext evaluationContext;
        private final CompositeRelationshipIterator relationshipIterator;
        private final NodeMapping inputNodes;
        private final NodeMapping outputNodes;
        private final RelationshipsBuilder relationshipsBuilder;
        private final RelationshipType relType;

        private RelationshipFilterTask(
            PrimitiveLongIterable nodeIterator,
            Expression expression,
            CompositeRelationshipIterator relationshipIterator,
            NodeMapping inputNodes,
            NodeMapping outputNodes,
            RelationshipsBuilder relationshipsBuilder,
            RelationshipType relType,
            Map<String, Integer> propertyIndices
        ) {
            this.nodeIterator = nodeIterator.iterator();
            this.expression = expression;
            this.relationshipIterator = relationshipIterator;
            this.inputNodes = inputNodes;
            this.outputNodes = outputNodes;
            this.relationshipsBuilder = relationshipsBuilder;
            this.relType = relType;
            this.evaluationContext = new EvaluationContext.RelationshipEvaluationContext(propertyIndices);
        }

        @Override
        public void run() {
            long node;

            while (nodeIterator.hasNext()) {
                node = nodeIterator.next();
                var neoSource = outputNodes.toOriginalNodeId(node);

                relationshipIterator.forEachRelationship(neoSource, (source, target, properties) -> {
                    var neoTarget = inputNodes.toOriginalNodeId(target);
                    var mappedTarget = outputNodes.toMappedNodeId(neoTarget);

                    if (mappedTarget != NOT_FOUND) {
                        evaluationContext.init(relType.name, properties);

                        if (expression.evaluate(evaluationContext) == Expression.TRUE) {
                            // TODO branching should happen somewhere else
                            if (properties.length == 0) {
                                relationshipsBuilder.add(neoSource, neoTarget);
                            } else if (properties.length == 1) {
                                relationshipsBuilder.add(neoSource, neoTarget, properties[0]);
                            } else {
                                relationshipsBuilder.add(neoSource, neoTarget, properties);
                            }
                        }
                    }

                    return true;
                });
            }
        }
    }

}
