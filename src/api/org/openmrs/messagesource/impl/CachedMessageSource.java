/**
 * 
 */
package org.openmrs.messagesource.impl;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.openmrs.messagesource.MutableMessageSource;
import org.openmrs.messagesource.PresentationMessage;
import org.openmrs.messagesource.PresentationMessageMap;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.context.support.StaticMessageSource;

/**
 * A MutableMessageSource backed by a localized map of PresentationMessageCollections, providing
 * in-memory storage of PresentationMessages. Useful for temporary storage, as a cache for other
 * sources, and for testing.
 */
public class CachedMessageSource extends AbstractMessageSource implements MutableMessageSource {
	
	Map<Locale, PresentationMessageMap> localizedMap = new HashMap<Locale, PresentationMessageMap>();
	
	/* (non-Javadoc)
	 * @see org.openmrs.messagesource.MutableMessageSource#addPresentation(org.openmrs.api.PresentationMessage)
	 */
	public void addPresentation(PresentationMessage message) {
		PresentationMessageMap codeMessageMap = localizedMap.get(message.getLocale());
		if (codeMessageMap == null) {
			codeMessageMap = new PresentationMessageMap(message.getLocale());
			localizedMap.put(message.getLocale(), codeMessageMap);
		}
		codeMessageMap.put(message.getCode(), message);
	}
	
	/* (non-Javadoc)
	 * @see org.openmrs.messagesource.MutableMessageSource#getLocales()
	 */
	public Collection<Locale> getLocales() {
		return localizedMap.keySet();
	}
	
	/* (non-Javadoc)
	 * @see org.openmrs.messagesource.MutableMessageSource#getPresentations()
	 */
	public Collection<PresentationMessage> getPresentations() {
		Collection<PresentationMessage> allMessages = new Vector<PresentationMessage>();
		
		for (Locale locale : localizedMap.keySet()) {
			PresentationMessageMap codeMessageMap = localizedMap.get(locale);
			allMessages.addAll(codeMessageMap.values());
		}
		
		return allMessages;
	}
	
	/* (non-Javadoc)
	 * @see org.openmrs.messagesource.MutableMessageSource#publishProperties(java.util.Properties, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void publishProperties(Properties arg0, String arg1, String arg2, String arg3, String arg4) {
		// ABKTODO: no-op?
	}
	
	/* (non-Javadoc)
	 * @see org.openmrs.messagesource.MutableMessageSource#removePresentation(org.openmrs.api.PresentationMessage)
	 */
	public void removePresentation(PresentationMessage message) {
		PresentationMessageMap codeMessageMap = localizedMap.get(message.getLocale());
		if ((codeMessageMap != null) && codeMessageMap.containsKey(message.getCode())) {
			codeMessageMap.remove(message.getCode());
		}
	}
	
	public void merge(MutableMessageSource fromSource, boolean overwrite) {
		for (PresentationMessage message : fromSource.getPresentations()) {
			addPresentation(message);
		}
	}
	
	/**
	 * @see org.openmrs.messagesource.MutableMessageSource#getPresentation(java.lang.String,
	 *      java.util.Locale)
	 */
	public PresentationMessage getPresentation(String key, Locale forLocale) {
		PresentationMessage foundPM = null;
		PresentationMessageMap codeMessageMap = localizedMap.get(forLocale);
		if ((codeMessageMap != null) && codeMessageMap.containsKey(key)) {
			foundPM = codeMessageMap.get(key);
		}
		return foundPM;
	}
	
	/**
	 * @see org.openmrs.messagesource.MutableMessageSource#getPresentationsInLocale(java.util.Locale)
	 */
	public Collection<PresentationMessage> getPresentationsInLocale(Locale locale) {
		Collection<PresentationMessage> foundPresentations = null;
		PresentationMessageMap codeMessageMap = localizedMap.get(locale);
		if (codeMessageMap != null) {
			foundPresentations = codeMessageMap.values();
		}
		return foundPresentations;
	}
	
	/**
	 * @see org.springframework.context.support.AbstractMessageSource#resolveCode(java.lang.String,
	 *      java.util.Locale)
	 */
	@Override
	protected MessageFormat resolveCode(String code, Locale locale) {
		MessageFormat resolvedMessageFormatForCode = null;
		
		PresentationMessage pmForCode = getPresentation(code, locale);
		if (pmForCode != null) {
			resolvedMessageFormatForCode = new MessageFormat(pmForCode.getMessage());
		}
		return resolvedMessageFormatForCode;
	}
	
}
