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
package org.neo4j.gds.paths.spanningtree;

import org.neo4j.gds.api.IdMap;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;
import org.neo4j.gds.spanningtree.Prim;
import org.neo4j.gds.spanningtree.SpanningTree;
import org.neo4j.gds.spanningtree.SpanningTreeAlgorithmFactory;
import org.neo4j.gds.spanningtree.SpanningTreeStreamConfig;

import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.neo4j.gds.LoggingUtil.runWithExceptionLogging;
import static org.neo4j.gds.executor.ExecutionMode.STREAM;

@GdsCallable(name = "gds.beta.spanningTree.stream", description = SpanningTreeWriteProc.DESCRIPTION, executionMode = STREAM)
public class SpanningTreeStreamSpec implements AlgorithmSpec<Prim, SpanningTree, SpanningTreeStreamConfig, Stream<StreamResult>, SpanningTreeAlgorithmFactory<SpanningTreeStreamConfig>> {

    @Override
    public String name() {
        return "SpanningTreeStream";
    }

    @Override
    public SpanningTreeAlgorithmFactory<SpanningTreeStreamConfig> algorithmFactory(ExecutionContext executionContext) {
        return new SpanningTreeAlgorithmFactory<>();
    }

    @Override
    public NewConfigFunction<SpanningTreeStreamConfig> newConfigFunction() {
        return (__, config) -> SpanningTreeStreamConfig.of(config);

    }

    public ComputationResultConsumer<Prim, SpanningTree, SpanningTreeStreamConfig, Stream<StreamResult>> computationResultConsumer() {
        return (computationResult, executionContext) -> runWithExceptionLogging(
            "Result streaming failed",
            executionContext.log(),
            () -> computationResult.result()
                .map(result -> {
                    var sourceNode = computationResult.config().sourceNode();
                    var graph = computationResult.graph();
                    return LongStream.range(IdMap.START_NODE_ID, graph.nodeCount())
                        .filter(nodeId -> result.parent(nodeId) >= 0 || sourceNode == graph.toOriginalNodeId(nodeId))
                        .mapToObj(nodeId -> {
                            var originalId = graph.toOriginalNodeId(nodeId);
                            return new StreamResult(
                                originalId,
                                (sourceNode == originalId) ? sourceNode : graph.toOriginalNodeId(result.parent(nodeId)),
                                result.costToParent(nodeId)
                            );
                        });
                }).orElseGet(Stream::empty)
        );
    }
}
