package org.exoplatform.services.injection.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.injection.DataInjector;
import org.exoplatform.services.injection.InjectorUtils;
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

public class DataInjectorImpl implements DataInjector {

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

    /**
     * Default data folder path
     */
    private final static String DATA_INJECTION_FOLDER_PATH = "data-injection-folder-path";

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

    private String dataFolderPath = "";

    public DataInjectorImpl(InitParams params, UserModule userModule, SpaceModule spaceModule, CalendarModule calendarModule, WikiModule wikiModule, ForumModule forumModule, DocumentModule documentModule, ActivityModule activityModule) {

        userModule_ = userModule;
        spaceModule_ = spaceModule;
        calendarModule_ = calendarModule;
        wikiModule_ = wikiModule;
        forumModule_ = forumModule;
        documentModule_ = documentModule;
        activityModule_ = activityModule;

        //--- Get default data folder
        ValueParam dataFolderPathParam = params.getValueParam(DATA_INJECTION_FOLDER_PATH);
        if (dataFolderPathParam != null) {
            dataFolderPath = dataFolderPathParam.getValue();
        }

        //--- Launch setup process
        setup(dataFolderPath);

    }


    /**
     * Load injection scripts
     */
    public void setup(String dataFolderPath) {
        scenarios = new HashMap<String, JSONObject>();
        try {

            //--- Get injection usescase
            File scenariosFolder = new File(InjectorUtils.getConfigPath(dataFolderPath) + SCENARIOS_FOLDER);

            for (String fileName : scenariosFolder.list()) {
                InputStream stream = FileUtils.openInputStream(new File(InjectorUtils.getConfigPath(dataFolderPath) + SCENARIOS_FOLDER + "/" + fileName));

                String fileContent = getData(stream);
                try {
                    JSONObject json = new JSONObject(fileContent);
                    String name = json.getString(SCENARIO_NAME_ATTRIBUTE);
                    scenarios.put(name, json);
                } catch (JSONException e) {
                    LOG.error("Syntax error in scenario " + fileName, e);
                }
            }
        } catch (URISyntaxException use) {
            LOG.error("Unable to read scenario file", use);
        } catch (Exception e) {
            LOG.error("Unable to find scenario file", e);
        }
    }

    @Override
    public void inject() throws Exception {
        //--- Inject Data into the Store
        scenarios.forEach((k, v) -> {
            inject(k);
        });

    }

    @Override
    public void purge() throws Exception {
        //--- Purge Data into the Store
        scenarios.forEach((k, v) -> {
            purge(k);
        });


    }

    public void inject(String scenarioName) {

        LOG.info("Start {} .............", this.getClass().getName());
        //--- Start data injection
        String downloadUrl = "";
        try {
            JSONObject scenarioData = scenarios.get(scenarioName).getJSONObject("data");
            if (scenarioData.has("users")) {
                LOG.info("Create " + scenarioData.getJSONArray("users").length() + " users.");
                userModule_.createUsers(scenarioData.getJSONArray("users"), dataFolderPath);

            }

            if (scenarioData.has("relations")) {
                LOG.info("Create " + scenarioData.getJSONArray("relations").length() + " relations.");
                userModule_.createRelations(scenarioData.getJSONArray("relations"));
            }

            if (scenarioData.has("spaces")) {
                LOG.info("Create " + scenarioData.getJSONArray("spaces").length() + " spaces.");
                spaceModule_.createSpaces(scenarioData.getJSONArray("spaces"), dataFolderPath);
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
                wikiModule_.createUserWiki(scenarioData.getJSONArray("wikis"),dataFolderPath);
            }


            if (scenarioData.has("activities")) {

                LOG.info("Create " + scenarioData.getJSONArray("activities").length() + " activities.");
                activityModule_.pushActivities(scenarioData.getJSONArray("activities"));
            }
            if (scenarioData.has("documents")) {
                LOG.info("Create " + scenarioData.getJSONArray("documents").length() + " documents.");
                documentModule_.uploadDocuments(scenarioData.getJSONArray("documents"),dataFolderPath);
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

    public void purge(String scenarioName) {

        LOG.info("Purge {} .............", this.getClass().getName());
        //--- Start data injection
        String downloadUrl = "";
        try {
            JSONObject scenarioData = scenarios.get(scenarioName).getJSONObject("data");

            if (scenarioData.has("spaces")) {
                LOG.info("Create " + scenarioData.getJSONArray("spaces").length() + " spaces.");
                spaceModule_.purgeSpaces(scenarioData.getJSONArray("spaces"));
            }

            if (scenarioData.has("users")) {
                LOG.info("Purge " + scenarioData.getJSONArray("users").length() + " users.");
                userModule_.purgeUsers(scenarioData.getJSONArray("users"));

            }
            /**

             if (scenarioData.has("relations")) {
             LOG.info("Purge " + scenarioData.getJSONArray("relations").length() + " relations.");
             userModule_.purgeRelations(scenarioData.getJSONArray("relations"));
             }
             */


            LOG.info("Data purging has been done successfully.............");

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
