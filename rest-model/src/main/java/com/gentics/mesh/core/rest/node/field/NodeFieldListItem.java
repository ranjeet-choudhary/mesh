package com.gentics.mesh.core.rest.node.field;

public interface NodeFieldListItem {

	/**
	 * Return the item node uuid.
	 * 
	 * @return Uuid of the node item
	 */
	String getUuid();

	/**
	 * Set the uuid of the node.
	 * 
	 * @param uuid
	 * @return Fluent API
	 */
	NodeFieldListItem setUuid(String uuid);

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
	NodeFieldListItem setPath(String path);
}
