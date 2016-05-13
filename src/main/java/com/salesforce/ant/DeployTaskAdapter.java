package com.salesforce.ant;

import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;


public class DeployTaskAdapter extends DeployTask {
    @Override
    public void handleResponse(
        MetadataConnection metadataConnection,
        SFDCMDAPIAntTask.StatusResult response) throws ConnectionException {

        AsyncResult result = new AsyncResult();
        result.setId(response.getId());
        result.setDone(response.isDone());
        handleResponse(metadataConnection, result);
        super.handleResponse(metadataConnection, response);
    }

    public void handleResponse(MetadataConnection metadataConnection, AsyncResult response) { }
}