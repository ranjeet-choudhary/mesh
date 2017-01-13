package com.gentics.mesh.search.index.schema;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateBatchEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;

/**
 * Handler for schema container index.
 */
@Singleton
public class SchemaContainerIndexHandler extends AbstractIndexHandler<SchemaContainer> {

	@Inject
	SchemaTransformator transformator;

	@Inject
	public SchemaContainerIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateBatchEntry entry) {
		return SchemaContainer.composeDocumentId(entry.getElementUuid());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateBatchEntry entry) {
		return SchemaContainer.composeIndexName();
	}

	@Override
	protected String composeIndexTypeFromEntry(UpdateBatchEntry entry) {
		return SchemaContainer.composeIndexType();
	}

	@Override
	public Class<SchemaContainer> getElementClass() {
		return SchemaContainer.class;
	}

	@Override
	public SchemaTransformator getTransformator() {
		return transformator;
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(SchemaContainer.TYPE.toLowerCase());
	}

	@Override
	public Map<String, String> getIndices() {
		return Collections.singletonMap(SchemaContainer.TYPE.toLowerCase(), SchemaContainer.TYPE.toLowerCase());
	}

	@Override
	protected RootVertex<SchemaContainer> getRootVertex() {
		return boot.meshRoot().getSchemaContainerRoot();
	}

}
