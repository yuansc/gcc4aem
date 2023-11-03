/**
 * Copyright 2023 Ling-Hsiung Yuan and The NetArtisan.Org GCC4AEM Authors
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netartisan.sling.gcc4aem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.adobe.granite.ui.clientlibs.LibraryType;
import com.adobe.granite.ui.clientlibs.script.ScriptProcessor;
import com.adobe.granite.ui.clientlibs.script.ScriptResource;

@ExtendWith(OsgiContextExtension.class)
public class Gcc4AemTests {

	private final OsgiContext ctx = new OsgiContext();

	public class TestScriptResource implements ScriptResource {
		protected File script;

		public TestScriptResource(File f) {
			this.script = f;
		}

		@Override
		public String getName() {
			return this.script.getName();
		}

		@Override
		public Reader getReader() throws IOException {
			return new FileReader(this.script);
		}

		@Override
		public long getSize() {
			return this.script.length();
		}

	}

	@SuppressWarnings("serial")
	@Test
	void osgi_gcc4aem_default_values() {
		ctx.registerInjectActivateService(new GoogleClosureCompilerProcessor());
		ScriptProcessor sp = ctx.getService(ScriptProcessor.class);
		assertEquals("gcc4aem", sp.getName(), "Incorrect default service name [" + sp.getName() + "] detected");
	}

	@SuppressWarnings("serial")
	@Test
	void osgi_gcc4aem_override_min_gcc() {
		ctx.registerInjectActivateService(new GoogleClosureCompilerProcessor(), new HashMap<String, String>() {
			{
				put("overridemingcc", "true");
				put("additional_params", "");
			}
		});
		ScriptProcessor sp = ctx.getService(ScriptProcessor.class);
		assertEquals("gcc", sp.getName(), "Incorrect default service name [" + sp.getName() + "] detected");
	}

	@Test
	void handles_library_type() {
		GoogleClosureCompilerProcessor gccp = new GoogleClosureCompilerProcessor();
		assertTrue(gccp.handles(LibraryType.JS), "Implementtation should handle " + LibraryType.JS + " but denied.");
		assertTrue(!gccp.handles(LibraryType.CSS),
				"Implementtation shouldn't handle " + LibraryType.CSS + " but accepted.");
	}

	@Test
	void process_basic() {
		ctx.registerInjectActivateService(new GoogleClosureCompilerProcessor());
		ScriptProcessor sp = ctx.getService(ScriptProcessor.class);

		StringWriter output = new StringWriter();
		TestScriptResource t = new TestScriptResource(
			new File(getClass().getClassLoader().getResource("let_and_const.js").getFile())
		);
		System.out.println("Test file: " + t.getName());
		
		try {
			Map<String,String> opts = new HashMap<String,String>();
			opts.put("--language_in", "UNSTABLE");
			opts.put("--language_out", "ECMASCRIPT5_STRICT");
			opts.put("--compilation_level", "SIMPLE");
			// should not process CSS 
			sp.process(LibraryType.CSS, t, output, opts);
			assertTrue( output.toString().isBlank(), "Unexpected result ["+output.toString()+"] with library type " + LibraryType.CSS );

			// should process JS 
			sp.process(LibraryType.JS, t, output, opts);
			assertTrue( !output.toString().isBlank(), "Unexpected result ["+output.toString()+"] with library type " + LibraryType.JS );
			System.out.println(output.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
