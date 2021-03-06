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
package org.openmrs.web.dwr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;

import uk.ltd.getahead.dwr.WebContextFactory;

/**
 *
 */
public class DWRObsService {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Void the given observation
	 * 
	 * @param obsId
	 * @param reason
	 */
	public void voidObservation(Integer obsId, String reason) {
		Obs obs = Context.getObsService().getObs(obsId);
		if (obs == null) {
			throw new IllegalArgumentException("Cannot find obs with id=" + obsId);
		}
		if (reason == null || reason.length() == 0)
			throw new IllegalArgumentException("reason is required");
		log.info("Voiding observation " + obs + " for reason " + reason);
		Context.getObsService().voidObs(obs, reason);
	}
	
	/**
	 * Get all observations for the given encounter TODO: rename to getObservationsByEncounter
	 * 
	 * @param encounterId
	 * @return
	 */
	public Vector<Object> getObservations(Integer encounterId) {
		
		log.info("Get observations for encounter " + encounterId);
		Vector<Object> obsList = new Vector<Object>();
		
		HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
		
		try {
			Encounter encounter = Context.getEncounterService().getEncounter(encounterId);
			
			Set<Obs> observations = encounter.getAllObs();
			if (observations != null)
				for (Obs obs : observations) {
					obsList.add(new ObsListItem(obs, request.getLocale()));
				}
			
		}
		catch (Exception e) {
			log.error(e);
			obsList.add("Error while attempting to find obs - " + e.getMessage());
		}
		
		return obsList;
	}
	
	/**
	 * Auto generated method comment
	 * 
	 * @param personId
	 * @param encounterId
	 * @param conceptId
	 * @param valueText
	 * @param obsDateStr
	 */
	public void createObs(Integer personId, Integer encounterId, Integer conceptId, String valueText, String obsDateStr) throws Exception {
		createNewObs(personId, encounterId, null, conceptId, valueText, obsDateStr);
	}
	
	/**
	 * Auto generated method comment
	 * 
	 * @param personId
	 * @param encounterId
	 * @param locationId
	 * @param conceptId
	 * @param valueText
	 * @param obsDateStr
	 */
	public void createNewObs(Integer personId, Integer encounterId, Integer locationId, Integer conceptId, String valueText,
	                         String obsDateStr) throws Exception {
		
		log.info("Create new observation ");
		
		Date obsDate = null;
		if (obsDateStr != null) {
			// TODO Standardize date input 
			SimpleDateFormat sdf = Context.getDateFormat();
			try {
				obsDate = sdf.parse(obsDateStr);
			}
			catch (ParseException e) {
				log.error("Error parsing date ... " + obsDate);
				throw new DWRException("Observation date has format error: " + obsDate);
			}
		}
		
		Person person = Context.getPersonService().getPerson(personId);
		Concept concept = Context.getConceptService().getConcept(conceptId);
		Encounter encounter = (encounterId == null) ? null : Context.getEncounterService().getEncounter(encounterId);
		
		Obs obs = new Obs();
		obs.setPerson(person);
		obs.setConcept(concept);
		
		obs.setObsDatetime(obsDate);
		if (encounter != null) {
			obs.setEncounter(encounter);
			obs.setLocation(encounter.getLocation());
		} else {
			Location location = null;
			if (locationId != null)
				Context.getLocationService().getLocation(locationId);
			if (location == null) {
				location = Context.getLocationService().getDefaultLocation();
				
			}
			obs.setLocation(location);
		}
		obs.setCreator(Context.getAuthenticatedUser());
		obs.setDateCreated(new Date());
		
		// Currently only handles text, numeric, and date values  
		// TODO  Expand support all other values
		String hl7DataType = concept.getDatatype().getHl7Abbreviation();
		if ("NM".equals(hl7DataType)) {
			obs.setValueNumeric(Double.valueOf(valueText));
		} else if ("DT".equals(hl7DataType)) {
			// Convert to Date format
			Date obsDateValue = null;
			if (valueText != null) {
				// TODO Standardize date input 
				SimpleDateFormat sdft = Context.getDateFormat();
				try {
					obsDateValue = sdft.parse(valueText);
				}
				catch (ParseException e) {
					log.warn("Date value has format error: " + obsDateValue, e);
					throw new DWRException("Date value: '" + obsDateValue + "' has format error: " + e.getMessage());
				}
			}
			obs.setValueDatetime(obsDateValue) ;
		} else {
			obs.setValueText(valueText);
		}
		
		// Create the observation
		Context.getObsService().saveObs(obs, null);
		
	}
	
