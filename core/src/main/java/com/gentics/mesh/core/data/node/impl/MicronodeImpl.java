package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_CONTAINER;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.AbstractGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
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
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.InternalActionContext;
import com.syncleus.ferma.traversals.EdgeTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public class MicronodeImpl extends AbstractGraphFieldContainerImpl implements Micronode {
	private static final Logger log = LoggerFactory.getLogger(MicronodeImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(MicronodeImpl.class);
	}

	@Override
	public Observable<MicronodeResponse> transformToRest(InternalActionContext ac, String... languageTags) {
		Database db = MeshSpringConfiguration.getInstance().database();

		return db.asyncNoTrxExperimental(() -> {
			List<Observable<MicronodeResponse>> obs = new ArrayList<>();
			MicronodeResponse restMicronode = new MicronodeResponse();
			MicroschemaContainer microschemaContainer = getMicroschemaContainer();
			if (microschemaContainer == null) {
				throw error(BAD_REQUEST, "The microschema container for micronode {" + getUuid() + "} could not be found.");
			}

			Microschema microschema = microschemaContainer.getMicroschema();
			if (microschema == null) {
				throw error(BAD_REQUEST, "The microschema for micronode {" + getUuid() + "} could not be found.");
			}

			// Microschema Reference
			restMicronode.setMicroschema(microschemaContainer.transformToReference(ac));

			// Uuid
			restMicronode.setUuid(getUuid());

			List<String> requestedLanguageTags = new ArrayList<>();
			if (languageTags.length == 0) {
				requestedLanguageTags.addAll(ac.getSelectedLanguageTags());
			} else {
				requestedLanguageTags.addAll(Arrays.asList(languageTags));
			}

			// Fields
			for (FieldSchema fieldEntry : microschema.getFields()) {
				Observable<MicronodeResponse> obsRestField = getRestFieldFromGraph(ac, fieldEntry.getName(), fieldEntry, requestedLanguageTags)
						.map(restField -> {
					if (fieldEntry.isRequired() && restField == null) {
						/* TODO i18n */
						// TODO no trx fail. Instead let obsRestField fail
						throw error(BAD_REQUEST, "The field {" + fieldEntry.getName()
								+ "} is a required field but it could not be found in the micronode. Please add the field using an update call or change the field schema and remove the required flag.");
					}
					if (restField == null) {
						log.info("Field for key {" + fieldEntry.getName() + "} could not be found. Ignoring the field.");
					} else {
						restMicronode.getFields().put(fieldEntry.getName(), restField);
					}
					return restMicronode;
				});
				obs.add(obsRestField);
			}

			return Observable.merge(obs).last();

		});
	}

	@Override
	public MicroschemaContainer getMicroschemaContainer() {
		return out(HAS_MICROSCHEMA_CONTAINER).has(MicroschemaContainerImpl.class).nextOrDefaultExplicit(MicroschemaContainerImpl.class, null);
	}

	@Override
	public void setMicroschemaContainer(MicroschemaContainer microschema) {
		setLinkOut(microschema.getImpl(), HAS_MICROSCHEMA_CONTAINER);
	}

	@Override
	public Microschema getMicroschema() {
		return getMicroschemaContainer().getMicroschema();
	}

	@Override
	public void delete() {
		getElement().remove();
	}

	@Override
	protected Node getParentNode() {
		// first try to get the container in case for normal fields
		NodeGraphFieldContainerImpl container = in(HAS_FIELD).has(NodeGraphFieldContainerImpl.class)
				.nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);

		if (container == null) {
			// the micronode may be part of a list field
			container = in(HAS_ITEM).in(HAS_LIST).has(NodeGraphFieldContainerImpl.class).nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class,
					null);
		}

		if (container == null) {
			throw error(BAD_REQUEST, "error_field_container_without_node");
		} else {
			return container.getParentNode();
		}
	}

	public Observable<? extends Field> getRestFieldFromGraph(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema,
			java.util.List<String> languageTags) {

		// Filter out unsupported field types
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case BINARY:
		case MICRONODE:
			throw error(BAD_REQUEST, "micronodes don't support fields of type {" + type + "}");
		case LIST:
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;
			switch (listFieldSchema.getListType()) {
			case MicronodeGraphFieldList.TYPE:
				throw error(BAD_REQUEST, "micronodes don't support micronode lists");
			default:
				return super.getRestFieldFromGraph(ac, fieldKey, fieldSchema, languageTags);
			}
		default:
			return super.getRestFieldFromGraph(ac, fieldKey, fieldSchema, languageTags);
		}

	}

	@Override
	protected void updateField(InternalActionContext ac, String key, Field restField, FieldSchema fieldSchema, Schema schema) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case BINARY:
		case MICRONODE:
			throw error(BAD_REQUEST, "micronodes don't support fields of type {" + type + "}");
		case LIST:
			ListFieldSchema listFieldSchema = (ListFieldSchema) fieldSchema;
			switch (listFieldSchema.getListType()) {
			case MicronodeGraphFieldList.TYPE:
				throw error(BAD_REQUEST, "micronodes don't support micronode lists");
			default:
				super.updateField(ac, key, restField, fieldSchema, schema);
			}
		default:
			super.updateField(ac, key, restField, fieldSchema, schema);
		}

	}

}
