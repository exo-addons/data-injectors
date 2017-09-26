package org.exoplatform.services.injection.module;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
public class ActivityModule {
    /** The log. */
    private final Log LOG = ExoLogger.getLogger(ActivityModule.class);

    private IdentityManager identityManager_;

    private ActivityManager activityManager_;

    /**
     * Instantiates a new activity service.
     */
    public ActivityModule(IdentityManager identityManager, ActivityManager activityManager) {
        identityManager_ = identityManager;
        activityManager_ = activityManager;

    }

    /**
     * Push activities.
     *
     * @param activities the activities
     */
    public void pushActivities(JSONArray activities)
    {

        for (int i =0;i<activities.length();i++) {
            try {
                JSONObject activity = activities.getJSONObject(i);
                pushActivity(activity);
            } catch (JSONException e) {
                LOG.error("Syntax error on activity n°" + i, e);

            } catch (Exception e) {
                LOG.error("Error when creating activity n°"+i,e);
            }
        }

        //likeRandomActivities(Utils.MARY);
        //likeRandomActivities(Utils.JAMES);
    }

    /**
     * Push activity.
     *
     * @param activityJSON the activity JSON
     * @throws Exception the exception
     */
    private void pushActivity(JSONObject activityJSON) throws Exception
    {

        String from = activityJSON.getString("from");
        Identity identity = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, from, false);
        ExoSocialActivity activity = new ExoSocialActivityImpl();
        activity.setBody(activityJSON.getString("body"));
        activity.setTitle(activityJSON.getString("body"));
        activity.setUserId(identity.getId());
        activity.setType("DEFAULT_ACTIVITY");
        // TODO cleanup
        //activity = activityManager_.saveActivity(identity, activity);
        activityManager_.saveActivityNoReturn(identity, activity);

        Thread.sleep(1000);
        JSONArray likes = activityJSON.getJSONArray("likes");

        for (int i=0;i<likes.length();i++)
        {
            String like = likes.getString(i);
            Identity identityLike = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, like, false);
            try {
                activityManager_.saveLike(activity, identityLike);
            } catch (Exception e) {
                LOG.error("Error when liking an activity with "+like,e);
            }
        }

        JSONArray comments = activityJSON.getJSONArray("comments");
        for (int i =0;i<comments.length();i++) {
            JSONObject commentJSON = comments.getJSONObject(i);

            Thread.sleep(1000);
            Identity identityComment = identityManager_.getOrCreateIdentity(OrganizationIdentityProvider.NAME, commentJSON.getString("from"), false);
            ExoSocialActivity comment = new ExoSocialActivityImpl();
            comment.setTitle(commentJSON.getString("body"));
            comment.setUserId(identityComment.getId());
            activityManager_.saveComment(activity, comment);
        }

    }
}
