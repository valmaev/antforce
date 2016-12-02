package com.salesforce.ant;

import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;
import java.io.IOException;


/**
 * This class exists to being able to override methods from base class hierarchy in descendant classes.
 * <p>
 * Since 36.0 version of Force.com Migration tool signature of
 * {@link SFDCMDAPIAntTask#handleResponse(MetadataConnection, StatusResult)} method was changed
 * and it takes instance of package-private {@link SFDCMDAPIAntTask.StatusResult} interface.
 * <p>
 * This class:<ul>
 * <li>have {@link com.salesforce.ant} package to being able to get access to package-private types
 * <li>is written in Java because in Kotlin we can't access to package-private (internal in Kotlin)
 * types and members from same package but from different .jar-file
 * </ul>
 */
public class DeployTaskAdapter extends DeployTask {
    @Override
    public void handleResponse(MetadataConnection metadataConnection, StatusResult response)
        throws ConnectionException {

        AsyncResult result = new AsyncResult();
        result.setId(response.getId());
        result.setDone(response.isDone());
        handleResponse(metadataConnection, result);
        super.handleResponse(metadataConnection, response);
    }

    @Override
    public void setZipBytes() throws IOException {
        super.setZipBytes();
    }

    public void setZipBytes(byte[] value) {
        zipBytes = value;
    }

    public byte[] getZipBytes() {
        return zipBytes;
    }

    public String getDeployRoot() {
        return deployRoot;
    }

    public void handleResponse(MetadataConnection metadataConnection, AsyncResult response) {
    }
}