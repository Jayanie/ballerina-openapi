/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.openapi.generators.schema;

import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MarkdownParameterDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeReferenceNode;
import io.ballerina.openapi.exception.BallerinaOpenApiException;
import io.ballerina.openapi.generators.GeneratorUtils;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.openapi.generators.GeneratorUtils.convertOpenAPITypeToBallerina;
import static io.ballerina.openapi.generators.GeneratorUtils.escapeIdentifier;
import static io.ballerina.openapi.generators.GeneratorUtils.extractReferenceType;
import static io.ballerina.openapi.generators.GeneratorUtils.getValidName;
import static io.ballerina.openapi.generators.GeneratorUtils.isValidSchemaName;

/**
 *This class wraps the {@link Schema} from openapi models inorder to overcome complications
 *while populating syntax tree.
 */
public class BallerinaSchemaGenerator {
    private List<TypeDefinitionNode> typeDefinitionNodeList;
    private boolean nullable;
    private  OpenAPI openAPI;
    private GeneratorUtils generatorUtils = new GeneratorUtils();

    public BallerinaSchemaGenerator(OpenAPI openAPI, boolean nullable) {
        this.openAPI = openAPI;
        this.nullable = nullable;
        this.typeDefinitionNodeList = new LinkedList<>();
    }

    public BallerinaSchemaGenerator(OpenAPI openAPI) {
        this.openAPI = openAPI;
        this.nullable = false;
        this.typeDefinitionNodeList = new LinkedList<>();
    }

    /**
     * Returns a list of type definition nodes.
     */
    public List<TypeDefinitionNode> getTypeDefinitionNodeList() {
        return typeDefinitionNodeList;
    }
    /**
     * Set the typeDefinitionNodeList.
     */
    public void setTypeDefinitionNodeList(List<TypeDefinitionNode> typeDefinitionNodeList) {
        this.typeDefinitionNodeList = typeDefinitionNodeList;
    }

    public SyntaxTree generateSyntaxTree() throws BallerinaOpenApiException {

        if (openAPI.getComponents() != null) {
            // Refactor schema name with valid name
            // Create typeDefinitionNode
            Components components = openAPI.getComponents();
            Map<String, Schema> componentsSchemas = components.getSchemas();
            if (componentsSchemas != null) {
                Map<String, Schema> refacSchema = new HashMap<>();
                for (Map.Entry<String, Schema> schemaEntry: componentsSchemas.entrySet()) {
                    String name = getValidName(schemaEntry.getKey(), true);
                    refacSchema.put(name, schemaEntry.getValue());
                }
                openAPI.getComponents().setSchemas(refacSchema);
            }
            Map<String, Schema> schemas = components.getSchemas();
            if (schemas != null) {
                // TODO Reset Component Name with valid Name
                for (Map.Entry<String, Schema> schema: schemas.entrySet()) {
                    List<Node> schemaDoc = new ArrayList<>();
                    if (schema.getValue().getDescription() != null) {
                        MarkdownDocumentationLineNode clientDescription =
                                createMarkdownDocumentationLineNode(null, createToken(SyntaxKind.HASH_TOKEN),
                                        createNodeList(createLiteralValueToken(null,
                                                schema.getValue().getDescription(), createEmptyMinutiaeList(),
                                                createEmptyMinutiaeList())));
                        schemaDoc.add(clientDescription);
                        MarkdownDocumentationLineNode newLine = createMarkdownDocumentationLineNode(null,
                                createToken(SyntaxKind.HASH_TOKEN), createEmptyNodeList());
                        schemaDoc.add(newLine);
                    }

                    List<String> required = schema.getValue().getRequired();
                    String recordName = getValidName(schema.getKey().trim(), true);
                    if (isValidSchemaName(recordName)) {
                        getTypeDefinitionNode(openAPI, typeDefinitionNodeList, schema, schemaDoc, required, recordName);
                    }
                }
            }
        }
        //Create imports
        NodeList<ImportDeclarationNode> imports = AbstractNodeFactory.createEmptyNodeList();
        // Create module member declaration
        NodeList<ModuleMemberDeclarationNode> moduleMembers =
                AbstractNodeFactory.createNodeList(typeDefinitionNodeList.toArray(
                        new TypeDefinitionNode[typeDefinitionNodeList.size()]));

        Token eofToken = AbstractNodeFactory.createIdentifierToken("");
        ModulePartNode modulePartNode = NodeFactory.createModulePartNode(imports, moduleMembers, eofToken);

        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        return syntaxTree.modifyWith(modulePartNode);
    }

