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
package org.openmrs.reporting.db;

import java.util.List;

import org.openmrs.api.db.DAOException;
import org.openmrs.reporting.AbstractReportObject;

/**
 *
 */
public interface ReportObjectDAO {
	
	/**
	 * Auto generated method comment
	 * 
	 * @return
	 */
	public List<AbstractReportObject> getAllReportObjects() throws DAOException;
	
	/**
	 * Auto generated method comment
	 * 
	 * @param reportObjId
	 * @return
	 * @throws DAOException
	 */
	public AbstractReportObject getReportObject(Integer reportObjId) throws DAOException;
	
	/**
	 * Auto generated method comment
	 * 
	 * @param reportObj
	 * @throws DAOException
	 */
	public void deleteReportObject(AbstractReportObject reportObj) throws DAOException;
	
	/**
	 * Auto generated method comment
	 * 
	 * @param reportObj
	 * @return
	 * @throws DAOException
	 */
	public AbstractReportObject saveReportObject(AbstractReportObject reportObj) throws DAOException;
	
	/**
	 * Auto generated method comment
	 * 
	 * @param reportObjectType
	 * @return
	 * @throws DAOException
	 */
	public List<AbstractReportObject> getReportObjectsByType(String reportObjectType) throws DAOException;
}
