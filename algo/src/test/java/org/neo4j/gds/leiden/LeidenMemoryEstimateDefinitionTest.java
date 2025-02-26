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
package org.neo4j.gds.leiden;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.gds.assertions.MemoryEstimationAssert;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeidenMemoryEstimateDefinitionTest {

    @ParameterizedTest(name = "Concurrency: {0}")
    @CsvSource({
        "1, 19976368,25426368",
        "4, 27248944,32698944"
    })
    void shouldEstimateMemory(int concurrency,long expectedMin, long expectedMax) {
        var config = mock(LeidenBaseConfig.class);

        when(config.maxLevels()).thenReturn(3);

        var estimate = new LeidenAlgorithmFactory<>().memoryEstimation(config);
        
        MemoryEstimationAssert.assertThat(estimate)
            .memoryRange(10_1000,100_000,concurrency)
            .hasMin(expectedMin)
            .hasMax(expectedMax);
    }

}
