package com.gentics.mesh.search.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.ElasticSearchProvider;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public abstract class AbstractIndexHandler<T extends GenericVertex<?>> {

	private static final Logger log = LoggerFactory.getLogger(AbstractIndexHandler.class);

	//	public static final String INDEX_EVENT_ADDRESS_PREFIX = "search-index-action-";

	@Autowired
	protected ElasticSearchProvider elasticSearchProvider;

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	protected Database db;

	abstract protected String getType();

	abstract protected String getIndex();

	abstract protected RootVertex<T> getRootVertex();

	abstract protected Map<String, Object> transformToDocumentMap(T object);

	public void update(T object, Handler<AsyncResult<ActionResponse>> handler) {
		updateDocument(object.getUuid(), transformToDocumentMap(object), getType(), handler);
	}

	public void update(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		getRootVertex().findByUuid(uuid, rh -> {
			if (rh.failed()) {
				handler.handle(Future.failedFuture(rh.cause()));
			} else if (rh.result() == null) {
				handler.handle(Future.failedFuture("Element {" + uuid + "} for index type {" + getType() + "} could not be found within graph."));
			} else {
				update(rh.result(), handler);
			}
		});
	}

	public void store(T object, Handler<AsyncResult<ActionResponse>> handler) {
		storeDocument(object.getUuid(), transformToDocumentMap(object), getType(), handler);
	}

	/**
	 * Load the given element and invoke store(T element) to store it in the index.
	 */
	public void store(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		getRootVertex().findByUuid(uuid, rh -> {
			if (rh.failed()) {
				handler.handle(Future.failedFuture(rh.cause()));
			} else if (rh.result() == null) {
				handler.handle(Future.failedFuture("Element {" + uuid + "} for index type {" + getType() + "} could not be found within graph."));
			} else {
				store(rh.result(), handler);
			}
		});
	}

	protected boolean isSearchClientAvailable() {
		return elasticSearchProvider != null;
	}

	protected Client getSearchClient() {
		if (elasticSearchProvider == null) {
			log.error("No search provider found.");
			return null;
		} else {
			return elasticSearchProvider.getNode().client();
		}
	}

	public void deleteDocument(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		getSearchClient().prepareDelete(getIndex(), getType(), uuid).execute().addListener(new ActionListener<DeleteResponse>() {

			@Override
			public void onResponse(DeleteResponse response) {
				if (log.isDebugEnabled()) {
					log.debug("Deleted object {" + uuid + ":" + getType() + "} from index {" + getIndex() + "}");
				}
				handler.handle(Future.succeededFuture(response));
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("Could not delete object {" + uuid + ":" + getType() + "} from index {" + getIndex() + "}");
				handler.handle(Future.failedFuture(e));
			}
		});
	}

	protected void updateDocument(String uuid, Map<String, Object> map, String type, Handler<AsyncResult<ActionResponse>> handler) {
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Updating object {" + uuid + ":" + type + "} to index.");
		}
		UpdateRequestBuilder builder = getSearchClient().prepareUpdate(getIndex(), type, uuid);
		builder.setDoc(map);
		builder.execute().addListener(new ActionListener<UpdateResponse>() {

			@Override
			public void onResponse(UpdateResponse response) {
				if (log.isDebugEnabled()) {
					log.debug("Update object {" + uuid + ":" + type + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
				handler.handle(Future.succeededFuture(response));
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("Updating object {" + uuid + ":" + type + "} to index failed. Duration " + (System.currentTimeMillis() - start) + "[ms]",
						e);
				handler.handle(Future.failedFuture(e));
			}
		});

	}

	protected void storeDocument(String uuid, Map<String, Object> map, String type, Handler<AsyncResult<ActionResponse>> handler) {
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Adding object {" + uuid + ":" + type + "} to index.");
		}
		IndexRequestBuilder builder = getSearchClient().prepareIndex(getIndex(), type, uuid);
		builder.setSource(map);
		builder.execute().addListener(new ActionListener<IndexResponse>() {

			@Override
			public void onResponse(IndexResponse response) {
				if (log.isDebugEnabled()) {
					log.debug("Added object {" + uuid + ":" + type + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
				handler.handle(Future.succeededFuture(response));
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("Adding object {" + uuid + ":" + type + "} to index failed. Duration " + (System.currentTimeMillis() - start) + "[ms]", e);
				handler.handle(Future.failedFuture(e));
			}
		});

	}

	protected void addBasicReferences(Map<String, Object> map, GenericVertex<?> vertex) {
		// TODO make sure field names match node response
		map.put("uuid", vertex.getUuid());
		addUser(map, "creator", vertex.getCreator());
		addUser(map, "editor", vertex.getEditor());
		map.put("lastEdited", vertex.getLastEditedTimestamp());
		map.put("created", vertex.getCreationTimestamp());
	}

	protected void addUser(Map<String, Object> map, String prefix, User user) {
		// TODO make sure field names match response UserResponse field names..
		Map<String, Object> userFields = new HashMap<>();
		userFields.put("username", user.getUsername());
		userFields.put("emailadress", user.getEmailAddress());
		userFields.put("firstname", user.getFirstname());
		userFields.put("lastname", user.getLastname());
		userFields.put("enabled", String.valueOf(user.isEnabled()));
		map.put(prefix, userFields);
	}

	protected void addTags(Map<String, Object> map, List<? extends Tag> tags) {
		List<String> tagUuids = new ArrayList<>();
		List<String> tagNames = new ArrayList<>();
		for (Tag tag : tags) {
			tagUuids.add(tag.getUuid());
			tagNames.add(tag.getName());
		}
		Map<String, List<String>> tagFields = new HashMap<>();
		tagFields.put("uuid", tagUuids);
		tagFields.put("name", tagNames);
		map.put("tags", tagFields);
	}

	public void handleAction(String uuid, String actionName, Handler<AsyncResult<ActionResponse>> handler) {
		if (!isSearchClientAvailable()) {
			log.error("Search client has not been initalized. It can't be used. Omitting search index handling!");
			handler.handle(Future.succeededFuture());
			return;
		}
		SearchQueueEntryAction action = SearchQueueEntryAction.valueOfName(actionName);
		try (Trx tx = db.trx()) {
			switch (action) {
			case CREATE_ACTION:
				store(uuid, handler);
				break;
			case DELETE_ACTION:
				// We don't need to resolve the uuid and load the graph object in this case.
				deleteDocument(uuid, handler);
				break;
			case UPDATE_ACTION:
				//update(uuid, handler);
				store(uuid, handler);
				break;
			default:
				handler.handle(Future.failedFuture("Action type {" + action + "} is unknown."));
			}
		}
	}

}
