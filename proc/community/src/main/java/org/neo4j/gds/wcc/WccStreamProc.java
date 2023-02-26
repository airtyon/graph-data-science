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
package org.neo4j.gds.wcc;

import org.neo4j.gds.CommunityProcCompanion;
import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.StreamProc;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.core.utils.paged.dss.DisjointSetStruct;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ExecutionMode;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.wcc.WccProc.WCC_DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

@GdsCallable(name = "gds.wcc.stream", description = WCC_DESCRIPTION, executionMode = ExecutionMode.STREAM)
public class WccStreamProc extends StreamProc<
    Wcc,
    DisjointSetStruct,
    WccStreamProc.StreamResult,
    WccStreamConfig> {

    @Procedure(value = "gds.wcc.stream", mode = READ)
    @Description(WCC_DESCRIPTION)
    public Stream<WccStreamProc.StreamResult> stream(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        ComputationResult<Wcc, DisjointSetStruct, WccStreamConfig> computationResult = compute(
            graphName,
            configuration
        );
        return stream(computationResult);
    }

    @Procedure(value = "gds.wcc.stream.estimate", mode = READ)
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        return computeEstimate(graphNameOrConfiguration, algoConfiguration);
    }

    @Override
    protected WccStreamConfig newConfig(String username, CypherMapWrapper config) {
        return WccStreamConfig.of(config);
    }

    @Override
    public GraphAlgorithmFactory<Wcc, WccStreamConfig> algorithmFactory() {
        return WccProc.algorithmFactory();
    }

    @Override
    protected StreamResult streamResult(
        long originalNodeId, long internalNodeId, NodePropertyValues nodePropertyValues
    ) {
        return new StreamResult(originalNodeId, nodePropertyValues.longValue(internalNodeId));
    }

    @Override
    protected NodePropertyValues nodeProperties(ComputationResult<Wcc, DisjointSetStruct, WccStreamConfig> computationResult) {
        return CommunityProcCompanion.nodeProperties(
                computationResult.config(),
                computationResult.result().asNodeProperties()
        );
    }

    @SuppressWarnings("unused")
    public static class StreamResult {

        public final long nodeId;

        public final long componentId;

        public StreamResult(long nodeId, long componentId) {
            this.nodeId = nodeId;
            this.componentId = componentId;
        }
    }
}
