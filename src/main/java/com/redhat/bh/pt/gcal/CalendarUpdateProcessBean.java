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

import com.google.api.services.calendar.model.Calendar;
import com.redhat.bh.pt.gcal.conf.ProjectConfiguration;
import com.redhat.bh.pt.gcal.CalendarAgent;

@Singleton
@Named("calendarUpdateProcess")
public class CalendarUpdateProcessBean {

	private static final Logger LOG = LoggerFactory.getLogger(CalendarUpdateProcessBean.class);

	@Inject
	private CalendarAgent calendarAgent;

	private CalendarUpdateProcessBean() {
	}

	public void processICS(Exchange exchange) {

		Message in = exchange.getIn();
		String ptCalendarName = (String) exchange.getProperty("calendarName");

		InputStream body = (InputStream) in.getBody();

		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(body, writer, "UTF-8");
		} catch (IOException e) {
			LOG.error("Error occurred in processICS()", e);
		}
		
		long start = System.currentTimeMillis();

		calendarAgent.clearPTCalendar(ptCalendarName);
		Calendar calendar = calendarAgent.createPTCalendar(ptCalendarName);
		boolean success = calendarAgent.importCalendar(calendar, writer.toString());

		long end = System.currentTimeMillis();

		exchange.getIn().setHeader(ProjectConfiguration.HEADER_IMPORT_RESULT, success);
		exchange.getIn().setHeader(ProjectConfiguration.HEADER_IMPORT_DURATION, (end - start));

		LOG.debug(String.format("Completed route with result: %B", success));
	}

}
