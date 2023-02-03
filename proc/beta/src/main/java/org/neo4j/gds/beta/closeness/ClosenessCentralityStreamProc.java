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
package org.neo4j.gds.beta.closeness;

import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.StreamProc;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.validation.ValidationConfiguration;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.beta.closeness.ClosenessCentralityProc.DESCRIPTION;
import static org.neo4j.gds.executor.ExecutionMode.STREAM;
import static org.neo4j.procedure.Mode.READ;

@GdsCallable(name = "gds.beta.closeness.stream", description = DESCRIPTION, executionMode = STREAM)
public class ClosenessCentralityStreamProc extends StreamProc<ClosenessCentrality, ClosenessCentralityResult, ClosenessCentralityStreamProc.StreamResult, ClosenessCentralityStreamConfig> {

    @Override
    public String name() {
        return "ClosenessCentrality";
    }

    @Procedure(value = "gds.beta.closeness.stream", mode = READ)
    @Description(DESCRIPTION)
    public Stream<StreamResult> stream(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        var computationResult = compute(
            graphName,
            configuration
        );
        return stream(computationResult);
    }

    @Override
    protected StreamResult streamResult(long originalNodeId, long internalNodeId, NodePropertyValues nodePropertyValues) {
        return new StreamResult(originalNodeId, nodePropertyValues.doubleValue(internalNodeId));
    }

    @Override
    protected NodePropertyValues nodeProperties(ComputationResult<ClosenessCentrality, ClosenessCentralityResult, ClosenessCentralityStreamConfig> computationResult) {
        return ClosenessCentralityProc.nodeProperties(computationResult);
    }

    @Override
    protected ClosenessCentralityStreamConfig newConfig(String username, CypherMapWrapper config) {
        return ClosenessCentralityStreamConfig.of(config);
    }

    @Override
    public ValidationConfiguration<ClosenessCentralityStreamConfig> validationConfig(ExecutionContext executionContext) {
        return ClosenessCentralityProc.getValidationConfig();
    }

    @Override
    public GraphAlgorithmFactory<ClosenessCentrality, ClosenessCentralityStreamConfig> algorithmFactory() {
        return ClosenessCentralityProc.algorithmFactory();
    }

    // TODO: remove this and use `org.neo4j.gds.common.CentralityStreamResult` when productizing.
    @SuppressWarnings("unused")
    public static final class StreamResult {

        public final long nodeId;

        public final double score;

        StreamResult(long nodeId, double score) {
            this.nodeId = nodeId;
            this.score = score;
        }
    }

}