    private TypeDefinitionNode getTypeDefinitionNode(OpenAPI openAPI,
                                                          List<TypeDefinitionNode> typeDefinitionNodeList,
                                       Map.Entry<String, Schema> schema, List<Node> schemaDoc, List<String> required,
                                       String recordName) throws BallerinaOpenApiException {

        IdentifierToken typeName = AbstractNodeFactory.createIdentifierToken(recordName);
        Token typeKeyWord = AbstractNodeFactory.createIdentifierToken("public type");
        //Generate RecordFiled
        List<Node> recordFieldList = new ArrayList<>();
        Schema schemaValue = schema.getValue();
        TypeDefinitionNode typeDefNode = null;
        if (schemaValue instanceof ComposedSchema) {
            ComposedSchema composedSchema = (ComposedSchema) schemaValue;
            if (composedSchema.getAllOf() != null) {
                List<Schema> allOf = composedSchema.getAllOf();
                typeDefNode = getAllOfTypeDefinitionNode(openAPI, schemaDoc, required, typeName,
                                recordFieldList, allOf);
                typeDefinitionNodeList.add(typeDefNode);
            } else if (composedSchema.getOneOf() != null) {
                List<Schema> oneOf = composedSchema.getOneOf();
                String unionTypeCont = generatorUtils.getOneOfUnionType(oneOf);
                String type  = escapeIdentifier(schema.getKey().trim());
                MarkdownDocumentationNode documentationNode =
                        createMarkdownDocumentationNode(createNodeList(schemaDoc));
                MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
                typeDefNode = createTypeDefinitionNode(metadataNode, null,
                        createIdentifierToken("public type "),
                        createIdentifierToken(type),
                        createSimpleNameReferenceNode(createIdentifierToken(unionTypeCont)),
                        createToken(SEMICOLON_TOKEN));
                typeDefinitionNodeList.add(typeDefNode);
            } else if (composedSchema.getAnyOf() != null) {
                List<Schema> anyOf = composedSchema.getAnyOf();
                String unionTypeCont = generatorUtils.getOneOfUnionType(anyOf);
                String type  = escapeIdentifier(schema.getKey().trim());
                MarkdownDocumentationNode documentationNode =
                        createMarkdownDocumentationNode(createNodeList(schemaDoc));
                MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
                typeDefNode = createTypeDefinitionNode(metadataNode, null,
                        createIdentifierToken("public type "),
                        createIdentifierToken(type),
                        createSimpleNameReferenceNode(createIdentifierToken(unionTypeCont)),
                        createToken(SEMICOLON_TOKEN));
                typeDefinitionNodeList.add(typeDefNode);
            }
        } else if (schema.getValue().getProperties() != null || (schema.getValue() instanceof ObjectSchema)) {
            Map<String, Schema> fields = schema.getValue().getProperties();
            String description;
            if (schema.getValue().getDescription() != null) {
                description = schema.getValue().getDescription().split("\n")[0];
            } else {
                description = "";
            }
            typeDefNode = getTypeDefinitionNodeForObjectSchema(required,
                    typeKeyWord, typeName, recordFieldList, fields, description, openAPI);
            typeDefinitionNodeList.add(typeDefNode);
        } else if (schema.getValue().getType() != null) {
            if (schema.getValue().getType().equals("array")) {
                if (schemaValue instanceof ArraySchema) {
                    ArraySchema arraySchema = (ArraySchema) schemaValue;
                    TypeDescriptorNode fieldTypeName;
                    if (arraySchema.getItems() != null) {
                        fieldTypeName = extractOpenAPISchema(arraySchema.getItems(), openAPI);
                    } else {
                        Token type =
                                AbstractNodeFactory.createIdentifierToken("string ");
                        fieldTypeName = NodeFactory.createBuiltinSimpleNameReferenceNode(null, type);
                    }
                    if (schemaValue.getDescription() != null) {
                        MarkdownParameterDocumentationLineNode paramAPIDoc = generatorUtils.createParamAPIDoc(
                                fieldTypeName.toString(), schemaValue.getDescription().split("\n")[0]);
                        schemaDoc.add(paramAPIDoc);
                    }
                    MarkdownDocumentationNode documentationNode =
                            createMarkdownDocumentationNode(createNodeList(schemaDoc));
                    MetadataNode metadataNode =
                            createMetadataNode(documentationNode, createEmptyNodeList());
                    String fieldTypeNameArr = fieldTypeName.toString().trim() + "[]";
                    if (schemaValue.getNullable() != null) {
                        if (schemaValue.getNullable()) {
                            String fieldTypeNameStr = fieldTypeName.toString();
                            fieldTypeNameArr = fieldTypeNameStr.substring(0,
                                    fieldTypeNameStr.length() - 1).trim() + "[]?";
                        }
                    } else if (nullable) {
                        String fieldTypeNameStr = fieldTypeName.toString();
                        fieldTypeNameArr = fieldTypeNameStr.substring(0,
                                fieldTypeNameStr.length() - 1).trim() + "[]?";
                    }
                    typeDefNode = createTypeDefinitionNode(metadataNode, null,
                            createIdentifierToken("public type"),
                            createIdentifierToken(escapeIdentifier(schema.getKey())),
                            createSimpleNameReferenceNode(createIdentifierToken(fieldTypeNameArr)),
                            createToken(SEMICOLON_TOKEN));
                    typeDefinitionNodeList.add(typeDefNode);
                }
            } else {
                Schema value = schema.getValue();
                MarkdownDocumentationNode documentationNode =
                        createMarkdownDocumentationNode(createNodeList(schemaDoc));
                MetadataNode metadataNode =
                        createMetadataNode(documentationNode, createEmptyNodeList());
                if (value.getType() != null) {
                    String typedescriptorName = convertOpenAPITypeToBallerina(value.getType().trim());
                    if (value.getNullable() != null) {
                        if (value.getNullable()) {
                            typedescriptorName = typedescriptorName + "?";
                        }
                    } else if (nullable) {
                        typedescriptorName = typedescriptorName + "?";
                    }
                    typeDefNode = createTypeDefinitionNode(metadataNode,
                            null, createIdentifierToken("public type"),
                            createIdentifierToken(getValidName((schema.getKey()), true)),
                            createSimpleNameReferenceNode(createIdentifierToken(typedescriptorName)),
                            createToken(SEMICOLON_TOKEN));
                    typeDefinitionNodeList.add(typeDefNode);
                }
            }
        } else if (schemaValue.get$ref() != null) {
            Token typeRef = AbstractNodeFactory.createIdentifierToken(getValidName(
                    extractReferenceType(schemaValue.get$ref()), true));
            Token asterisk = AbstractNodeFactory.createIdentifierToken("*");
            Token semicolon = AbstractNodeFactory.createIdentifierToken(";");
            TypeReferenceNode recordField =
                    NodeFactory.createTypeReferenceNode(asterisk, typeRef, semicolon);
            recordFieldList.add(recordField);
            NodeList<Node> fieldNodes = AbstractNodeFactory.createNodeList(recordFieldList);
            RecordTypeDescriptorNode recordTypeDescriptorNode =
                    NodeFactory.createRecordTypeDescriptorNode(createToken(SyntaxKind.RECORD_KEYWORD),
                            createToken(OPEN_BRACE_TOKEN), fieldNodes, null,
                            createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
            MarkdownDocumentationNode documentationNode =
                    createMarkdownDocumentationNode(createNodeList(schemaDoc));
            MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
            typeDefNode = NodeFactory.createTypeDefinitionNode(metadataNode,
                    null, typeKeyWord, typeName, recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
            typeDefinitionNodeList.add(typeDefNode);

        } else if (schemaValue.getType() == null) {

            MarkdownDocumentationNode documentationNode =
                    createMarkdownDocumentationNode(createNodeList(schemaDoc));
            MetadataNode metadataNode =
                    createMetadataNode(documentationNode, createEmptyNodeList());
            String anyType = "any";
            if (schemaValue.getNullable() != null) {
                if (schemaValue.getNullable()) {
                    anyType = anyType + "?";
                }
            } else if (nullable) {
                anyType = anyType + "?";
            }
            typeDefNode = createTypeDefinitionNode(metadataNode,
                    null, createIdentifierToken("public type"),
                    createIdentifierToken(getValidName((schema.getKey()), true)),
                    createSimpleNameReferenceNode(createIdentifierToken(
                            anyType)),
                    createToken(SEMICOLON_TOKEN));
            typeDefinitionNodeList.add(typeDefNode);

        } else {
            throw new BallerinaOpenApiException("Unsupported OAS schema type.");
        }
        return typeDefNode;
    }

    /**
     * Generate Node for allOf data binding.
     *
     * @param openApi           - OpenAPI specification
     * @param schemaDoc         - API docs list
     * @param required          - required fields
     * @param typeName          - main record name
     * @param recordFieldList   - generated previous record field list
     * @param allOf             - allOf schema values
     * @return                  TypeDefinitionNode for allOf
     * @throws BallerinaOpenApiException
     */
    public TypeDefinitionNode getAllOfTypeDefinitionNode(OpenAPI openApi, List<Node> schemaDoc, List<String> required
            , IdentifierToken typeName, List<Node> recordFieldList, List<Schema> allOf)
            throws BallerinaOpenApiException {

        RecordTypeDescriptorNode recordTypeDescriptorNode =
                getAllOfRecordTypeDescriptorNode(openApi, schemaDoc, required, recordFieldList, allOf);
        Token semicolon = AbstractNodeFactory.createIdentifierToken(";");
        MarkdownDocumentationNode documentationNode =
                createMarkdownDocumentationNode(createNodeList(schemaDoc));
        MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
        return NodeFactory.createTypeDefinitionNode(metadataNode,
                null, createIdentifierToken("public type"), typeName, recordTypeDescriptorNode,
                semicolon);
    }

    private RecordTypeDescriptorNode getAllOfRecordTypeDescriptorNode(OpenAPI openApi, List<Node> schemaDoc,
                                                                      List<String> required, List<Node> recordFieldList,
                                                                      List<Schema> allOf)
            throws BallerinaOpenApiException {

        for (Schema allOfSchema: allOf) {
            if (allOfSchema.getType() == null && allOfSchema.get$ref() != null) {
                //Generate typeReferenceNode
                getAllOfRecordFieldForReference(schemaDoc, recordFieldList, allOfSchema);
            } else if (allOfSchema instanceof ObjectSchema && (allOfSchema.getProperties() != null)) {
                if (allOfSchema.getDescription() != null) {
                    MarkdownDocumentationLineNode recordDes = createMarkdownDocumentationLineNode(null,
                                    createToken(SyntaxKind.HASH_TOKEN), createNodeList(createLiteralValueToken(
                                            null, allOfSchema.getDescription().split("\n")[0],
                                            createEmptyMinutiaeList(), createEmptyMinutiaeList())));
                    schemaDoc.add(recordDes);
                }
                Map<String, Schema> properties = allOfSchema.getProperties();
                for (Map.Entry<String, Schema> field : properties.entrySet()) {
                    addRecordFields(required, recordFieldList, field, openApi);
                }
            } else if (allOfSchema instanceof ComposedSchema) {
                ComposedSchema allOfNested = (ComposedSchema) allOfSchema;
                if (allOfNested.getAllOf() != null) {
                    for (Schema schema: allOfNested.getAllOf()) {
                        if (schema instanceof ObjectSchema) {
                            ObjectSchema objectSchema = (ObjectSchema) schema;
                            List<String> requiredField = objectSchema.getRequired();
                            Map<String, Schema> properties = objectSchema.getProperties();
                            // TODO: add api documentation
                            for (Map.Entry<String, Schema> field : properties.entrySet()) {
                                addRecordFields(requiredField, recordFieldList, field, openApi);
                            }
                        }
                    }
                }
                // TODO handle OneOf, AnyOf
            }
        }
        NodeList<Node> fieldNodes = AbstractNodeFactory.createNodeList(recordFieldList);
        return NodeFactory.createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD), createToken(OPEN_BRACE_TOKEN),
                fieldNodes, null, createToken(CLOSE_BRACE_TOKEN));
    }

