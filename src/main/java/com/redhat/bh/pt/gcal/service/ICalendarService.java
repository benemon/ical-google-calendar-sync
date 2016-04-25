package com.redhat.bh.pt.gcal.service;

import java.io.IOException;

import com.google.api.client.auth.oauth2.Credential;

public interface ICalendarService {

	/**
	 * Creates an authorized Credential object.
	 * 
	 * @return an authorized Credential object.
	 * @throws IOException
	 */
	public Credential authorise();

	/**
	 * Build and return an authorized Calendar client service.
	 * 
	 * @return an authorized Calendar client service
	 * @throws IOException
	 */
	public com.google.api.services.calendar.Calendar getCalendarService();

}