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
package org.neo4j.gds.embeddings.hashgnn;

import org.neo4j.gds.BaseProc;
import org.neo4j.gds.executor.MemoryEstimationExecutor;
import org.neo4j.gds.executor.ProcedureExecutor;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.embeddings.hashgnn.HashGNNProcCompanion.DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

public class HashGNNMutateProc extends BaseProc {

    @Procedure(value = "gds.hashgnn.mutate", mode = READ)
    @Description(DESCRIPTION)
    public Stream<MutateResult> mutate(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return new ProcedureExecutor<>(
            new HashGNNMutateSpec(),
            executionContext()
        ).compute(graphName, configuration);
    }

    @Internal
    @Deprecated(forRemoval = true)
    @Procedure(value = "gds.beta.hashgnn.mutate", deprecatedBy = "gds.hashgnn.mutate", mode = READ)
    @Description(DESCRIPTION)
    public Stream<MutateResult> betaMutate(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        executionContext()
            .metricsFacade()
            .deprecatedProcedures().called("gds.beta.hashgnn.mutate");
        executionContext().log()
            .warn("Procedure `gds.beta.hashgnn.mutate` has been deprecated, please use `gds.hashgnn.mutate`.");
        return mutate(graphName, configuration);
    }

    @Procedure(value = "gds.hashgnn.mutate.estimate", mode = READ)
    @Description(DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        return new MemoryEstimationExecutor<>(
            new HashGNNMutateSpec(),
            executionContext(),
            transactionContext()
        ).computeEstimate(graphNameOrConfiguration, algoConfiguration);
    }

    @Internal
    @Deprecated(forRemoval = true)
    @Procedure(value = "gds.beta.hashgnn.mutate.estimate", deprecatedBy = "gds.hashgnn.mutate.estimate", mode = READ)
    @Description(DESCRIPTION)
    public Stream<MemoryEstimateResult> betaEstimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        executionContext()
            .metricsFacade()
            .deprecatedProcedures().called("gds.beta.hashgnn.mutate.estimate");
        executionContext().log()
            .warn("Procedure `gds.beta.hashgnn.mutate.estimate` has been deprecated, please use `gds.hashgnn.mutate.estimate`.");
        return estimate(graphNameOrConfiguration, algoConfiguration);
    }
}
