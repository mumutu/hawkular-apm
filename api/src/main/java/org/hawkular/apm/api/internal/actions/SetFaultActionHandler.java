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
package org.hawkular.apm.api.internal.actions;

import java.util.Map;

import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.btxn.ProcessorAction;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;

/**
 * This handler is associated with the SetFault action.
 *
 * @author gbrown
 */
public class SetFaultActionHandler extends ExpressionBasedActionHandler {

    /**
     * This constructor initialises the action.
     *
     * @param action The action
     */
    public SetFaultActionHandler(ProcessorAction action) {
        super(action);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.client.collector.internal.actions.ProcessorActionHandler#process(
     *      org.hawkular.apm.api.model.trace.Trace, org.hawkular.apm.api.model.trace.Node,
     *      org.hawkular.apm.api.model.config.Direction, java.util.Map, java.lang.Object[])
     */
    @Override
    public boolean process(Trace trace, Node node, Direction direction, Map<String, ?> headers,
            Object[] values) {
        if (super.process(trace, node, direction, headers, values)) {
            String value = getValue(trace, node, direction, headers, values);
            if (value != null) {
                node.setFault(value);
                return true;
            }
        }
        return false;
    }

}
