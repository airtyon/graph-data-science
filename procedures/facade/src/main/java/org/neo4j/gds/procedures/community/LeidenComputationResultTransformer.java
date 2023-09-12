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
package org.neo4j.gds.procedures.community;

import org.neo4j.gds.CommunityProcCompanion;
import org.neo4j.gds.algorithms.LeidenSpecificFields;
import org.neo4j.gds.algorithms.NodePropertyMutateResult;
import org.neo4j.gds.algorithms.StreamComputationResult;
import org.neo4j.gds.leiden.LeidenBaseConfig;
import org.neo4j.gds.leiden.LeidenResult;
import org.neo4j.gds.nodeproperties.LongNodePropertyValuesAdapter;
import org.neo4j.gds.procedures.community.leiden.LeidenMutateResult;
import org.neo4j.gds.procedures.community.leiden.LeidenStreamResult;

import java.util.stream.LongStream;
import java.util.stream.Stream;

final class LeidenComputationResultTransformer {
    private LeidenComputationResultTransformer() {}

    static Stream<LeidenStreamResult> toStreamResult(StreamComputationResult<LeidenResult> computationResult, LeidenBaseConfig configuration) {
        return computationResult.result().map(leidenResult -> {
            var graph = computationResult.graph();

            var nodePropertyValues = CommunityProcCompanion.nodeProperties(
                configuration,
                LongNodePropertyValuesAdapter.create(leidenResult.dendrogramManager().getCurrent())
            );
            var includeIntermediateCommunities = configuration.includeIntermediateCommunities();

            return LongStream.range(0, graph.nodeCount())
                .boxed().
                filter(nodePropertyValues::hasValue)
                .map(nodeId -> {
                    var communities = includeIntermediateCommunities
                        ? leidenResult.getIntermediateCommunities(nodeId)
                        : null;
                    var communityId = nodePropertyValues.longValue(nodeId);
                    return new LeidenStreamResult(graph.toOriginalNodeId(nodeId), communities, communityId);
                });
        }).orElseGet(Stream::empty);
    }

    static LeidenMutateResult toMutateResult(NodePropertyMutateResult<LeidenSpecificFields> computationResult) {
        return new LeidenMutateResult(
            computationResult.algorithmSpecificFields().ranLevels(),
            computationResult.algorithmSpecificFields().didConverge(),
            computationResult.algorithmSpecificFields().nodeCount(),
            computationResult.algorithmSpecificFields().communityCount(),
            computationResult.preProcessingMillis(),
            computationResult.computeMillis(),
            computationResult.postProcessingMillis(),
            computationResult.mutateMillis(),
            computationResult.nodePropertiesWritten(),
            computationResult.algorithmSpecificFields().communityDistribution(),
            computationResult.algorithmSpecificFields().modularities(),
            computationResult.algorithmSpecificFields().modularity(),
            computationResult.configuration().toMap()
        );
    }


}