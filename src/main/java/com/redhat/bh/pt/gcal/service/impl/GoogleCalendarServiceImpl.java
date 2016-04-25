package com.redhat.bh.pt.gcal.service.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.redhat.bh.pt.gcal.service.GoogleService;
import com.redhat.bh.pt.gcal.service.ICalendarService;

@Singleton
@Named("calendarService")
@GoogleService
public class GoogleCalendarServiceImpl implements ICalendarService {

	private static final Logger LOG = LoggerFactory.getLogger(GoogleCalendarServiceImpl.class);

	/**
	 * Client token location file
	 */
	@Inject
	@ConfigProperty(name = "GCAL_CLIENT_TOKEN_FILE")
	private String clientToken;

	@Inject
	@ConfigProperty(name = "GCAL_ACCESS_TOKEN")
	private String accessToken;

	@Inject
	@ConfigProperty(name = "GCAL_REFRESH_TOKEN")
	private String refreshToken;

	/** Application name. */
	private final String APPLICATION_NAME = "Google Calendar Integration";

	/** Global instance of the JSON factory. */
	private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private HttpTransport HTTP_TRANSPORT;

	private TokenResponse tokenResponse;

	/**
	 * Global instance of the scopes
	 *
	 * If modifying these scopes, delete your previously saved credentials at
	 * ~/.credentials/calendar-java-google.json
	 */
	private final List<String> SCOPES = Arrays.asList(CalendarScopes.CALENDAR);

	private com.google.api.services.calendar.Calendar service;

	public GoogleCalendarServiceImpl() {

		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			tokenResponse = new TokenResponse();
		} catch (GeneralSecurityException | IOException e) {
			LOG.error("Error occurred creating GoogleCalendarService", e);
			System.exit(1);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.redhat.bh.pt.gcal.service.impl.CalendarService#authorize()
	 */
	public Credential authorise() {

		GoogleClientSecrets clientSecrets = null;
		try {
			InputStreamReader is = new InputStreamReader(new FileInputStream(clientToken));
			clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, is);
		} catch (IOException e) {
			LOG.error("Error occurred on authorise()", e);
		}
		GoogleCredential cred = new GoogleCredential.Builder().setTransport(HTTP_TRANSPORT).setJsonFactory(JSON_FACTORY)
				.setClientSecrets(clientSecrets).build().setFromTokenResponse(tokenResponse);

		cred.setExpiresInSeconds((long) 60);
		if (cred.getAccessToken() == null && cred.getRefreshToken() == null) {
			cred.setAccessToken(accessToken);
			cred.setRefreshToken(refreshToken);
		}

		return cred;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.redhat.bh.pt.gcal.service.impl.CalendarService#getCalendarService()
	 */
	@Override
	public com.google.api.services.calendar.Calendar getCalendarService() {
		if (service == null) {
			Credential credential = authorise();
			service = new com.google.api.services.calendar.Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
					.setApplicationName(APPLICATION_NAME).build();
		}
		return service;
	}

}