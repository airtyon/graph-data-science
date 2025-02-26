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

import org.neo4j.gds.AlgorithmMemoryEstimateDefinition;
import org.neo4j.gds.collections.ha.HugeDoubleArray;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.mem.MemoryEstimations;

public class LocalClusteringCoefficientMemoryEstimateDefinition implements AlgorithmMemoryEstimateDefinition<LocalClusteringCoefficientBaseConfig> {
    @Override
    public MemoryEstimation memoryEstimation(LocalClusteringCoefficientBaseConfig configuration) {
        MemoryEstimations.Builder builder = MemoryEstimations
            .builder(LocalClusteringCoefficient.class)
            .perNode("local-clustering-coefficient", HugeDoubleArray::memoryEstimation);

        if(configuration.seedProperty() == null) {
            builder.add(
                "computed-triangle-counts",
                new IntersectingTriangleCountFactory<>().memoryEstimation(configuration.triangleCountConfig())
            );
        }

        return builder.build();
    }
}
