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
package org.neo4j.gds.compat._5x;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.neo4j.common.DependencyResolver;
import org.neo4j.configuration.BootloaderSettings;
import org.neo4j.configuration.Config;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.SettingValueParsers;
import org.neo4j.configuration.connectors.ConnectorPortRegister;
import org.neo4j.configuration.connectors.ConnectorType;
import org.neo4j.configuration.helpers.DatabaseNameValidator;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.gds.annotation.SuppressForbidden;
import org.neo4j.gds.compat.CompatCallableProcedure;
import org.neo4j.gds.compat.CompatExecutionMonitor;
import org.neo4j.gds.compat.CompatInput;
import org.neo4j.gds.compat.CompatUserAggregationFunction;
import org.neo4j.gds.compat.CompositeNodeCursor;
import org.neo4j.gds.compat.CustomAccessMode;
import org.neo4j.gds.compat.GdsDatabaseLayout;
import org.neo4j.gds.compat.GdsDatabaseManagementServiceBuilder;
import org.neo4j.gds.compat.GdsGraphDatabaseAPI;
import org.neo4j.gds.compat.GraphDatabaseApiProxy;
import org.neo4j.gds.compat.InputEntityIdVisitor;
import org.neo4j.gds.compat.Neo4jProxyApi;
import org.neo4j.gds.compat.PropertyReference;
import org.neo4j.gds.compat.StoreScan;
import org.neo4j.gds.compat.TestLog;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.internal.batchimport.AdditionalInitialIds;
import org.neo4j.internal.batchimport.BatchImporter;
import org.neo4j.internal.batchimport.BatchImporterFactory;
import org.neo4j.internal.batchimport.Configuration;
import org.neo4j.internal.batchimport.IndexConfig;
import org.neo4j.internal.batchimport.InputIterable;
import org.neo4j.internal.batchimport.Monitor;
import org.neo4j.internal.batchimport.input.Collector;
import org.neo4j.internal.batchimport.input.IdType;
import org.neo4j.internal.batchimport.input.Input;
import org.neo4j.internal.batchimport.input.InputEntityVisitor;
import org.neo4j.internal.batchimport.input.PropertySizeCalculator;
import org.neo4j.internal.batchimport.input.ReadableGroups;
import org.neo4j.internal.batchimport.staging.ExecutionMonitor;
import org.neo4j.internal.batchimport.staging.StageExecution;
import org.neo4j.internal.helpers.HostnamePort;
import org.neo4j.internal.id.IdGenerator;
import org.neo4j.internal.id.IdGeneratorFactory;
import org.neo4j.internal.kernel.api.NodeCursor;
import org.neo4j.internal.kernel.api.NodeLabelIndexCursor;
import org.neo4j.internal.kernel.api.PropertyCursor;
import org.neo4j.internal.kernel.api.Read;
import org.neo4j.internal.kernel.api.RelationshipScanCursor;
import org.neo4j.internal.kernel.api.connectioninfo.ClientConnectionInfo;
import org.neo4j.internal.kernel.api.procs.FieldSignature;
import org.neo4j.internal.kernel.api.procs.Neo4jTypes;
import org.neo4j.internal.kernel.api.procs.QualifiedName;
import org.neo4j.internal.kernel.api.procs.UserFunctionSignature;
import org.neo4j.internal.kernel.api.security.AccessMode;
import org.neo4j.internal.kernel.api.security.AuthSubject;
import org.neo4j.internal.kernel.api.security.SecurityContext;
import org.neo4j.internal.recordstorage.RecordIdType;
import org.neo4j.internal.schema.IndexCapability;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.io.layout.Neo4jLayout;
import org.neo4j.io.layout.recordstorage.RecordDatabaseLayout;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.io.pagecache.context.CursorContext;
import org.neo4j.io.pagecache.context.CursorContextFactory;
import org.neo4j.io.pagecache.tracing.PageCacheTracer;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.KernelTransactionHandle;
import org.neo4j.kernel.api.procedure.CallableProcedure;
import org.neo4j.kernel.api.procedure.CallableUserAggregationFunction;
import org.neo4j.kernel.database.NormalizedDatabaseName;
import org.neo4j.kernel.impl.coreapi.InternalTransaction;
import org.neo4j.kernel.impl.index.schema.IndexImporterFactoryImpl;
import org.neo4j.kernel.impl.query.QueryExecutionConfiguration;
import org.neo4j.kernel.impl.query.TransactionalContext;
import org.neo4j.kernel.impl.query.TransactionalContextFactory;
import org.neo4j.kernel.impl.store.format.RecordFormatSelector;
import org.neo4j.kernel.impl.store.format.RecordFormats;
import org.neo4j.kernel.impl.transaction.log.EmptyLogTailMetadata;
import org.neo4j.kernel.impl.transaction.log.files.TransactionLogInitializer;
import org.neo4j.logging.Log;
import org.neo4j.logging.internal.LogService;
import org.neo4j.memory.EmptyMemoryTracker;
import org.neo4j.scheduler.JobScheduler;
import org.neo4j.ssl.config.SslPolicyLoader;
import org.neo4j.storageengine.api.PropertySelection;
import org.neo4j.storageengine.api.StorageEngineFactory;
import org.neo4j.values.storable.TextArray;
import org.neo4j.values.storable.ValueCategory;
import org.neo4j.values.virtual.MapValue;
import org.neo4j.values.virtual.NodeValue;
import org.neo4j.values.virtual.VirtualValues;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.neo4j.gds.compat.InternalReadOps.countByIdGenerator;

