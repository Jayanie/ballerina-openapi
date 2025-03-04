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
 
package io.ballerina.openapi.converter.utils;

import io.ballerina.openapi.converter.Constants;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

/**
 * Utilities used in Ballerina  to OpenAPI converter.
 */
public class ConverterCommonUtils {

    /**
     * This util function is for converting ballerina type to openapi type.
     * @param type this string type parameter according to ballerina type
     * @return  this return the string value of openAPI type
     */
    public static String convertBallerinaTypeToOpenAPIType(String type) {
        String convertedType;
        switch (type) {
            case Constants.INT:
                convertedType = Constants.OpenAPIType.INTEGER.toString();
                break;
            case Constants.STRING:
                convertedType = Constants.OpenAPIType.STRING.toString();
                break;
            case Constants.BOOLEAN:
                convertedType = Constants.OpenAPIType.BOOLEAN.toString();
                break;
            case Constants.ARRAY:
                convertedType = Constants.OpenAPIType.ARRAY.toString();
                break;
            case Constants.RECORD:
                convertedType = Constants.OpenAPIType.RECORD.toString();
                break;
            case Constants.DECIMAL:
                convertedType = Constants.OpenAPIType.DECIMAL.toString();
                break;
            default:
                convertedType = "";
        }
        return convertedType;
    }

    /**
     * Retrieves a matching OpenApi {@link Schema} for a provided ballerina type.
     *
     * @param type ballerina type name as a String
     * @return OpenApi {@link Schema} for type defined by {@code type}
     */
    public static Schema getOpenApiSchema(String type) {
        Schema schema;
        switch (type) {
            case Constants.STRING:
            case Constants.PLAIN:
                schema = new StringSchema();
                break;
            case Constants.BOOLEAN:
                schema = new BooleanSchema();
                break;
            case Constants.ARRAY:
                schema = new ArraySchema();
                break;
            case Constants.NUMBER:
            case Constants.INT:
            case Constants.INTEGER:
                schema = new IntegerSchema();
                break;
            case Constants.TYPE_REFERENCE:
            case Constants.TYPEREFERENCE:
                schema = new Schema();
//                schema.$ref("true");
                break;
            case Constants.BYTE_ARRAY:
            case Constants.OCTET_STREAM:
                schema = new StringSchema();
                schema.setFormat("uuid");
                break;
            case Constants.XML:
            case Constants.JSON:
            default:
                schema = new ObjectSchema();
                break;
        }
        return schema;
    }
}
