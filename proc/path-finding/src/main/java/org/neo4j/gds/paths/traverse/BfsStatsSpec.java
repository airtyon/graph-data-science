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
package org.neo4j.gds.paths.traverse;

import org.neo4j.gds.core.utils.paged.HugeLongArray;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;
import org.neo4j.gds.results.StandardStatsResult;

import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.MUTATE_RELATIONSHIP;

@GdsCallable(name = "gds.bfs.stats", description = BfsStreamProc.DESCRIPTION, executionMode = MUTATE_RELATIONSHIP)
public class BfsStatsSpec implements AlgorithmSpec<BFS, HugeLongArray, BfsStatsConfig, Stream<StandardStatsResult>, BfsAlgorithmFactory<BfsStatsConfig>> {
    @Override
    public String name() {
        return "gds.bfs.stats";
    }

    @Override
    public BfsAlgorithmFactory<BfsStatsConfig> algorithmFactory() {
        return new BfsAlgorithmFactory<>();
    }

    @Override
    public NewConfigFunction<BfsStatsConfig> newConfigFunction() {
        return (__, config) -> BfsStatsConfig.of(config);
    }

    @Override
    public ComputationResultConsumer<BFS, HugeLongArray, BfsStatsConfig, Stream<StandardStatsResult>> computationResultConsumer() {
        return (computationResult, executionContext) -> Stream.of(new StandardStatsResult(
            computationResult.preProcessingMillis(),
            computationResult.computeMillis(),
            0,
            computationResult.config().toMap()
        ));
    }
}
