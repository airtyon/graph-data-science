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
package org.neo4j.gds.projection;

import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.compat.PropertyReference;
import org.neo4j.gds.core.loading.NodeLabelTokenSet;
import org.neo4j.internal.kernel.api.NodeCursor;

public final class NodeCursorReference implements NodeReference {

    private final NodeCursor nodeCursor;

    NodeCursorReference(NodeCursor nodeCursor) {
        this.nodeCursor = nodeCursor;
    }

    @Override
    public long nodeId() {
        return nodeCursor.nodeReference();
    }

    @Override
    public NodeLabelTokenSet labels() {
        return NodeLabelTokenSet.from(nodeCursor.labels().all());
    }

    @Override
    public long relationshipReference() {
        return nodeCursor.relationshipsReference();
    }

    @Override
    public PropertyReference propertiesReference() {
        return Neo4jProxy.propertyReference(nodeCursor);
    }
}