    private void getAllOfRecordFieldForReference(List<Node> schemaDoc, List<Node> recordFieldList, Schema allOfSchema)
            throws BallerinaOpenApiException {

        Token typeRef = AbstractNodeFactory.createIdentifierToken(getValidName(
                        extractReferenceType(allOfSchema.get$ref()), true));
        Token asterisk = AbstractNodeFactory.createIdentifierToken("*");
        Token semicolon = AbstractNodeFactory.createIdentifierToken(";");
        TypeReferenceNode recordField =
                NodeFactory.createTypeReferenceNode(asterisk, typeRef, semicolon);
        recordFieldList.add(recordField);
        if (allOfSchema.getDescription() != null) {
            MarkdownDocumentationLineNode recordDes = createMarkdownDocumentationLineNode(null,
                    createToken(SyntaxKind.HASH_TOKEN), createNodeList(createLiteralValueToken(
                            null, allOfSchema.getDescription().split("\n")[0],
                            createEmptyMinutiaeList(), createEmptyMinutiaeList())));
            schemaDoc.add(recordDes);
        }
    }

    /**
     * This function use to create typeDefinitionNode for objectSchema.
     *
     * @param required - This string list include required fields in properties.
     * @param typeKeyWord   - Type keyword for record.
     * @param typeName      - Record Name
     * @param recordFieldList - RecordFieldList
     * @param fields          - schema properties map
     * @return This return record TypeDefinitionNode
     * @throws BallerinaOpenApiException
     */

