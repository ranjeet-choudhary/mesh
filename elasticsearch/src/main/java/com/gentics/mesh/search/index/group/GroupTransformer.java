package com.gentics.mesh.search.index.group;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.search.index.MappingHelper;
import com.gentics.mesh.util.ETag;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for group search index documents.
 */
@Singleton
public class GroupTransformer extends AbstractTransformer<Group> {

	@Inject
	public GroupTransformer() {
	}

	public String generateVersion(Group group) {
		// No need to add users since the creator/editor edge affects the group version
		return ETag.hash(group.getElementVersion());
	}

	private JsonObject toDocument(Group group, boolean withVersion) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, group.getName());
		addBasicReferences(document, group);
		addPermissionInfo(document, group);
		if (withVersion) {
			document.put(MappingHelper.VERSION_KEY, generateVersion(group));
		}
		return document;
	}

	@Override
	public JsonObject toDocument(Group group) {
		return toDocument(group, true);
	}

}
