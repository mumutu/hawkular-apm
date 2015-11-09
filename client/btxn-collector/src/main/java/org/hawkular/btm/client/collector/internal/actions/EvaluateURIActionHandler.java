/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.client.collector.internal.actions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.EvaluateURIAction;
import org.hawkular.btm.api.model.config.btxn.ProcessorAction;
import org.hawkular.btm.client.collector.internal.NodeUtil;

/**
 * This handler is associated with the EvaluateURI action.
 *
 * @author gbrown
 */
public class EvaluateURIActionHandler extends ProcessorActionHandler {

    private static final Logger log = Logger.getLogger(EvaluateURIActionHandler.class.getName());

    private String pathTemplate;
    private List<String> queryParameters = new ArrayList<String>();

    /**
     * This constructor initialises the action.
     *
     * @param action The action
     */
    public EvaluateURIActionHandler(ProcessorAction action) {
        super(action);

        pathTemplate = ((EvaluateURIAction) getAction()).getTemplate();

        // If template contains a query string component, then separate the details
        if (pathTemplate != null && pathTemplate.indexOf('?') != -1) {
            int index = pathTemplate.indexOf('?');
            String queryString = pathTemplate.substring(index + 1);

            pathTemplate = pathTemplate.substring(0, index);

            StringTokenizer st = new StringTokenizer(queryString, "&");

            while (st.hasMoreTokens()) {
                String token = st.nextToken();

                if (token.charAt(0) == '{' && token.charAt(token.length() - 1) == '}') {
                    queryParameters.add(token.substring(1, token.length() - 1));
                } else {
                    // TODO: Needs reporting back to the configuration service (HWKBTM-230)
                    log.severe("Expecting query parameter template, e.g. {name}, but got '" + token + "'");
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.collector.internal.actions.ProcessorActionHandler#process(
     *      org.hawkular.btm.api.model.btxn.BusinessTransaction, org.hawkular.btm.api.model.btxn.Node,
     *      org.hawkular.btm.api.model.config.Direction, java.util.Map, java.lang.Object[])
     */
    @Override
    public boolean process(BusinessTransaction btxn, Node node, Direction direction, Map<String, ?> headers,
            Object[] values) {
        if (super.process(btxn, node, direction, headers, values)) {
            if (node.getUri() != null && pathTemplate != null) {
                StringTokenizer uriTokens = new StringTokenizer(node.getUri(), "/");
                StringTokenizer templateTokens =
                        new StringTokenizer(pathTemplate, "/");

                if (uriTokens.countTokens() == templateTokens.countTokens()) {
                    Map<String, String> props = null;
                    while (uriTokens.hasMoreTokens()) {
                        String uriToken = uriTokens.nextToken();
                        String templateToken = templateTokens.nextToken();

                        if (templateToken.charAt(0) == '{' && templateToken.charAt(templateToken.length() - 1) == '}') {
                            String name = templateToken.substring(1, templateToken.length() - 1);
                            if (props == null) {
                                props = new HashMap<String, String>();
                            }
                            try {
                                props.put(name, URLDecoder.decode(uriToken, "UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                                if (log.isLoggable(Level.FINEST)) {
                                    log.finest("Failed to decode value '" + uriToken + "': " + e);
                                }
                            }
                        } else if (!uriToken.equals(templateToken)) {
                            // URI template mismatch
                            return false;
                        }
                    }

                    // If properties extracted, then add to business txn properties, and set the node's
                    // URI to the template, to make it stable/consistent - to make analytics easier
                    boolean processed = false;

                    if (props != null) {
                        btxn.getProperties().putAll(props);
                        NodeUtil.rewriteURI(node, pathTemplate);
                        processed = true;
                    }

                    // If query parameter template defined, then process
                    if (!queryParameters.isEmpty()) {
                        if (processQueryParameters(btxn, node)) {
                            processed = true;
                        }
                    }

                    return processed;
                }
            }
        }

        return false;
    }

    /**
     * This method processes the query parameters associated with the supplied node to extract
     * templated named values as properties on the business transaction.
     *
     * @param btxn The business transaction
     * @param node The node
     * @return Whether query parameters were processed
     */
    protected boolean processQueryParameters(BusinessTransaction btxn, Node node) {
        boolean ret = false;

        // Translate query string into a map
        String queryString = node.getDetails().get("http_query");
        if (queryString != null) {
            StringTokenizer st = new StringTokenizer(queryString, "&");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                String[] namevalue = token.split("=");
                if (namevalue.length == 2) {
                    if (queryParameters.contains(namevalue[0])) {
                        try {
                            btxn.getProperties().put(namevalue[0], URLDecoder.decode(namevalue[1], "UTF-8"));
                            ret = true;
                        } catch (UnsupportedEncodingException e) {
                            if (log.isLoggable(Level.FINEST)) {
                                log.finest("Failed to decode value '" + namevalue[1] + "': " + e);
                            }
                        }
                    } else if (log.isLoggable(Level.FINEST)) {
                        log.finest("Ignoring query parameter '" + namevalue[0] + "'");
                    }
                } else if (log.isLoggable(Level.FINEST)) {
                    log.finest("Query string part does not include name/value pair: " + token);
                }
            }
        }

        return ret;
    }
}