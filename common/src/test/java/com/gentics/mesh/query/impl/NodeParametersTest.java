package com.gentics.mesh.query.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;

public class NodeParametersTest {

	@Test
	public void testNodeParams() {
		NodeParametersImpl params = new NodeParametersImpl();

		// Language List
		//assertThat(params.getLanguageList()).containsExactly("en");

		// Resolve Link Type
		assertEquals(LinkType.OFF, params.getResolveLinks());
		assertEquals("The method did not return a fluent API", params, params.setResolveLinks(LinkType.FULL));
		assertEquals("The parameter should have been changed.", LinkType.FULL, params.getResolveLinks());

		assertEquals("expandAll=true&expand=ä,b,c&resolveLinks=full", params.getQueryParameters());
	}

}
