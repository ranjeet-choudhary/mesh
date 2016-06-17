package com.gentics.mesh.rest;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.ParameterProvider;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import rx.Observable;

public abstract class AbstractMeshRestHttpClient implements MeshRestClient {

	protected static final Logger log = LoggerFactory.getLogger(AbstractMeshRestHttpClient.class);

	public static final String BASEURI = "/api/v1";
	public static final int DEFAULT_PORT = 8080;

	protected HttpClient client;

	protected MeshRestClientAuthenticationProvider authentication;

	@Override
	public MeshRestClient setLogin(String username, String password) {
		authentication.setLogin(username, password);
		return this;
	}

	@Override
	public MeshRestClient initializeAuthenticationProvider(RoutingContext context) {
		String authHeader = context.request().getHeader("Authentication");
		if (authHeader == null || authHeader.startsWith("Basic")) {
			authentication = new BasicAuthentication(context);
		} else if (authHeader.startsWith("Bearer")) {
			authentication = new JWTAuthentication(context);
		}
		return this;
	}

	@Override
	public HttpClient getClient() {
		return client;
	}

	@Override
	public void close() {
		client.close();
	}

	public static String getBaseuri() {
		return BASEURI;
	}

	public void setClient(HttpClient client) {
		this.client = client;
	}

	@Override
	public Observable<GenericMessageResponse> login() {
		return authentication.login(getClient());
	}

	@Override
	public Observable<GenericMessageResponse> logout() {
		return authentication.logout(getClient());
	}

	public void setAuthentication(MeshRestClientAuthenticationProvider authentication) {
		this.authentication = authentication;
	}

	protected <T> Future<T> handleRequest(HttpMethod method, String path, Class<? extends T> classOfT, Buffer bodyData, String contentType) {
		return MeshRestRequestUtil.handleRequest(method, path, classOfT, bodyData, contentType, client, authentication);
	}

	protected <T> Future<T> handleRequest(HttpMethod method, String path, Class<? extends T> classOfT, RestModel restModel) {
		return MeshRestRequestUtil.handleRequest(method, path, classOfT, restModel, client, authentication);
	}

	protected <T> Future<T> handleRequest(HttpMethod method, String path, Class<? extends T> classOfT, String jsonBodyData) {
		return MeshRestRequestUtil.handleRequest(method, path, classOfT, jsonBodyData, client, authentication);
	}

	protected <T> Future<T> handleRequest(HttpMethod method, String path, Class<? extends T> classOfT) {
		return MeshRestRequestUtil.handleRequest(method, path, classOfT, client, authentication);
	}

	/**
	 * Return the query aggregated parameter string for the given providers.
	 * 
	 * @param parameters
	 * @return
	 */
	public static String getQuery(ParameterProvider... parameters) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < parameters.length; i++) {
			ParameterProvider provider = parameters[i];
			builder.append(provider.getQueryParameters());
			if (i != parameters.length - 1) {
				builder.append("&");
			}
		}
		if (builder.length() > 0) {
			return "?" + builder.toString();
		} else {
			return "";
		}
	}

}
