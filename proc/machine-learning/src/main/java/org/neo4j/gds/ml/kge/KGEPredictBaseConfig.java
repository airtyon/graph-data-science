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
package org.neo4j.gds.ml.kge;

import org.immutables.value.Value;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.similarity.filtering.NodeFilterSpec;

import java.util.Collection;
import java.util.List;

public interface KGEPredictBaseConfig extends AlgoBaseConfig {

    @Value.Default
    @Configuration.ConvertWith(method = "org.neo4j.gds.similarity.filtering.NodeFilterSpecFactory#create")
    @Configuration.ToMapValue("org.neo4j.gds.similarity.filtering.NodeFilterSpecFactory#render")
    default NodeFilterSpec sourceNodeFilter() {
        return NodeFilterSpec.noOp;
    }

    @Value.Default
    @Configuration.ConvertWith(method = "org.neo4j.gds.similarity.filtering.NodeFilterSpecFactory#create")
    @Configuration.ToMapValue("org.neo4j.gds.similarity.filtering.NodeFilterSpecFactory#render")
    default NodeFilterSpec targetNodeFilter() {
        return NodeFilterSpec.noOp;
    }

    @Value.Default
    @Configuration.Key(RELATIONSHIP_TYPES_KEY)
    default List<String> relationshipTypes() {
        return List.of();
    }

    String nodeEmbeddingProperty();

    //Consider using HugeList or double[] if that saves mem
    List<Double> relationshipTypeEmbedding();

    @Configuration.ConvertWith(method = "org.neo4j.gds.ml.kge.ScoreFunction#parse")
    @Configuration.ToMapValue("org.neo4j.gds.ml.kge.ScoreFunction#toString")
    ScoreFunction scoringFunction();

    @Configuration.IntegerRange(min = 1)
    int topK();

    @Configuration.GraphStoreValidationCheck
    default void validateSourceNodeFilter(
        GraphStore graphStore,
        Collection<NodeLabel> selectedLabels,
        Collection<RelationshipType> selectedRelationshipTypes
    ) {
        sourceNodeFilter().validate(graphStore, selectedLabels, "sourceNodeFilter");
    }

    @Configuration.GraphStoreValidationCheck
    default void validateTargetNodeFilter(
        GraphStore graphStore,
        Collection<NodeLabel> selectedLabels,
        Collection<RelationshipType> selectedRelationshipTypes
    ) {
        targetNodeFilter().validate(graphStore, selectedLabels, "targetNodeFilter");
    }

}
