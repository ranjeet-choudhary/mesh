package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Stack;

import javax.naming.InvalidNameException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.error.NameConflictException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see ProjectRoot
 */
public class ProjectRootImpl extends AbstractRootVertex<Project> implements ProjectRoot {

	private static final Logger log = LoggerFactory.getLogger(ProjectRootImpl.class);

	public static void init(Database database) {
		database.addVertexType(ProjectRootImpl.class, MeshVertexImpl.class);
		database.addEdgeType(HAS_PROJECT);
		database.addEdgeIndex(HAS_PROJECT, true, false, true);
	}

	@Override
	public Class<? extends Project> getPersistanceClass() {
		return ProjectImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_PROJECT;
	}

	@Override
	public void addProject(Project project) {
		addItem(project);
	}

	@Override
	public void removeProject(Project project) {
		removeItem(project);
	}

	@Override
	public Project create(String name, User creator, SchemaContainerVersion schemaContainerVersion) {
		Project project = getGraph().addFramedVertex(ProjectImpl.class);
		project.setName(name);
		project.getNodeRoot();

		// Create the initial release for the project and add the used schema
		// version to it
		Release release = project.getReleaseRoot().create(name, creator).setMigrated(true);
		release.assignSchemaVersion(schemaContainerVersion);

		// Assign the provided schema container to the project
		project.getSchemaContainerRoot().addItem(schemaContainerVersion.getSchemaContainer());
		project.getLatestRelease().assignSchemaVersion(schemaContainerVersion);
		project.createBaseNode(creator, schemaContainerVersion);

		project.setCreated(creator);
		project.setEditor(creator);
		project.getSchemaContainerRoot();
		project.getTagFamilyRoot();

		addItem(project);

		return project;
	}

	@Override
	public MeshVertex resolveToElement(Stack<String> stack) {
		if (stack.isEmpty()) {
			return this;
		} else {
			String uuidSegment = stack.pop();
			Project project = findByUuid(uuidSegment);
			if (project == null) {
				return null;
			}
			if (stack.isEmpty()) {
				return project;
			} else {
				String nestedRootNode = stack.pop();
				switch (nestedRootNode) {
				case ReleaseRoot.TYPE:
					ReleaseRoot releasesRoot = project.getReleaseRoot();
					return releasesRoot.resolveToElement(stack);
				case TagFamilyRoot.TYPE:
					TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
					return tagFamilyRoot.resolveToElement(stack);
				case SchemaContainerRoot.TYPE:
					SchemaContainerRoot schemaRoot = project.getSchemaContainerRoot();
					return schemaRoot.resolveToElement(stack);
				case MicroschemaContainerRoot.TYPE:
					MicroschemaContainerRoot microschemaRoot = project.getMicroschemaContainerRoot();
					return microschemaRoot.resolveToElement(stack);
				case NodeRoot.TYPE:
					NodeRoot nodeRoot = project.getNodeRoot();
					return nodeRoot.resolveToElement(stack);
				default:
					throw error(NOT_FOUND, "Unknown project element {" + nestedRootNode + "}");
				}
			}
		}

	}

	@Override
	public void delete(SearchQueueBatch batch) {
		throw new NotImplementedException("The project root should never be deleted.");
	}

	@Override
	public Project create(InternalActionContext ac, SearchQueueBatch batch) {
		RouterStorage routerStorage = RouterStorage.getIntance();
		BootstrapInitializer boot = MeshInternal.get().boot();

		// TODO also create a default object schema for the project. Move this
		// into service class
		// ObjectSchema defaultContentSchema = objectSchemaRoot.findByName(,
		// name)
		ProjectCreateRequest requestModel = ac.fromJson(ProjectCreateRequest.class);
		String projectName = requestModel.getName();
		MeshAuthUser creator = ac.getUser();

		if (StringUtils.isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "project_missing_name");
		}
		if (!creator.hasPermission(boot.projectRoot(), CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", boot.projectRoot().getUuid());
		}
		// TODO instead of this check, a constraint in the db should be added
		Project conflictingProject = boot.projectRoot().findByName(requestModel.getName());
		if (conflictingProject != null) {
			throw new NameConflictException("project_conflicting_name", projectName, conflictingProject.getUuid());
		}
		if (routerStorage.getCoreRouters().containsKey(requestModel.getName())) {
			throw error(BAD_REQUEST, "project_error_name_already_reserved", requestModel.getName());
		}

		if (requestModel.getSchema() == null || !requestModel.getSchema().isSet()) {
			throw error(BAD_REQUEST, "project_error_no_schema_reference");
		}
		SchemaContainerVersion schemaContainerVersion = MeshInternal.get().boot().schemaContainerRoot().fromReference(requestModel.getSchema());

		Project project = create(projectName, creator, schemaContainerVersion);
		Release initialRelease = project.getInitialRelease();

		// Add project permissions
		creator.addCRUDPermissionOnRole(this, CREATE_PERM, project);
		creator.addCRUDPermissionOnRole(this, CREATE_PERM, project.getBaseNode());
		creator.addPermissionsOnRole(this, CREATE_PERM, project.getBaseNode(), READ_PUBLISHED_PERM, PUBLISH_PERM);
		creator.addCRUDPermissionOnRole(this, CREATE_PERM, project.getTagFamilyRoot());
		creator.addCRUDPermissionOnRole(this, CREATE_PERM, project.getSchemaContainerRoot());
		// TODO add microschema root crud perms
		creator.addCRUDPermissionOnRole(this, CREATE_PERM, project.getNodeRoot());
		creator.addPermissionsOnRole(this, CREATE_PERM, initialRelease);

		// Store the project in the index
		batch.store(project, true);

		String releaseUuid = initialRelease.getUuid();
		String projectUuid = project.getUuid();

		// 1. Create needed indices
		batch.createNodeIndex(projectUuid, releaseUuid, schemaContainerVersion.getUuid(), DRAFT);
		batch.createNodeIndex(projectUuid, releaseUuid, schemaContainerVersion.getUuid(), PUBLISHED);
		batch.createTagIndex(projectUuid);
		batch.createTagFamilyIndex(projectUuid);

		// 3. Add created basenode to SQB
		NodeGraphFieldContainer baseNodeFieldContainer = project.getBaseNode().getAllInitialGraphFieldContainers().iterator().next();
		batch.store(project.getBaseNode(), releaseUuid, ContainerType.DRAFT, false);

		try {
			// TODO BUG project should only be added to router when tx and ES
			// finished successfully
			routerStorage.addProjectRouter(projectName);
			if (log.isInfoEnabled()) {
				log.info("Registered project {" + projectName + "}");
			}
		} catch (InvalidNameException e) {
			// TODO should we really fail here?
			throw error(BAD_REQUEST, "Error while adding project to router storage", e);
		}

		return project;

	}

}
