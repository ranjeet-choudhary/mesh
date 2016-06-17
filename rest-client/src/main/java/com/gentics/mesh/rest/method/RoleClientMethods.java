package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;

import io.vertx.core.Future;

public interface RoleClientMethods {

	/**
	 * Load the role.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	Future<RoleResponse> findRoleByUuid(String uuid, ParameterProvider... parameters);

	/**
	 * Load multiple roles.
	 * 
	 * @param parameter
	 * @return
	 */
	Future<RoleListResponse> findRoles(ParameterProvider... parameter);

	/**
	 * Create a new role.
	 * 
	 * @param request
	 * @return
	 */
	Future<RoleResponse> createRole(RoleCreateRequest request);

	/**
	 * Delete the role.
	 * 
	 * @param uuid
	 * @return
	 */
	Future<GenericMessageResponse> deleteRole(String uuid);

	/**
	 * Load multiple roles that were assigned to the given group.
	 * 
	 * @param groupUuid
	 * @param parameter
	 * @return
	 */
	Future<RoleListResponse> findRolesForGroup(String groupUuid, ParameterProvider... parameter);

	/**
	 * Update the role permissions for the the given path.
	 * 
	 * @param roleUuid
	 *            Role to which the permissions should be updated
	 * @param pathToElement
	 *            Path to an element or an aggregation element
	 * @param request
	 *            Request that defines how the permissions should be changed
	 * @return
	 */
	Future<GenericMessageResponse> updateRolePermissions(String roleUuid, String pathToElement, RolePermissionRequest request);

	/**
	 * Read the role permissions for the given path.
	 * 
	 * @param roleUuid
	 * @param pathToElement
	 * @return
	 */
	Future<RolePermissionResponse> readRolePermissions(String roleUuid, String pathToElement);

	/**
	 * Update the role using the given update request.
	 * 
	 * @param uuid
	 * @param restRole
	 * @return
	 */
	Future<RoleResponse> updateRole(String uuid, RoleUpdateRequest restRole);
}
