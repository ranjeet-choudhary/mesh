package com.gentics.mesh.core.rest.node.field.list.impl;

import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;

public class NodeFieldListItemImpl implements NodeFieldListItem {

	private String uuid;

	private String path;

	public NodeFieldListItemImpl() {
	}

	public NodeFieldListItemImpl(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public NodeFieldListItem setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public NodeFieldListItem setPath(String path) {
		this.path = path;
		return this;
	}
}
