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
package org.neo4j.gds.embeddings.fastrp;

import org.neo4j.gds.AlgorithmMemoryEstimateDefinition;
import org.neo4j.gds.collections.ha.HugeObjectArray;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.mem.MemoryEstimations;
import org.neo4j.gds.mem.MemoryUsage;

public final class FastRPMemoryEstimateDefinition implements AlgorithmMemoryEstimateDefinition<FastRPBaseConfig> {

    @Override
    public MemoryEstimation memoryEstimation(FastRPBaseConfig configuration) {
        var sizeOfEmbeddingArray = MemoryUsage.sizeOfFloatArray(configuration.embeddingDimension());
        return MemoryEstimations
            .builder(FastRP.class.getSimpleName())
            .fixed(
                "propertyVectors",
                MemoryUsage.sizeOfFloatArray((long) configuration.featureProperties().size() * configuration.propertyDimension())
            )
            .perNode("embeddings", nodeCount -> HugeObjectArray.memoryEstimation(nodeCount, sizeOfEmbeddingArray))
            .perNode("embeddingsA", nodeCount -> HugeObjectArray.memoryEstimation(nodeCount, sizeOfEmbeddingArray))
            .perNode("embeddingsB", nodeCount -> HugeObjectArray.memoryEstimation(nodeCount, sizeOfEmbeddingArray))
            .build();
    }

}
