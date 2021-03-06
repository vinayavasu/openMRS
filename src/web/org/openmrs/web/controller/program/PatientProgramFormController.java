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
package org.openmrs.web.controller.program;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.web.WebConstants;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;

public class PatientProgramFormController implements Controller {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// can't do anything without a method
		return null;
	}
	
	public ModelAndView enroll(HttpServletRequest request, HttpServletResponse response) throws ServletException,
	                                                                                    IOException {
		
		String returnPage = request.getParameter("returnPage");
		if (returnPage == null) {
			throw new IllegalArgumentException("must specify a returnPage parameter in a call to enroll()");
		}
		
		String patientIdStr = request.getParameter("patientId");
		String programIdStr = request.getParameter("programId");
		String enrollmentDateStr = request.getParameter("dateEnrolled");
		String completionDateStr = request.getParameter("dateCompleted");
		
		log.debug("enroll " + patientIdStr + " in " + programIdStr + " on " + enrollmentDateStr);
		
		ProgramWorkflowService pws = Context.getProgramWorkflowService();
		
		// make sure we parse dates the same was as if we were using the initBinder + property editor method 
		CustomDateEditor cde = new CustomDateEditor(Context.getDateFormat(), true, 10);
		cde.setAsText(enrollmentDateStr);
		Date enrollmentDate = (Date) cde.getValue();
		cde.setAsText(completionDateStr);
		Date completionDate = (Date) cde.getValue();
		Patient patient = Context.getPatientService().getPatient(Integer.valueOf(patientIdStr));
		Program program = pws.getProgram(Integer.valueOf(programIdStr));
		if (pws.isInProgram(patient, program, enrollmentDate, completionDate))
			request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Program.error.already");
		else
			Context.getProgramWorkflowService().enrollPatientInProgram(patient, program, enrollmentDate, completionDate,
			    null);
		
		return new ModelAndView(new RedirectView(returnPage));
	}
	
	public ModelAndView complete(HttpServletRequest request, HttpServletResponse response) throws ServletException,
	                                                                                      IOException {
		
		String returnPage = request.getParameter("returnPage");
		if (returnPage == null) {
			throw new IllegalArgumentException("must specify a returnPage parameter in a call to enroll()");
		}
		
		String patientProgramIdStr = request.getParameter("patientProgramId");
		String dateCompletedStr = request.getParameter("dateCompleted");
		
		// make sure we parse dates the same was as if we were using the initBinder + property editor method 
		CustomDateEditor cde = new CustomDateEditor(Context.getDateFormat(), true, 10);
		cde.setAsText(dateCompletedStr);
		Date dateCompleted = (Date) cde.getValue();
		
		PatientProgram p = Context.getProgramWorkflowService().getPatientProgram(Integer.valueOf(patientProgramIdStr));
		p.setDateCompleted(dateCompleted);
		Context.getProgramWorkflowService().updatePatientProgram(p);
		
		return new ModelAndView(new RedirectView(returnPage));
	}
	
}
