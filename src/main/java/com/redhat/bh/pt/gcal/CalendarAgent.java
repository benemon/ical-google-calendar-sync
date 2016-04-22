package com.redhat.bh.pt.gcal;



import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.redhat.bh.pt.gcal.helper.DateTimeHelper;
import com.redhat.bh.pt.gcal.GoogleCalendarService;


import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;

@Singleton
@Named("calendarAgent")
public class CalendarAgent {

	private static final Logger LOG = LoggerFactory.getLogger(CalendarAgent.class);

	private static final String SUMMARY_MASK = "%s - %s";

	private static final String VERSION_MASK = "%s-%s";

	private static final String LOG_ADDED_CALENDAR = "Calendar %s is not present. Creating...";
	private static final String LOG_CLEARED_EVENTS = "Cleared %d events from Calendar %s";
	private static final String LOG_ADDED_EVENTS = "Added %d events to Calendar %s";

	private static final String ICAL_RECURRENCE_PROPETRY_ID = "RRULE";
	private static final String NON_BILLABLE_KEY = "non-billable";

	private static com.google.api.services.calendar.Calendar client;

	public CalendarAgent() {
		client = GoogleCalendarService.getCalendarService();
	}

	public CalendarList getCalendarList() {

		CalendarList list = null;
		try {
			list = client.calendarList().list().execute();
		} catch (IOException e) {
			LOG.error("Error occurred on getCalendarList()", e);
		}

		return list;
	}
	
	public int getNumberOfEvents(String calendarName) {

		try {
		    return client.events().list(getCalendarId(calendarName)).execute().getItems().size();

		} catch (IOException e) {
			LOG.error("Error occurred on getNumberOfEvents()", e);
		}
		return -1;
	}
	
	public int addEvent(String calendarName,Event event) {

		try {			
			client.events().insert(getCalendarId(calendarName), event).execute();
			
		} catch (IOException e) {
			LOG.error("Error occurred on addEvent()", e);
		}
		return -1;
	}

	public String getCalendarId(String calendarName) {
		
		String calenderId = "";
		try{
			CalendarList list = client.calendarList().list().execute();
	
			Optional<CalendarListEntry> calendarListEntryOptional = list.getItems().stream()
					.filter(t -> t.getSummary().equalsIgnoreCase(calendarName)).findFirst();
			
			calenderId = calendarListEntryOptional.get().getId();
		
		} catch (IOException e) {
			LOG.error("Error occurred on getCalendarId()", e);
		}
		catch (NoSuchElementException e) {
			LOG.info("getCalendarId() -> Couldn't find GCalendar: " + calendarName);
		}

	    return calenderId;
	}
	
	public boolean deleteCalendar(String calendarName) {
		try {
			
			client.calendars().delete(getCalendarId(calendarName));
			
		} catch (IOException e) {
			LOG.error("Error occurred on deleteCalendar()", e);
			return false;
		}
		LOG.debug("GCalendar "+calendarName+" has been deleted");

		return true;
	}
	protected CalendarListEntry getCalendarListEntry( String calendarName) {

		CalendarListEntry listEntry = null;

		try {
			CalendarList list = client.calendarList().list().execute();

			Optional<CalendarListEntry> calendarListEntryOptional = list.getItems().stream()
					.filter(t -> t.getSummary().equalsIgnoreCase(calendarName)).findFirst();

			listEntry = calendarListEntryOptional.get();
		} catch (IOException e) {
			LOG.error("Error occurred on getCalendarListEntry()", e);
		}
		catch (NoSuchElementException e) {
			LOG.info("getCalendarListEntry() -> Couldn't find GCalendar: " + calendarName);
		}

		return listEntry;
	}

	protected com.google.api.services.calendar.model.Calendar getCalendar(
			String calendarName) {
		com.google.api.services.calendar.model.Calendar calendar = null;

		CalendarListEntry listEntry = this.getCalendarListEntry( calendarName);

		if (listEntry != null) {
			try {
				calendar = client.calendars().get(listEntry.getId()).execute();
			} catch (IOException e) {
				LOG.error("Error occurred on getCalendar()", e);
			}
		}

		return calendar;
	}

