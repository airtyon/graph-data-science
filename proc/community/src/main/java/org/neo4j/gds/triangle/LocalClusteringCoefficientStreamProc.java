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
package org.neo4j.gds.triangle;


import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.StreamProc;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.validation.ValidationConfiguration;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.STREAM;
import static org.neo4j.gds.triangle.LocalClusteringCoefficientCompanion.DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

@GdsCallable(name = "gds.localClusteringCoefficient.stream", description = DESCRIPTION, executionMode = STREAM)
public class LocalClusteringCoefficientStreamProc extends StreamProc<
    LocalClusteringCoefficient,
    LocalClusteringCoefficient.Result,
    LocalClusteringCoefficientStreamProc.Result,
    LocalClusteringCoefficientStreamConfig
    > {

    @Procedure(name = "gds.localClusteringCoefficient.stream", mode = READ)
    @Description(DESCRIPTION)
    public Stream<Result> stream(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return stream(compute(graphName, configuration));
    }

    @Procedure(value = "gds.localClusteringCoefficient.stream.estimate", mode = READ)
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        return computeEstimate(graphNameOrConfiguration, algoConfiguration);
    }

    @Override
    protected LocalClusteringCoefficientStreamConfig newConfig(String username, CypherMapWrapper config) {
        return LocalClusteringCoefficientStreamConfig.of(config);
    }

    @Override
    public GraphAlgorithmFactory<LocalClusteringCoefficient, LocalClusteringCoefficientStreamConfig> algorithmFactory() {
        return new LocalClusteringCoefficientFactory<>();
    }

    @Override
    protected Result streamResult(
        long originalNodeId, long internalNodeId, NodePropertyValues nodePropertyValues
    ) {
        return new Result(originalNodeId, nodePropertyValues.doubleValue(internalNodeId));
    }

    @Override
    public ValidationConfiguration<LocalClusteringCoefficientStreamConfig> validationConfig(ExecutionContext executionContext) {
        return LocalClusteringCoefficientCompanion.getValidationConfig(executionContext.log());
    }

    @Override
    protected NodePropertyValues nodeProperties(
        ComputationResult<LocalClusteringCoefficient, LocalClusteringCoefficient.Result, LocalClusteringCoefficientStreamConfig> computationResult
    ) {
        return LocalClusteringCoefficientCompanion.nodeProperties(computationResult);
    }

    @SuppressWarnings("unused")
    public static class Result {
        public final long nodeId;
        public final double localClusteringCoefficient;

        public Result(long nodeId, double localClusteringCoefficient) {
            this.nodeId = nodeId;
            this.localClusteringCoefficient = localClusteringCoefficient;
        }
    }
}

