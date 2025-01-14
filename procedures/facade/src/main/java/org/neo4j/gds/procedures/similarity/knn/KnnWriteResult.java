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
package org.neo4j.gds.procedures.similarity.knn;

import org.neo4j.gds.procedures.similarity.SimilarityWriteResult;

import java.util.Map;

@SuppressWarnings("unused")
public class KnnWriteResult extends SimilarityWriteResult {
    public final long ranIterations;
    public final boolean didConverge;
    public final long nodePairsConsidered;

    public KnnWriteResult(
        long preProcessingMillis,
        long computeMillis,
        long writeMillis,
        long postProcessingMillis,
        long nodesCompared,
        long relationshipsWritten,
        boolean didConverge,
        long ranIterations,
        long nodePairsCompared,
        Map<String, Object> similarityDistribution,
        Map<String, Object> configuration
    ) {
        super(
            preProcessingMillis,
            computeMillis,
            writeMillis,
            postProcessingMillis,
            nodesCompared,
            relationshipsWritten,
            similarityDistribution,
            configuration
        );

        this.nodePairsConsidered = nodePairsCompared;
        this.ranIterations = ranIterations;
        this.didConverge = didConverge;
    }

}
