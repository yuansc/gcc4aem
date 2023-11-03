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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.adobe.granite.ui.clientlibs.LibraryType;
import com.adobe.granite.ui.clientlibs.script.ScriptProcessor;
import com.adobe.granite.ui.clientlibs.script.ScriptResource;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.SourceFile;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is a JavaScript processor for Adobe Granite HTML Library 
 * Manager that uses the latest Google Closure Compiler for 
 * processing JavaScript written in the newer ECMAScript 
 * versions into Rhino executable script. 
 * 
 * @see <a href="https://experienceleague.adobe.com/docs/experience-manager-65/developing/introduction/clientlibs.html?lang=en">Using Client-Side Libraries</a>
 */
@Component( 
	service = ScriptProcessor.class, 
	immediate = true, 
	configurationPolicy = ConfigurationPolicy.OPTIONAL, 
	property = {
		Constants.SERVICE_DESCRIPTION
				+ "=org.netartisan.sling.gcc4ame - GCC4AEM JavaScript Processor",
		Constants.SERVICE_RANKING + ":Integer="+Integer.MAX_VALUE
	}
)
@Designate(ocd = GoogleClosureCompilerProcessor.Config.class, factory=false)
public class GoogleClosureCompilerProcessor implements ScriptProcessor {

    @ObjectClassDefinition(
            name = "org.netartisan.sling.gcc4aem - GCC4AEM JavaScript Processor",
            description = "JavaScript processor for Adobe Granite HTML Library Manager that uses the latest Google Closure Compiler"
    )
    public @interface Config {
        @AttributeDefinition(
                name = "Service Ranking",
                description = "Higher value gives higher execution priority.",
                min = "1",
                max = Integer.MAX_VALUE+"",
                required = true, // Defaults to true
                cardinality = 0
        )
        int service_ranking() default Integer.MAX_VALUE;
        
        @AttributeDefinition(
                name = "Override min:gcc",
                description = "Return 'gcc' as the compiler name so all clientlibs processing by min:gcc will be processed by GCC4AEM instead.  Do set service ranking to the highest value when enable this option.",
                required = false, // Defaults to true
                cardinality = 0
        		)
        boolean overridemingcc() default false;

        @AttributeDefinition(
                name = "Additional Google Closure Compiler command line options",
                description = "The options will be passing to the Google Closure Compiler as addtional parameters",
                required = false, // Defaults to true
                cardinality = 20  
        )
        String[] additional_params() default {};
    }
    
    protected class GoogleClosureCompilerCLR extends CommandLineRunner {
		protected GoogleClosureCompilerCLR(String[] args) {
			super(args);
		}

		public CompilerOptions getCompilerOptions() {
			return super.createOptions();
		}

		@Override
		public Compiler createCompiler() {
			return super.createCompiler();
		}
    }

    protected Logger log = LoggerFactory.getLogger(this.getClass());
    protected BundleContext bundleContext;
    protected Config bundleConfig;
    
    @Activate
    protected void activate(ComponentContext ctx, Config cfg) {
    	log.info(String.format("GCC4AEM activated as '%s' compiler", cfg.overridemingcc() ? "gcc" : "gcc4aem"));
    	modified( ctx, cfg );
    }
    
    @Modified
    protected void modified(ComponentContext ctx, Config cfg) {
        this.bundleContext = ctx.getBundleContext();
        this.bundleConfig = cfg;
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx, Config cfg) {
        this.bundleContext = null;
        this.bundleConfig = null;
    }

	@Override
	public String getName() {
		return this.bundleConfig.overridemingcc() ? "gcc" : "gcc4aem";
	}

	@Override
	public boolean handles(LibraryType type) {
		return (LibraryType.JS == type);
	}

