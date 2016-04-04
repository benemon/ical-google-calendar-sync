package com.redhat.bh.pt.gcal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;

public class CalendarAuthTest {

	private static CalendarAgent agent;

	private final String ACCESS_TOKEN = "ya29..twITnIDkfQdmc_SCxVxwCxZ2oxOPqLxCYpy8hq-yMbeoc7Z-jMBK7zSxfa5P1yolmtc";
	private final String REFRESH_TOKEN = "1/96TUNjU10YRLwLcOgiRn4IdmODYYR91bfdT1LZoc5ko";

	private final String CLIENT_SECRET_LOCATION = "/Users/benjaminholmes/client_secret_147163994967-9vkmddng96idomioflvpl3bs0566282g.apps.googleusercontent.com.json";
	private final String ICS_FILE = "/Users/benjaminholmes/Downloads/allocations.ics";
	private final String PT_CALENDAR = "PTExport";
	private final String CALENDAR_ID = "testtesttest@googleusercontent.something.com";

	@BeforeClass
	public static void setUp() throws Exception {
		agent = new CalendarAgent();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		agent = null;
	}

	@Test
	public void testAuthorise() throws Exception {
		TokenResponse response = new TokenResponse();
		GoogleCredential credentials = agent.authorise(CLIENT_SECRET_LOCATION, ACCESS_TOKEN, REFRESH_TOKEN, response);
		assertNotNull(credentials);
	}

	@Test
	public void testGetCalendarList() throws Exception {
		TokenResponse response = new TokenResponse();
		GoogleCredential credentials = agent.authorise(CLIENT_SECRET_LOCATION, ACCESS_TOKEN, REFRESH_TOKEN, response);
		assertNotNull(credentials);

		CalendarList list = agent.getCalendarList(credentials);
		assertNotNull(list);
		Optional<CalendarListEntry> calendarListEntry = list.getItems().stream()
				.filter(t -> t.getSummary().equalsIgnoreCase(PT_CALENDAR)).findFirst();

		assertNotNull(calendarListEntry);
		assertTrue(calendarListEntry.isPresent());
	}

	@Test
	public void testGetCalendarListEntry() throws Exception {
		TokenResponse response = new TokenResponse();
		GoogleCredential credentials = agent.authorise(CLIENT_SECRET_LOCATION, ACCESS_TOKEN, REFRESH_TOKEN, response);
		assertNotNull(credentials);

		CalendarListEntry entry = agent.getCalendarListEntry(credentials, PT_CALENDAR);
		assertNotNull(entry);
	}

	@Test
	public void testEndToEndImportCalendar() throws Exception {

		TokenResponse response = new TokenResponse();
		GoogleCredential credentials = agent.authorise(CLIENT_SECRET_LOCATION, ACCESS_TOKEN, REFRESH_TOKEN, response);
		assertNotNull(credentials);

		boolean result = agent.clearPTCalendar(credentials, PT_CALENDAR);
		assertTrue(result);

		Calendar ptCal = agent.createPTCalendar(credentials, PT_CALENDAR);
		assertNotNull(ptCal);

		InputStream is = new FileInputStream(ICS_FILE);

		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(is, writer, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		boolean output = agent.importCalendar(credentials, ptCal, writer.toString());
		assertTrue(output);
	}

	@Test
	public void testImportCalendar() throws Exception {

		Calendar cal = new Calendar();
		cal.setId(CALENDAR_ID);
		InputStream is = new FileInputStream(ICS_FILE);

		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(is, writer, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		boolean output = agent.importCalendar(null, cal, writer.toString(), true);

		assertTrue(output);
	}

	@Test
	public void testCreateCalendar() throws Exception {
		TokenResponse response = new TokenResponse();
		GoogleCredential credentials = agent.authorise(CLIENT_SECRET_LOCATION, ACCESS_TOKEN, REFRESH_TOKEN, response);
		assertNotNull(credentials);

		Calendar ptCal = agent.createPTCalendar(credentials, PT_CALENDAR);

		assertNotNull(ptCal);

		// needs fixing

	}

	@Test
	public void testClearCalendar() throws Exception {
		TokenResponse response = new TokenResponse();
		GoogleCredential credentials = agent.authorise(CLIENT_SECRET_LOCATION, ACCESS_TOKEN, REFRESH_TOKEN, response);
		assertNotNull(credentials);

		boolean result = agent.clearPTCalendar(credentials, PT_CALENDAR);
		assertTrue(result);
	}

}
