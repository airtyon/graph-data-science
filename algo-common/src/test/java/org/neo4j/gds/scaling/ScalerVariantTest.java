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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScalerVariantTest {

    @Test
    void parse() {
        assertThat(ScalarScaler.Variant.parse("log")).isEqualTo(ScalarScaler.Variant.LOG);
        assertThat(ScalarScaler.Variant.parse("minmax")).isEqualTo(ScalarScaler.Variant.MINMAX);
        assertThat(ScalarScaler.Variant.parse("max")).isEqualTo(ScalarScaler.Variant.MAX);
        assertThat(ScalarScaler.Variant.parse("center")).isEqualTo(ScalarScaler.Variant.CENTER);
        assertThat(ScalarScaler.Variant.parse("l1norm")).isEqualTo(ScalarScaler.Variant.L1NORM);
        assertThat(ScalarScaler.Variant.parse("l2norm")).isEqualTo(ScalarScaler.Variant.L2NORM);
        assertThat(ScalarScaler.Variant.parse("mean")).isEqualTo(ScalarScaler.Variant.MEAN);
        assertThat(ScalarScaler.Variant.parse("stdscore")).isEqualTo(ScalarScaler.Variant.STDSCORE);
        assertThat(ScalarScaler.Variant.parse("none")).isEqualTo(ScalarScaler.Variant.NONE);

        // case insensitive
        assertThat(ScalarScaler.Variant.parse("L1NORM")).isEqualTo(ScalarScaler.Variant.L1NORM);
        assertThat(ScalarScaler.Variant.parse("StdScore")).isEqualTo(ScalarScaler.Variant.STDSCORE);
    }

    @Test
    void badInput() {
        // bad strings
        assertThatThrownBy(() -> ScalarScaler.Variant.parse("mean  ")).hasMessageContaining("Scaler `mean  ` is not supported.");
        assertThatThrownBy(() -> ScalarScaler.Variant.parse("yo")).hasMessageContaining("Scaler `yo` is not supported.");

        // bad types
        assertThatThrownBy(() -> ScalarScaler.Variant.parse(1L)).hasMessageContaining("Unsupported scaler specified: `1`.");
        assertThatThrownBy(() -> ScalarScaler.Variant.parse(42D)).hasMessageContaining("Unsupported scaler specified: `42.0`.");
        assertThatThrownBy(() -> ScalarScaler.Variant.parse(List.of("mean"))).hasMessageContaining("Unsupported scaler specified: `[mean]`.");
        assertThatThrownBy(() -> ScalarScaler.Variant.parse(Map.of("mean", "scaler"))).hasMessageContaining("Unsupported scaler specified: `{mean=scaler}`.");
        assertThatThrownBy(() -> ScalarScaler.Variant.parse(false)).hasMessageContaining("Unsupported scaler specified: `false`.");
    }

}
