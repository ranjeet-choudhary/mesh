package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RoutingContextHelper;

public class TagTest extends AbstractBasicObjectTest {

	private static Logger log = LoggerFactory.getLogger(TagTest.class);

	private TagRoot tagRoot;

	public static final String GERMAN_NAME = "test german name";

	public static final String ENGLISH_NAME = "test english name";

	@Before
	public void setup() throws Exception {
		super.setup();
		tagRoot = boot.tagRoot();
	}

	@Test
	public void testTagFamilyTagCreation() {
		final String TAG_FAMILY_NAME = "mycustomtagFamily";
		TagFamily tagFamily = data().getProject().getTagFamilyRoot().create(TAG_FAMILY_NAME);
		assertNotNull(tagFamily);
		assertEquals(TAG_FAMILY_NAME, tagFamily.getName());
		assertNull(tagFamily.getDescription());
		tagFamily.setDescription("description");
		assertEquals("description", tagFamily.getDescription());
		assertEquals(0, tagFamily.getTags().size());
		Tag tag = tagFamily.create(GERMAN_NAME);
		assertEquals(1, tagFamily.getTags().size());
	}

	@Test
	public void testSimpleTag() {
		TagFamily root = data().getTagFamily("basic");
		Tag tag = root.create("test");
		assertEquals("test", tag.getName());
		tag.setName("test2");
		assertEquals("test2", tag.getName());
	}

	@Test
	public void testNodeTaggging() {

		// 1. Create the tag
		TagFamily root = data().getTagFamily("basic");
		Tag tag = root.create(ENGLISH_NAME);
		String uuid = tag.getUuid();
		tagRoot.findByUuid(uuid, rh -> {
			assertNotNull(rh.result());
		});

		// 2. Create the node
		final String GERMAN_TEST_FILENAME = "german.html";
		Node parentNode = data().getFolder("2015");
		Node node = parentNode.create();
		Language german = boot.languageRoot().findByLanguageTag("de");
		NodeFieldContainer germanContainer = node.getOrCreateFieldContainer(german);

		germanContainer.createString("displayName").setString(GERMAN_TEST_FILENAME);
		germanContainer.createString("name").setString("german node name");

		// 3. Assign the tag to the node
		node.addTag(tag);

		// 4. Reload the tag and inspect the tagged nodes
		tagRoot.findByUuid(tag.getUuid(), rh -> {
			Tag reloadedTag = rh.result();
			assertEquals("The tag should have exactly one node.", 1, reloadedTag.getNodes().size());
			Node contentFromTag = reloadedTag.getNodes().iterator().next();
			NodeFieldContainer fieldContainer = contentFromTag.getFieldContainer(german);

			assertNotNull(contentFromTag);
			assertEquals("We did not get the correct content.", node.getUuid(), contentFromTag.getUuid());
			String filename = fieldContainer.getString("displayName").getString();
			assertEquals("The name of the file from the loaded tag did not match the expected one.", GERMAN_TEST_FILENAME, filename);

			// Remove the file/content and check whether the content was really removed
				reloadedTag.removeNode(contentFromTag);
				// TODO verify for removed node
				assertEquals("The tag should not have any file.", 0, reloadedTag.getNodes().size());
			});

	}

	@Test
	public void testNodeTagging() {
		final String TEST_TAG_NAME = "testTag";
		TagFamily tagFamily = data().getTagFamily("basic");
		Tag tag = tagFamily.create(TEST_TAG_NAME);

		Node node = data().getFolder("news");
		node.addTag(tag);

		boot.nodeRoot().findByUuid(node.getUuid(), rh -> {
			Node reloadedNode = rh.result();
			boolean found = false;
			for (Tag currentTag : reloadedNode.getTags()) {
				if (currentTag.getUuid().equals(tag.getUuid())) {
					found = true;
				}
			}
			assertTrue("The tag {" + tag.getUuid() + "} was not found within the node tags.", found);
		});

	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = RoutingContextHelper.getUser(rc);

		Page<? extends Tag> tagPage = tagRoot.findAll(requestUser, new PagingInfo(1, 10));
		assertEquals(12, tagPage.getTotalElements());
		assertEquals(10, tagPage.getSize());

		tagPage = tagRoot.findAll(requestUser, new PagingInfo(1, 14));
		assertEquals(data().getTags().size(), tagPage.getTotalElements());
		assertEquals(12, tagPage.getSize());
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		Page<? extends Tag> page = tagRoot.findAll(getRequestUser(), new PagingInfo(1, 20));
		assertNotNull(page);

		int nTags = 0;
		for (Tag tag : page) {
			assertNotNull(tag.getName());
			nTags++;
		}
		assertEquals(data().getTags().size(), nTags);
		assertEquals(data().getTags().size(), page.getTotalElements());
		assertEquals(1, page.getNumber());
		assertEquals(1, page.getTotalPages());
	}