	/* Commenting out an unused method
	public Vector findObs(String phrase, boolean includeVoided) {
		
		// List to return
		// Object type gives ability to return error strings
		Vector<Object> objectList = new Vector<Object>();	

		try {
			EncounterService es = Context.getEncounterService();
			Set<Encounter> encs = new HashSet<Encounter>();
			
	//			if (phrase.matches("\\d+")) {
	//				// user searched on a number.  Insert obs with corresponding obsId
	//				Obs e = os.getObs(Integer.valueOf(phrase));
	//				if (e != null) {
	//					encs.add(e);
	//				}
	//			}
			
			if (phrase == null || phrase.equals("")) {
				//TODO get all concepts for testing purposes?
			}
			else {
				encs.addAll(es.getEncountersByPatientIdentifier(phrase));
			}

			if (encs.size() == 0) {
				objectList.add("No matches found for <b>" + phrase + "</b>");
			}
			else {
				objectList = new Vector<Object>(encs.size());
				for (Encounter e : encs) {
					objectList.add(new EncounterListItem(e));
				}
			}
		} catch (Exception e) {
			log.error(e);
			objectList.add("Error while attempting to find obs - " + e.getMessage());
		}

		return objectList;
	}
	*/

	/**
	 * Auto generated method comment
	 * 
	 * @param personId
	 * @param conceptId
	 * @param encounterId
	 * @return
	 */
	public Vector<ObsListItem> getObsByPatientConceptEncounter(String personId, String conceptId, String encounterId) {
		log.debug("Started with: [" + personId + "] [" + conceptId + "] [" + encounterId + "]");
		
		Vector<ObsListItem> ret = new Vector<ObsListItem>();
		
		Integer pId = null;
		try {
			pId = new Integer(personId);
		}
		catch (NumberFormatException nfe) {
			pId = null;
		}
		
		Integer eId = null;
		try {
			eId = new Integer(encounterId);
		}
		catch (NumberFormatException nfe) {
			eId = null;
		}
		
		Person p = null;
		Concept c = null;
		Encounter e = null;
		
		if (pId != null)
			p = Context.getPersonService().getPerson(pId);
		if (conceptId != null)
			c = OpenmrsUtil.getConceptByIdOrName(conceptId);
		if (eId != null)
			e = Context.getEncounterService().getEncounter(eId);
		
		Collection<Obs> obss = null;
		
		if (p != null && c != null) {
			log.debug("Getting obss with patient and concept");
			obss = Context.getObsService().getObservationsByPersonAndConcept(p, c);
		} else if (e != null) {
			log.debug("Getting obss by encounter");
			obss = e.getAllObs();
		} else if (p != null) {
			log.debug("Getting obss with just patient");
			obss = Context.getObsService().getObservationsByPerson(p);
		}
		
		if (obss != null) {
			for (Obs obs : obss) {
				ObsListItem newItem = new ObsListItem(obs, Context.getLocale());
				ret.add(newItem);
			}
			log.debug("obss was size " + obss.size());
		}
		
		return ret;
	}
	
	/**
	 * Auto generated method comment
	 * 
	 * @param obsId
	 * @return
	 */
	public ObsListItem getObs(Integer obsId) {
		Obs o = null;
		if (obsId != null) {
			o = Context.getObsService().getObs(obsId);
		}
		
		ObsListItem oItem = null;
		
		if (o != null) {
			oItem = new ObsListItem(o, Context.getLocale());
		}
		
		return oItem;
	}
}
