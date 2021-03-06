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
package org.openmrs;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseContextSensitiveTest;
import org.openmrs.test.Verifies;

/**
 * Tests methods on the PersonAttribute class
 */
public class PersonAttributeTest extends BaseContextSensitiveTest {
	
	/**
	 * @see {@link PersonAttribute#toString()}
	 */
	@Test
	@Verifies(value = "should return toString of hydrated value", method = "toString()")
	public void toString_shouldReturnToStringOfHydratedValue() throws Exception {
		// type = CIVIL STATUS, concept = MARRIED
		PersonAttributeType type = Context.getPersonService().getPersonAttributeType(8);
		PersonAttribute attr = new PersonAttribute(type, "6");
		Assert.assertEquals("MARRIED", attr.toString());
	}
}
