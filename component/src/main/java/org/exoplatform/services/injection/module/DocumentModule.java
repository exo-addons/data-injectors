package org.exoplatform.services.injection.module;

import org.exoplatform.services.injection.AbstractInjector;
import org.exoplatform.services.injection.helper.InjectorUtils;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.Node;
import javax.jcr.Session;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

public class DocumentModule extends AbstractInjector {
    /** The log. */
    private final Log      LOG                   = ExoLogger.getLogger(DocumentModule.class);

    /** The file created activity. */
    public static String   FILE_CREATED_ACTIVITY = "ActivityNotify.event.FileCreated";

    /**
     * Upload documents.
     *
     * @param documents the documents
     * @param defaultDataFolderPath the documents
     */
    public void uploadDocuments(JSONArray documents, String defaultDataFolderPath) {
        for (int i = 0; i < documents.length(); i++) {
            try {
                JSONObject document = documents.getJSONObject(i);
                String filename = document.getString("filename");
                String owner = document.getString("owner");
                String path = document.has("path") ? document.getString("path") : null;
                boolean isPrivate = document.getBoolean("isPrivate");
                String spaceName = document.has("spaceName") ? document.getString("spaceName") : "";
                storeFile(filename, spaceName, isPrivate, null, owner, path, "collaboration", "documents", defaultDataFolderPath);
                // createOrEditPage(wiki, wiki.has("parent") ? wiki.getString("parent") : "");
            } catch (JSONException e) {
                LOG.error("Syntax error on document n°" + i, e);

            }
        }
    }

