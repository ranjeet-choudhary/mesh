package com.gentics.mesh.core.rest.node.field;

import java.util.Map;

/**
 * A node field is a field which contains a node reference to other nodes.
 */
public interface NodeField extends ListableField, MicroschemaListableField, NodeFieldListItem {

	/**
	 * Return the uuid of the node.
	 * 
	 * @return Uuid of the node
	 */
	String getUuid();

	/**
	 * Set the uuid of the node.
	 * 
	 * @param uuid
	 * @return Fluent API
	 */
	NodeField setUuid(String uuid);

	/**
	 * Get the webroot URL to the node
	 * 
	 * @return webroot URL
	 */
	String getPath();

	/**
	 * Set the webroot path
	 * 
	 * @param path
	 *            webroot path
	 * @return Fluent API
	 */
	NodeField setPath(String path);

	/**
	 * Return the language specific webroot paths to the node.
	 * 
	 * @return
	 */
	Map<String, String> getLanguagePaths();

	/**
	 * Set the language specific webroot paths.
	 * 
	 * @param languagePaths
	 * @return Fluent API
	 */
	NodeField setLanguagePaths(Map<String, String> languagePaths);

}
