package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class NodeLanguagesVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testDeleteLanguage() {
		Node node = content();
		String uuid = node.getUuid();
		int nLanguagesBefore = node.getAvailableLanguageNames().size();
		assertThat(node.getAvailableLanguageNames()).contains("en", "de");

		// Delete the english version 
		Future<GenericMessageResponse> future = getClient().deleteNode(PROJECT_NAME, node.getUuid(), "en");
		latchFor(future);
		assertSuccess(future);
		expectResponseMessage(future, "node_deleted_language", node.getUuid(), "en");

		// Loading is still be possible but the node will contain no fields
		Future<NodeResponse> response = getClient().findNodeByUuid(PROJECT_NAME, uuid, new NodeParameters().setLanguages("en"));
		latchFor(response);
		assertSuccess(response);
		assertThat(response.result().getAvailableLanguages()).contains("de");
		assertThat(response.result().getFields()).isEmpty();

		response = getClient().findNodeByUuid(PROJECT_NAME, uuid, new NodeParameters().setLanguages("de"));
		latchFor(response);
		assertSuccess(future);

		// Delete the english version again
		future = getClient().deleteNode(PROJECT_NAME, node.getUuid(), "en");
		latchFor(future);
		expectException(future, NOT_FOUND, "node_no_language_found", "en");

		// Check the deletion
		node.reload();
		assertThat(searchProvider).recordedDeleteEvents(1);
		assertFalse(node.getAvailableLanguageNames().contains("en"));
		assertEquals(nLanguagesBefore - 1, node.getAvailableLanguageNames().size());

		// Now delete the remaining german version
		future = getClient().deleteNode(PROJECT_NAME, node.getUuid(), "de");
		latchFor(future);
		assertThat(searchProvider).recordedDeleteEvents(2);
		response = getClient().findNodeByUuid(PROJECT_NAME, uuid);
		latchFor(response);
		expectException(response, NOT_FOUND, "object_not_found_for_uuid", uuid);

	}

	@Test
	public void testDeleteBogusLanguage() {
		Node node = content();
		Future<GenericMessageResponse> future = getClient().deleteNode(PROJECT_NAME, node.getUuid(), "blub");
		latchFor(future);
		expectException(future, NOT_FOUND, "error_language_not_found", "blub");
	}

	@Test
	public void testDeleteLanguageNoPerm() {
		Node node = content();
		role().revokePermissions(node, DELETE_PERM);
		Future<GenericMessageResponse> future = getClient().deleteNode(PROJECT_NAME, node.getUuid(), "en");
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", node.getUuid());
	}
}
