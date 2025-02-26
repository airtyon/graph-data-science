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
package org.neo4j.gds.embeddings.hashgnn;

import org.neo4j.gds.AlgorithmMemoryEstimateDefinition;
import org.neo4j.gds.collections.ha.HugeObjectArray;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.mem.MemoryEstimations;
import org.neo4j.gds.core.utils.mem.MemoryRange;
import org.neo4j.gds.core.utils.paged.HugeAtomicBitSet;
import org.neo4j.gds.mem.MemoryUsage;

import java.util.Optional;
import java.util.function.LongUnaryOperator;

public class HashGNNMemoryEstimateDefinition implements AlgorithmMemoryEstimateDefinition<HashGNNConfig> {

    @Override
    public MemoryEstimation memoryEstimation(HashGNNConfig configuration) {
        int FUDGED_BINARY_DIMENSION = 1024;
        int binaryDimension = configuration
            .generateFeatures()
            .map(GenerateFeaturesConfig::dimension)
            .orElse(configuration.binarizeFeatures().map(BinarizeFeaturesConfig::dimension).orElse(FUDGED_BINARY_DIMENSION));

        MemoryEstimations.Builder builder = MemoryEstimations.builder(HashGNN.class.getSimpleName());

        builder.perNode(
            "Embeddings cache 1",
            n -> HugeObjectArray.memoryEstimation(n, HugeAtomicBitSet.memoryEstimation(binaryDimension))
        );
        builder.perNode(
            "Embeddings cache 2",
            n -> HugeObjectArray.memoryEstimation(n, HugeAtomicBitSet.memoryEstimation(binaryDimension))
        );

        builder.perGraphDimension("Hashes cache", (dims, concurrency) -> MemoryRange.of(
            configuration.embeddingDensity() * HashTask.Hashes.memoryEstimation(
                binaryDimension,
                configuration.heterogeneous() ? dims.relationshipCounts().size() : 1
            )));

        Optional<Integer> outputDimension = configuration.outputDimension();
        LongUnaryOperator denseResultEstimation = n -> HugeObjectArray.memoryEstimation(
            n,
            MemoryUsage.sizeOfDoubleArray(outputDimension.orElse(binaryDimension))
        );

        if (outputDimension.isPresent()) {
            builder.perNode("Embeddings output", denseResultEstimation);
        } else {
            // in the sparse case we store the bitset, but we convert the result to double[] before returning to the user
            builder.rangePerNode("Embeddings output", n -> MemoryRange.of(
                HugeObjectArray.memoryEstimation(n, MemoryUsage.sizeOfBitset(binaryDimension)),
                denseResultEstimation.applyAsLong(n)
            ));
        }


        return builder.build();
    }

}
