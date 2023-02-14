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
package org.neo4j.gds.beta.k1coloring;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.AlgoBaseProc;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.core.utils.paged.HugeLongArray;
import org.neo4j.gds.mem.MemoryUsage;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class K1ColoringStreamProcTest extends K1ColoringProcBaseTest<K1ColoringStreamConfig> {

    @Override
    public Class<? extends AlgoBaseProc<K1Coloring, HugeLongArray, K1ColoringStreamConfig, ?>> getProcedureClazz() {
        return K1ColoringStreamProc.class;
    }

    @Override
    public K1ColoringStreamConfig createConfig(CypherMapWrapper mapWrapper) {
        return K1ColoringStreamConfig.of(mapWrapper);
    }

    @Test
    void testStreamingImplicit() {
        @Language("Cypher")
        String yields = algoBuildStage()
            .streamMode()
            .yields("nodeId", "color");

        Map<Long, Long> coloringResult = new HashMap<>(4);
        runQueryWithRowConsumer(yields, (row) -> {
            long nodeId = row.getNumber("nodeId").longValue();
            long color = row.getNumber("color").longValue();
            coloringResult.put(nodeId, color);
        });

        assertNotEquals(coloringResult.get(0L), coloringResult.get(1L));
        assertNotEquals(coloringResult.get(0L), coloringResult.get(2L));
    }

    @Test
    void testStreamingEstimate() {
        @Language("Cypher")
        String query = algoBuildStage()
            .estimationMode(GdsCypher.ExecutionModes.STREAM)
            .yields("requiredMemory", "treeView", "bytesMin", "bytesMax");

        runQueryWithRowConsumer(query, row -> {
            assertTrue(row.getNumber("bytesMin").longValue() > 0);
            assertTrue(row.getNumber("bytesMax").longValue() > 0);

            String bytesHuman = MemoryUsage.humanReadable(row.getNumber("bytesMin").longValue());
            assertNotNull(bytesHuman);
            assertTrue(row.getString("requiredMemory").contains(bytesHuman));
        });
    }

    static Stream<Arguments> communitySizeInputs() {
        return Stream.of(
                Arguments.of(Map.of("minCommunitySize", 1), Map.of(
                        0L, 1L,
                        1L, 0L,
                        2L, 0L,
                        3L, 0L
                )),
                Arguments.of(Map.of("minCommunitySize", 2), Map.of(
                        1L, 0L,
                        2L, 0L,
                        3L, 0L
                ))
        );
    }

    @ParameterizedTest
    @MethodSource("communitySizeInputs")
    void testStreamingMinCommunitySize(Map<String, Long> parameter, Map<Long, Long> expectedResult) {
        @Language("Cypher")
        String yields = algoBuildStage()
                .streamMode()
                .addAllParameters(parameter)
                .yields("nodeId", "color");

        Map<Long, Long> coloringResult = new HashMap<>(4);
        runQueryWithRowConsumer(yields, (row) -> {
            long nodeId = row.getNumber("nodeId").longValue();
            long color = row.getNumber("color").longValue();
            coloringResult.put(nodeId, color);
        });

        assertEquals(coloringResult, expectedResult);
    }
}
