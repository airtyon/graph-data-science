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
package org.neo4j.gds;

import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.api.PropertyState;
import org.neo4j.gds.api.properties.nodes.LongNodePropertyValues;
import org.neo4j.gds.api.properties.nodes.NodeProperty;
import org.neo4j.gds.config.ComponentSizeConfig;
import org.neo4j.gds.config.ConcurrencyConfig;
import org.neo4j.gds.config.ConsecutiveIdsConfig;
import org.neo4j.gds.config.SeedConfig;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.nodeproperties.ConsecutiveLongNodePropertyValues;
import org.neo4j.gds.nodeproperties.LongIfChangedNodePropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityProcCompanionTest {

    @Test
    void shouldReturnConsecutiveIds() {
        LongNodePropertyValues nonConsecutiveIds = new TestNodePropertyValues(10, id -> id % 3 * 10);

        var config = CommunityProcCompanionConfig.of(CypherMapWrapper.empty().withBoolean("consecutiveIds", true));

        var result = CommunityProcCompanion.nodeProperties(
            config,
            "result",
            nonConsecutiveIds,
            () -> { throw new UnsupportedOperationException("Not implemented"); }
        );

        assertThat(result).isInstanceOf(ConsecutiveLongNodePropertyValues.class);
        for (int i = 0; i < 10; i++) {
            assertThat(result.longValue(i)).isEqualTo(i % 3);
        }
    }

    @Test
    void shouldReturnInputPropertiesInSeedOnlyCase() {
        LongNodePropertyValues inputProperties = new TestNodePropertyValues(10, id -> id);

        var config = CommunityProcCompanionConfig.of(CypherMapWrapper.empty().withString("seed", "seed"));

        var result = CommunityProcCompanion.nodeProperties(
            config,
            "result",
            inputProperties,
            () -> { throw new UnsupportedOperationException("Not implemented"); }
        );

        assertThat(result).isEqualTo(inputProperties);
    }

    @Test
    void shouldReturnOnlyChangedProperties() {
        LongNodePropertyValues inputProperties = new TestNodePropertyValues(10, id -> id);
        var seedProperty = NodeProperty.of("seed", PropertyState.PERSISTENT, inputProperties);

        var config = CommunityProcCompanionConfig.of(CypherMapWrapper.empty().withString("seedProperty", "seed"));

        var result = CommunityProcCompanion.nodeProperties(
            config,
            "seed",
            inputProperties,
            () -> seedProperty
        );

        assertThat(result).isInstanceOf(LongIfChangedNodePropertyValues.class);
        for (long i = 0; i < result.size(); i++) {
            assertThat(result.longValue(i)).isEqualTo(inputProperties.longValue(i));
            assertThat(result.value(i)).isNull();
        }
    }

    @Test
    void shouldRestrictComponentSize() {
        LongNodePropertyValues inputProperties = new TestNodePropertyValues(10, id -> id < 5 ? id : 5 );

        var config = ConfigWithComponentSize.of(CypherMapWrapper.empty().withNumber("minComponentSize", 2L));

        var result = CommunityProcCompanion.nodeProperties(
            config,
            "seed",
            inputProperties,
            () -> { throw new UnsupportedOperationException("Not implemented"); }
        );

        for (long i = 0L; i < result.size(); i++) {

            if (i < 5) {
                assertThat(result.longValue(i)).isEqualTo(inputProperties.longValue(i));
                assertThat(result.value(i)).isNull();
            } else {
                assertThat(result.longValue(i)).isEqualTo(inputProperties.longValue(i));
                assertThat(result.value(i).asObject()).isEqualTo(5L);
            }
        }
    }

    private static final class TestNodePropertyValues implements LongNodePropertyValues {
        private final long size;
        private final LongToLongFunction transformer;

        private TestNodePropertyValues(
            long size,
            LongToLongFunction transformer
        ) {
            this.size = size;
            this.transformer = transformer;
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public long longValue(long nodeId) {
            return transformer.applyAsLong(nodeId);
        }
    }

    @Configuration
    interface CommunityProcCompanionConfig extends ConsecutiveIdsConfig, SeedConfig, ConcurrencyConfig {
        static CommunityProcCompanionConfig of(CypherMapWrapper map) {
            return new CommunityProcCompanionConfigImpl(map);
        }
    }

    @Configuration
    interface ConfigWithComponentSize extends ConsecutiveIdsConfig, SeedConfig, ComponentSizeConfig, ConcurrencyConfig {
        static ConfigWithComponentSize of(CypherMapWrapper map) {
            return new ConfigWithComponentSizeImpl(map);
        }
    }
}
