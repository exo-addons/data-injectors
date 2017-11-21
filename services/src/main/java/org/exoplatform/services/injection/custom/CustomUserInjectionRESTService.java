package org.exoplatform.services.injection.custom;

import com.atisnetwork.services.collaborator.CollaboratorService;
import com.atisnetwork.services.independent.IndependentService;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.Parameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.Constants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

import org.apache.commons.lang.StringUtils;
import org.fluttercode.datafactory.impl.DataFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 17/11/17.
 */
@Path("/userinjection/")
public class CustomUserInjectionRESTService implements ResourceContainer {


    private static Log LOG = ExoLogger.getLogger(CustomUserInjectionRESTService.class);

    private DataFactory dataFactory;
    private OrganizationService organizationService;
    private IdentityManager identityManager;
    private CollaboratorService collaboratorService;
    private PortalContainer portalContainer;

    private static final String DOMAIN = "exoplatform.int";
    private static final String DEFAULT_PASSWORD = "!%b@Ti5%!";
    private static final String PLATFORM_USERS_GROUP = "/platform/users";
    private static final String ATIS_INDEPENDENTS_GROUP = "/Atis/Independents";
    private static final String PREMIUM_ROLE = "premium";
    private static final String MEMBER_ROLE = "member";
    private static final String COLLAB_TVA_NUM = "collabTvaNum";
    private String EMPLOYER_GROUP="/Atis/Employer";
    private static final String ATIS_CHARTER_CHECKED = "atisCharterChecked";

    private int batchSize = 100;

    public CustomUserInjectionRESTService(PortalContainer portalContainer, OrganizationService organizationService, IdentityManager identityManager,
                                          CollaboratorService collaboratorService, InitParams initParams) {
        this.organizationService = organizationService;
        this.identityManager = identityManager;
        this.collaboratorService = collaboratorService;
        this.portalContainer = portalContainer;
        if(initParams != null && initParams.containsKey("batch_size")) {
          ValueParam batchSizeParam = initParams.getValueParam("batch_size");
          String batchSizeValue = batchSizeParam.getValue();
          if (StringUtils.isNotBlank(batchSizeValue)) {
            try {
              batchSize = Integer.parseInt(batchSizeValue);
            } catch (Exception e) {
              LOG.warn("Unable to parse batch_size parameter '" + batchSizeValue + "'", e);
            }
          }
        }
        dataFactory = new DataFactory();
    }

    @GET
    @Path("/usersandprofiles")
    @RolesAllowed({"administrators"})
    public Response createUsersAndProfiles(@QueryParam("nbIndependants") int nbIndependants, @QueryParam("nbCollaborators") int nbCollaborators,@QueryParam("batchSize") int batchSize) {
        LOG.info("Start the creation of {} independants and {} collaborators by independants.",nbIndependants,nbCollaborators);

        int customBatchSize = this.batchSize;
        if (batchSize!=0) {
            customBatchSize=batchSize;
        }

        int nbCreatedUsers = 0;
        for (int i=0; i<nbIndependants;i++) {
            try {
                //createUser
                User user = createUser();
                nbCreatedUsers++;
                //fill profile
                fillProfile(user);
                testAndCommitBatch(nbCreatedUsers, customBatchSize);
                //addCollaborators
                for (int j=0;j<nbCollaborators;j++) {
                    addCollaborators(user);
                    nbCreatedUsers++;
                    testAndCommitBatch(nbCreatedUsers, customBatchSize);
                }
                LOG.info("User {}/{} created as independant, profile filled, and {} collaborators created.",i+1, nbIndependants, nbCollaborators);
            } catch (Exception e) {
                LOG.error("Error when treating user n°{}",i,e);
            }
        }
        LOG.info("End of data injection");
        return Response.ok().build();

    }

    private void testAndCommitBatch(int injectedUsers, int customBatchSize) {
      if (injectedUsers % customBatchSize == 0) {
          LOG.info("Commit batch "+injectedUsers+", batchSize = "+customBatchSize);
        RequestLifeCycle.end();
        RequestLifeCycle.begin(this.portalContainer);
      }
    }

