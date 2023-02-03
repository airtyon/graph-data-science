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
package org.neo4j.gds.modularity;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableLong;
import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.concurrency.RunWithConcurrency;
import org.neo4j.gds.core.utils.paged.HugeAtomicBitSet;
import org.neo4j.gds.core.utils.paged.HugeAtomicDoubleArray;
import org.neo4j.gds.core.utils.paged.HugeObjectArray;
import org.neo4j.gds.core.utils.partition.PartitionUtils;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;

import java.util.Optional;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.LongUnaryOperator;

public class ModularityCalculator extends Algorithm<ModularityResult> {

    private final Graph graph;
    private final LongUnaryOperator communityIdProvider;
    private final int concurrency;


    public ModularityCalculator(
        Graph graph,
        LongUnaryOperator communityIdProvider,
        int concurrency
    ) {
        super(ProgressTracker.NULL_TRACKER);
        this.graph = graph;
        this.communityIdProvider = communityIdProvider;
        this.concurrency = concurrency;
    }

    @Override
    public ModularityResult compute() {
        var nodeCount = graph.nodeCount();

        var insideRelationships = HugeAtomicDoubleArray.newArray(nodeCount);
        var totalCommunityRelationships = HugeAtomicDoubleArray.newArray(nodeCount);
        var communityTracker = HugeAtomicBitSet.create(nodeCount);
        var totalRelationshipWeight = new DoubleAdder();

        var tasks = PartitionUtils.rangePartition(
            concurrency,
            nodeCount,
            partition -> new RelationshipCountCollector(
                partition,
                graph,
                insideRelationships,
                totalCommunityRelationships,
                communityTracker,
                communityIdProvider,
                totalRelationshipWeight
            ), Optional.empty()
        );

        RunWithConcurrency.builder()
            .concurrency(concurrency)
            .tasks(tasks)
            .run();

        var communityCount = communityTracker.cardinality();
        var communityModularities = HugeObjectArray.newArray(
            CommunityModularity.class,
            communityCount
        );
        var totalRelWeight = totalRelationshipWeight.doubleValue();
        var resultTracker = new MutableLong();
        var totalModularity = new MutableDouble();
        communityTracker.forEachSetBit(communityId -> {
            var ec = insideRelationships.get(communityId);
            var Kc = totalCommunityRelationships.get(communityId);
            var modularity = (ec - Kc * Kc * (1.0 / totalRelWeight)) / totalRelWeight;
            totalModularity.add(modularity);
            communityModularities.set(resultTracker.getAndIncrement(), CommunityModularity.of(communityId, modularity));
        });

        return ModularityResult.of(totalModularity.doubleValue(), communityCount, communityModularities);
    }

}