public abstract class CommonNeo4jProxyImpl implements Neo4jProxyApi {

    @Override
    public GdsGraphDatabaseAPI newDb(DatabaseManagementService dbms) {
        return new CompatGraphDatabaseAPIImpl(dbms);
    }

    @Override
    public String validateExternalDatabaseName(String databaseName) {
        var normalizedName = new NormalizedDatabaseName(databaseName);
        DatabaseNameValidator.validateExternalDatabaseName(normalizedName);
        return normalizedName.name();
    }

    @Override
    public AccessMode accessMode(CustomAccessMode customAccessMode) {
        return new CompatAccessModeImpl(customAccessMode);
    }

    @Override
    public String username(AuthSubject subject) {
        return subject.executingUser();
    }

    @Override
    public SecurityContext securityContext(
        String username,
        AuthSubject authSubject,
        AccessMode mode,
        String databaseName
    ) {
        return new SecurityContext(
            new CompatUsernameAuthSubjectImpl(username, authSubject),
            mode,
            // GDS is always operating from an embedded context
            ClientConnectionInfo.EMBEDDED_CONNECTION,
            databaseName
        );
    }

    @Override
    public List<StoreScan<NodeLabelIndexCursor>> entityCursorScan(
        KernelTransaction transaction,
        int[] labelIds,
        int batchSize,
        boolean allowPartitionedScan
    ) {
        return PartitionedStoreScan.createScans(transaction, batchSize, labelIds);
    }

    @Override
    public List<StoreScan<NodeLabelIndexCursor>> partitionedCursorScan(
        KernelTransaction transaction,
        int batchSize,
        int... labelIds
    ) {
        return PartitionedStoreScan.createScans(transaction, batchSize, labelIds);
    }

    @Override
    public PropertyCursor allocatePropertyCursor(KernelTransaction kernelTransaction) {
        return kernelTransaction
            .cursors()
            .allocatePropertyCursor(kernelTransaction.cursorContext(), kernelTransaction.memoryTracker());
    }

    @Override
    public PropertyReference propertyReference(NodeCursor nodeCursor) {
        return ReferencePropertyReference.of(nodeCursor.propertiesReference());
    }

    @Override
    public PropertyReference propertyReference(RelationshipScanCursor relationshipScanCursor) {
        return ReferencePropertyReference.of(relationshipScanCursor.propertiesReference());
    }

    @Override
    public PropertyReference noPropertyReference() {
        return ReferencePropertyReference.empty();
    }

    @Override
    public void nodeProperties(
        KernelTransaction kernelTransaction,
        long nodeReference,
        PropertyReference reference,
        PropertyCursor cursor
    ) {
        var neoReference = ((ReferencePropertyReference) reference).reference;
        kernelTransaction
            .dataRead()
            .nodeProperties(nodeReference, neoReference, PropertySelection.ALL_PROPERTIES, cursor);
    }

