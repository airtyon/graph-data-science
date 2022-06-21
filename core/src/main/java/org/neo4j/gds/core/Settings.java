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
package org.neo4j.gds.core;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.SettingImpl;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.connectors.HttpConnector;
import org.neo4j.configuration.connectors.HttpsConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.graphdb.config.Setting;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

public final class Settings {

    public static Setting<Boolean> authEnabled() {
        return GraphDatabaseSettings.auth_enabled;
    }

    public static String defaultDatabaseName() {
        return GraphDatabaseSettings.DEFAULT_DATABASE_NAME;
    }

    public static Setting<Boolean> boltEnabled() {
        return BoltConnector.enabled;
    }

    public static Setting<SocketAddress> boltListenAddress() {
        return BoltConnector.listen_address;
    }

    public static Setting<Boolean> httpEnabled() {
        return HttpConnector.enabled;
    }

    public static Setting<Boolean> httpsEnabled() {
        return HttpsConnector.enabled;
    }

    public static Setting<ZoneId> dbTemporalTimezone() {
        return GraphDatabaseSettings.db_temporal_timezone;
    }

    public static Setting<Path> neo4jHome() {
        return GraphDatabaseSettings.neo4j_home;
    }

    @ValueClass
    public interface PageCacheMemorySetting<T> {
        Setting<T> setting();

        T value();
    }

    public static <T> Setting<T> pageCacheMemory() {
        return Neo4jProxy.pageCacheMemory();
    }

    public static <T> T pageCacheMemoryValue(String value) {
        return Neo4jProxy.pageCacheMemoryValue(value);
    }

    public static Setting<GraphDatabaseSettings.TransactionStateMemoryAllocation> transactionStateAllocation() {
        return GraphDatabaseSettings.tx_state_memory_allocation;
    }

    public static Setting<Long> transactionStateMaxOffHeapMemory() {
        return GraphDatabaseSettings.tx_state_max_off_heap_memory;
    }

    public static Setting<Path> storeInternalLogPath() {
        return GraphDatabaseSettings.store_internal_log_path;
    }

    public static <T> T disableOnlineBackup(T builder, SetConfig<T, Boolean> setConfig) {
        return tryConfigure(
            builder,
            setConfig,
            "com.neo4j.configuration.OnlineBackupSettings",
            "online_backup_enabled",
            Boolean.FALSE
        );
    }

    public static <T> T disableReplication(T builder, SetConfig<T, Boolean> setConfig) {
        return tryConfigure(
            builder,
            setConfig,
            "com.neo4j.configuration.EnterpriseEditionInternalSettings",
            "enable_replication",
            Boolean.FALSE
        );
    }

    private static <T, U> T tryConfigure(
        T builder,
        SetConfig<T, U> setConfig,
        String className,
        String settingName,
        U settingValue
    ) {
        var lookup = MethodHandles.lookup();
        try {
            var settingsClass = Class.forName(className);
            var settingHandle = lookup.findStaticGetter(settingsClass, settingName, Setting.class);
            //noinspection unchecked
            var setting = (Setting<U>) settingHandle.invoke();
            setConfig.set(builder, setting, settingValue);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            // Setting is not available on this version
        } catch (Throwable e) {
            // Actually applying the setting failed
            throw new IllegalStateException(formatWithLocale(
                "The %s setting could not be configured: %s",
                settingName,
                e.getMessage()
            ), e);
        }

        return builder;
    }

    public static Setting<List<String>> procedureUnrestricted() {
        return GraphDatabaseSettings.procedure_unrestricted;
    }

    public static Setting<Boolean> failOnMissingFiles() {
        return GraphDatabaseSettings.fail_on_missing_files;
    }

    public static Setting<String> additionalJvm() {
        return Neo4jProxy.additionalJvm();
    }

    public static Setting<Path> loadCsvFileUrlRoot() {
        return GraphDatabaseSettings.load_csv_file_url_root;
    }

    public static Setting<Long> memoryTransactionMaxSize() {
        return GraphDatabaseSettings.memory_transaction_max_size;
    }

    private Settings() {
        throw new UnsupportedOperationException();
    }

    public interface SetConfig<T, S> {
        void set(T config, Setting<S> setting, S value);
    }
}
