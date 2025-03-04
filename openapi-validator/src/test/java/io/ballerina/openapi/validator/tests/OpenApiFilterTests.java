/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ballerina.openapi.validator.tests;

import io.ballerina.openapi.validator.Filters;
import io.ballerina.openapi.validator.OpenAPIPathSummary;
import io.ballerina.openapi.validator.OpenApiValidatorException;
import io.ballerina.openapi.validator.ResourceWithOperation;
import io.ballerina.openapi.validator.ServiceValidator;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.swagger.v3.oas.models.OpenAPI;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This unit tests for filterOpenApi function.
 */
public class OpenApiFilterTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/project-based-tests/modules/contractValidation/")
            .toAbsolutePath();
    private OpenAPI api;
    private List<OpenAPIPathSummary> openAPIPathSummaries = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private List<String> operations = new ArrayList<>();
    private List<String> excludeTags = new ArrayList<>();
    private List<String> excludeOperations = new ArrayList<>();


    @Test(description = "When Tag filter is enable")
    public void testTagFilter() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/valid/petstore.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        tags.add("pets");
        Filters filter = new Filters(tags, excludeTags, operations, excludeOperations, DiagnosticSeverity.ERROR);
        openAPIPathSummaries = ResourceWithOperation.filterOpenapi(api, filter);
        Assert.assertEquals(openAPIPathSummaries.get(0).getPath(), "/pets");
        Assert.assertEquals(openAPIPathSummaries.get(0).getOperations().get("post").getOperationId(), "postPet");
        tags.clear();
    }

    @Test(description = "When exclude tag filter enable")
    public void testExcludeTag() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/valid/petstore.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        excludeTags.add("pets");
        Filters filter = new Filters(tags, excludeTags, operations, excludeOperations, DiagnosticSeverity.ERROR);
        openAPIPathSummaries = ResourceWithOperation.filterOpenapi(api, filter);
        Assert.assertEquals(openAPIPathSummaries.get(0).getPath(), "/pets");
        Assert.assertEquals(openAPIPathSummaries.get(0).getOperations().get("get").getOperationId(), "listPets");
        excludeTags.clear();
    }

    @Test(description = "When operation filter enable")
    public void testOperations() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/valid/petstore02.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        operations.add("listPets");
        Filters filter = new Filters(tags, excludeTags, operations, excludeOperations, DiagnosticSeverity.ERROR);
        openAPIPathSummaries = ResourceWithOperation.filterOpenapi(api, filter);
        Assert.assertEquals(openAPIPathSummaries.size(), 1);
        Assert.assertEquals(openAPIPathSummaries.get(0).getPath(), "/pets");
        Assert.assertEquals(openAPIPathSummaries.get(0).getOperations().get("get").getOperationId(), "listPets");
        operations.clear();
    }

    @Test(description = "When exclude operation filter enable")
    public void testExcludeOperations() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/valid/petstore03.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        excludeOperations.add("showUser");
        Filters filter = new Filters(tags, excludeTags, operations, excludeOperations, DiagnosticSeverity.ERROR);
        openAPIPathSummaries = ResourceWithOperation.filterOpenapi(api, filter);
        Assert.assertEquals(openAPIPathSummaries.size(), 3);
        Assert.assertEquals(openAPIPathSummaries.get(0).getPath(), "/pets");
        Assert.assertEquals(openAPIPathSummaries.get(0).getOperations().get("get").getOperationId(), "listPets");
        excludeOperations.clear();
    }

    @Test(description = "When tag and operation filter enable")
    public void testOperationsWithTag() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/valid/petstore04.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        tags.add("pets");
        operations.add("listPets");
        operations.add("postPet");
        operations.add("showUser");
        Filters filter = new Filters(tags, excludeTags, operations, excludeOperations, DiagnosticSeverity.ERROR);
        openAPIPathSummaries = ResourceWithOperation.filterOpenapi(api, filter);
        Assert.assertEquals(openAPIPathSummaries.size(), 1);
        Assert.assertEquals(openAPIPathSummaries.get(0).getPath(), "/pets");
        Assert.assertEquals(openAPIPathSummaries.get(0).getOperations().get("post").getOperationId(), "postPet");
        tags.clear();
        operations.clear();
    }

    @Test(description = "When operation and exclude tag filter enable")
    public void testOperationsWithExcludeTag() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/valid/petstore05.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        excludeTags.add("pets");
        operations.add("listPets");
        operations.add("postPets");
        operations.add("showUser");
        Filters filter = new Filters(tags, excludeTags, operations, excludeOperations, DiagnosticSeverity.ERROR);
        openAPIPathSummaries = ResourceWithOperation.filterOpenapi(api, filter);
        Assert.assertEquals(openAPIPathSummaries.size(), 2);
        Assert.assertEquals(openAPIPathSummaries.get(0).getPath(), "/pets");
        Assert.assertEquals(openAPIPathSummaries.get(0).getOperations().get("get").getOperationId(), "listPets");
        operations.clear();
        excludeTags.clear();
    }

    @Test(description = "When tag and exclude operation filter enable")
    public void testExcludeOperationsWithTag() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/valid/petstore06.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        tags.add("pets");
        excludeOperations.add("listPets");
        excludeOperations.add("postPet");
        excludeOperations.add("showUser");
        Filters filter = new Filters(tags, excludeTags, operations, excludeOperations, DiagnosticSeverity.ERROR);
        openAPIPathSummaries = ResourceWithOperation.filterOpenapi(api, filter);
        Assert.assertEquals(openAPIPathSummaries.size(), 2);
        Assert.assertEquals(openAPIPathSummaries.get(0).getPath(), "/pets/{petId}");
        Assert.assertEquals(openAPIPathSummaries.get(1).getPath(), "/user");
        Assert.assertEquals(openAPIPathSummaries.get(0).getOperations().get("get").getOperationId(), "showPetById");
        tags.clear();
        excludeOperations.clear();
    }

    @Test(description = "When exclude tag and exclude operations enables")
     public void testBothExcludeTagsAndOperations() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/valid/petstore07.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        excludeTags.add("list");
        excludeOperations.add("postPet");
        excludeOperations.add("showUser");
        Filters filter = new Filters(tags, excludeTags, operations, excludeOperations, DiagnosticSeverity.ERROR);
        openAPIPathSummaries = ResourceWithOperation.filterOpenapi(api, filter);
        Assert.assertEquals(openAPIPathSummaries.size(), 3);
        Assert.assertEquals(openAPIPathSummaries.get(0).getPath(), "/pets");
        excludeOperations.clear();
        excludeTags.clear();
    }
}