    @Override
    public void relationshipProperties(
        KernelTransaction kernelTransaction,
        long relationshipReference,
        PropertyReference reference,
        PropertyCursor cursor
    ) {
        var neoReference = ((ReferencePropertyReference) reference).reference;
        kernelTransaction
            .dataRead()
            .relationshipProperties(relationshipReference, neoReference, PropertySelection.ALL_PROPERTIES, cursor);
    }

    @Override
    public NodeCursor allocateNodeCursor(KernelTransaction kernelTransaction) {
        return kernelTransaction.cursors().allocateNodeCursor(kernelTransaction.cursorContext());
    }

    @Override
    public RelationshipScanCursor allocateRelationshipScanCursor(KernelTransaction kernelTransaction) {
        return kernelTransaction.cursors().allocateRelationshipScanCursor(kernelTransaction.cursorContext());
    }

    @Override
    public NodeLabelIndexCursor allocateNodeLabelIndexCursor(KernelTransaction kernelTransaction) {
        return kernelTransaction.cursors().allocateNodeLabelIndexCursor(kernelTransaction.cursorContext());
    }

    @Override
    public boolean hasNodeLabelIndex(KernelTransaction kernelTransaction) {
        return NodeLabelIndexLookupImpl.hasNodeLabelIndex(kernelTransaction);
    }

    @Override
    public StoreScan<NodeLabelIndexCursor> nodeLabelIndexScan(
        KernelTransaction transaction,
        int labelId,
        int batchSize,
        boolean allowPartitionedScan
    ) {
        return PartitionedStoreScan.createScans(transaction, batchSize, labelId).get(0);
    }

    @Override
    public StoreScan<NodeCursor> nodesScan(KernelTransaction ktx, long nodeCount, int batchSize) {
        int numberOfPartitions = PartitionedStoreScan.getNumberOfPartitions(nodeCount, batchSize);
        return new PartitionedStoreScan<>(ktx.dataRead().allNodesScan(numberOfPartitions, ktx.cursorContext()));
    }

    @Override
    public StoreScan<RelationshipScanCursor> relationshipsScan(
        KernelTransaction ktx,
        long relationshipCount,
        int batchSize
    ) {
        int numberOfPartitions = PartitionedStoreScan.getNumberOfPartitions(relationshipCount, batchSize);
        return new PartitionedStoreScan<>(ktx.dataRead().allRelationshipsScan(numberOfPartitions, ktx.cursorContext()));
    }

    @Override
    public CompositeNodeCursor compositeNodeCursor(List<NodeLabelIndexCursor> cursors, int[] labelIds) {
        return new CompositeNodeCursorImpl(cursors, labelIds);
    }

    @Override
    public Configuration batchImporterConfig(
        int batchSize,
        int writeConcurrency,
        Optional<Long> pageCacheMemory,
        boolean highIO,
        IndexConfig indexConfig
    ) {
        return new org.neo4j.internal.batchimport.Configuration() {
            @Override
            public int batchSize() {
                return batchSize;
            }

            @Override
            public int maxNumberOfWorkerThreads() {
                return writeConcurrency;
            }

            @Override
            public boolean highIO() {
                return highIO;
            }

            @Override
            public IndexConfig indexConfig() {
                return indexConfig;
            }
        };
    }

    @Override
    public int writeConcurrency(Configuration batchImportConfiguration) {
        return batchImportConfiguration.maxNumberOfWorkerThreads();
    }

    @Override
    public BatchImporter instantiateBatchImporter(
        BatchImporterFactory factory,
        GdsDatabaseLayout directoryStructure,
        FileSystemAbstraction fileSystem,
        PageCacheTracer pageCacheTracer,
        Configuration configuration,
        LogService logService,
        ExecutionMonitor executionMonitor,
        AdditionalInitialIds additionalInitialIds,
        Config dbConfig,
        RecordFormats recordFormats,
        JobScheduler jobScheduler,
        Collector badCollector
    ) {
        dbConfig.set(GraphDatabaseSettings.db_format, recordFormats.name());
        var databaseLayout = ((GdsDatabaseLayoutImpl) directoryStructure).databaseLayout();
        return factory.instantiate(
            databaseLayout,
            fileSystem,
            pageCacheTracer,
            configuration,
            logService,
            executionMonitor,
            additionalInitialIds,
            new EmptyLogTailMetadata(dbConfig),
            dbConfig,
            Monitor.NO_MONITOR,
            jobScheduler,
            badCollector,
            TransactionLogInitializer.getLogFilesInitializer(),
            new IndexImporterFactoryImpl(),
            EmptyMemoryTracker.INSTANCE,
            cursorContextFactory(Optional.empty())
        );
    }

