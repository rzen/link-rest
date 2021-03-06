package com.nhl.link.rest.client;

import java.util.function.Supplier;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhl.link.rest.client.protocol.Expression.ExpressionBuilder;
import com.nhl.link.rest.client.protocol.Include;
import com.nhl.link.rest.client.protocol.Include.IncludeBuilder;
import com.nhl.link.rest.client.protocol.LrcRequest;
import com.nhl.link.rest.client.protocol.LrcRequest.LrRequestBuilder;
import com.nhl.link.rest.client.protocol.Sort;
import com.nhl.link.rest.client.runtime.jackson.IJsonEntityReader;
import com.nhl.link.rest.client.runtime.jackson.IJsonEntityReaderFactory;
import com.nhl.link.rest.client.runtime.jackson.JsonEntityReaderFactory;
import com.nhl.link.rest.client.runtime.response.DataResponseHandler;
import com.nhl.link.rest.client.runtime.run.InvocationBuilder;

/**
 * @since 2.0
 */
public class LinkRestClient {

	private static JsonFactory jsonFactory;
	private static IJsonEntityReaderFactory jsonEntityReaderFactory;

	static {
		jsonFactory = new ObjectMapper().getFactory();
		jsonEntityReaderFactory = new JsonEntityReaderFactory();
	}

	public static LinkRestClient client(WebTarget target) {
		return new LinkRestClient(target);
	}

	private WebTarget target;
	private LrRequestBuilder request;

	private LinkRestClient(WebTarget target) {
		this.target = target;
		request = LrcRequest.builder();
	}

	public LinkRestClient exclude(String... excludePaths) {
		request.exclude(excludePaths);
		return this;
	}

	public LinkRestClient include(String... includePaths) {
		request.include(includePaths);
		return this;
	}

	public LinkRestClient include(Include include) {
		request.include(include);
		return this;
	}

	public LinkRestClient include(IncludeBuilder include) {
		request.include(include.build());
		return this;
	}

	public LinkRestClient cayenneExp(ExpressionBuilder cayenneExp) {
		request.cayenneExp(cayenneExp.build());
		return this;
	}

	public LinkRestClient sort(String... properties) {
		request.sort(properties);
		return this;
	}

	public LinkRestClient sort(Sort ordering) {
		request.sort(ordering);
		return this;
	}

	public LinkRestClient start(long startIndex) {
		request.start(startIndex);
		return this;
	}

	public LinkRestClient limit(long limit) {
		request.limit(limit);
		return this;
	}

	public <T> ClientDataResponse<T> get(Class<T> targetType) {

		if (targetType == null) {
			throw new NullPointerException("Target type");
		}

		IJsonEntityReader<T> jsonEntityReader = jsonEntityReaderFactory.getReaderForType(targetType);
		if (jsonEntityReader == null) {
			throw new LinkRestClientException("Unsupported target type: " + targetType.getName());
		}

		Supplier<Response> invocation = InvocationBuilder.target(target).request(request.build()).build();
		Response response = invocation.get();

		DataResponseHandler<T> responseHandler = new DataResponseHandler<>(jsonFactory, jsonEntityReader);
		return responseHandler.handleResponse(response);
	}
}
