import  ballerina/http;
import  ballerina/xmldata;

public isolated client class Client {
    public final http:Client clientEp;
    # Gets invoked to initialize the `connector`.
    #
    # + clientConfig - The configurations to be used when initializing the `connector`
    # + serviceUrl - URL of the target service
    # + return - An error if connector initialization failed
    public isolated function init(http:ClientConfiguration  clientConfig =  {}, string serviceUrl = "http://localhost:9090/petstore/v1") returns error? {
        http:Client httpEp = check new (serviceUrl, clientConfig);
        self.clientEp = httpEp;
    }
    remote isolated function  pet(Pet payload) returns http:Response | error {
        string  path = string `/pet`;
        http:Request request = new;
        json jsonBody = check payload.cloneWithType(json);
        xml? xmlBody = check xmldata:fromJson(jsonBody);
        request.setPayload(xmlBody);
        http:Response  response = check self.clientEp->post(path, request, targetType=http:Response );
        return response;
    }
    remote isolated function getPetId(string petId, string payload) returns http:Response | error {
        string  path = string `/pets/${petId}`;
        http:Request request = new;
        request.setPayload(payload);
        http:Response  response = check self.clientEp->post(path, request, targetType=http:Response );
        return response;
    }
    remote isolated function  ImageByimageId(int petId, string imageId) returns http:Response | error {
        string  path = string `/pets/${petId}/Image/${imageId}`;
        http:Response  response = check self.clientEp-> get(path, targetType = http:Response );
        return response;
    }
}