    @Override
    public Input batchInputFrom(CompatInput compatInput) {
        return new InputFromCompatInput(compatInput);
    }

    @Override
    public InputEntityIdVisitor.Long inputEntityLongIdVisitor(IdType idType, ReadableGroups groups) {
        switch (idType) {
            case ACTUAL -> {
                return new InputEntityIdVisitor.Long() {
                    @Override
                    public void visitNodeId(InputEntityVisitor visitor, long id) {
                        visitor.id(id);
                    }

                    @Override
                    public void visitSourceId(InputEntityVisitor visitor, long id) {
                        visitor.startId(id);
                    }

                    @Override
                    public void visitTargetId(InputEntityVisitor visitor, long id) {
                        visitor.endId(id);
                    }
                };
            }
            case INTEGER -> {
                var globalGroup = groups.get(null);

                return new InputEntityIdVisitor.Long() {
                    @Override
                    public void visitNodeId(InputEntityVisitor visitor, long id) {
                        visitor.id(id, globalGroup);
                    }

                    @Override
                    public void visitSourceId(InputEntityVisitor visitor, long id) {
                        visitor.startId(id, globalGroup);
                    }

                    @Override
                    public void visitTargetId(InputEntityVisitor visitor, long id) {
                        visitor.endId(id, globalGroup);
                    }
                };
            }
            default -> throw new IllegalStateException("Unexpected value: " + idType);
        }
    }

    @Override
    public InputEntityIdVisitor.String inputEntityStringIdVisitor(ReadableGroups groups) {
        var globalGroup = groups.get(null);

        return new InputEntityIdVisitor.String() {
            @Override
            public void visitNodeId(InputEntityVisitor visitor, String id) {
                visitor.id(id, globalGroup);
            }

            @Override
            public void visitSourceId(InputEntityVisitor visitor, String id) {
                visitor.startId(id, globalGroup);
            }

            @Override
            public void visitTargetId(InputEntityVisitor visitor, String id) {
                visitor.endId(id, globalGroup);
            }
        };
    }

    @Override
    public Setting<String> additionalJvm() {
        return BootloaderSettings.additional_jvm;
    }

    @Override
    public Setting<Long> pageCacheMemory() {
        return GraphDatabaseSettings.pagecache_memory;
    }

    @Override
    public Long pageCacheMemoryValue(String value) {
        return SettingValueParsers.BYTES.parse(value);
    }

    @Override
    public long getHighestPossibleNodeCount(
        Read read, IdGeneratorFactory idGeneratorFactory
    ) {
        return countByIdGenerator(idGeneratorFactory, RecordIdType.NODE).orElseGet(read::nodesGetCount);
    }

    @Override
    public long getHighestPossibleRelationshipCount(
        Read read, IdGeneratorFactory idGeneratorFactory
    ) {
        return countByIdGenerator(idGeneratorFactory, RecordIdType.RELATIONSHIP).orElseGet(read::relationshipsGetCount);
    }

