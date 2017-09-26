package org.exoplatform.services.injection.module;

import org.exoplatform.community.service.injector.InjectorUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.image.ImageUtils;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.webui.exception.MessageException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;

public class UserModule {

    /** The log. */
    private final Log           LOG                     = ExoLogger.getLogger(UserModule.class);

    /** The Constant PLATFORM_USERS_GROUP. */
    private final static String PLATFORM_USERS_GROUP    = "/platform/administrators";

    private static Boolean requestStarted = false;

    /** The Constant MEMBERSHIP_TYPE_MANAGER. */
    private final static String MEMBERSHIP_TYPE_MANAGER = "*";

    /** The Constant WIDTH. */
    private final static int    WIDTH                   = 200;

    private OrganizationService organizationService_;

    private IdentityManager identityManager_;

    private RelationshipManager relationshipManager_;

    /**
     * Instantiates a new user service.
     */
    public UserModule(OrganizationService organizationService, IdentityManager identityManager, RelationshipManager relationshipManager) {
        organizationService_ = organizationService;
        identityManager_ = identityManager;
        relationshipManager_ = relationshipManager;
    }

    /**
     * Creates the users.
     *
     * @param users the users
     */
    public void createUsers(JSONArray users) {

        for (int i = 0; i < users.length(); i++) {
            try {
                startRequest();
                JSONObject user = users.getJSONObject(i);
                boolean created = createUser(user.getString("username"),
                        user.getString("position"),
                        user.getString("firstname"),
                        user.getString("lastname"),
                        user.getString("email"),
                        user.getString("password"),
                        user.getString("isadmin"));
                if (created) {
                    saveUserAvatar(user.getString("username"), user.getString("avatar"));
                }
                endRequest();


            } catch (JSONException e) {
                LOG.error("Syntax error on user n°" + i, e);
            }
        }

    }

    /**
     * Creates the user.
     *
     * @param username the username
     * @param position the position
     * @param firstname the firstname
     * @param lastname the lastname
     * @param email the email
     * @param password the password
     * @param isAdmin the is admin
     * @return true, if successful
     */
    private boolean createUser(String username,
                               String position,
                               String firstname,
                               String lastname,
                               String email,
                               String password,
                               String isAdmin) {
        Boolean ok = true;

        User user = null;
        try {
            user = organizationService_.getUserHandler().findUserByName(username);
        } catch (Exception e) {
            LOG.info(e.getMessage());
        }

        if (user != null) {
            return false;
        }

        user = organizationService_.getUserHandler().createUserInstance(username);
        user.setDisplayName(firstname + " " + lastname);
        user.setEmail(email);
        user.setFirstName(firstname);
        user.setLastName(lastname);
        user.setPassword(password);

        try {
            organizationService_.getUserHandler().createUser(user, true);
        } catch (Exception e) {
            LOG.info(e.getMessage());
            ok = false;
        }

        if (isAdmin != null && isAdmin.equals("true")) {
            // Assign the membership "*:/platform/administrators" to the created user
            try {
                Group group = organizationService_.getGroupHandler().findGroupById(PLATFORM_USERS_GROUP);
                MembershipType membershipType = organizationService_.getMembershipTypeHandler()
                        .findMembershipType(MEMBERSHIP_TYPE_MANAGER);
                organizationService_.getMembershipHandler().linkMembership(user, group, membershipType, true);
            } catch (Exception e) {
                LOG.warn("Can not assign *:/platform/administrators membership to the created user");
                ok = false;
            }

        }

        if (!"".equals(position)) {
            Identity identity = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, true);
            if (identity != null) {
                Profile profile = identity.getProfile();
                profile.setProperty(Profile.POSITION, position);
                profile.setListUpdateTypes(Arrays.asList(Profile.UpdateType.CONTACT));
                try {
                    identityManager_.updateProfile(profile);
                } catch (MessageException e) {
                    e.printStackTrace();
                }
            }
        }

        return ok;
    }

    /**
     * Save user avatar.
     *
     * @param username the username
     * @param fileName the file name
     */
    private void saveUserAvatar(String username, String fileName) {
        try {

            AvatarAttachment avatarAttachment = InjectorUtils.getAvatarAttachment(fileName);
            Profile p =identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, true).getProfile();
            p.setProperty(Profile.AVATAR, avatarAttachment);
            p.setListUpdateTypes(Arrays.asList(Profile.UpdateType.AVATAR));


            Map<String, Object> props = p.getProperties();

            // Removes avatar url and resized avatar
            for (String key : props.keySet()) {
                if (key.startsWith(Profile.AVATAR + ImageUtils.KEY_SEPARATOR)) {
                    p.removeProperty(key);
                }
            }

            identityManager_.updateProfile(p);

        } catch (Exception e) {
            LOG.info(e.getMessage());
        }
    }

    /**
     * Creates the relations.
     *
     * @param relations the relations
     */
    public void createRelations(JSONArray relations) {
        for (int i = 0; i < relations.length(); i++) {

            try {
                JSONObject relation = relations.getJSONObject(i);
                Identity idInviting = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME,relation.getString("inviting"),false);
                Identity idInvited = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME,relation.getString("invited"),false);
                relationshipManager_.inviteToConnect(idInviting, idInvited);
                if (relation.has("confirmed") && relation.getBoolean("confirmed")) {
                    relationshipManager_.confirm(idInvited, idInviting);
                }
            } catch (JSONException e) {
                LOG.error("Syntax error on relation n°" + i, e);
            }
        }
    }

    private void endRequest() {
        if (requestStarted && organizationService_ instanceof ComponentRequestLifecycle) {
            try {
                ((ComponentRequestLifecycle) organizationService_).endRequest(PortalContainer.getInstance());
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }
            requestStarted = false;
        }
    }

    private void startRequest() {
        if (organizationService_ instanceof ComponentRequestLifecycle) {
            ((ComponentRequestLifecycle) organizationService_).startRequest(PortalContainer.getInstance());
            requestStarted = true;
        }
    }
}
