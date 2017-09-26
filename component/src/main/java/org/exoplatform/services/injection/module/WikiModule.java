package org.exoplatform.services.injection.module;


import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.community.service.injector.InjectorUtils;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.WikiService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwiki.rendering.syntax.Syntax;

import java.io.IOException;

public class WikiModule {

    /** The log. */
    private final Log                        LOG = ExoLogger.getLogger(WikiModule.class);

    private WikiService wikiService_;

    /**
     * Instantiates a new wiki service.
     */
    public WikiModule(WikiService wikiService) {
        wikiService_ = wikiService;
    }

    /**
     * Creates the user wiki.
     *
     * @param wikis the wikis
     */
    public void createUserWiki(JSONArray wikis) {
        for (int i = 0; i < wikis.length(); i++) {
            try {
                JSONObject wiki = wikis.getJSONObject(i);
                createOrEditPage(wiki, wiki.has("parent") ? wiki.getString("parent") : "");
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
     * @throws JSONException the JSON exception
     */
    private void createOrEditPage(JSONObject wiki, String parentTitle) throws JSONException {
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
            if (wikiService_.getWikiByTypeAndOwner(type, owner) == null) {
                wikiService_.createWiki(type, owner);
            }

            if (forceNew && !title.equals("WikiHome")) {
                if (wikiService_.isExisting(type, owner, TitleResolver.getId(title, false))) {
                    wikiService_.deletePage(type, owner, TitleResolver.getId(title, false));
                }
            }

            Page page;
            if (wikiService_.isExisting(type, owner, TitleResolver.getId(title, false))) {
                page = wikiService_.getPageOfWikiByName(type, owner, TitleResolver.getId(title, false));
            } else {
                page = wikiService_.createPage(new Wiki(type, owner), TitleResolver.getId(parent, false), new Page(title, title));
            }

            String content = "= " + title + " =";
            if (filename != null && !filename.equals(""))
                content = InjectorUtils.getWikiPage(filename);
            page.setContent(content);
            page.setSyntax(Syntax.XWIKI_2_1.toIdString());
            wikiService_.updatePage(page, null);
            // wikiService_.createVersionOfPage(page);

            if (wiki.has("wikis") && wiki.getJSONArray("wikis").length() > 0) {
                for (int j = 0; j < wiki.getJSONArray("wikis").length(); j++) {
                    JSONObject childWiki = wiki.getJSONArray("wikis").getJSONObject(j);
                    createOrEditPage(childWiki, wiki.getString("title"));
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
