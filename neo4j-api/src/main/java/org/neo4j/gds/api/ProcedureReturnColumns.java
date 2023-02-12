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
package org.neo4j.gds.api;

import java.util.function.Function;

public interface ProcedureReturnColumns {

    boolean contains(String fieldName);

    ProcedureReturnColumns withReturnColumnNameTransformationFunction(Function<String, String> transformationFunction);

    ProcedureReturnColumns EMPTY = new ProcedureReturnColumns() {
        @Override
        public boolean contains(String fieldName) {
            return false;
        }

        @Override
        public ProcedureReturnColumns withReturnColumnNameTransformationFunction(Function<String, String> transformationFunction) {
            return this;
        }
    };
}