    /**
     * Store file.
     *
     * @param filename the filename
     * @param name the name
     * @param isPrivateContext the is private context
     * @param uuid the uuid
     * @param username the username
     * @param path the path
     * @param workspace the workspace
     * @param fileType the file type
     * @param dataFolderPath the data folder path
     */
    protected void storeFile(String filename,
                             String name,
                             boolean isPrivateContext,
                             String uuid,
                             String username,
                             String path,
                             String workspace,
                             String fileType,
                                String dataFolderPath) {
        SessionProvider sessionProvider = null;
        if (!"root".equals(username)) {
            sessionProvider = startSessionAs(username);
        } else {
            sessionProvider = SessionProvider.createSystemProvider();
        }

        try {
            // get info
            Session session = sessionProvider.getSession(workspace, repositoryService.getCurrentRepository());

            Node homeNode;

            if (isPrivateContext) {
                Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, username);
                homeNode = userNode.getNode("Private");
            } else {
                Node rootNode = session.getRootNode();
                homeNode = rootNode.getNode(getSpacePath(name));
            }

            Node docNode = homeNode.getNode("Documents");

            if (path != null) {
                Node rootNode = session.getRootNode();
                docNode = rootNode.getNode(path.substring(1));
            }

            if (!docNode.hasNode(filename) && (uuid == null || "---".equals(uuid))) {
                Node fileNode = docNode.addNode(filename, "nt:file");
                Node jcrContent = fileNode.addNode("jcr:content", "nt:resource");
                InputStream inputStream = InjectorUtils.getFile(filename, fileType,dataFolderPath);
                jcrContent.setProperty("jcr:data", inputStream);
                jcrContent.setProperty("jcr:lastModified", Calendar.getInstance());
                jcrContent.setProperty("jcr:encoding", "UTF-8");
                if (filename.endsWith(".jpg"))
                    jcrContent.setProperty("jcr:mimeType", "image/jpeg");
                else if (filename.endsWith(".png"))
                    jcrContent.setProperty("jcr:mimeType", "image/png");
                else if (filename.endsWith(".pdf"))
                    jcrContent.setProperty("jcr:mimeType", "application/pdf");
                else if (filename.endsWith(".doc"))
                    jcrContent.setProperty("jcr:mimeType", "application/vnd.ms-word");
                else if (filename.endsWith(".xls"))
                    jcrContent.setProperty("jcr:mimeType", "application/vnd.ms-excel");
                else if (filename.endsWith(".ppt"))
                    jcrContent.setProperty("jcr:mimeType", "application/vnd.ms-powerpoint");
                else if (filename.endsWith(".docx"))
                    jcrContent.setProperty("jcr:mimeType", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                else if (filename.endsWith(".xlsx"))
                    jcrContent.setProperty("jcr:mimeType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                else if (filename.endsWith(".pptx"))
                    jcrContent.setProperty("jcr:mimeType",
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation");
                else if (filename.endsWith(".odp"))
                    jcrContent.setProperty("jcr:mimeType", "application/vnd.oasis.opendocument.presentation");
                else if (filename.endsWith(".odt"))
                    jcrContent.setProperty("jcr:mimeType", "application/vnd.oasis.opendocument.text");
                else if (filename.endsWith(".ods"))
                    jcrContent.setProperty("jcr:mimeType", "application/vnd.oasis.opendocument.spreadsheet");
                else if (filename.endsWith(".zip")) {
                    jcrContent.setProperty("jcr:mimeType", "application/zip");
                }
                else if (filename.endsWith(".mp3")) {
                    jcrContent.setProperty("jcr:mimeType", "audio/mpeg");
                }
                session.save();
                if (!"root".equals(username)) {
                    listenerService.broadcast(FILE_CREATED_ACTIVITY, null, fileNode);
                }

            }

        } catch (Exception e) {
            System.out.println("JCR::" + e.getMessage());
        }
        endSession();
    }

    /**
     * Store videos.
     *
     * @param filename the filename
     * @param name the name
     * @param isPrivateContext the is private context
     * @param uuid the uuid
     * @param username the username
     * @param path the path
     * @param workspace the workspace
     * @param type the type
     * @param fileType the file type
     * @param dataFolderPath the data folder path type
     */
    protected void storeVideos(String filename,
                               String name,
                               boolean isPrivateContext,
                               String uuid,
                               String username,
                               String path,
                               String workspace,
                               String type,
                               String fileType,
                               String dataFolderPath) {

        SessionProvider sessionProvider = startSessionAs(username);

        try {
            // get info
            Session session = sessionProvider.getSession(workspace, repositoryService.getCurrentRepository());

            Node homeNode;

            Node rootNode = session.getRootNode();

            homeNode = rootNode.getNode(getSpacePath(name));

            Node docNode = homeNode.getNode("Documents");

            if (!docNode.hasNode(filename) && (uuid == null || "---".equals(uuid))) {
                Node fileNode = docNode.addNode(filename, "nt:file");
                Node jcrContent = fileNode.addNode("jcr:content", "nt:resource");
                InputStream inputStream = InjectorUtils.getFile(filename, fileType,dataFolderPath);
                jcrContent.setProperty("jcr:data", inputStream);
                jcrContent.setProperty("jcr:lastModified", Calendar.getInstance());
                jcrContent.setProperty("jcr:encoding", "UTF-8");
                if (filename.endsWith(".mp4")) {
                    jcrContent.setProperty("jcr:mimeType", "video/mp4");
                }
                session.save();
                if (!"root".equals(name)) {
                    listenerService.broadcast(FILE_CREATED_ACTIVITY, null, fileNode);
                }

            }

        } catch (Exception e) {
            System.out.println("JCR::" + e.getMessage());
        }
        endSession();
    }

    /**
     * Gets the space path.
     *
     * @param space the space
     * @return the space path
     */
    private static String getSpacePath(String space) {
        return "Groups/spaces/" + space;
    }

    /**
     * Start session as.
     *
     * @param user the user
     * @return the session provider
     */
    protected SessionProvider startSessionAs(String user) {
        Identity identity = new Identity(user);

        try {
            Collection<MembershipEntry> membershipEntries = new ArrayList<MembershipEntry>();

            Collection<Membership> memberships = organizationService.getMembershipHandler().findMembershipsByUser(user);
            for (Membership membership : memberships) {
                membershipEntries.add(new MembershipEntry(membership.getGroupId(), membership.getMembershipType()));
            }
            identity.setMemberships(membershipEntries);
        } catch (Exception e) {
            LOG.info(e.getMessage());
        }
        ConversationState state = new ConversationState(identity);
        ConversationState.setCurrent(state);
        sessionProviderService.setSessionProvider(null, new SessionProvider(state));
        return sessionProviderService.getSessionProvider(null);
    }

    /**
     * End session.
     */
    protected void endSession() {
        sessionProviderService.removeSessionProvider(null);
        ConversationState.setCurrent(null);
    }

    /**
     * Store script.
     *
     * @param scriptData the script data
     * @param dataFolderPath  the data folder path
     * @return the string
     */
    public String storeScript(String scriptData, String dataFolderPath) {
        removeFileIfExists(scriptData, "root", "/Application Data", "collaboration");
        storeFile(scriptData, scriptData, true, null, "root", "/Application Data", "collaboration", "scriptData",dataFolderPath);
        return ("/rest/jcr/repository/collaboration/Application Data/" + scriptData);

    }

    /**
     * Removes the file if exists.
     *
     * @param filename the filename
     * @param username the username
     * @param path the path
     * @param workspace the workspace
     */
    private void removeFileIfExists(String filename, String username, String path, String workspace) {
        SessionProvider sessionProvider = null;
        if (!"root".equals(username)) {
            sessionProvider = startSessionAs(username);
        } else {
            sessionProvider = SessionProvider.createSystemProvider();
        }

        try {
            Session session = sessionProvider.getSession(workspace, repositoryService.getCurrentRepository());

            Node docNode;
            if (path != null) {
                Node rootNode = session.getRootNode();
                docNode = rootNode.getNode(path.substring(1));
                if (docNode.hasNode(filename)) {
                    docNode = docNode.getNode(filename);
                    docNode.remove();
                    session.save();
                }
            }

        } catch (Exception e) {
            LOG.error("Error when removing file " + path + "/" + filename, e);
        }
        endSession();

    }
}