	// @see <a href="https://varunaem.blogspot.com/2019/02/all-about-gui-compiler.html">All About GCC Compiler</a>
	protected String translateAemGccOptions( String key ) {
		if (key.startsWith("-")) {
			return key;
		}
		return "failOnWarning".equalsIgnoreCase(key) ? "--jscomp_error"
				: "languageIn".equalsIgnoreCase(key) ? "--language_in"
						: "languageOut".equalsIgnoreCase(key) ? "--language_out"
								: "compilationLevel".equalsIgnoreCase(key) ? "--compilation_level" : "";
	}
	protected String translateAemGccCompilationLevel( String key ) {
		return "simple".equalsIgnoreCase(key) ? "SIMPLE"
				: "whitespace".equalsIgnoreCase(key) ? "WHITSPACE_ONLY"
						: "advanced".equalsIgnoreCase(key) ? "ADVANCED"
								: "SIMPLE";
	}
	
	protected Map<String,String> populateDefaultParameters(Map<String, String> opts){
		HashMap<String,String> paramsMap = new HashMap<String,String>();
		paramsMap.put("--compilation_level", "ADVANCED");
		paramsMap.put("--language_in", "ECMASCRIPT_NEXT");
		paramsMap.put("--language_out", "ECMASCRIPT5"); // for Rhino compatibility
		for( Map.Entry<String,String> opt: opts.entrySet() ) {
			String GccFlag = translateAemGccOptions( opt.getKey() );
			String GccVal = opt.getValue();
			if( "compilationLevel".equalsIgnoreCase(opt.getKey()) ) {
				GccVal = translateAemGccCompilationLevel(opt.getValue());
			} else if( "failOnWarning".equalsIgnoreCase(opt.getKey())){
				GccVal = "true".equalsIgnoreCase(opt.getValue()) ? "*" : "";
				GccFlag = "";
			}
			if( !GccFlag.isBlank() ) {
				paramsMap.put(GccFlag, GccVal);
			}
		}
		return paramsMap;
	}
	
	protected String[] createGccCommandLineArguments(Map<String, String> opts) {
		ArrayList<String> arguments = new ArrayList<String>();
		for( Map.Entry<String,String> opt: opts.entrySet() ) {
			arguments.add(opt.getKey());
			if( !opt.getValue().isBlank() ) arguments.add(opt.getValue());
		}
		return arguments.toArray(new String[arguments.size()]);
	}
	
	@Override
	public boolean process(LibraryType type, ScriptResource script, Writer out, Map<String, String> opts)
			throws IOException {
		if( handles(type) ) {
			Map<String,String> GccArgs = populateDefaultParameters(opts);
			GoogleClosureCompilerCLR GccCLR = new GoogleClosureCompilerCLR(createGccCommandLineArguments(GccArgs));
			if( GccCLR.shouldRunCompiler() ) {
				CompilerOptions GccOpts = GccCLR.getCompilerOptions();

				// A way to disable strict mode input for handling not so clean JS codes. git diff
				
				if( !GccArgs.containsKey("--strict_mode_input") ) GccOpts.setStrictModeInput(false);

				List<SourceFile> defaultExterns = GoogleClosureCompilerCLR.getBuiltinExterns(GccOpts.getEnvironment());
				Compiler Gcc = GccCLR.createCompiler();
				SourceFile src = SourceFile.fromCode(script.getName(), IOUtils.toString(script.getReader()));
				Gcc.compile(defaultExterns, Collections.singletonList(src), GccOpts);

				ErrorManager ew = Gcc.getErrorManager();
				if (ew.getErrorCount() > 0) {
					if (ew.getWarningCount() > 0) {
						log.error("{} file {} processed with {} error(s) and {} warning(s).", type, script.getName(),
								ew.getErrorCount(), ew.getWarningCount());
					} else {
						log.error("{} file {} processed with {} error(s).", type, script.getName(), ew.getErrorCount());
					}
					return false;
				} else if (ew.getWarningCount() > 0) {
					log.warn("{} file {} processed with {} warning(s).", type, script.getName(), ew.getWarningCount());
				} else {
					log.info("{} file {} processed successfully.", type, script.getName());
				}
				out.write(Gcc.toSource());
				return true;
			} else {
				log.error("Invalid parameters");
			}
		} else {
			log.debug(String.format("Not handling library type %s", type));
		}
		return false;
	}
}

