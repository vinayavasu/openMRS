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
package org.openmrs.logic.datasource;

import java.util.Collection;
import java.util.Map;

import org.openmrs.Cohort;
import org.openmrs.logic.LogicContext;
import org.openmrs.logic.LogicCriteria;
import org.openmrs.logic.LogicException;
import org.openmrs.logic.result.Result;

/**
 * Provides data to the logic service engine. Each data source is responsible for exposing a set of
 * keys to the logic service and delivering results to the logic service engine upon request.
 * Requests to the data source reference the evaluation context, a patient cohort, and criteria.
 * <ul>
 * <li><strong>Context.</strong> The evaluation context (org.openmrs.logic.LogicContext) defines the
 * index date and any global parameters. All criteria should be applied as if the current date is
 * the index date, allowing for retrospective queries.</li>
 * <li><strong>Cohort.</strong> The list of patients for which results should be returned</li>
 * <li><strong>Criteria.</strong> A chain of criteria to be applied to the results, passed to the
 * data source, since each data source knows how to apply these criteria in the most efficient
 * matter.</li>
 * </ul>
 * Keys (and their subsequent results) work best if they are well documented and maximize the re-use
 * of data. For example, consider a data source that has access to a historical listing of pharmacy
 * visits for patients. For each visit to the name of the pharmacy, the date of the visit and the
 * number of prescriptions filled at that visit are known. You might want to make a key for each
 * attribute, e.g.
 * <ul>
 * <li><strong>pharmacy</strong> &mdash; returns a text result with the name of the pharmacy</li>
 * <li><strong>visit date</strong> &mdash; returns a date result with the date of the pharmacy visit
 * </li>
 * <li><strong>number of prescriptions</strong &mdash; returns a numeric result with the number of
 * prescriptions filled</li>
 * </ul>
 * A consumer of the logic service could then use
 * 
 * <pre>
 *   Context.getLogicService().eval(myPatient, "@pharmacy visit.pharmacy");
 * </pre>
 * 
 * to get a list of the names of pharamacies visited by the patient. However, this limits the ways
 * we can use the result. On the other hand, consider defining the key:
 * <ul>
 * <li><strong>visit</strong> &mdash; returns a text result with the result date equal to the
 * pharmacy visit date, the text value equal to the name of the pharmacy, and the numeric result
 * overloaded with the number of prescriptions filled at the pharmacy visit.
 * </ul>
 * Now a consumer of the logic service can use the same result in several different ways:
 * 
 * <pre>
 *   Result result = Context.getLogicService().eval(myPatient, "@pharmacy visit.visit");
 *   Result lastVisit = result.latest();
 *   Date dateOfVisit = result.getResultDate();
 *   String pharmacy = result.toString();
 *   int numberOfPrescriptions = result.toNumber();
 * </pre>
 * 
 * One way to approach the design of a new data service is to avoid thinking of the individual
 * attributes, but rather think about turning the data into the fewest number of observations (a key
 * for each) filled with as much data as possible. Remember that you can overload values within
 * results; however, such overloading should aim to be as intuitive as possible and well documented.
 * New logic data sources should be documented on the <a
 * href="http://openmrs.org/wiki/Logic_Data_Sources">OpenMRS wiki</a>, including a description of
 * the keys available from the data source as well as the characteristics of the result returned for
 * each key.
 */
public interface LogicDataSource {
	
	/**
	 * Extracts data from the data source. Actually, this function only checks for cached data and
	 * forwards all non-cached requests to its subclass(es).
	 * 
	 * @param who patient(s) for whom to perform the queries
	 * @param dataElements the data elements to be extracted
	 * @return a map of results for each patient, grouped by requested data element
	 * @see org.openmrs.logic.LogicDataElement
	 */
	public Map<Integer, Result> read(LogicContext context, Cohort patients, LogicCriteria criteria) throws LogicException;
	
	public abstract Collection<String> getKeys();
	
	public boolean hasKey(String key);
	
	public abstract int getDefaultTTL();
	
}
