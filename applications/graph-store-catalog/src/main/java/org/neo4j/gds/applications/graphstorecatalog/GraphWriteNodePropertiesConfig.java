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
package org.neo4j.gds.applications.graphstorecatalog;

import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.config.WriteConfig;
import org.neo4j.gds.core.CypherMapWrapper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ValueClass
@Configuration
@SuppressWarnings("immutables:subtype")
public interface GraphWriteNodePropertiesConfig extends GraphNodePropertiesConfig, WriteConfig {

    @Configuration.Parameter
    @Configuration.ConvertWith(method = "org.neo4j.gds.applications.graphstorecatalog.GraphWriteNodePropertiesConfig#parseNodeProperties")
    List<UserInputWriteProperties.PropertySpec> nodeProperties();

    static List<UserInputWriteProperties.PropertySpec> parseNodeProperties(Object userInput) {
        return UserInputWriteProperties.parse(userInput, "nodeProperties");
    }

    /**
     * Returns the node labels that are to be considered for writing properties.
     * <p>
     * If nodeLabels contains '*`, this returns all node labels in the graph store
     * that have the specified nodeProperties.
     * <p>
     * Otherwise, it just returns all the labels in the graph store since validation
     * made sure that all node labels have the specified properties.
     */
    @Configuration.Ignore
    default Collection<NodeLabel> validNodeLabels(GraphStore graphStore) {
        List<String> nodePropertiesRequestedByUser = nodeProperties()
            .stream()
            .map(UserInputWriteProperties.PropertySpec::nodeProperty)
            .collect(Collectors.toList());

        return nodeLabelIdentifiers(graphStore)
            .stream()
            .filter(nodeLabel -> graphStore.nodePropertyKeys(nodeLabel).containsAll(nodePropertiesRequestedByUser))
            .collect(Collectors.toList());

    }

    static GraphWriteNodePropertiesConfig of(
        String graphName,
        Object nodeProperties,
        Object nodeLabels,
        CypherMapWrapper config
    ) {
        return new GraphWriteNodePropertiesConfigImpl(
            nodeProperties,
            Optional.of(graphName),
            nodeLabels,
            config
        );
    }

}
