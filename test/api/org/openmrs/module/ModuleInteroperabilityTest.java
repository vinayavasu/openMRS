/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.test.BaseContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;
import org.openmrs.util.OpenmrsClassLoader;

/**
 * Tests how modules interact and call each other. Both when loaded by Spring during OpenMRS startup
 * and during normal file usage.
 */
@SkipBaseSetup
public class ModuleInteroperabilityTest extends BaseContextSensitiveTest {
	
	@Before
	public void startupBeforeEachTest() throws Exception {
		// create the basic user and give it full rights
		initializeInMemoryDatabase();
		
		ModuleUtil.startup(getRuntimeProperties());
		
		// authenticate to the temp database
		authenticate();
	}
	
	@After
	public void cleanupAfterEachTest() throws Exception {
		ModuleUtil.shutdown();
	}
	
	/**
	 * This class file uses the atd and dss modules to test the compatibility
	 * 
	 * @see org.openmrs.test.BaseContextSensitiveTest#getRuntimeProperties()
	 */
	public Properties getRuntimeProperties() {
		Properties props = super.getRuntimeProperties();
		
		// NOTE! These two modules are modified heavily from the original atd and dss modules.
		// the "/lib" folder has been emptied to compact the size.
		// the "/metadata/sqldiff.xml" file has been deleted in order to load the modules into hsql.
		//    (the sql tables are built from hibernate mapping files automatically in unit tests)
		props.setProperty(ModuleConstants.RUNTIMEPROPERTY_MODULE_LIST_TO_LOAD, "org/openmrs/module/include/atd-0.51.omod "
		        + "org/openmrs/module/include/dssmodule-1.44.omod");
		
		return props;
	}
	
	/**
	 * Test that module A that requires module B can call a service method on module B
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldModuleALoadingModuleB() throws Exception {
		
		OpenmrsClassLoader loader = OpenmrsClassLoader.getInstance();
		Class<?> atdServiceClass = loader.loadClass("org.openmrs.module.atdproducer.service.ATDService");
		Class<?> dssServiceClass = loader.loadClass("org.openmrs.module.dssmodule.DssService");
		assertNotNull(atdServiceClass);
		assertNotNull(dssServiceClass);
		
		ModuleClassLoader atdClassLoader = (ModuleClassLoader) atdServiceClass.getClassLoader();
		assertEquals("atd", atdClassLoader.getModule().getModuleId());
		
		ModuleClassLoader dssClassLoader = (ModuleClassLoader) dssServiceClass.getClassLoader();
		assertEquals("dssmodule", dssClassLoader.getModule().getModuleId());
		
		// load a dss class from the atd classloader.  This simulates a normal class (like a
		// controller) in one module loading another class that is located in a separate module 
		Class<?> dssUtilClass = atdClassLoader.loadClass("org.openmrs.module.dssmodule.util.Util");
		ModuleClassLoader dssUtilClassLoader = (ModuleClassLoader) dssUtilClass.getClassLoader();
		assertEquals("dssmodule", dssUtilClassLoader.getModule().getModuleId());
		
		// try the same as above except with an already loaded class (the DssService class)
		Class<?> dssServiceClass2 = atdClassLoader.loadClass("org.openmrs.module.dssmodule.DssService");
		ModuleClassLoader dssServiceClassLoader = (ModuleClassLoader) dssServiceClass2.getClassLoader();
		assertEquals("dssmodule", dssServiceClassLoader.getModule().getModuleId());
		
	}
	
}
