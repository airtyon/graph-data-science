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
package org.neo4j.gds.storageengine;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import org.eclipse.collections.api.IntIterable;
import org.neo4j.gds.core.cypher.CypherGraphStore;
import org.neo4j.gds.core.cypher.UpdatableNodeProperty;
import org.neo4j.gds.core.cypher.nodeproperties.UpdatableLongNodeProperty;
import org.neo4j.gds.core.loading.ValueConverter;
import org.neo4j.internal.helpers.collection.Iterables;
import org.neo4j.storageengine.api.StorageProperty;
import org.neo4j.storageengine.api.txstate.TxStateVisitor;
import org.neo4j.token.TokenHolders;
import org.neo4j.values.storable.LongValue;
import org.neo4j.values.storable.Value;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

public class InMemoryTransactionStateVisitor extends TxStateVisitor.Adapter {

    private final CypherGraphStore graphStore;
    private final TokenHolders tokenHolders;
    private final IntObjectMap<UpdatableNodeProperty> nodePropertiesCache;

    public InMemoryTransactionStateVisitor(
        CypherGraphStore graphStore,
        TokenHolders tokenHolders
    ) {
        this.graphStore = graphStore;
        this.tokenHolders = tokenHolders;
        this.nodePropertiesCache = new IntObjectHashMap<>();
    }

    @Override
    public void visitNodePropertyChanges(
        long nodeId, Iterable<StorageProperty> added, Iterable<StorageProperty> changed, IntIterable removed
    ) {
        visitAddedOrChangedNodeProperties(nodeId, added, changed);
        visitDeletedNodeProperties(nodeId, removed);
    }

    public void removeNodeProperty(String propertyKey) {
        var propertyToken = tokenHolders.propertyKeyTokens().getIdByName(propertyKey);
        var usedByOtherLabels = graphStore.nodeLabels().stream().anyMatch(label -> graphStore.hasNodeProperty(label, propertyKey));
        if (!usedByOtherLabels) {
            this.nodePropertiesCache.remove(propertyToken);
        }
    }

    private void visitAddedOrChangedNodeProperties(long nodeId, Iterable<StorageProperty> added, Iterable<StorageProperty> changed) {
        var addedOrChangedProperties = Iterables.concat(added, changed);
        for (StorageProperty storageProperty : addedOrChangedProperties) {
            var propertyKeyId = storageProperty.propertyKeyId();
            var propertyKey = tokenHolders.propertyKeyGetName(propertyKeyId);
            var propertyValue = storageProperty.value();

            var updatableNodeProperty = getOrCreateUpdatableNodeProperty(propertyKeyId, propertyValue);

            graphStore.nodes().forEachNodeLabel(nodeId, nodeLabel -> {
                UpdatableNodeProperty nodeProperties;
                if (graphStore.hasNodeProperty(nodeLabel, propertyKey)) {
                    var maybeNodeProperties = graphStore.nodePropertyValues(nodeLabel, propertyKey);
                    if (!(maybeNodeProperties instanceof UpdatableNodeProperty)) {
                        throw new UnsupportedOperationException(formatWithLocale("Cannot update immutable property %s", propertyKey));
                    }
                    nodeProperties = (UpdatableNodeProperty) maybeNodeProperties;
                } else {
                    nodeProperties = updatableNodeProperty;
                    graphStore.addNodeProperty(nodeLabel, propertyKey, nodeProperties);
                }

                nodeProperties.updatePropertyValue(nodeId, propertyValue);

                return true;
            });
        }
    }

    private void visitDeletedNodeProperties(long nodeId, IntIterable removed) {
        removed.forEach(propertyKeyId -> {
            if (this.nodePropertiesCache.containsKey(propertyKeyId)) {
                this.nodePropertiesCache.get(propertyKeyId).removePropertyValue(nodeId);
            }
        });
    }

    private UpdatableNodeProperty getOrCreateUpdatableNodeProperty(int propertyKeyToken, Value value) {
        if (this.nodePropertiesCache.containsKey(propertyKeyToken)) {
            return this.nodePropertiesCache.get(propertyKeyToken);
        }

        var updatableNodeProperty = updatableNodePropertyFromValue(value);
        this.nodePropertiesCache.put(propertyKeyToken, updatableNodeProperty);
        return updatableNodeProperty;
    }

    private UpdatableNodeProperty updatableNodePropertyFromValue(Value value) {
        var defaultValue = ValueConverter.valueType(value).fallbackValue();
        if (value instanceof LongValue) {
            return new UpdatableLongNodeProperty(graphStore.nodeCount(), defaultValue.longValue());
        }
        throw new IllegalArgumentException(formatWithLocale("Unsupported property type %s", value.getTypeName()));
    }
}
