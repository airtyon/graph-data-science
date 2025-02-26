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
package org.neo4j.gds.compat._513;

import org.neo4j.bolt.dbapi.BoltGraphDatabaseServiceSPI;
import org.neo4j.bolt.dbapi.BoltTransaction;
import org.neo4j.bolt.protocol.common.message.AccessMode;
import org.neo4j.bolt.protocol.common.message.request.connection.RoutingContext;
import org.neo4j.bolt.tx.statement.StatementQuerySubscriber;
import org.neo4j.exceptions.KernelException;
import org.neo4j.gds.compat.BoltQuerySubscriber;
import org.neo4j.gds.compat.BoltTransactionRunner;
import org.neo4j.graphdb.QueryStatistics;
import org.neo4j.internal.kernel.api.connectioninfo.ClientConnectionInfo;
import org.neo4j.internal.kernel.api.security.LoginContext;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.impl.query.QueryExecutionConfiguration;
import org.neo4j.kernel.impl.query.QueryExecutionKernelException;
import org.neo4j.values.virtual.MapValue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class BoltTransactionRunnerImpl extends BoltTransactionRunner<StatementQuerySubscriber, String> {

    @Override
    protected BoltQuerySubscriber<StatementQuerySubscriber> boltQuerySubscriber() {
        var subscriber = new StatementQuerySubscriber();
        return new BoltQuerySubscriber<>() {
            @Override
            public void assertSucceeded() throws KernelException {
                subscriber.assertSuccess();
            }

            @Override
            public QueryStatistics queryStatistics() {
                return subscriber.getStatistics();
            }

            @Override
            public StatementQuerySubscriber innerSubscriber() {
                return subscriber;
            }
        };
    }

    @Override
    protected void executeQuery(
        BoltTransaction boltTransaction,
        String query,
        MapValue parameters,
        StatementQuerySubscriber querySubscriber
    ) throws QueryExecutionKernelException {
        boltTransaction.executeQuery(query, parameters, true, querySubscriber);
    }

    @Override
    protected BoltTransaction beginBoltWriteTransaction(
        BoltGraphDatabaseServiceSPI fabricDb,
        LoginContext loginContext,
        KernelTransaction.Type kernelTransactionType,
        ClientConnectionInfo clientConnectionInfo,
        List<String> bookmarks,
        Duration txTimeout,
        Map<String, Object> txMetadata
    ) {
        return fabricDb.beginTransaction(
            kernelTransactionType,
            loginContext,
            clientConnectionInfo,
            bookmarks,
            txTimeout,
            AccessMode.WRITE,
            txMetadata,
            new RoutingContext(true, Map.of()),
            QueryExecutionConfiguration.DEFAULT_CONFIG
        );
    }
}
