package com.redhat.bh.pt.gcal;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.io.IOUtils;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.calendar.model.Calendar;
import com.redhat.bh.pt.gcal.conf.ProjectConfiguration;

@Singleton
@Named("calendarUpdateProcess")
public class CalendarUpdateProcessBean {

	private static final Logger LOG = LoggerFactory.getLogger(CalendarUpdateProcessBean.class);

	@Inject
	private CalendarAgent calendarAgent;

	@Inject
	@ConfigProperty(name = "GCAL_CLIENT_TOKEN_FILE")
	private String clientToken;

	@Inject
	@ConfigProperty(name = "GCAL_ACCESS_TOKEN")
	private String accessToken;

	@Inject
	@ConfigProperty(name = "GCAL_REFRESH_TOKEN")
	private String refreshToken;

	@Inject
	@ConfigProperty(name = "GCAL_TARGET_CALENDAR")
	private String ptCalendarName;

	private static TokenResponse token;

	private CalendarUpdateProcessBean() {
		token = new TokenResponse();
	}

	public void processICS(Exchange exchange) {

		Message in = exchange.getIn();
		InputStream body = (InputStream) in.getBody();

		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(body, writer, "UTF-8");
		} catch (IOException e) {
			LOG.error("Error occurred in processICS()", e);
		}

		long start = System.currentTimeMillis();

		GoogleCredential credential = calendarAgent.authorise(clientToken, accessToken, refreshToken, token);
		calendarAgent.clearPTCalendar(credential, ptCalendarName);
		Calendar calendar = calendarAgent.createPTCalendar(credential, ptCalendarName);
		boolean success = calendarAgent.importCalendar(credential, calendar, writer.toString());

		long end = System.currentTimeMillis();

		exchange.getIn().setHeader(ProjectConfiguration.HEADER_IMPORT_RESULT, success);
		exchange.getIn().setHeader(ProjectConfiguration.HEADER_IMPORT_DURATION, (end - start));

		LOG.debug(String.format("Completed route with result: %B", success));
	}

}
