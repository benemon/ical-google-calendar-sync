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

import javax.inject.Inject;

import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;

/**
 * Configures all our Camel routes, components, endpoints and beans
 */
@ContextName("camelRoute")
public class MyRoutes extends RouteBuilder {

	@Inject
	@Uri("scheduler://icsPoll?delay={{env:GCAL_REFRESH_RATE_SECONDS}}")
	private Endpoint inputEndpoint;

	@Inject
	@Uri("{{env:ICAL_ENDPOINT}}")
	private Endpoint icalEndpoint;

	@Inject
	@Uri("log:output")
	private Endpoint resultEndpoint;

	// Properties
	@Override
	public void configure() throws Exception {

		this.getContext().setStreamCaching(true);
		from(inputEndpoint).id("gcalUpdateRoute").to(icalEndpoint).log("${body}").beanRef("calendarUpdateProcess")
				.log("Update completed");
	}

}
