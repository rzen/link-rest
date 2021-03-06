package com.nhl.link.rest.client.runtime.response;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhl.link.rest.client.ClientSimpleResponse;
import com.nhl.link.rest.client.LinkRestClientException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

/**
 * @since 2.0
 */
public abstract class BaseResponseHandler<T extends ClientSimpleResponse> implements ClientResponseHandler<T> {

    private static final String MESSAGE_NODE = "message";

    protected JsonFactory jsonFactory;

    protected BaseResponseHandler(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    @Override
    public final T handleResponse(Response response) {

        Status status = Status.fromStatusCode(response.getStatus());
        if (status.getFamily() != Status.Family.SUCCESSFUL) {
            throw new LinkRestClientException(buildErrorMessage(status, response));
        }
        return doHandleResponse(status, response);
    }

    private String buildErrorMessage(Status status, Response response) {

        String errorMessage = "Server returned " + status.getStatusCode() +
                    " (" + status.getReasonPhrase() + ")";

        String serverMessage = readMessage(response);
        if (serverMessage != null) {
            errorMessage += ". Message: " + serverMessage;
        }

        return errorMessage;
    }

    private String readMessage(Response response) {

        String entity = response.readEntity(String.class);
        String message = null;

        if (entity != null) {
            try {
                JsonNode entityNode = new ObjectMapper().readTree(jsonFactory.createParser(entity));
                JsonNode messageNode = entityNode.get(MESSAGE_NODE);
                if (messageNode != null) {
                    message = messageNode.textValue();
                }
            } catch (IOException e) {
                // do nothing...
            }
        }
        return message;
    }

    protected abstract T doHandleResponse(Status status, Response response);

}
