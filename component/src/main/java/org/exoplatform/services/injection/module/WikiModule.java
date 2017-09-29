package org.exoplatform.services.injection.module;


import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.injection.AbstractInjector;
import org.exoplatform.services.injection.InjectorUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwiki.rendering.syntax.Syntax;

import java.io.IOException;

public class WikiModule extends AbstractInjector {

    /** The log. */
    private final Log                        LOG = ExoLogger.getLogger(WikiModule.class);

    /**
     * Creates the user wiki.
     *
     * @param wikis the wikis
     * @param defaultDataFolderPath the default data folder path
     */
    public void createUserWiki(JSONArray wikis, String defaultDataFolderPath) {
        for (int i = 0; i < wikis.length(); i++) {
            try {
                JSONObject wiki = wikis.getJSONObject(i);
                createOrEditPage(wiki, wiki.has("parent") ? wiki.getString("parent") : "",defaultDataFolderPath);
            } catch (JSONException e) {
                LOG.error("Syntax error on wiki n°" + i, e);

            }
        }
    }

    /**
     * Creates the or edit page.
     *
     * @param wiki the wiki
     * @param parentTitle the parent title
     *  @param defaultDataFolderPath the default data folder path
     * @throws JSONException the JSON exception
     */
    private void createOrEditPage(JSONObject wiki, String parentTitle, String defaultDataFolderPath) throws JSONException {
        boolean forceNew = wiki.has("new") && wiki.getBoolean("new");
        String title = wiki.getString("title");
        String filename = wiki.has("filename") ? wiki.getString("filename") : "";
        String parent = parentTitle;
        String type = wiki.has("type") ? wiki.getString("type") : "";
        if ("group".equals(type)) {
            type = PortalConfig.GROUP_TYPE;
        } else if ("portal".equals(type)) {
            type = PortalConfig.PORTAL_TYPE;
        } else {
            type = PortalConfig.USER_TYPE;
        }
        String owner = wiki.has("owner") ? wiki.getString("owner") : "";

        try {
            // does wiki exists ?
            if (wikiService.getWikiByTypeAndOwner(type, owner) == null) {
                wikiService.createWiki(type, owner);
            }

            if (forceNew && !title.equals("WikiHome")) {
                if (wikiService.isExisting(type, owner, TitleResolver.getId(title, false))) {
                    wikiService.deletePage(type, owner, TitleResolver.getId(title, false));
                }
            }

            Page page;
            if (wikiService.isExisting(type, owner, TitleResolver.getId(title, false))) {
                page = wikiService.getPageOfWikiByName(type, owner, TitleResolver.getId(title, false));
            } else {
                page = wikiService.createPage(new Wiki(type, owner), TitleResolver.getId(parent, false), new Page(title, title));
            }

            String content = "= " + title + " =";
            if (filename != null && !filename.equals(""))
                content = InjectorUtils.getWikiPage(filename,defaultDataFolderPath);
            page.setContent(content);
            page.setSyntax(Syntax.XWIKI_2_1.toIdString());
            wikiService.updatePage(page, null);
            // wikiService_.createVersionOfPage(page);

            if (wiki.has("wikis") && wiki.getJSONArray("wikis").length() > 0) {
                for (int j = 0; j < wiki.getJSONArray("wikis").length(); j++) {
                    JSONObject childWiki = wiki.getJSONArray("wikis").getJSONObject(j);
                    createOrEditPage(childWiki, wiki.getString("title"),defaultDataFolderPath);
                }
            }

        } catch (WikiException e) {
            LOG.error("Error when creating wiki page", e); // To change body of catch statement use File | Settings
            // | File Templates.
        } catch (IOException e) {
            LOG.error("Error when reading wiki content", e);
        }

    }
}
