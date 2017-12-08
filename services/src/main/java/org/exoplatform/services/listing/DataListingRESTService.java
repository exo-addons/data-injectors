package org.exoplatform.services.listing;

import com.atisnetwork.services.independent.IndependentService;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.injection.custom.CustomUserInjectionRESTService;
import org.exoplatform.services.injection.social.PatternInjectorConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profile.ProfileFilter;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 10/11/17.
 */
@Path("/datalisting/")
public class DataListingRESTService implements ResourceContainer {

    private OrganizationService organizationService;

    private PatternInjectorConfig patternInjectorConfig;

    private IdentityManager identityManager;

    private static final Log LOG = ExoLogger.getLogger(DataListingRESTService.class);


    public DataListingRESTService(OrganizationService organizationService, InitParams params, PatternInjectorConfig
            patternInjectorConfig, IdentityManager identityManager) {
        this.organizationService=organizationService;
        this.patternInjectorConfig = patternInjectorConfig;

        this.identityManager = identityManager;
    }

    @GET
    @Path("/identities")
    @RolesAllowed({"administrators"})
    public Response identities() {
        long startTime = System.currentTimeMillis();

        String result = "";
        int limit =1000;
        int offset=0;
        try {
            ListAccess<Identity> identities = identityManager.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME,new ProfileFilter(), false);
            int total = identities.getSize();
            while (offset<total) {

                Identity[] identitiessArray;
                if (total-offset<limit) {
                    //there is less than limit element to read. load to the end
                    identitiessArray=identities.load(offset, total-offset);
                } else {
                    identitiessArray=identities.load(offset,limit);
                }
                for (Identity identity:identitiessArray) {
                    if (!identity.getRemoteId().equals("root")) {
                        result+=identity.getRemoteId()+","+ CustomUserInjectionRESTService.DEFAULT_PASSWORD+"\n";
                    }
                }
                offset+=limit;
            }
        } catch (Exception e) {
            LOG.error("Error when getting user list",e);
        }
        long endTime = System.currentTimeMillis();
        LOG.info("Get all user with identities take {} ms.", (endTime-startTime));


        return Response.ok(result, MediaType.TEXT_PLAIN).build();
    }


    @GET
    @Path("/searchTerms")
    @RolesAllowed({"administrators"})
    public Response searchTerms() {
        long startTime = System.currentTimeMillis();



        //Saving each element of the input file in an arraylist
        List<String> results = new ArrayList<String>();

        int limit =1000;
        int offset=0;
        try {
            ListAccess<Identity> identities = identityManager.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME,new ProfileFilter(), true);
            int total = identities.getSize();
            while (offset<total) {

                Identity[] identitiessArray;
                if (total-offset<limit) {
                    //there is less than limit element to read. load to the end
                    identitiessArray=identities.load(offset, total-offset);
                } else {
                    identitiessArray=identities.load(offset,limit);
                }
                for (Identity identity:identitiessArray) {
                    if (identity.getProfile().getProperty(Profile.FIRST_NAME) != null) {
                        results.add(identity.getProfile().getProperty(Profile.FIRST_NAME).toString());
                    }
                    if (identity.getProfile().getProperty(Profile.LAST_NAME) != null) {
                        results.add(identity.getProfile().getProperty(Profile.LAST_NAME).toString());
                    }
                    if (identity.getProfile().getProperty(IndependentService.COMPANY_NAME) != null) {
                        String companyName = identity.getProfile().getProperty(IndependentService.COMPANY_NAME).toString();
                        results.addAll(Arrays.asList(companyName.split(" ")));
                    }
                }
                offset+=limit;
            }
        } catch (Exception e) {
            LOG.error("Error when getting user list",e);
        }


        String result= results.stream()
                .distinct()
                .map(i -> i.toString ())
                .collect (Collectors.joining ("\n"));



        long endTime = System.currentTimeMillis();
        LOG.info("Get searchTerms take {} ms.", (endTime-startTime));


        return Response.ok(result).type(MediaType.TEXT_PLAIN+ "; charset=UTF-8").build();


    }
}

