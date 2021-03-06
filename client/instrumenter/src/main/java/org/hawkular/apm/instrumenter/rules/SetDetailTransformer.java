/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.apm.instrumenter.rules;

import org.hawkular.apm.api.model.config.instrumentation.jvm.InstrumentAction;
import org.hawkular.apm.api.model.config.instrumentation.jvm.SetDetail;

/**
 * This class transforms the SetDetail action type.
 *
 * @author gbrown
 */
public class SetDetailTransformer implements InstrumentActionTransformer {

    /* (non-Javadoc)
     * @see org.hawkular.apm.instrumenter.config.InstrumentActionTransformer#getActionType()
     */
    @Override
    public Class<? extends InstrumentAction> getActionType() {
        return SetDetail.class;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.instrumenter.config.InstrumentActionTransformer#convertToRuleAction(
     *                      org.hawkular.apm.api.model.admin.InstrumentAction)
     */
    @Override
    public String convertToRuleAction(InstrumentAction action) {
        SetDetail setAction = (SetDetail) action;
        StringBuilder builder = new StringBuilder(64);

        builder.append("collector().");

        builder.append("setDetail(getRuleName(),\"");

        builder.append(setAction.getName());

        builder.append("\",");

        if (setAction.getValueExpression() == null) {
            builder.append("null");
        } else {
            builder.append(setAction.getValueExpression());
        }

        builder.append(",");

        if (setAction.getNodeType() == null) {
            builder.append("null");
        } else {
            builder.append("\"");
            builder.append(setAction.getNodeType());
            builder.append("\"");
        }

        builder.append(",");

        builder.append(setAction.isOnStack());

        builder.append(")");

        return builder.toString();
    }

}
