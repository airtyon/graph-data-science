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
package org.neo4j.gds.ml.pipeline.linkPipeline.linkfunctions;

import com.carrotsearch.hppc.predicates.LongLongPredicate;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.ml.pipeline.linkPipeline.LinkFeatureAppender;
import org.neo4j.gds.ml.pipeline.linkPipeline.LinkFeatureStep;
import org.neo4j.gds.ml.pipeline.linkPipeline.LinkFeatureStepFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SameCategoryStep implements LinkFeatureStep {


    private final List<String> nodeProperties;

    public SameCategoryStep(List<String> nodeProperties) {

        this.nodeProperties = nodeProperties;
    }

    @Override
    public List<String> inputNodeProperties() {
        return nodeProperties;
    }

    @Override
    public String name() {
        return LinkFeatureStepFactory.SAME_CATEGORY.name();
    }

    @Override
    public Map<String, Object> configuration() {
        return Map.of("nodeProperty", nodeProperties);
    }

    @Override
    public int featureDimension(Graph graph) {
        return nodeProperties.size();
    }

    @Override
    public LinkFeatureAppender linkFeatureAppender(Graph graph) {
        var isSamePredicates = new ArrayList<LongLongPredicate>();

        for (String nodeProperty : nodeProperties) {
            isSamePredicates.add(sameCategoryPredicate(graph, nodeProperty));
        }

        return (src, trg, features, offset) -> {
            int localOffset = offset;
            for (LongLongPredicate predicate : isSamePredicates) {
                features[localOffset++] = predicate.apply(src, trg) ? 1.0 : 0.0;
            }
        };
    }

    private LongLongPredicate sameCategoryPredicate(Graph graph, String nodeProperty) {
        var propertyValues = graph.nodeProperties(nodeProperty);


        switch (propertyValues.valueType()) {
            case LONG:
                return (source, target) -> propertyValues.longValue(source) == propertyValues.longValue(target);
            case DOUBLE:
                return (source, target) -> propertyValues.doubleValue(source) == propertyValues.doubleValue(target);
            case DOUBLE_ARRAY:
                return (source, target) -> {
                    var sourceArray = propertyValues.doubleArrayValue(source);
                    var targetArray = propertyValues.doubleArrayValue(target);

                    boolean isSame = false;
                    if (sourceArray.length == targetArray.length) {
                        isSame = true;
                        for (int i = 0; i < sourceArray.length; i++) {
                            if (sourceArray[i] != targetArray[i]) {
                                isSame = false;
                                break;
                            }
                        }
                    }
                    return isSame;
                };
            case FLOAT_ARRAY:
                return (source, target) -> {
                    var sourceArray = propertyValues.floatArrayValue(source);
                    var targetArray = propertyValues.floatArrayValue(target);

                    boolean isSame = false;
                    if (sourceArray.length == targetArray.length) {
                        isSame = true;
                        for (int i = 0; i < sourceArray.length; i++) {
                            if (sourceArray[i] != targetArray[i]) {
                                isSame = false;
                                break;
                            }
                        }
                    }
                    return isSame;
                };
            case LONG_ARRAY:
                return (source, target) -> {
                    var sourceArray = propertyValues.longArrayValue(source);
                    var targetArray = propertyValues.longArrayValue(target);

                    boolean isSame = false;
                    if (sourceArray.length == targetArray.length) {
                        isSame = true;
                        for (int i = 0; i < sourceArray.length; i++) {
                            if (sourceArray[i] != targetArray[i]) {
                                isSame = false;
                                break;
                            }
                        }
                    }
                    return isSame;
                };
            default:
                throw new IllegalStateException();
        }
    }
}