	public boolean clearPTCalendar(String calendarName) {

		boolean success = true;
		try {
			CalendarListEntry calendar = this.getCalendarListEntry(calendarName);

			if (calendar != null) {
				// client.calendars().delete(calendar.getId()).execute();

				BatchRequest batchDelete = client.batch();

				// Create the callback.
				JsonBatchCallback<Void> callback = new JsonBatchCallback<Void>() {
					@Override
					public void onSuccess(Void event, HttpHeaders responseHeaders) {
					}

					@Override
					public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
						LOG.error("Error occurred on onFailure()", e.getErrors());
					}
				};

				String pageToken = null;
				int totalEvents = 0;
				do {
					Events events = client.events().list(calendar.getId()).setSingleEvents(true).setPageToken(pageToken)
							.execute();
					if (events.getItems() != null && events.getItems().size() > 0) {
						for (Event e : events.getItems()) {
							client.events().delete(calendar.getId(), e.getId()).queue(batchDelete, callback);
							totalEvents++;
						}
						pageToken = events.getNextPageToken();
					}

				} while (pageToken != null);

				if (totalEvents > 0) {
					batchDelete.execute();
					LOG.info(String.format(LOG_CLEARED_EVENTS, totalEvents, calendarName));
				} else {
					LOG.info("Nothing to clear. Skipping...");
				}

			}

		} catch (IOException e) {
			LOG.error("Error occurred on clearPTCalendar()", e);
			success = false;
		}
		return success;

	}

	public com.google.api.services.calendar.model.Calendar createPTCalendar(
			String calendarName) {

		com.google.api.services.calendar.model.Calendar targetCalendar = null;
		try {
			targetCalendar = this.getCalendar( calendarName);

			if (targetCalendar == null) {
				targetCalendar = new com.google.api.services.calendar.model.Calendar();
				targetCalendar.setSummary(calendarName);
				targetCalendar = client.calendars().insert(targetCalendar).execute();
				LOG.info(String.format(LOG_ADDED_CALENDAR, calendarName));
			}

		} catch (IOException e) {
			LOG.error("Error occurred on createPTCalendar()", e);
		}
		return targetCalendar;
	}

	public boolean importCalendar( com.google.api.services.calendar.model.Calendar calendar,
			String icsContent) {
		return importCalendar( calendar, icsContent, false);
	}

	public boolean importCalendar( com.google.api.services.calendar.model.Calendar calendar,
			String icsContent, boolean dryRun) {
		boolean result = true;
		Calendar calendarContent = this.buildCalendar(icsContent);

		List<Event> processedEvents = this.processCalendarContent(calendarContent);

		if (!dryRun) {
			result = this.importCalendarContent( calendar, processedEvents);
		}
		return result;

	}

	private boolean importCalendarContent(
			com.google.api.services.calendar.model.Calendar calendar, List<Event> events) {

		boolean success = true;
		try {
			BatchRequest batchImport = client.batch();

			// Create the callback.
			JsonBatchCallback<Event> callback = new JsonBatchCallback<Event>() {

				@Override
				public void onSuccess(Event event, HttpHeaders responseHeaders) {
				}

				@Override
				public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
					LOG.error("Error occurred on onFailure()", e.getErrors());
				}
			};

			for (Event e : events) {
				// client.events().insert(calendar.getId(), e).execute());
				client.events().insert(calendar.getId(), e).queue(batchImport, callback);
			}

			batchImport.execute();
			LOG.info(String.format(LOG_ADDED_EVENTS, events.size(), calendar.getSummary()));

		} catch (IOException e) {
			LOG.error("Error occurred on importCalendarContent()", e);
			success = false;
		}

		return success;
	}

	private Calendar buildCalendar(InputStream stream) {
		System.setProperty("ical4j.parsing.relaxed", "true");
		CalendarBuilder builder = new CalendarBuilder();
		InputStreamReader is = new InputStreamReader(stream);
		Calendar calendar = null;
		try {
			calendar = builder.build(is);
		} catch (IOException | ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return calendar;
	}
   /*
	private Calendar buildCalendar(File icsFile) {

		Calendar calendar = null;
		try {
			calendar = buildCalendar(new FileInputStream(icsFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return calendar;
	} */

	private Calendar buildCalendar(String icsFileContent) {

		Calendar calendar = null;
		calendar = buildCalendar(IOUtils.toInputStream(icsFileContent));
		return calendar;
	}

	private List<Event> processCalendarContent(Calendar calendar) {

		List<Event> events = null;

		List<CalendarComponent> components = calendar.getComponents();
		events = new ArrayList<Event>();
		for (CalendarComponent c : components) {
			if (c instanceof VEvent) {
				VEvent v = (VEvent) c;
				Event gcalEvent = new Event();

				// Grab the dates
				Date startDate = v.getStartDate().getDate();
				Date endDate = v.getEndDate().getDate();

				// Make some incredibly sketchy assumptions and hope everything
				// works out OK in the end.
				// If anyone has a better suggestion, feel free. My source iCal
				// is...dubious.
				Property recurrenceRule = v.getProperty(ICAL_RECURRENCE_PROPETRY_ID);
				String ruleValue = null;
				if (recurrenceRule != null) {
					ruleValue = recurrenceRule.getValue();
					endDate = DateTimeHelper.computeEndDateFromRRule(ruleValue);
				}

				gcalEvent.setStart(DateTimeHelper.gteEventDateTimeForDate(startDate));
				gcalEvent.setEnd(DateTimeHelper.gteEventDateTimeForDate(endDate));

				// Null checks on optional fields
				String summary = v.getSummary() != null ? v.getSummary().getValue() : "";
				String description = v.getDescription() != null ? v.getDescription().getValue() : "";
				String status = v.getStatus() != null ? v.getStatus().getValue() : "";

				// Fixes use case where ics summary is only filled out for
				// billable activity in PT
				if (summary.equalsIgnoreCase(NON_BILLABLE_KEY)) {
					gcalEvent.setSummary(String.format(SUMMARY_MASK, description, status));
				} else {
					gcalEvent.setSummary(String.format(SUMMARY_MASK, summary, status));
				}

				gcalEvent.setDescription(description);
				gcalEvent.setICalUID(String.format(VERSION_MASK, generateUid(), v.getUid().getValue()));
				events.add(gcalEvent);
			}
		}

		return events;
	}

	public String generateUid() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}
}
