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

import org.junit.jupiter.api.Test;
import org.neo4j.gds.AlgoBaseProc;
import org.neo4j.gds.core.CypherMapWrapper;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TriangleCountStreamProcTest extends TriangleCountBaseProcTest<TriangleCountStreamConfig> {

    @Test
    void testStreaming() {

        var query = "CALL gds.triangleCount.stream('" + DEFAULT_GRAPH_NAME + "')";

        var rowCount = new AtomicInteger();

        runQueryWithRowConsumer(query, row -> {
            assertEquals(1L, row.getNumber("triangleCount"));
            rowCount.incrementAndGet();
        });

        assertEquals(3, rowCount.get());
    }

    @Override
    public Class<? extends AlgoBaseProc<IntersectingTriangleCount, IntersectingTriangleCount.TriangleCountResult, TriangleCountStreamConfig, ?>> getProcedureClazz() {
        return TriangleCountStreamProc.class;
    }

    @Override
    public TriangleCountStreamConfig createConfig(CypherMapWrapper mapWrapper) {
        return TriangleCountStreamConfig.of(mapWrapper);
    }

}