	@Test
	@Override
	public void testRootNode() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindByName() {
		Tag tag = data().getTag("car");
		Tag foundTag = tagRoot.findByName("Car");
		assertNotNull(foundTag);
		assertEquals("Car", foundTag.getName());
		assertNotNull(tagRoot.findByName("dummy", tag.getName()));
		assertNull(tagRoot.findByName("bogus", tag.getName()));
		assertNull(tagRoot.findByName("dummy", "bogus"));
		assertNull(tagRoot.findByName("bogus", "bogus"));
	}

	@Test
	@Override
	public void testFindByUUID() {
		Tag tag = data().getTag("car");
		tagRoot.findByUuid(tag.getUuid(), rh -> {
			assertNotNull(rh.result());
		});
		tagRoot.findByUuid("bogus", rh -> {
			assertNull(rh.result());
		});
	}

	@Test
	@Override
	public void testCreate() {
		TagFamily tagFamily = data().getTagFamily("basic");
		Tag tag = tagFamily.create(GERMAN_NAME);
		assertNotNull(tag);
		String uuid = tag.getUuid();
		tagRoot.findByUuid(uuid, rh -> {
			Tag loadedTag = rh.result();
			assertNotNull("The folder could not be found.", loadedTag);
			String name = loadedTag.getName();
			assertEquals("The loaded name of the folder did not match the expected one.", GERMAN_NAME, name);
			assertEquals(10, tagFamily.getTags().size());
		});
	}

	@Test
	@Override
	public void testTransformation() {
		Tag tag = data().getTag("red");
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
		List<String> languageTags = new ArrayList<>();
		languageTags.add("en");
		languageTags.add("de");
		int depth = 3;

		RoutingContext rc = getMockedRoutingContext("lang=de,en");
		MeshAuthUser requestUser = RoutingContextHelper.getUser(rc);
		for (int i = 0; i < 100; i++) {
			long start = System.currentTimeMillis();
			tag.transformToRest(requestUser, th -> {
				if (th.failed()) {
					rc.fail(th.cause());
				}
				TagResponse response = th.result();
				assertNotNull(response);
				long dur = System.currentTimeMillis() - start;
				log.info("Transformation with depth {" + depth + "} took {" + dur + "} [ms]");
				JsonUtil.toJson(response);
			});
		}
		// assertEquals(2, response.getChildTags().size());
		// assertEquals(4, response.getPerms().length);

	}

	@Test
	@Override
	public void testCreateDelete() {
		TagFamily tagFamily = data().getTagFamily("basic");
		Tag tag = tagFamily.create("someTag");
		String uuid = tag.getUuid();
		tagRoot.findByUuid(uuid, rh -> {
			assertNotNull(rh.result());
			tag.delete();
			tagRoot.findByUuid(uuid, rh2 -> {
				assertNull(rh2.result());
			});
		});
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		TagFamily tagFamily = data().getTagFamily("basic");
		Tag tag = tagFamily.create("someTag");
		assertTrue(getUser().hasPermission(tagFamily, Permission.READ_PERM));
		assertFalse(getUser().hasPermission(tag, Permission.READ_PERM));
		getRequestUser().addCRUDPermissionOnRole(tagFamily, Permission.CREATE_PERM, tag);
		assertTrue(getUser().hasPermission(tag, Permission.READ_PERM));
	}

	@Test
	@Override
	public void testRead() {
		Tag tag = data().getTag("car");

		assertEquals("Car", tag.getName());
		assertNotNull(tag.getCreationTimestamp());
		assertNotNull(tag.getLastEditedTimestamp());
		assertNotNull(tag.getEditor());
		assertNotNull(tag.getCreator());
		assertNotNull(tag.getTagFamily());

	}

	@Test
	@Override
	public void testDelete() {
		Tag tag = data().getTag("red");
		String uuid = tag.getUuid();
		tag.remove();
		tagRoot.findByUuid(uuid, rh -> {
			assertNull(rh.result());
		});
	}

	@Test
	@Override
	public void testUpdate() {
		Tag tag = data().getTag("red");
		tag.setName("Blue");
		assertEquals("Blue", tag.getName());
	}

	@Test
	@Override
	public void testReadPermission() {
		Tag tag = data().getTag("red");
		testPermission(Permission.READ_PERM, tag);
	}

	@Test
	@Override
	public void testDeletePermission() {
		Tag tag = data().getTag("red");
		testPermission(Permission.DELETE_PERM, tag);
	}

	@Test
	@Override
	public void testUpdatePermission() {
		Tag tag = data().getTag("red");
		testPermission(Permission.UPDATE_PERM, tag);
	}

	@Test
	@Override
	public void testCreatePermission() {
		Tag tag = data().getTag("red");
		testPermission(Permission.CREATE_PERM, tag);
	}

}
