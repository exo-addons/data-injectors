package org.exoplatform.services.injection.impl;

import org.apache.commons.io.IOUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.injection.DataInjector;
import org.exoplatform.services.injection.module.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
public class DataInjectorImpl implements DataInjector{
    private static final Log LOG = ExoLogger.getLogger(DataInjectorImpl.class);

    /**
     * The scenario folder.
     */
    public static final String SCENARIOS_FOLDER = "/scenarios";

    /**
     * The scenario name attribute.
     */
    public static final String SCENARIO_NAME_ATTRIBUTE = "scenarioName";

    /**
     * The scenarios.
     */
    private static Map<String, JSONObject> scenarios;

    UserModule userModule_;

    /**
     * The space service.
     */
    SpaceModule spaceModule_;

    /**
     * The calendar service.
     */
    CalendarModule calendarModule_;

    /**
     * The wiki service.
     */
    WikiModule wikiModule_;

    /**
     * The forum service.
     */
    ForumModule forumModule_;

    /**
     * The document service.
     */
    DocumentModule documentModule_;

    /**
     * The activity service.
     */
    ActivityModule activityModule_;

    public DataInjectorImpl(UserModule userModule,SpaceModule spaceModule, CalendarModule calendarModule, WikiModule wikiModule ,ForumModule forumModule, DocumentModule documentModule, ActivityModule activityModule) {

        userModule_ = userModule;
        spaceModule_ =spaceModule;
        calendarModule_= calendarModule;
        wikiModule_= wikiModule;
        forumModule_ = forumModule;
        documentModule_ = documentModule;
        activityModule_ = activityModule;
        /**

         //--- Init Services
         userModule_ = CommonsUtils.getService(UserModule.class);
         spaceModule_ = CommonsUtils.getService(SpaceModule.class);
         calendarModule_ = CommonsUtils.getService(CalendarModule.class);
         wikiModule_ = CommonsUtils.getService(WikiModule.class);
         forumModule_ = CommonsUtils.getService(ForumModule.class);
         documentModule_ = CommonsUtils.getService(DocumentModule.class);
         activityModule_ = CommonsUtils.getService(ActivityModule.class);
         //--- Init configuration
         */
        setup();

    }

    /**
     *
     */
    public void setup() {
        scenarios = new HashMap<String, JSONObject>();
        try {
            File folder = new File(Thread.currentThread().getContextClassLoader().getResource(SCENARIOS_FOLDER).toURI());

            for (String fileName : folder.list()) {
                InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(SCENARIOS_FOLDER + "/" + fileName);
                String fileContent = getData(stream);
                try {
                    JSONObject json = new JSONObject(fileContent);
                    String name = json.getString(SCENARIO_NAME_ATTRIBUTE);
                    scenarios.put(name, json);
                } catch (JSONException e) {
                    LOG.error("Syntax error in scenario " + fileName, e);
                }
            }
        } catch (URISyntaxException e) {
            LOG.error("Unable to read scenario file", e);
        }
    }

    @Override
    public void inject() throws Exception {
        //--- Inject Data into the Store
        scenarios.forEach((k, v) -> {
            process(k);
        });

    }

    @Override
    public void purge() throws Exception {

    }

    public void process(String scenarioName) {

        LOG.info("Start {} .............", this.getClass().getName());
        //--- Start data injection
        String downloadUrl = "";
        try {
            JSONObject scenarioData = scenarios.get(scenarioName).getJSONObject("data");
            if (scenarioData.has("users")) {
                LOG.info("Create " + scenarioData.getJSONArray("users").length() + " users.");
                userModule_.createUsers(scenarioData.getJSONArray("users"));

            }

            if (scenarioData.has("relations")) {
                LOG.info("Create " + scenarioData.getJSONArray("relations").length() + " relations.");
                userModule_.createRelations(scenarioData.getJSONArray("relations"));
            }

            if (scenarioData.has("spaces")) {
                LOG.info("Create " + scenarioData.getJSONArray("spaces").length() + " spaces.");
                spaceModule_.createSpaces(scenarioData.getJSONArray("spaces"));
            }
/**
 if (scenarioData.has("calendars")) {
 LOG.info("Create " + scenarioData.getJSONArray("calendars").length() + " calendars.");
 calendarModule_.setCalendarColors(scenarioData.getJSONArray("calendars"));
 calendarModule_.createEvents(scenarioData.getJSONArray("calendars"));
 }
 */

            if (scenarioData.has("wikis")) {
                LOG.info("Create " + scenarioData.getJSONArray("wikis").length() + " wikis.");
                wikiModule_.createUserWiki(scenarioData.getJSONArray("wikis"));
            }

            if (scenarioData.has("activities")) {

                LOG.info("Create " + scenarioData.getJSONArray("activities").length() + " activities.");
                activityModule_.pushActivities(scenarioData.getJSONArray("activities"));
            }
            if (scenarioData.has("documents")) {
                LOG.info("Create " + scenarioData.getJSONArray("documents").length() + " documents.");
                documentModule_.uploadDocuments(scenarioData.getJSONArray("documents"));
            }
            if (scenarioData.has("forums")) {
                forumModule_.createForumContents(scenarioData.getJSONArray("forums"));
            }


            /**

             if (scenarios.get(datasetUsesCaseName).has("scriptData")) {
             try {
             downloadUrl = documentModule_.storeScript(scenarios.get(datasetUsesCaseName).getString("scriptData"));

             } catch (Exception E) {
             LOG.error("Error to store Data Script {}",  scenarios.get(datasetUsesCaseName).getString("scriptData"), E);
             } finally {

             }

             }
             */
            LOG.info("Data Injection has been done successfully.............");

        } catch (JSONException e) {
            LOG.error("Syntax error when reading scenario " + scenarioName, e);
        }
    }

    /**
     * Gets the data.
     *
     * @param inputStream the input stream
     * @return the data
     */
    public String getData(InputStream inputStream) {
        String out = "";
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(inputStream, writer);
            out = writer.toString();

        } catch (IOException e) {
            e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
        }

        return out;
    }
}
