import  ballerina/http;


public isolated client class Client {
    final http:Client clientEp;
    # Gets invoked to initialize the `connector`.
    #
    # + clientConfig - The configurations to be used when initializing the `connector`
    # + serviceUrl - URL of the target service
    # + return - An error if connector initialization failed
    public isolated function init(http:ClientConfiguration clientConfig =  {}, string serviceUrl = "localhost:9090/payloadV") returns error? {
        http:Client httpEp = check new (serviceUrl, clientConfig);
        self.clientEp = httpEp;
    }
    #
    # + id - id value
    # + return - Ok
    remote isolated function operationId03(Id id) returns string|error {
        string  path = string `/v1/${id}`;
        string response = check self.clientEp-> get(path, targetType = string);
        return response;
    }
}
