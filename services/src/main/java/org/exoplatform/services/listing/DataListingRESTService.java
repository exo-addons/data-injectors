package org.exoplatform.services.listing;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.injection.social.PatternInjectorConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.rest.resource.ResourceContainer;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 10/11/17.
 */
@Path("/datalisting/")
public class DataListingRESTService implements ResourceContainer {

    private OrganizationService organizationService;

    private PatternInjectorConfig patternInjectorConfig;

    private static final Log LOG = ExoLogger.getLogger(DataListingRESTService.class);


    public DataListingRESTService(OrganizationService organizationService, InitParams params, PatternInjectorConfig
            patternInjectorConfig) {
        this.organizationService=organizationService;
        this.patternInjectorConfig = patternInjectorConfig;
    }

    @GET
    @Path("/identities")
    @RolesAllowed({"administrators"})
    public Response identities() {
        UserHandler userHandler=organizationService.getUserHandler();
        String result = "";
        int limit =100;
        int offset=0;
        try {
            ListAccess<User> users = userHandler.findAllUsers();
            int total = limit + offset;
            if (total > users.getSize())
                total = users.getSize();
            for (int i = offset; i < total; i++) {
                User[] usersArray =users.load(i, limit);
                for (User user:usersArray) {
                    if (!user.getUserName().equals("root")) {
                        result+=user.getUserName()+","+patternInjectorConfig.getUserPasswordValue()+"\n";
                    }
                }
                offset+=limit;
            }
        } catch (Exception e) {
            LOG.error("Error when getting user list",e);
        }

        return Response.ok(result, MediaType.TEXT_PLAIN).build();


    }
}
