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
package org.openmrs.web.controller.order;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.web.WebConstants;
import org.openmrs.propertyeditor.ConceptEditor;
import org.openmrs.propertyeditor.DrugEditor;
import org.openmrs.propertyeditor.EncounterEditor;
import org.openmrs.propertyeditor.OrderTypeEditor;
import org.openmrs.propertyeditor.UserEditor;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

public class OrderPlaceFormController extends SimpleFormController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Allows for Integers to be used as values in input tags. Normally, only strings and lists are
	 * expected
	 * 
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
		super.initBinder(request, binder);
		
		binder.registerCustomEditor(OrderType.class, new OrderTypeEditor());
		binder.registerCustomEditor(Boolean.class, new CustomBooleanEditor("t", "f", true));
		binder.registerCustomEditor(Concept.class, new ConceptEditor());
		binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("dd/MM/yyyy"), true));
		binder.registerCustomEditor(User.class, new UserEditor());
		binder.registerCustomEditor(Encounter.class, new EncounterEditor());
		binder.registerCustomEditor(Drug.class, new DrugEditor());
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Map referenceData(HttpServletRequest request) throws Exception {
		Map refData = new HashMap();
		
		Integer patientId = ServletRequestUtils.getIntParameter(request, "patientId");
		if (patientId != null) {
			refData.put("patientId", patientId);
		}
		
		return refData;
	}
	
	/**
	 * The onSubmit function receives the form/command object that was modified by the input form
	 * and saves it to the db
	 * 
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object obj,
	                                BindException errors) throws Exception {
		
		HttpSession httpSession = request.getSession();
		
		String view = getFormView();
		
		if (Context.isAuthenticated()) {
			DrugOrder order = (DrugOrder) obj;
			
			// need to do a lot more to this drug order
			
			// you can get the concept from the drug
			Drug thisDrug = order.getDrug();
			Concept thisConcept = thisDrug.getConcept();
			order.setConcept(thisConcept);
			
			// TODO: for now, orderType will have to be hard-coded?
			order.setOrderType(new OrderType(new Integer(2)));
			
			Patient thisPatient = null;
			if (order.getEncounter() == null) {
				Integer patientId = ServletRequestUtils.getIntParameter(request, "patientId");
				if (patientId != null) {
					thisPatient = Context.getPatientService().getPatient(patientId);
				}
			}
			
			order.setPatient(thisPatient);
			if (order.getDateCreated() == null)
				order.setDateCreated(new Date());
			if (order.getVoided() == null)
				order.setVoided(new Boolean(false));
			Context.getOrderService().updateOrder(order);
			view = getSuccessView();
			httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Order.drug.saved");
		}
		
		return new ModelAndView(new RedirectView(view));
	}
	
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring the
	 * form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		
		OrderService os = Context.getOrderService();
		
		DrugOrder order = null;
		
		if (Context.isAuthenticated()) {
			Integer orderId = ServletRequestUtils.getIntParameter(request, "orderId");
			if (orderId != null)
				order = (DrugOrder) os.getOrder(orderId);
		}
		
		// if this is a new order, let's see if the user has picked a type yet
		if (order == null) {
			order = new DrugOrder();
			//Integer orderTypeId = ServletRequestUtils.getIntParameter(request, "orderTypeId");
			// TODO: again, we need to hand select the ordertype here
			/*
			if ( orderTypeId != null ) {
				OrderType ot = os.getOrderType(orderTypeId);
				order.setOrderType(ot);
			}
			*/
		}
		
		return order;
	}
}
