package com.gentics.mesh.parameter;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.Mesh;

public interface NodeParameters extends ParameterProvider {

	/**
	 * Query parameter key: {@value #LANGUAGES_QUERY_PARAM_KEY}
	 */
	public static final String LANGUAGES_QUERY_PARAM_KEY = "lang";

	/**
	 * Query parameter key: {@value #EXPANDFIELDS_QUERY_PARAM_KEY}
	 */
	public static final String EXPANDFIELDS_QUERY_PARAM_KEY = "expand";

	/**
	 * Query parameter key: {@value #EXPANDALL_QUERY_PARAM_KEY}
	 */
	public static final String EXPANDALL_QUERY_PARAM_KEY = "expandAll";

	/**
	 * Query parameter key: {@value #RESOLVE_LINKS_QUERY_PARAM_KEY}
	 */
	public static final String RESOLVE_LINKS_QUERY_PARAM_KEY = "resolveLinks";

	/**
	 * Set the <code>{@value #LANGUAGES_QUERY_PARAM_KEY}</code> request parameter values.
	 * 
	 * @param languageTags
	 * @return Fluent API
	 */
	default NodeParameters setLanguages(String... languageTags) {
		setParameter(LANGUAGES_QUERY_PARAM_KEY, convertToStr(languageTags));
		return this;
	}

	/**
	 * Return the <code>{@value #LANGUAGES_QUERY_PARAM_KEY}</code> request parameter values.
	 * 
	 * @return
	 */
	default String[] getLanguages() {
		String value = getParameter(LANGUAGES_QUERY_PARAM_KEY);
		String[] languages = null;
		if (value != null) {
			languages = value.split(",");
		}
		if (languages == null) {
			languages = new String[] { Mesh.mesh()
					.getOptions()
					.getDefaultLanguage() };
		}
		return languages;
	}

	/**
	 * Set the <code>{@value #RESOLVE_LINKS_QUERY_PARAM_KEY}</code> request parameter.
	 * 
	 * @param type
	 * @return Fluent API
	 */
	default NodeParameters setResolveLinks(LinkType type) {
		setParameter(RESOLVE_LINKS_QUERY_PARAM_KEY, type.name()
				.toLowerCase());
		return this;
	}

	/**
	 * @see #getLanguages()
	 * @return
	 */
	default List<String> getLanguageList() {
		return Arrays.asList(getLanguages());
	}

}