    public TypeDefinitionNode getTypeDefinitionNodeForObjectSchema(List<String> required, Token typeKeyWord,
                                                                           IdentifierToken typeName,
                                                                           List<Node> recordFieldList,
                                                                           Map<String, Schema> fields,
                                                                           String description, OpenAPI openApi)
            throws BallerinaOpenApiException {
        List<Node> schemaDoc = new ArrayList<>();
        if (!description.isBlank()) {
            MarkdownDocumentationLineNode paramAPIDoc =
                    createMarkdownDocumentationLineNode(null, createToken(SyntaxKind.HASH_TOKEN),
                            createNodeList(createLiteralValueToken(null, description,
                                    createEmptyMinutiaeList(), createEmptyMinutiaeList())));
            schemaDoc.add(paramAPIDoc);
        }
        TypeDefinitionNode typeDefinitionNode;
        if (fields != null) {
            for (Map.Entry<String, Schema> field : fields.entrySet()) {
                addRecordFields(required, recordFieldList, field, openApi);
            }
            NodeList<Node> fieldNodes = AbstractNodeFactory.createNodeList(recordFieldList);
            RecordTypeDescriptorNode recordTypeDescriptorNode =
                    NodeFactory.createRecordTypeDescriptorNode(createToken(SyntaxKind.RECORD_KEYWORD),
                            createToken(OPEN_BRACE_TOKEN), fieldNodes, null,
                            createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
            MarkdownDocumentationNode documentationNode =
                    createMarkdownDocumentationNode(createNodeList(schemaDoc));
            MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
            typeDefinitionNode = NodeFactory.createTypeDefinitionNode(metadataNode,
                    null, typeKeyWord, typeName, recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
        } else {
            RecordTypeDescriptorNode recordTypeDescriptorNode =
                    NodeFactory.createRecordTypeDescriptorNode(createToken(SyntaxKind.RECORD_KEYWORD),
                            createToken(OPEN_BRACE_TOKEN), createEmptyNodeList(), null,
                            createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
            MarkdownDocumentationNode documentationNode =
                    createMarkdownDocumentationNode(createNodeList(schemaDoc));
            MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
            typeDefinitionNode = NodeFactory.createTypeDefinitionNode(metadataNode,
                    null, typeKeyWord, typeName, recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
        }
        return typeDefinitionNode;
    }

    /**
     * This util for generate record field with given schema properties.
     */
    private void addRecordFields(List<String> required, List<Node> recordFieldList, Map.Entry<String, Schema> field,
                                 OpenAPI openApi) throws BallerinaOpenApiException {
        // TODO: Handle allOf , oneOf, anyOf
        RecordFieldNode recordFieldNode;
        // API doc
        List<Node> schemaDoc = new ArrayList<>();
        String fieldN = escapeIdentifier(field.getKey().trim());
        if (field.getValue().getDescription() != null) {
            MarkdownDocumentationLineNode paramAPIDoc =
                    createMarkdownDocumentationLineNode(null, createToken(SyntaxKind.HASH_TOKEN),
                            createNodeList(createLiteralValueToken(null,
                                    field.getValue().getDescription().split("\n")[0],
                                    createEmptyMinutiaeList(), createEmptyMinutiaeList())));
            schemaDoc.add(paramAPIDoc);
        } else if (field.getValue().get$ref() != null) {
            String[] split = field.getValue().get$ref().trim().split("/");
            String componentName = getValidName(split[split.length - 1], true);
            if (openApi.getComponents().getSchemas().get(componentName) != null) {
                Schema schema = openApi.getComponents().getSchemas().get(componentName);
                if (schema.getDescription() != null) {
                    MarkdownDocumentationLineNode paramAPIDoc =
                            createMarkdownDocumentationLineNode(null, createToken(SyntaxKind.HASH_TOKEN),
                                    createNodeList(createLiteralValueToken(null,
                                            schema.getDescription().split("\n")[0],
                                            createEmptyMinutiaeList(), createEmptyMinutiaeList())));
                    schemaDoc.add(paramAPIDoc);
                }
            }
        }
        //FiledName
        IdentifierToken fieldName = AbstractNodeFactory.createIdentifierToken(fieldN);

        TypeDescriptorNode fieldTypeName = extractOpenAPISchema(field.getValue(), openApi);
        Token semicolonToken = AbstractNodeFactory.createIdentifierToken(";");
        Token questionMarkToken = AbstractNodeFactory.createIdentifierToken("?");
        MarkdownDocumentationNode documentationNode = createMarkdownDocumentationNode(createNodeList(schemaDoc));
        MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
        if (required != null) {
            if (!required.contains(field.getKey().trim())) {
                recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
                        fieldTypeName, fieldName, questionMarkToken, semicolonToken);
            } else {
                recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
                        fieldTypeName, fieldName, null, semicolonToken);
            }
        } else {
            recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
                    fieldTypeName, fieldName, questionMarkToken, semicolonToken);
        }
        recordFieldList.add(recordFieldNode);
    }

    /**
     * Common method to extract OpenApi Schema type objects in to Ballerina type compatible schema objects.
     *
     * @param schema - OpenApi Schema
     */
    private TypeDescriptorNode extractOpenAPISchema(Schema schema, OpenAPI openApi) throws BallerinaOpenApiException {

        if (schema.getType() != null || schema.getProperties() != null) {
            if (schema.getType() != null && ((schema.getType().equals("integer") || schema.getType().equals("number"))
                    || schema.getType().equals("string") || schema.getType().equals("boolean"))) {
                String type = convertOpenAPITypeToBallerina(schema.getType().trim());
                if (schema.getType().equals("number")) {
                    if (schema.getFormat() != null) {
                        type = convertOpenAPITypeToBallerina(schema.getFormat().trim());
                    }
                }
                if (schema.getNullable() != null) {
                    if (schema.getNullable()) {
                        type = type + "?";
                    }
                } else if (nullable) {
                    type = type + "?";
                }
                Token typeName = AbstractNodeFactory.createIdentifierToken(type);
                return createBuiltinSimpleNameReferenceNode(null, typeName);
            } else if (schema.getType() != null && schema.getType().equals("array")) {
                if (schema instanceof ArraySchema) {
                    final ArraySchema arraySchema = (ArraySchema) schema;
                    if (arraySchema.getItems() != null) {
                        // single array
                        return getTypeDescriptorNodeForArraySchema(openApi, arraySchema);
                    }
                }
            } else if ((schema.getType() != null && schema.getType().equals("object")) ||
                    (schema.getProperties() != null))  {
                if (schema.getProperties() != null) {
                    Map<String, Schema> properties = schema.getProperties();
                    Token recordKeyWord = AbstractNodeFactory.createIdentifierToken("record ");
                    Token bodyStartDelimiter = AbstractNodeFactory.createIdentifierToken("{ ");
                    Token bodyEndDelimiter = AbstractNodeFactory.createIdentifierToken("} ");
                    List<Node> recordFList = new ArrayList<>();
                    List<String> required = schema.getRequired();
                    for (Map.Entry<String, Schema> property: properties.entrySet()) {
                        addRecordFields(required, recordFList, property, openApi);
                    }
                    NodeList<Node> fieldNodes = AbstractNodeFactory.createNodeList(recordFList);

                    return NodeFactory.createRecordTypeDescriptorNode(recordKeyWord, bodyStartDelimiter, fieldNodes,
                            null, bodyEndDelimiter);
                } else if (schema.get$ref() != null) {
                    String type = getValidName(extractReferenceType(schema.get$ref()), true);
                    Schema refSchema = openApi.getComponents().getSchemas().get(type);
                    if (refSchema.getNullable() != null) {
                        if (refSchema.getNullable()) {
                            type = type + "?";
                        }
                    } else if (nullable) {
                        type = type + "?";
                    }
                    Token typeName = AbstractNodeFactory.createIdentifierToken(type);
                    return createBuiltinSimpleNameReferenceNode(null, typeName);
                } else {
                    Token typeName = AbstractNodeFactory.createIdentifierToken(
                                    convertOpenAPITypeToBallerina(schema.getType().trim()));
                    return createBuiltinSimpleNameReferenceNode(null, typeName);
                }
            } else if (schema.getType() != null && schema.getType().equals("object")) {
                String type = convertOpenAPITypeToBallerina(schema.getType().trim());
                Token typeName = AbstractNodeFactory.createIdentifierToken(type);
                return createBuiltinSimpleNameReferenceNode(null, typeName);
            } else {
                throw new BallerinaOpenApiException("Unsupported OAS data type `" + schema.getType() + "`.");
            }
        } else if (schema.get$ref() != null) {
            String type = extractReferenceType(schema.get$ref());
            type = getValidName(type, true);
            Schema refSchema = openApi.getComponents().getSchemas().get(type);
            if (refSchema.getNullable() != null) {
                if (refSchema.getNullable()) {
                    type = type + "?";
                }
            } else if (nullable) {
                type = type + "?";
            }
            Token typeName = AbstractNodeFactory.createIdentifierToken(type);
            return createBuiltinSimpleNameReferenceNode(null, typeName);
        }  else if (schema instanceof ComposedSchema) {
            //TODO: API doc generator
            ComposedSchema composedSchema = (ComposedSchema) schema;
            return getTypeDescriptorNodeForComposedSchema(openApi, composedSchema, new ArrayList<>());
        } else if (schema.getType() == null) {
            //This contains a fallback to Ballerina common type `anydata` if the OpenApi specification type is not
            // defined.
            String type = "anydata";
            if (schema.getNullable() != null) {
                if (schema.getNullable()) {
                    type = type + "?";
                }
            } else if (nullable) {
                type = type + "?";
            }
            Token typeName = AbstractNodeFactory.createIdentifierToken(type);
            return createBuiltinSimpleNameReferenceNode(null, typeName);
        } else {
            //This contains the OpenApi specification type is not compatible with any of the current Ballerina types.
            throw new BallerinaOpenApiException("Unsupported OAS data type.");
        }
        String type = "anydata";
        if (schema.getNullable() != null) {
            if (schema.getNullable()) {
                type = type + "?";
            }
        } else if (nullable) {
            type = type + "?";
        }
        Token typeName = AbstractNodeFactory.createIdentifierToken(type);
        return createBuiltinSimpleNameReferenceNode(null, typeName);
    }

    /**
     * Generate TypeDeccriptorNode for Array Schema.
     * @param openApi        - OpenAPI specification
     * @param arraySchema    - Array schema
     * @return
     * @throws BallerinaOpenApiException
     */
    public TypeDescriptorNode getTypeDescriptorNodeForArraySchema(OpenAPI openApi, ArraySchema arraySchema)
            throws BallerinaOpenApiException {

        String type;
        Token typeName;
        TypeDescriptorNode memberTypeDesc;
        Token openSBracketToken = AbstractNodeFactory.createIdentifierToken("[");
        Token closeSBracketToken = AbstractNodeFactory.createIdentifierToken("]");
        Schema<?> schemaItem = arraySchema.getItems();
        if (schemaItem.get$ref() != null) {
            type = getValidName(extractReferenceType(arraySchema.getItems().get$ref()), true);
            if (arraySchema.getNullable() != null) {
                if (arraySchema.getNullable()) {
                    closeSBracketToken = AbstractNodeFactory.createIdentifierToken("]?");
                }
            } else if (nullable) {
                closeSBracketToken = AbstractNodeFactory.createIdentifierToken("]?");
            }
            typeName = AbstractNodeFactory.createIdentifierToken(type);
            memberTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
            return NodeFactory.createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        } else if (schemaItem instanceof ArraySchema) {
            memberTypeDesc = extractOpenAPISchema(arraySchema.getItems(), openApi);
            return NodeFactory.createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        } else if (schemaItem instanceof ObjectSchema) {
            //Array has inline record
            ObjectSchema inlineSchema = (ObjectSchema) schemaItem;
            memberTypeDesc = extractOpenAPISchema(inlineSchema, openApi);
            return NodeFactory.createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        } else if (schemaItem instanceof ComposedSchema) {
            // TODO: API Doc generator
            ComposedSchema composedSchema = (ComposedSchema) schemaItem;
            memberTypeDesc = getTypeDescriptorNodeForComposedSchema(openApi, composedSchema, new ArrayList<>());
            return NodeFactory.createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        } else if (schemaItem.getType() != null) {
            type = schemaItem.getType();
            if (arraySchema.getNullable() != null) {
                if (arraySchema.getNullable()) {
                    closeSBracketToken = AbstractNodeFactory.createIdentifierToken("]?");
                }
            } else if (nullable) {
                closeSBracketToken = AbstractNodeFactory.createIdentifierToken("]?");
            }
            typeName = AbstractNodeFactory.createIdentifierToken(convertOpenAPITypeToBallerina(type));
            memberTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
            return NodeFactory.createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        } else {
            type = "anydata";
            if (arraySchema.getNullable() != null) {
                if (arraySchema.getNullable()) {
                    closeSBracketToken = AbstractNodeFactory.createIdentifierToken("]?");
                }
            } else if (nullable) {
                closeSBracketToken = AbstractNodeFactory.createIdentifierToken("]?");
            }
            typeName = AbstractNodeFactory.createIdentifierToken(type);
            memberTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
            return NodeFactory.createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        }
    }

    private TypeDescriptorNode getTypeDescriptorNodeForComposedSchema(OpenAPI openAPI, ComposedSchema composedSchema,
                                                                      List<Node> schemaDoc)
            throws BallerinaOpenApiException {

        if (composedSchema.getOneOf() != null) {
            String  oneOf = generatorUtils.getOneOfUnionType(composedSchema.getOneOf());
            return createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(oneOf));
        } else if (composedSchema.getAnyOf() != null) {
            String  anyOf = generatorUtils.getOneOfUnionType(composedSchema.getAnyOf());
            return createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(anyOf));
        } else if (composedSchema.getAllOf() != null) {
            return getAllOfRecordTypeDescriptorNode(openAPI, schemaDoc, composedSchema.getRequired(),
                    new ArrayList<>(), composedSchema.getAllOf());
        } else if (composedSchema.getType() != null) {
            return createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken(convertOpenAPITypeToBallerina(composedSchema.getType().trim())));
        } else if (composedSchema.get$ref() != null) {
            return createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken(getValidName(extractReferenceType(composedSchema.get$ref().trim()),
                            true)));
        } else {
            throw new BallerinaOpenApiException("Unsupported OAS data type.");
        }
    }

}
