package com.gentics.mesh.core.data.model.tinkerpop;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_USER;
import static com.gentics.mesh.etc.MeshSpringConfiguration.getMeshSpringConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.Permission;
import com.syncleus.ferma.traversals.VertexTraversal;

public class MeshAuthUser extends MeshUser implements ClusterSerializable, User {

	//	private Vertx vertx;

	//	public MeshAuthUser(Vertx vertx, String username, String rolePrefix) {
	//		this.vertx = vertx;
	//	}
	//
	//	public MeshAuthUser() {
	//	}

	@Override
	public JsonObject principal() {
		throw new NotImplementedException();
	}

	@Override
	public void setAuthProvider(AuthProvider authProvider) {
		throw new NotImplementedException();
	}

	@Override
	public User isAuthorised(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
		throw new NotImplementedException("Please use the MeshShiroUser method instead.");
	}

	public MeshAuthUser isAuthorised(MeshVertex targetNode, Permission permission, Handler<AsyncResult<Boolean>> resultHandler) {
		final MeshAuthUser user = this;
		getMeshSpringConfiguration().vertx().executeBlocking(fut -> fut.complete(user.hasPermission(targetNode, permission)), resultHandler);
		return this;
	}

	@Override
	public User clearCache() {
		throw new NotImplementedException();
	}

	@Override
	public void writeToBuffer(Buffer buffer) {
		throw new NotImplementedException();
	}

	@Override
	public int readFromBuffer(int pos, Buffer buffer) {
		throw new NotImplementedException();
	}

	public VertexTraversal<?, ?, ?> getPermTraversal(Permission permission) {
		// TODO out/in/out!
		return out(HAS_USER).in(HAS_ROLE).out(permission.label());
	}

	//	public void setVertx(Vertx vertx) {
	//		this.vertx = vertx;
	//	}

}
