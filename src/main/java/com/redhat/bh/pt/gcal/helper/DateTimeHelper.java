package com.redhat.bh.pt.gcal.helper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalUnit;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.EventDateTime;

public class DateTimeHelper {

	private static SimpleDateFormat gcalSdf = new SimpleDateFormat("yyyy-MM-dd");
	private static SimpleDateFormat icalSdf = new SimpleDateFormat("yyyyMMdd");

	private static String RRULE_RULE_SEPERATOR = ";";
	private static String RRULE_FREQ = "FREQ";
	private static String RRULE_UNTIL = "UNTIL";
	private static String RRULE_PROPERTY_SEPERATOR = "=";

	public static EventDateTime gteEventDateTimeForDate(Date date) {
		EventDateTime evt = new EventDateTime();
		DateTime dt = new DateTime(gcalSdf.format(date));
		evt.setDate(dt);
		return evt;
	}

	public static boolean compareStartDateEndDate(EventDateTime start, EventDateTime end) {
		boolean different = true;

		DateTime dtStart = start.getDateTime();
		DateTime dtEnd = end.getDateTime();

		if (dtStart.getValue() == dtEnd.getValue()) {
			different = false;
		}

		return different;
	}

	public static boolean compareStartDateEndDate(Date start, Date end) {
		boolean different = true;

		String startString = gcalSdf.format(start);
		String endString = gcalSdf.format(end);

		if (startString.equalsIgnoreCase(endString)) {
			different = false;
		}

		return different;
	}

	public static Date plusDate(Date date, int amount) {
		Date updatedDate = DateUtils.addDays(date, amount);
		return updatedDate;
	}

	public static Date computeEndDateFromRRule(String rrule) {
		Date excludedEndDate = null;
		String[] conditionals = rrule.split(RRULE_RULE_SEPERATOR);
		String endDateString = null;
		for (String condition : conditionals) {
			if (condition.startsWith(RRULE_UNTIL)) {
				// magic number to cope with the 'I also want to remove the
				// token seperator' syndrome
				endDateString = condition.substring(condition.indexOf(RRULE_PROPERTY_SEPERATOR) + 1);
			}
		}

		// GCAL uses an exclusion system, so the end date is always a day
		// after.
		try {
			excludedEndDate = DateTimeHelper.plusDate(icalSdf.parse(endDateString), 1);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return excludedEndDate;
	}
}
