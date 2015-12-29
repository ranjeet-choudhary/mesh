package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.nesting.MicroschemaGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicroschemaGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.MicroschemaFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicroschemaFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.InternalActionContext;
import com.syncleus.ferma.traversals.EdgeTraversal;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeGraphFieldContainerImpl extends AbstractGraphFieldContainerImpl implements NodeGraphFieldContainer {

	private static final Logger log = LoggerFactory.getLogger(NodeGraphFieldContainerImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(NodeGraphFieldContainerImpl.class);
	}

	private void failOnMissingMandatoryField(ActionContext ac, GraphField field, Field restField, FieldSchema fieldSchema, String key, Schema schema)
			throws MeshSchemaException {
		if (field == null && fieldSchema.isRequired() && restField == null) {
			throw new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("node_error_missing_mandatory_field_value", key, schema.getName()));
		}
	}

	@Override
	public String getDisplayFieldValue(Schema schema) {
		String displayFieldName = schema.getDisplayField();
		StringGraphField field = getString(displayFieldName);
		if (field != null) {
			return field.getString();
		}
		return null;
	}

	@Override
	public void updateFieldsFromRest(ActionContext ac, Map<String, Field> restFields, Schema schema) throws MeshSchemaException {

		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		// Initially all fields are not yet handled
		List<String> unhandledFieldKeys = new ArrayList<>(restFields.size());
		unhandledFieldKeys.addAll(restFields.keySet());

		// Iterate over all known field that are listed in the schema for the node
		for (FieldSchema entry : schema.getFields()) {
			String key = entry.getName();
			Field restField = restFields.get(key);

			unhandledFieldKeys.remove(key);

			FieldTypes type = FieldTypes.valueByName(entry.getType());
			switch (type) {
			case HTML:
				HtmlGraphField htmlGraphField = getHtml(key);
				failOnMissingMandatoryField(ac, htmlGraphField, restField, entry, key, schema);
				HtmlField htmlField = (HtmlFieldImpl) restField;
				if (restField == null) {
					continue;
				}

				// Create new graph field if no existing one could be found
				if (htmlGraphField == null) {
					createHTML(key).setHtml(htmlField.getHTML());
				} else {
					htmlGraphField.setHtml(htmlField.getHTML());
				}
				break;
			case STRING:
				StringGraphField graphStringField = getString(key);
				failOnMissingMandatoryField(ac, graphStringField, restField, entry, key, schema);
				StringField stringField = (StringFieldImpl) restField;
				if (restField == null) {
					continue;
				}
				// Create new graph field if no existing one could be found
				if (graphStringField == null) {
					createString(key).setString(stringField.getString());
				} else {
					graphStringField.setString(stringField.getString());
				}
				break;
			case NUMBER:
				NumberGraphField numberGraphField = getNumber(key);
				failOnMissingMandatoryField(ac, numberGraphField, restField, entry, key, schema);
				NumberField numberField = (NumberFieldImpl) restField;
				if (restField == null) {
					continue;
				}
				if (numberGraphField == null) {
					createNumber(key).setNumber(numberField.getNumber());
				} else {
					numberGraphField.setNumber(numberField.getNumber());
				}
				break;
			case BOOLEAN:
				BooleanGraphField booleanGraphField = getBoolean(key);
				failOnMissingMandatoryField(ac, booleanGraphField, restField, entry, key, schema);
				BooleanField booleanField = (BooleanFieldImpl) restField;
				if (restField == null) {
					continue;
				}
				if (booleanGraphField == null) {
					createBoolean(key).setBoolean(booleanField.getValue());
				} else {
					booleanGraphField.setBoolean(booleanField.getValue());
				}
				break;
			case DATE:
				DateGraphField dateGraphField = getDate(key);
				failOnMissingMandatoryField(ac, dateGraphField, restField, entry, key, schema);
				DateField dateField = (DateFieldImpl) restField;
				if (restField == null) {
					continue;
				}
				if (dateGraphField == null) {
					createDate(key).setDate(dateField.getDate());
				} else {
					dateGraphField.setDate(dateField.getDate());
				}
				break;
			case NODE:
				NodeGraphField graphNodeField = getNode(key);
				failOnMissingMandatoryField(ac, graphNodeField, restField, entry, key, schema);
				NodeField nodeField = (NodeField) restField;
				if (restField == null) {
					continue;
				}
				BootstrapInitializer.getBoot().nodeRoot().findByUuid(nodeField.getUuid(), rh -> {
					Node node = rh.result();
					if (node == null) {
						//TODO We want to delete the field when the field has been explicitly set to null
						if (log.isDebugEnabled()) {
							log.debug("Node field {" + key + "} could not be populated since node {" + nodeField.getUuid() + "} could not be found.");
						}
						// TODO we need to fail here - the node could not be found.
						// throw new HttpStatusCodeErrorException(NOT_FOUND, ac.i18n("The field {, parameters))
					} else {
						// Check whether the container already contains a node field
						// TODO check node permissions
						if (graphNodeField == null) {
							createNode(key, node);
						} else {
							// We can't update the graphNodeField since it is in
							// fact an edge. We need to delete it and create a new
							// one.
							deleteField(key);
							createNode(key, node);
						}
					}
				});
				break;
			case LIST:

				if (restField instanceof NodeFieldListImpl) {
					NodeGraphFieldList graphNodeFieldList = getNodeList(key);
					failOnMissingMandatoryField(ac, graphNodeFieldList, restField, entry, key, schema);
					NodeFieldListImpl nodeList = (NodeFieldListImpl) restField;

					if (graphNodeFieldList == null) {
						graphNodeFieldList = createNodeList(key);
					} else {
						graphNodeFieldList.removeAll();
					}

					// Add the listed items
					AtomicInteger integer = new AtomicInteger();
					for (NodeFieldListItem item : nodeList.getItems()) {
						Node node = boot.nodeRoot().findByUuidBlocking(item.getUuid());
						graphNodeFieldList.createNode(String.valueOf(integer.incrementAndGet()), node);
					}
				} else if (restField instanceof StringFieldListImpl) {
					StringGraphFieldList graphStringList = getStringList(key);
					failOnMissingMandatoryField(ac, graphStringList, restField, entry, key, schema);
					StringFieldListImpl stringList = (StringFieldListImpl) restField;

					if (graphStringList == null) {
						graphStringList = createStringList(key);
					} else {
						graphStringList.removeAll();
					}
					for (String item : stringList.getItems()) {
						graphStringList.createString(item);
					}

				} else if (restField instanceof HtmlFieldListImpl) {
					HtmlGraphFieldList graphHtmlFieldList = getHTMLList(key);
					failOnMissingMandatoryField(ac, graphHtmlFieldList, restField, entry, key, schema);
					HtmlFieldListImpl htmlList = (HtmlFieldListImpl) restField;

					if (graphHtmlFieldList == null) {
						graphHtmlFieldList = createHTMLList(key);
					} else {
						graphHtmlFieldList.removeAll();
					}
					for (String item : htmlList.getItems()) {
						graphHtmlFieldList.createHTML(item);
					}
				} else if (restField instanceof NumberFieldListImpl) {
					NumberGraphFieldList graphNumberFieldList = getNumberList(key);
					failOnMissingMandatoryField(ac, graphNumberFieldList, restField, entry, key, schema);
					NumberFieldListImpl numberList = (NumberFieldListImpl) restField;

					if (graphNumberFieldList == null) {
						graphNumberFieldList = createNumberList(key);
					} else {
						graphNumberFieldList.removeAll();
					}
					for (Number item : numberList.getItems()) {
						graphNumberFieldList.createNumber(item);
					}
				} else if (restField instanceof BooleanFieldListImpl) {
					BooleanGraphFieldList graphBooleanFieldList = getBooleanList(key);
					failOnMissingMandatoryField(ac, graphBooleanFieldList, restField, entry, key, schema);
					BooleanFieldListImpl booleanList = (BooleanFieldListImpl) restField;

					if (graphBooleanFieldList == null) {
						graphBooleanFieldList = createBooleanList(key);
					} else {
						graphBooleanFieldList.removeAll();
					}
					for (Boolean item : booleanList.getItems()) {
						graphBooleanFieldList.createBoolean(item);
					}
				} else if (restField instanceof DateFieldListImpl) {

					DateGraphFieldList graphDateFieldList = getDateList(key);
					failOnMissingMandatoryField(ac, graphDateFieldList, restField, entry, key, schema);
					DateFieldListImpl dateList = (DateFieldListImpl) restField;

					// Create new list if no existing one could be found
					if (graphDateFieldList == null) {
						graphDateFieldList = createDateList(key);
					} else {
						graphDateFieldList.removeAll();
					}
					for (Long item : dateList.getItems()) {
						graphDateFieldList.createDate(item);
					}
				} else if (restField instanceof MicroschemaFieldListImpl) {
					throw new NotImplementedException();
				} else {
					if (restField == null) {
						continue;
					} else {
						throw new NotImplementedException();
					}
				}
				break;
			case SELECT:
				// SelectField restSelectField = (SelectFieldImpl) restField;
				// com.gentics.mesh.core.data.node.field.nesting.SelectGraphField<ListableGraphField> selectField = createSelect(key);
				// TODO impl
				throw new NotImplementedException();
				// break;
			case MICROSCHEMA:
				// com.gentics.mesh.core.rest.node.field.MicroschemaField restMicroschemaField =
				// (com.gentics.mesh.core.rest.node.field.impl.MicroschemaFieldImpl) restField;
				// MicroschemaGraphField microschemaField = createMicroschema(key);
				// TODO impl
				throw new NotImplementedException();
				// break;
			}

		}

		// Some fields were specified within the json but were not specified in the schema. Those fields can't be handled. We throw an error to inform the user about this.
		String extraFields = "";
		for (String key : unhandledFieldKeys) {
			extraFields += "[" + key + "]";
		}
		if (!StringUtils.isEmpty(extraFields)) {
			throw error(BAD_REQUEST, "node_unhandled_fields", schema.getName(), extraFields);
		}

	}

	private void deleteField(String key) {
		EdgeTraversal<?, ?, ?> traversal = outE(HAS_FIELD).has(com.gentics.mesh.core.data.node.field.GraphField.FIELD_KEY_PROPERTY_KEY, key);
		if (traversal.hasNext()) {
			traversal.next().remove();
		}
	}

	@Override
	public MicroschemaGraphField getMicroschema(String key) {
		throw new NotImplementedException();
	}

	@Override
	public MicroschemaGraphField createMicroschema(String key) {
		MicroschemaGraphFieldImpl field = getGraph().addFramedVertex(MicroschemaGraphFieldImpl.class);
		field.setFieldKey(key);
		linkOut(field, GraphRelationships.HAS_FIELD);
		return field;
	}

	private static <T extends Field> Handler<AsyncResult<T>> wrap(Handler<AsyncResult<Field>> handler) {
		Handler<AsyncResult<T>> returnHandler = ele -> {
			if (ele.failed()) {
				handler.handle(Future.failedFuture(ele.cause()));
			} else {
				handler.handle(Future.succeededFuture(ele.result()));
			}
		};
		return returnHandler;
	}

	@Override
	public void getRestFieldFromGraph(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, boolean expandField,
			Handler<AsyncResult<Field>> handler) {

		Database db = MeshSpringConfiguration.getInstance().database();
		//		db.asyncNoTrx(noTrx -> {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case STRING:
			// TODO validate found fields has same type as schema
			// StringGraphField graphStringField = new com.gentics.mesh.core.data.node.field.impl.basic.StringGraphFieldImpl(
			// fieldKey, this);
			StringGraphField graphStringField = getString(fieldKey);
			if (graphStringField == null) {
				handler.handle(Future.succeededFuture(new StringFieldImpl()));
				return;
			} else {
				graphStringField.transformToRest(ac, th -> {
					if (th.failed()) {
						handler.handle(Future.failedFuture(th.cause()));
						return;
					} else {
						StringField stringField = th.result();
						if (ac.getResolveLinksFlag()) {
							stringField.setString(WebRootLinkReplacer.getInstance().replace(stringField.getString()));
						}
						handler.handle(Future.succeededFuture(stringField));
						return;
					}
				});
				return;
			}
		case NUMBER:
			NumberGraphField graphNumberField = getNumber(fieldKey);
			if (graphNumberField == null) {
				handler.handle(Future.succeededFuture(new NumberFieldImpl()));
				return;
			} else {
				graphNumberField.transformToRest(ac, wrap(handler));
				return;
			}

		case DATE:
			DateGraphField graphDateField = getDate(fieldKey);
			if (graphDateField == null) {
				handler.handle(Future.succeededFuture(new DateFieldImpl()));
				return;
			} else {
				graphDateField.transformToRest(ac, wrap(handler));
				return;
			}
		case BOOLEAN:
			BooleanGraphField graphBooleanField = getBoolean(fieldKey);
			if (graphBooleanField == null) {
				handler.handle(Future.succeededFuture(new BooleanFieldImpl()));
				return;
			} else {
				graphBooleanField.transformToRest(ac, wrap(handler));
				return;
			}
		case NODE:
			NodeGraphField graphNodeField = getNode(fieldKey);
			if (graphNodeField == null) {
				handler.handle(Future.succeededFuture(new NodeFieldImpl()));
				return;
			} else {
				graphNodeField.transformToRest(ac, fieldKey, handler);
				return;
			}
		case HTML:
			HtmlGraphField graphHtmlField = getHtml(fieldKey);
			if (graphHtmlField == null) {
				handler.handle(Future.succeededFuture(new HtmlFieldImpl()));
				return;
			} else {
				graphHtmlField.transformToRest(ac,  rhRest -> {
					if (rhRest.failed()) {
						handler.handle(Future.failedFuture(rhRest.cause()));
					} else {
						// If needed resolve links within the html 
						HtmlField field = rhRest.result();
						if (ac.getResolveLinksFlag()) {
							field.setHTML(WebRootLinkReplacer.getInstance().replace(field.getHTML()));
						}
						handler.handle(Future.succeededFuture(field));
					}
				});
				return;
			}
		case LIST:
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;

			switch (listFieldSchema.getListType()) {
			case NodeGraphFieldList.TYPE:
				NodeGraphFieldList nodeFieldList = getNodeList(fieldKey);
				if (nodeFieldList == null) {
					handler.handle(Future.succeededFuture(new NodeFieldListImpl()));
					return;
				} else {
					nodeFieldList.transformToRest(ac, fieldKey, wrap(handler));
					return;
				}
			case NumberGraphFieldList.TYPE:
				NumberGraphFieldList numberFieldList = getNumberList(fieldKey);
				if (numberFieldList == null) {
					handler.handle(Future.succeededFuture(new NumberFieldListImpl()));
					return;
				} else {
					numberFieldList.transformToRest(ac, fieldKey, wrap(handler));
					return;
				}
			case BooleanGraphFieldList.TYPE:
				BooleanGraphFieldList booleanFieldList = getBooleanList(fieldKey);
				if (booleanFieldList == null) {
					handler.handle(Future.succeededFuture(new BooleanFieldListImpl()));
					return;
				} else {
					booleanFieldList.transformToRest(ac, fieldKey, wrap(handler));
					return;
				}
			case HtmlGraphFieldList.TYPE:
				HtmlGraphFieldList htmlFieldList = getHTMLList(fieldKey);
				if (htmlFieldList == null) {
					handler.handle(Future.succeededFuture(new HtmlFieldListImpl()));
					return;
				} else {
					htmlFieldList.transformToRest(ac, fieldKey, wrap(handler));
					return;
				}
			case MicroschemaGraphFieldList.TYPE:
				MicroschemaGraphFieldList graphMicroschemaField = getMicroschemaList(fieldKey);
				if (graphMicroschemaField == null) {
					handler.handle(Future.succeededFuture(new MicroschemaFieldListImpl()));
					return;
				} else {
					graphMicroschemaField.transformToRest(ac, fieldKey, wrap(handler));
					return;
				}
			case StringGraphFieldList.TYPE:
				StringGraphFieldList stringFieldList = getStringList(fieldKey);
				if (stringFieldList == null) {
					handler.handle(Future.succeededFuture(new StringFieldListImpl()));
					return;
				} else {
					stringFieldList.transformToRest(ac, fieldKey, wrap(handler));
					return;
				}
			case DateGraphFieldList.TYPE:
				DateGraphFieldList dateFieldList = getDateList(fieldKey);
				if (dateFieldList == null) {
					handler.handle(Future.succeededFuture(new DateFieldListImpl()));
					return;
				} else {
					dateFieldList.transformToRest(ac, fieldKey, wrap(handler));
					return;
				}
			}
			// String listType = listFielSchema.getListType();
			break;
		case SELECT:
			// GraphSelectField graphSelectField = getSelect(fieldKey);
			// if (graphSelectField == null) {
			// return new SelectFieldImpl();
			// } else {
			// //TODO impl me
			// //graphSelectField.transformToRest(ac);
			// }
			// throw new NotImplementedException();
			break;
		case MICROSCHEMA:
			MicroschemaGraphField graphMicroschemaField = getMicroschema(fieldKey);
			if (graphMicroschemaField == null) {
				handler.handle(Future.succeededFuture(new MicroschemaFieldImpl()));
				return;
			} else {
				// TODO impl me
				// graphMicroschemaField.transformToRest(ac);
				handler.handle(Future.failedFuture(new NotImplementedException()));
				return;
			}
		}

		//		} , rh -> {
		//
		//		});
	}

	@Override
	public void delete() {
		// TODO delete linked aggregation nodes for node lists etc
		getElement().remove();
	}

}
