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
package org.neo4j.gds.spanningtree;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.assertions.MemoryEstimationAssert;
import org.neo4j.gds.core.GraphDimensions;

import static org.mockito.Mockito.mock;

class SpanningTreeMemoryEstimateDefinitionTest {

    @Test
    void shouldEstimateMemory() {

        var config = mock(SpanningTreeBaseConfig.class);

        var memoryEstimation = new SpanningTreeMemoryEstimateDefinition().memoryEstimation(config);
        MemoryEstimationAssert.assertThat(memoryEstimation)
            .memoryRange( GraphDimensions.of(10_000, 100_000), 1)
            .hasSameMinAndMaxEqualTo(321544);
    }
}