    @Override
    public String versionLongToString(long storeVersion) {
        // copied from org.neo4j.kernel.impl.store.LegacyMetadataHandler.versionLongToString which is private
        if (storeVersion == -1) {
            return "Unknown";
        }
        var bits = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(storeVersion).rewind();
        int length = bits.get() & 0xFF;
        if (length == 0 || length > 7) {
            throw new IllegalArgumentException(format(
                Locale.ENGLISH,
                "The read version string length %d is not proper.",
                length
            ));
        }
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            result[i] = (char) (bits.get() & 0xFF);
        }
        return new String(result);
    }

    private static final class InputFromCompatInput implements Input {
        private final CompatInput delegate;

        private InputFromCompatInput(CompatInput delegate) {
            this.delegate = delegate;
        }

        @Override
        public InputIterable nodes(Collector badCollector) {
            return delegate.nodes(badCollector);
        }

        @Override
        public InputIterable relationships(Collector badCollector) {
            return delegate.relationships(badCollector);
        }

        @Override
        public IdType idType() {
            return delegate.idType();
        }

        @Override
        public ReadableGroups groups() {
            return delegate.groups();
        }

        @Override
        public Estimates calculateEstimates(PropertySizeCalculator propertySizeCalculator) throws IOException {
            return delegate.calculateEstimates((values, kernelTransaction) -> propertySizeCalculator.calculateSize(
                values,
                kernelTransaction.cursorContext(),
                kernelTransaction.memoryTracker()
            ));
        }
    }

    @Override
    public TestLog testLog() {
        return new TestLogImpl();
    }

    @Override
    @SuppressForbidden(reason = "This is the compat specific use")
    public Log getUserLog(LogService logService, Class<?> loggingClass) {
        return logService.getUserLog(loggingClass);
    }

    @Override
    @SuppressForbidden(reason = "This is the compat specific use")
    public Log getInternalLog(LogService logService, Class<?> loggingClass) {
        return logService.getInternalLog(loggingClass);
    }

    @Override
    public NodeValue nodeValue(long id, TextArray labels, MapValue properties) {
        return VirtualValues.nodeValue(id, String.valueOf(id), labels, properties);
    }

    @Override
    public Relationship virtualRelationship(long id, Node startNode, Node endNode, RelationshipType type) {
        return new VirtualRelationshipImpl(id, startNode, endNode, type);
    }

    @Override
    public GdsDatabaseManagementServiceBuilder databaseManagementServiceBuilder(Path storeDir) {
        return new GdsDatabaseManagementServiceBuilderImpl(storeDir);
    }

    @Override
    @SuppressForbidden(reason = "This is the compat specific use")
    public RecordFormats selectRecordFormatForStore(
        DatabaseLayout databaseLayout,
        FileSystemAbstraction fs,
        PageCache pageCache,
        LogService logService,
        PageCacheTracer pageCacheTracer
    ) {
        return RecordFormatSelector.selectForStore(
            (RecordDatabaseLayout) databaseLayout,
            fs,
            pageCache,
            logService.getInternalLogProvider(),
            cursorContextFactory(Optional.ofNullable(pageCacheTracer))
        );
    }


    @Override
    public boolean isNotNumericIndex(IndexCapability indexCapability) {
        return !indexCapability.areValueCategoriesAccepted(ValueCategory.NUMBER);
    }

    @Override
    public void setAllowUpgrades(Config.Builder configBuilder, boolean value) {
    }

    @Override
    public String defaultRecordFormatSetting() {
        return GraphDatabaseSettings.db_format.defaultValue();
    }

    @Override
    public void configureRecordFormat(Config.Builder configBuilder, String recordFormat) {
        var databaseRecordFormat = recordFormat.toLowerCase(Locale.ENGLISH);
        configBuilder.set(GraphDatabaseSettings.db_format, databaseRecordFormat);
    }

    @Override
    public GdsDatabaseLayout databaseLayout(Config config, String databaseName) {
        var storageEngineFactory = StorageEngineFactory.selectStorageEngine(config);
        var dbLayout = neo4jLayout(config).databaseLayout(databaseName);
        var databaseLayout = storageEngineFactory.formatSpecificDatabaseLayout(dbLayout);
        return new GdsDatabaseLayoutImpl(databaseLayout);
    }

    @Override
    @SuppressForbidden(reason = "This is the compat specific use")
    public Neo4jLayout neo4jLayout(Config config) {
        return Neo4jLayout.of(config);
    }

    @Override
    public HostnamePort getLocalBoltAddress(ConnectorPortRegister connectorPortRegister) {
        return connectorPortRegister.getLocalAddress(ConnectorType.BOLT);
    }

    @Override
    @SuppressForbidden(reason = "This is the compat specific use")
    public SslPolicyLoader createSllPolicyLoader(
        FileSystemAbstraction fileSystem,
        Config config,
        LogService logService
    ) {
        return SslPolicyLoader.create(fileSystem, config, logService.getInternalLogProvider());
    }

    @Override
    @SuppressForbidden(reason = "This is the compat specific use")
    public RecordFormats recordFormatSelector(
        String databaseName,
        Config databaseConfig,
        FileSystemAbstraction fs,
        LogService logService,
        GraphDatabaseService databaseService
    ) {
        var neo4jLayout = Neo4jLayout.of(databaseConfig);
        var recordDatabaseLayout = RecordDatabaseLayout.of(neo4jLayout, databaseName);
        return RecordFormatSelector.selectForStoreOrConfigForNewDbs(
            databaseConfig,
            recordDatabaseLayout,
            fs,
            GraphDatabaseApiProxy.resolveDependency(databaseService, PageCache.class),
            logService.getInternalLogProvider(),
            GraphDatabaseApiProxy.resolveDependency(databaseService, CursorContextFactory.class)
        );
    }

    @Override
    public ExecutionMonitor executionMonitor(CompatExecutionMonitor compatExecutionMonitor) {
        return new ExecutionMonitor.Adapter(
            compatExecutionMonitor.checkIntervalMillis(),
            TimeUnit.MILLISECONDS
        ) {

            @Override
            public void initialize(DependencyResolver dependencyResolver) {
                compatExecutionMonitor.initialize(dependencyResolver);
            }

            @Override
            public void start(StageExecution execution) {
                compatExecutionMonitor.start(execution);
            }

            @Override
            public void end(StageExecution execution, long totalTimeMillis) {
                compatExecutionMonitor.end(execution, totalTimeMillis);
            }

            @Override
            public void done(boolean successful, long totalTimeMillis, String additionalInformation) {
                compatExecutionMonitor.done(successful, totalTimeMillis, additionalInformation);
            }

            @Override
            public void check(StageExecution execution) {
                compatExecutionMonitor.check(execution);
            }
        };
    }

    @Override
    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE") // We assign nulls because it makes the code more readable
    public UserFunctionSignature userFunctionSignature(
        QualifiedName name,
        List<FieldSignature> inputSignature,
        Neo4jTypes.AnyType type,
        String description,
        boolean internal,
        boolean threadSafe,
        Optional<String> deprecatedBy
    ) {
        String category = null;      // No predefined categpry (like temporal or math)
        var caseInsensitive = false; // case sensitive name match
        var isBuiltIn = false;       // is built in; never true for GDS

        return new UserFunctionSignature(
            name,
            inputSignature,
            type,
            deprecatedBy.orElse(null),
            description,
            category,
            caseInsensitive,
            isBuiltIn,
            internal,
            threadSafe
        );
    }

    @Override
    @SuppressForbidden(reason = "This is the compat API")
    public CallableProcedure callableProcedure(CompatCallableProcedure procedure) {
        return new CallableProcedureImpl(procedure);
    }

    @Override
    @SuppressForbidden(reason = "This is the compat API")
    public CallableUserAggregationFunction callableUserAggregationFunction(CompatUserAggregationFunction function) {
        return new CallableUserAggregationFunctionImpl(function);
    }

    @Override
    public long transactionId(KernelTransactionHandle kernelTransactionHandle) {
        return kernelTransactionHandle.getTransactionSequenceNumber();
    }

    @Override
    public long transactionId(KernelTransaction kernelTransaction) {
        return kernelTransaction.getTransactionSequenceNumber();
    }

    @Override
    public void reserveNeo4jIds(IdGeneratorFactory generatorFactory, int size, CursorContext cursorContext) {
        IdGenerator idGenerator = generatorFactory.get(RecordIdType.NODE);

        idGenerator.nextConsecutiveIdRange(size, false, cursorContext);
    }

    @Override
    public TransactionalContext newQueryContext(
        TransactionalContextFactory contextFactory,
        InternalTransaction tx,
        String queryText,
        MapValue queryParameters
    ) {
        return contextFactory.newContext(tx, queryText, queryParameters, QueryExecutionConfiguration.DEFAULT_CONFIG);
    }

    public abstract CursorContextFactory cursorContextFactory(Optional<PageCacheTracer> pageCacheTracer);

    @Override
    public String metricsManagerClass() {
        return "com.neo4j.metrics.global.MetricsManager";
    }
}
