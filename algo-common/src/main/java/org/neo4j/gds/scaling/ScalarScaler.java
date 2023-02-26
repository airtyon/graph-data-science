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
package org.neo4j.gds.scaling;

import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.core.utils.partition.Partition;

public abstract class ScalarScaler implements Scaler {

    protected final NodePropertyValues properties;

    protected ScalarScaler(NodePropertyValues properties) {this.properties = properties;}

    @Override
    public int dimension() {
        return 1;
    }

    static final ScalarScaler ZERO = new ScalarScaler(null) {
        @Override
        public double scaleProperty(long nodeId) {
            return 0;
        }
    };

    abstract static class AggregatesComputer implements Runnable {

        private final Partition partition;
        final NodePropertyValues properties;

        AggregatesComputer(Partition partition, NodePropertyValues property) {
            this.partition = partition;
            this.properties = property;
        }

        @Override
        public void run() {
            long end = partition.startNode() + partition.nodeCount();
            for (long nodeId = partition.startNode(); nodeId < end; nodeId++) {
                compute(nodeId);
            }
        }

        abstract void compute(long nodeId);
    }
}
