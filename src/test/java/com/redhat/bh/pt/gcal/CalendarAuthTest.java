package com.redhat.bh.pt.gcal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.redhat.bh.pt.gcal.service.ICalendarService;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CalendarAuthTest {

	private static CalendarAgent agent;
	private static final Logger LOG = LoggerFactory.getLogger(CalendarAuthTest.class);

	private final String PT_CALENDAR = "PTExportTest";
	private final String CALENDAR_ID = "testtesttest@googleusercontent.something.com";
	private final String ICS_FILE = "src/main/resources/allocation.ics";

	@BeforeClass
	public static void setUp() throws Exception {
		agent = new CalendarAgent();
		ICalendarService gcservice = new GoogleCalendarServiceTestImpl();
		
		agent.setCalendarService(gcservice);
		agent.postConstruct();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		agent = null;
	}

	@Test
	public void test1CreateCalendar() throws Exception {
		Calendar ptCal = agent.createPTCalendar(PT_CALENDAR);
		assertNotNull(ptCal);
	}

	@Test
	public void test2GetCalendarList() throws Exception {

		CalendarList list = agent.getCalendarList();
		assertNotNull(list);
		Optional<CalendarListEntry> calendarListEntry = list.getItems().stream()
				.filter(t -> t.getSummary().equalsIgnoreCase(PT_CALENDAR)).findFirst();

		assertNotNull(calendarListEntry);
		assertTrue(calendarListEntry.isPresent());
	}

	@Test
	public void test3AddEventToCalendar() throws Exception {

		int numEvents = agent.getNumberOfEvents(PT_CALENDAR);
		Event event = new Event().setSummary("RedHat Test Event").setLocation("800 Howard St., San Francisco, CA 94103")
				.setDescription("A chance to hear more about Google's developer products.");

		DateTime startDateTime = new DateTime("2016-04-21T13:00:00+01:00");

		EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone("Europe/London");
		event.setStart(start);

		DateTime endDateTime = new DateTime("2016-04-21T14:00:00+01:00");
		EventDateTime end = new EventDateTime().setDateTime(endDateTime).setTimeZone("Europe/London");
		event.setEnd(end);

		agent.addEvent(PT_CALENDAR, event);

		int newNumEvents = agent.getNumberOfEvents(PT_CALENDAR);

		assertTrue(numEvents + 1 == newNumEvents);

	}

	@Test
	public void test4ImportCalendar() throws Exception {

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

		boolean output = agent.importCalendar(cal, writer.toString(), true);

		assertTrue(output);
	}

	@Test
	public void test5GetCalendarListEntry() throws Exception {

		CalendarListEntry entry = agent.getCalendarListEntry(PT_CALENDAR);
		assertNotNull(entry);
	}

	@Test
	public void test6EndToEndImportCalendar() throws Exception {
		LOG.info("End2End test Started");

		boolean result = agent.clearPTCalendar(PT_CALENDAR);
		assertTrue(result);

		Calendar ptCal = agent.createPTCalendar(PT_CALENDAR);
		assertNotNull(ptCal);

		InputStream is = new FileInputStream(ICS_FILE);

		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(is, writer, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LOG.info("Importing test calendar from: " + ICS_FILE);
		boolean output = agent.importCalendar(ptCal, writer.toString());
		assertTrue(output);
		LOG.info("End2End test Completed");
	}

	@Test
	public void test7ClearEventsFromCalendar() throws Exception {

		boolean result = agent.clearPTCalendar(PT_CALENDAR);
		assertTrue(result);
	}

	@Test
	public void test8DeleteCalendar() throws Exception {

		boolean result = agent.deleteCalendar(PT_CALENDAR);
		assertTrue(result);
	}

	@Test
	public void test9UUIDRandomness() {

		Set<String> set = new HashSet<String>();
		for (int i = 0; i < 5000; i++) {
			String uid = agent.generateUid();
			assertNotNull(uid);
			assertFalse(set.contains(uid));
			set.add(uid.toString());
		}
		assertEquals(set.size(), 5000);

	}

}
