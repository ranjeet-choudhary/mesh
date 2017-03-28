package com.gentics.mesh.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;

/**
 * Deserializer which is used to deserialize node list items.
 * @deprecated We should be able to remove this deserializer
 */
@Deprecated
public class NodeFieldListItemDeserializer extends JsonDeserializer<NodeFieldListItem> {

	@Override
	public NodeFieldListItem deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode jsonNode = oc.readTree(jsonParser);
		return deserialize(jsonNode, jsonParser);
	}

	/**
	 * Deserialize the node field list item.
	 * 
	 * @param jsonNode
	 *            Node which represents the node field list item.
	 * @param jsonParser
	 * @return Deserialized field list item.
	 * @throws JsonProcessingException
	 */
	public NodeFieldListItem deserialize(JsonNode jsonNode, JsonParser jsonParser) throws JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();

		// Fallback and deseralize the element using the collapsed form.
		NodeFieldListItemImpl collapsedItem = oc.treeToValue(jsonNode, NodeFieldListItemImpl.class);
		return collapsedItem;
	}

}
