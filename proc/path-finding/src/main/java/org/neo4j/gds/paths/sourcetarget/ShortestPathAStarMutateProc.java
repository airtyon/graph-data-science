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
package org.neo4j.gds.paths.sourcetarget;

import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.paths.MutateResult;
import org.neo4j.gds.paths.ShortestPathMutateProc;
import org.neo4j.gds.paths.astar.AStar;
import org.neo4j.gds.paths.astar.AStarFactory;
import org.neo4j.gds.paths.astar.config.ShortestPathAStarMutateConfig;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.MUTATE_RELATIONSHIP;
import static org.neo4j.gds.paths.sourcetarget.ShortestPathAStarProc.ASTAR_DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

@GdsCallable(name = "gds.shortestPath.astar.mutate", description = ASTAR_DESCRIPTION, executionMode = MUTATE_RELATIONSHIP)
public class ShortestPathAStarMutateProc extends ShortestPathMutateProc<AStar, ShortestPathAStarMutateConfig> {

    @Procedure(name = "gds.shortestPath.astar.mutate", mode = READ)
    @Description(ASTAR_DESCRIPTION)
    public Stream<MutateResult> mutate(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return mutate(compute(graphName, configuration));
    }

    @Procedure(name = "gds.shortestPath.astar.mutate.estimate", mode = READ)
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        return computeEstimate(graphNameOrConfiguration, algoConfiguration);
    }

    @Override
    protected ShortestPathAStarMutateConfig newConfig(String username, CypherMapWrapper config) {
        return ShortestPathAStarMutateConfig.of(config);
    }

    @Override
    public GraphAlgorithmFactory<AStar, ShortestPathAStarMutateConfig> algorithmFactory() {
        return new AStarFactory<>();
    }
}
