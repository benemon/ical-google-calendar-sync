# ical-google-calendar-sync

This is a small project based on Camel and Fuse Integration Services, to syncronise an iCalendar source with Google Calendar.

The following environment variables need to be set:

* **GCAL_ACCESS_TOKEN**
The Google Calendar API Access Token. Source from https://developers.google.com/oauthplayground/

* **GCAL_REFRESH_TOKEN**
The Google Calendar API Refresh Token. Source from https://developers.google.com/oauthplayground/. Will be refreshed by the route as required.

* **GCAL_CLIENT_TOKEN_FILE**
The location of the Google Calendar API Token. Downloadable from https://console.developers.google.com/.

* **GCAL_REFRESH_RATE_SECONDS**
The rate at which we want to refresh Google Calendar from the iCalendar source

* **GCAL_TARGET_CALENDAR**
The target Google Calendar. Will be created if it doesn't exist. 

**NOTE: DO NOT MAKE THIS YOUR PRIMARY CALENDAR. ALL YOUR CONTENT WILL BE DELETED IF YOU DO. I CANNOT STRESS HIGHLY ENOUGH HOW MUCH OF A BAD IDEA THAT IS.**

* **ICAL_ENDPOINT**
The source iCalendar endpoint

----------
### Running the example locally

The route can be run locally using the following Maven goal:

    mvn clean install exec:java

### Running the example using OpenShift S2I template

The example can also be built and run using the included S2I template *calendar-sync-template.yaml*

The template file can be used to create an OpenShift application template by executing the command:

    oc create -f calendar-sync-template.yaml

There are slightly differences here, as OpenShift required your GCAL_CLIENT_TOKEN_FILE to be mounted as a Secret. This can be done by executing the add secret command:

	oc secrets new gcal google-client-secret.json

This adds a new Secret 'gcal' to the namespace.

After providing the secret, the following parameters must be taken into account in the S2I template, in addition to those provided above.

 * **GCAL_CLIENT_TOKEN_VOLUME**
The Volume in which to mount the GCAL_CLIENT_TOKEN_FILE. Must include trailing slash (long story). Thereafter, GCAL_CLIENT_TOKEN_FILE simply becomes the file name, rather than the entire path.

 * **GCAL_CLIENT_TOKEN_FILE_SECRET**
The name of the OpenShift Secret into which the GCAL_CLIENT_TOKEN_FILE has been passed. This is the object mounted as a Volume within the Pod. Must be created before this template is instantiated.

The application template can then be instatiated, providing the environment variables listed above as parameters on either the OpenShift CLI, or the Web Console.





