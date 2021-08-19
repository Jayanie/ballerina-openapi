/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */


package io.ballerina.openapi.converter.service;

import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.openapi.converter.Constants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Extract OpenApi server information from and Ballerina endpoint.
 */
public class OpenAPIEndpointMapper {

    /**
     * Convert endpoints bound to {@code service} openapi server information.
     *
     * @param openAPI   openapi definition to attach extracted information
     * @param endpoints all endpoints defined in ballerina source
     * @param service   service node with bound endpoints
     * @return openapi definition with Server information
     */
    public OpenAPI convertListenerEndPointToOpenAPI (OpenAPI openAPI, List<ListenerDeclarationNode> endpoints,
                                                     ServiceDeclarationNode service) {
        List<Server> servers = new ArrayList<>();
        for (ListenerDeclarationNode ep : endpoints) {
            SeparatedNodeList<ExpressionNode> exprNodes = service.expressions();
            for (ExpressionNode node : exprNodes) {
                if (node.toString().trim().equals(ep.variableName().text().trim())) {
                    String serviceBasePath = getServiceBasePath(service);
                    Server server = extractServer(ep, serviceBasePath);
                    servers.add(server);
                }
            }
        }
        if (openAPI == null) {
            OpenAPI openapi = new OpenAPI();
            openapi.setServers(servers);
            return openapi;
        }
        if (!openAPI.getServers().isEmpty()) {
            List<Server> oldServers = openAPI.getServers();
            List<Server> filterServer = new ArrayList<>();
            for (Server newS: servers) {
                boolean isExit = false;
                for (Server old: oldServers) {
                    if (old.getUrl().trim().equals(newS.getUrl().trim())) {
                        isExit = true;
                        break;
                    }
                }
                if (!isExit) {
                    filterServer.add(newS);
                }
            }
            oldServers.addAll(filterServer);
            openAPI.setServers(oldServers);
        } else {
            openAPI.setServers(servers);
        }
        return openAPI;
    }

    private Server extractServer(ListenerDeclarationNode ep, String serviceBasePath) {
        Optional<ParenthesizedArgList> list;
        if (ep.initializer().kind().equals(SyntaxKind.EXPLICIT_NEW_EXPRESSION)) {
           ExplicitNewExpressionNode bTypeExplicit = (ExplicitNewExpressionNode) ep.initializer();
            list = Optional.ofNullable(bTypeExplicit.parenthesizedArgList());
        } else {
            ImplicitNewExpressionNode  bTypeInit = (ImplicitNewExpressionNode) ep.initializer();
            list = bTypeInit.parenthesizedArgList();
        }

        return getServer(serviceBasePath, list);
    }

    //Function for handle both ExplicitNewExpressionNode and ImplicitNewExpressionNode in listener.
    public OpenAPI extractServerForExpressionNode(OpenAPI openAPI,
                                                                    SeparatedNodeList<ExpressionNode> bTypeExplicit,
                                                                    ServiceDeclarationNode service) {
        if (openAPI == null) {
            return new OpenAPI();
        }
        String serviceBasePath = getServiceBasePath(service);
        Optional<ParenthesizedArgList> list = null;
        List<Server> servers = new ArrayList<>();
        for (ExpressionNode expressionNode: bTypeExplicit) {
            if (expressionNode.kind().equals(SyntaxKind.EXPLICIT_NEW_EXPRESSION)) {
                ExplicitNewExpressionNode explicit = (ExplicitNewExpressionNode) expressionNode;
                list = Optional.ofNullable(explicit.parenthesizedArgList());
                Server server = getServer(serviceBasePath, list);
                servers.add(server);
            } else if (expressionNode.kind().equals(SyntaxKind.IMPLICIT_NEW_EXPRESSION)) {
                ImplicitNewExpressionNode implicit = (ImplicitNewExpressionNode) expressionNode;
                list = implicit.parenthesizedArgList();
                Server server = getServer(serviceBasePath, list);
                servers.add(server);
            }
        }
        openAPI.setServers(servers);
        return openAPI;
    }

    //Assign host and port values
    private Server getServer(String serviceBasePath, Optional<ParenthesizedArgList> list) {

        String port = null;
        String host = null;
        if (list != null && list.isPresent()) {
            SeparatedNodeList<FunctionArgumentNode> arg = (list.get()).arguments();
            port = arg.get(0).toString();
            ExpressionNode bLangRecordLiteral = null;
            if (arg.size() > 1) {
                if (arg.get(1) instanceof NamedArgumentNode) {
                    bLangRecordLiteral = ((NamedArgumentNode) arg.get(1)).expression();
                } else if (arg.get(1) instanceof PositionalArgumentNode) {
                    bLangRecordLiteral = ((PositionalArgumentNode) arg.get(1)).expression();
                }
                if (bLangRecordLiteral instanceof MappingConstructorExpressionNode) {
                    host = extractHost((MappingConstructorExpressionNode) bLangRecordLiteral);
                }
            }
        }
        // Set default values to host and port if values are not defined
        if (host == null) {
            host = Constants.ATTR_DEF_HOST;
        }
        if (port != null) {
            host += ':' + port;
        }
        if (!serviceBasePath.isBlank()) {
            host += serviceBasePath;
        }
        Server server = new Server();
        server.setUrl(host);
        return server;
    }

    // Extract host value for creating URL.
    private String extractHost(MappingConstructorExpressionNode bLangRecordLiteral) {
        String host = null;
        MappingConstructorExpressionNode recordConfig = bLangRecordLiteral;
        if (recordConfig.fields() != null && !recordConfig.fields().isEmpty()) {
            SeparatedNodeList<MappingFieldNode> recordFields = recordConfig.fields();
            for (MappingFieldNode filed: recordFields) {
                if (filed instanceof SpecificFieldNode) {
                    Node fieldName = ((SpecificFieldNode) filed).fieldName();
                    if (fieldName.toString().equals(Constants.ATTR_HOST)) {
                        if (((SpecificFieldNode) filed).valueExpr().isPresent()) {
                              host = ((SpecificFieldNode) filed).valueExpr().get().toString();
                        }
                    }
                }

            }
        }
        if (host != null) {
           host = host.replaceAll("\"", "");
        }
        return host;
    }

    /**
     * Gets the base path of a service.
     *
     * @param serviceDefinition The service definition node.
     * @return The base path.
     */
    public String getServiceBasePath(ServiceDeclarationNode serviceDefinition) {
        StringBuilder currentServiceName = new StringBuilder();
        NodeList<Node> serviceNameNodes = serviceDefinition.absoluteResourcePath();
        for (Node serviceBasedPathNode : serviceNameNodes) {
            currentServiceName.append(serviceBasedPathNode.toString());
        }
        return currentServiceName.toString().trim();
    }
}
