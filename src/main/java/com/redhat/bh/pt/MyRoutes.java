/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.redhat.bh.pt;

import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;
import org.apache.camel.component.properties.DefaultPropertiesParser;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.properties.PropertiesParser;
import org.apache.camel.component.stax.StAXBuilder;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.deltaspike.core.api.config.ConfigResolver;

import com.redhat.bh.pt.gcal.conf.ProjectConfiguration;
import com.redhat.bh.pt.gcal.helper.CalendarInfo;

/**
 * Configures all our Camel routes, components, endpoints and beans
 */




@ContextName("camelContext")
public class MyRoutes extends RouteBuilder {
	

	@Inject
	@Uri("timer://icsPoll?fixedRate=true&period={{env:GCAL_REFRESH_RATE_SECONDS}}s")
	private Endpoint timerEndpoint;
	
	@Inject
	@Uri("log:output")
	private Endpoint resultEndpoint;
	
	@Inject
	@Uri("file://src/main/resources/?fileName=UKI_ICal_Endpoints.xml&noop=true&idempotent=false")
	private Endpoint sourceInputFile;
	
	private static final String HEADER_IN_FORMAT = "${in.header.%s}";
	
	@Override
	public void configure() throws Exception {
		this.getContext().setStreamCaching(true);
		
		
		from("seda:processRequest?concurrentConsumers=8")
		.setProperty("calendarName", simple("${body.name}"))
		.setProperty("calendarEndpoint", simple("${body.endpoint}"))
		.process(new Processor() {		
			@Override
			public void process(Exchange exchange) throws Exception {		
				exchange.getOut();
				}
		})
		.id("gcalUpdateRoute")
		.recipientList(simple("${exchangeProperty.calendarEndpoint}")).id("ical-source")
		.log("Beginning update").id("start-log")
		.beanRef("calendarUpdateProcess").id("calendar-processor")
		.choice().id("output-msg-selector")
			.when().simple(String.format(HEADER_IN_FORMAT, ProjectConfiguration.HEADER_IMPORT_RESULT) + "== true")
				.log(String.format("Update for ${exchangeProperty.calendarName} completed successfully in ${in.header.%s}s", ProjectConfiguration.HEADER_IMPORT_DURATION)).id("output-msg-success")
			.otherwise()
				.log("Update failed for ${exchangeProperty.calendarEndpoint}").id("output-msg-fail");
		
		 
		 from("direct:pollfile")
		 .pollEnrich(sourceInputFile.getEndpointUri())
	     .split(StAXBuilder.stax(CalendarInfo.class))
		 .to("seda:processRequest");
		 
		 from(timerEndpoint)
		 .to("direct:pollfile");

		
	}
	
}

	

