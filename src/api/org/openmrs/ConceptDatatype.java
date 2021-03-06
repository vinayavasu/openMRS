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

import java.util.Date;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * ConceptDatatype
 */
@Root
public class ConceptDatatype implements java.io.Serializable {
	
	public static final long serialVersionUID = 473L;
	
	// HL7 abbreviations (along with our own boolean creature)
	
	public static final String BOOLEAN = "BIT";
	
	public static final String CODED = "CWE";
	
	public static final String DATE = "DT";
	
	public static final String DATETIME = "TS";
	
	public static final String DOCUMENT = "RP";
	
	public static final String NUMERIC = "NM";
	
	public static final String TEXT = "ST";
	
	public static final String TIME = "TM";
	
	// Fields
	
	private Integer conceptDatatypeId;
	
	private String name;
	
	private String description;
	
	private String hl7Abbreviation;
	
	private Date dateCreated;
	
	private User creator;
	
	private User retiredBy;
	
	private Boolean retired = Boolean.FALSE;
	
	private Date dateRetired;
	
	private String retireReason;
	
	// Constructors
	
	/** default constructor */
	public ConceptDatatype() {
	}
	
	/** constructor with id */
	public ConceptDatatype(Integer conceptDatatypeId) {
		this.conceptDatatypeId = conceptDatatypeId;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof ConceptDatatype) {
			ConceptDatatype c = (ConceptDatatype) obj;
			return (this.conceptDatatypeId.equals(c.getConceptDatatypeId()));
		}
		return false;
	}
	
	public int hashCode() {
		if (this.getConceptDatatypeId() == null)
			return super.hashCode();
		return this.getConceptDatatypeId().hashCode();
	}
	
	// Property accessors
	
	/**
	 * 
	 */
	@Attribute
	public Integer getConceptDatatypeId() {
		return this.conceptDatatypeId;
	}
	
	@Attribute
	public void setConceptDatatypeId(Integer conceptDatatypeId) {
		this.conceptDatatypeId = conceptDatatypeId;
	}
	
	/**
	 * 
	 */
	@Attribute
	public String getName() {
		return this.name;
	}
	
	@Attribute
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * 
	 */
	@Element(data = true)
	public String getDescription() {
		return this.description;
	}
	
	@Element(data = true)
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * @return Returns the hl7Abbreviation.
	 */
	@Attribute
	public String getHl7Abbreviation() {
		return hl7Abbreviation;
	}
	
	/**
	 * @param hl7Abbreviation The hl7Abbreviation to set.
	 */
	@Attribute
	public void setHl7Abbreviation(String hl7Abbreviation) {
		this.hl7Abbreviation = hl7Abbreviation;
	}
	
	/**
	 * 
	 */
	@Element
	public User getCreator() {
		return this.creator;
	}
	
	@Element
	public void setCreator(User creator) {
		this.creator = creator;
	}
	
	/**
	 * 
	 */
	@Element
	public Date getDateCreated() {
		return this.dateCreated;
	}
	
	@Element
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	/*
	 * Convenience methods for resolving common data types
	 */

	/**
	 * @return <code>true</code> if datatype is a numeric datatype
	 */
	public boolean isNumeric() {
		return NUMERIC.equals(getHl7Abbreviation());
	}
	
	/**
	 * @return <code>true</code> if datatype is coded (i.e., an identifier from a vocabulary)
	 */
	public boolean isCoded() {
		return CODED.equals(getHl7Abbreviation());
	}
	
	/**
	 * @return <code>true</code> if datatype is some representation of date or time
	 */
	public boolean isDate() {
		return DATE.equals(getHl7Abbreviation()) || DATETIME.equals(getHl7Abbreviation())
		        || TIME.equals(getHl7Abbreviation());
	}
	
	/**
	 * @return <code>true</code> if datatype is text-based
	 */
	public boolean isText() {
		return TEXT.equals(getHl7Abbreviation()) || DOCUMENT.equals(getHl7Abbreviation());
	}
	
	/**
	 * @return <code>true</code> if datatype is boolean
	 */
	public boolean isBoolean() {
		return BOOLEAN.equals(getHl7Abbreviation());
	}
	
	/**
	 * @return the retiredBy
	 */
	public User getRetiredBy() {
		return retiredBy;
	}
	
	/**
	 * @param retiredBy the retiredBy to set
	 */
	public void setRetiredBy(User retiredBy) {
		this.retiredBy = retiredBy;
	}
	
	/**
	 * @return the retired
	 */
	public Boolean getRetired() {
		return retired;
	}
	
	/**
	 * @param retired the retired to set
	 */
	public void setRetired(Boolean retired) {
		this.retired = retired;
	}
	
	/**
	 * @return the dateRetired
	 */
	public Date getDateRetired() {
		return dateRetired;
	}
	
	/**
	 * @param dateRetired the dateRetired to set
	 */
	public void setDateRetired(Date dateRetired) {
		this.dateRetired = dateRetired;
	}
	
	/**
	 * @return the retireReason
	 */
	public String getRetireReason() {
		return retireReason;
	}
	
	/**
	 * @param retireReason the retireReason to set
	 */
	public void setRetireReason(String retireReason) {
		this.retireReason = retireReason;
	}
	
}
