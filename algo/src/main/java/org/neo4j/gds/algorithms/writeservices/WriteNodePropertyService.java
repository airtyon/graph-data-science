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
package org.neo4j.gds.algorithms.writeservices;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.config.ArrowConnectionInfo;
import org.neo4j.gds.core.utils.progress.TaskRegistryFactory;
import org.neo4j.gds.core.write.NodePropertyExporterBuilder;
import org.neo4j.gds.logging.Log;
import org.neo4j.gds.termination.TerminationFlag;

import java.util.Optional;

public class WriteNodePropertyService {

    private final Log log;
    private  final NodePropertyExporterBuilder nodePropertyExporterBuilder;
    private final TaskRegistryFactory taskRegistryFactory;
    private final TerminationFlag terminationFlag;

    public WriteNodePropertyService(
        Log log,
        NodePropertyExporterBuilder nodePropertyExporterBuilder,
        TaskRegistryFactory taskRegistryFactory,
        TerminationFlag terminationFlag
    ) {
        this.nodePropertyExporterBuilder=nodePropertyExporterBuilder;
        this.log = log;
        this.taskRegistryFactory = taskRegistryFactory;
        this.terminationFlag = terminationFlag;
    }

    public WriteNodePropertyResult write(
        Graph graph,
        GraphStore graphStore,
        NodePropertyValues nodePropertyValues,
        int writeConcurrency,
        String writeProperty,
        String procedureName,
        Optional<ArrowConnectionInfo> arrowConnectionInfo
    ) {
        return Neo4jDatabaseNodePropertyWriter.writeNodeProperty(
            nodePropertyExporterBuilder,
            taskRegistryFactory,
            graph,
            graphStore,
            nodePropertyValues,
            writeConcurrency,
            writeProperty,
            procedureName,
            arrowConnectionInfo,
            terminationFlag,
            log
        );
    }
}