    private void addCollaborators(User independant) throws Exception {
        RandomUser randomUser=getNewRandomUser();
        String username = randomUser.getUsername();
        String firstName = randomUser.getFirstName();
        String lastName = randomUser.getLastName();
        String email = username+"@"+DOMAIN;
        collaboratorService.inviteCollaborator(firstName, lastName, email, independant.getUserName());
        User collaborator = organizationService.getUserHandler().findUserByName(username);
        collaborator.setPassword(DEFAULT_PASSWORD);
        organizationService.getUserHandler().saveUser(collaborator,true);

        //save user locale
        UserProfile userProfile = organizationService.getUserProfileHandler().createUserProfileInstance(collaborator.getUserName());
        userProfile.setAttribute(Constants.USER_LANGUAGE, "fr");
        organizationService.getUserProfileHandler().saveUserProfile(userProfile, true);

        //save charte
        Profile currentProfile = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,collaborator.getUserName()
                ,true).getProfile();
        currentProfile.setProperty(ATIS_CHARTER_CHECKED,true);
        identityManager.updateProfile(currentProfile);

    }

    private void fillProfile(User user) throws Exception {
        Profile currentProfile = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,user.getUserName()
                ,true).getProfile();

        organizationService.getMembershipHandler().linkMembership(user,
                organizationService.getGroupHandler().findGroupById(ATIS_INDEPENDENTS_GROUP),
                organizationService.getMembershipTypeHandler().findMembershipType(MEMBER_ROLE),true);

        IndependentService independentService = new IndependentService();

        String companyName = dataFactory.getBusinessName();
        String brandName = companyName;
        Date minDate=dataFactory.getDate(2000, 1, 1);
        Date maxDate = new Date();
        Calendar startDate = Calendar.getInstance();
        startDate.setTime(dataFactory.getDateBetween(minDate,maxDate));
        String activityFirstDate=startDate.get(Calendar.DAY_OF_MONTH)+"-"+(startDate.get(Calendar.MONTH)+1)+"-"+startDate.get(Calendar.YEAR);
        String headOfficeStreetName=dataFactory.getAddress();
        String headOfficePostalCode=dataFactory.getNumberText(4);
        if (headOfficePostalCode.charAt(0)=='0') {
            headOfficePostalCode="1"+headOfficePostalCode.substring(1);
        }
        String headOfficeCity=dataFactory.getCity();
        String establishmentsNumber="1";



        Group employerGroup = organizationService.getGroupHandler().findGroupById(EMPLOYER_GROUP);
        Collection<Group> employerGroups= organizationService.getGroupHandler().findGroups(employerGroup);

        String[] employerValues = new String[employerGroups.size()];
        int currentIndex=0;
        for (Group current : employerGroups) {
            employerValues[currentIndex]=current.getGroupName();
            currentIndex++;
        }
        String employer=dataFactory.getItem(employerValues);
        String tvaNum = dataFactory.getNumberText(10);
        Calendar endSubsDate = Calendar.getInstance();
        endSubsDate.add(Calendar.YEAR,2);
        String endOfSubscriptionDate=endSubsDate.get(Calendar.DAY_OF_MONTH)+"-"+(endSubsDate.get(Calendar.MONTH)+1)+"-"+endSubsDate.get(Calendar.YEAR);

        //save user locale
        UserProfile userProfile = organizationService.getUserProfileHandler().createUserProfileInstance(user.getUserName());
        userProfile.setAttribute(Constants.USER_LANGUAGE, "fr");
        organizationService.getUserProfileHandler().saveUserProfile(userProfile, true);

        //save tvaNum
        currentProfile.setProperty(IndependentService.TVA_NUM, tvaNum);
        currentProfile.setProperty(ATIS_CHARTER_CHECKED,true);
        identityManager.updateProfile(currentProfile);

        independentService.setUserName(user.getUserName());
        independentService.saveMyIndependentCard(companyName,brandName,headOfficeStreetName,headOfficePostalCode,headOfficeCity,null,null,null,establishmentsNumber,null,activityFirstDate,employer,null);

    }

    private User createUser() throws Exception{

        RandomUser randomUser = getNewRandomUser();
        String username = randomUser.getUsername();
        String firstName = randomUser.getFirstName();
        String lastName = randomUser.getLastName();
        User user = organizationService.getUserHandler().createUserInstance(username);
        user.setEmail(username + "@" + DOMAIN);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(DEFAULT_PASSWORD);
        organizationService.getUserHandler().createUser(user, true);
        identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, false);
        return user;
    }

    private class RandomUser {
        private String firstName;
        private String lastName;
        private String username;

        public RandomUser(String firstName, String lastName, String username) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.username = username;
        }

        public String getUsername() {
            return username;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }
    }

    public RandomUser getNewRandomUser() {
        String firstName = "";
        String lastName = "";
        String username = "";
        int nbTry=0;

        while (firstName.equals("")) {
            firstName=dataFactory.getFirstName();
            lastName=dataFactory.getLastName();
            username = firstName.toLowerCase().replace("'","")+"."+lastName.toLowerCase().replace("'","");
            try {
                if (organizationService.getUserHandler().findUserByName(username) != null) {
                    //user already exist, one more turn
                    firstName = "";
                    lastName = "";
                }
            } catch (Exception e) {
                LOG.error("unable to check if user "+username+ " exists.", e);
                firstName = "";
                lastName = "";
            }
            nbTry++;

        }
        LOG.info("Username generated with "+nbTry+" loops in random part.");
        return new RandomUser(firstName, lastName, username);
    }
}
