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
package org.neo4j.gds.kcore;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.IdFunction;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.TestGraph;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@GdlExtension
class KCoreDecompositionTest {

    @GdlExtension
    @Nested
    class BlossomGraph {
        @GdlGraph(orientation = Orientation.UNDIRECTED)
        private static final String DB_CYPHER =
            "CREATE " +
            "  (z:node)," +
            "  (a:node)," +
            "  (b:node)," +
            "  (c:node)," +
            "  (d:node)," +
            "  (e:node)," +
            "  (f:node)," +
            "  (g:node)," +
            "  (h:node)," +

            "(a)-[:R]->(b)," +
            "(b)-[:R]->(c)," +
            "(c)-[:R]->(d)," +
            "(d)-[:R]->(e)," +
            "(e)-[:R]->(f)," +
            "(f)-[:R]->(g)," +
            "(g)-[:R]->(h)," +
            "(h)-[:R]->(c)";


        @Inject
        private TestGraph graph;

        @Inject
        private IdFunction idFunction;

        @ParameterizedTest
        @ValueSource(ints = {1, 4})
        void shouldComputeCoreDecomposition(int concurrency) {

            var kcore = new KCoreDecomposition(graph, concurrency, ProgressTracker.NULL_TRACKER, 1).compute();
            assertThat(kcore.degeneracy()).isEqualTo(2);
            var coreValues = kcore.coreValues();

            assertThat(coreValues.get(idFunction.of("z"))).isEqualTo(0);
            assertThat(coreValues.get(idFunction.of("a"))).isEqualTo(1);
            assertThat(coreValues.get(idFunction.of("b"))).isEqualTo(1);
            assertThat(coreValues.get(idFunction.of("c"))).isEqualTo(2);
            assertThat(coreValues.get(idFunction.of("d"))).isEqualTo(2);
            assertThat(coreValues.get(idFunction.of("e"))).isEqualTo(2);
            assertThat(coreValues.get(idFunction.of("f"))).isEqualTo(2);
            assertThat(coreValues.get(idFunction.of("g"))).isEqualTo(2);
            assertThat(coreValues.get(idFunction.of("h"))).isEqualTo(2);
            
        }
    }

}
