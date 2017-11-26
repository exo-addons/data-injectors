package org.exoplatform.services.injection.custom;

import com.atisnetwork.services.collaborator.CollaboratorService;
import com.atisnetwork.services.independent.IndependentService;


import com.lowagie.text.pdf.PdfWriter;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
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
import org.exoplatform.social.core.space.SpaceListAccess;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.fluttercode.datafactory.impl.DataFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;


import java.io.OutputStream;
import java.util.*;

/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 17/11/17.
 */
@Path("/userinjection/")
public class CustomUserInjectionRESTService implements ResourceContainer {


    private static Log LOG = ExoLogger.getLogger(CustomUserInjectionRESTService.class);

    private DataFactory dataFactory;
    private DiskFileItemFactory diskFilefactory;
    private OrganizationService organizationService;
    private IdentityManager identityManager;
    private CollaboratorService collaboratorService;
    private PortalContainer portalContainer;
    private SpaceStorage spaceStorage;
    private SpaceService spaceService;
    private IndependentService independentService;

    private static final String DOMAIN = "exoplatform.int";
    private static final String DEFAULT_PASSWORD = "!%b@Ti5%!";
    private static final String PLATFORM_USERS_GROUP = "/platform/users";
    private static final String ATIS_INDEPENDENTS_GROUP = "/Atis/Independents";
    private static final String PREMIUM_ROLE = "premium";
    private static final String MEMBER_ROLE = "member";
    private static final String COLLAB_TVA_NUM = "collabTvaNum";
    private String EMPLOYER_GROUP="/Atis/Employer";
    private static final String ATIS_CHARTER_CHECKED = "atisCharterChecked";
    public static final String ACTIVITIES = "activities";
    public static final String PROVINCES = "provinces";


    private int batchSize = 100;

    public CustomUserInjectionRESTService(PortalContainer portalContainer, OrganizationService organizationService, IdentityManager identityManager,
                                          CollaboratorService collaboratorService, InitParams initParams,
                                          SpaceStorage spaceStorage, SpaceService spaceService, IndependentService
                                                  independentService) {
        this.organizationService = organizationService;
        this.identityManager = identityManager;
        this.collaboratorService = collaboratorService;
        this.portalContainer = portalContainer;
        this.spaceStorage = spaceStorage;
        this.spaceService = spaceService;
        this.independentService = independentService;
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
        diskFilefactory = new DiskFileItemFactory();

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
        Collection<Group> employerGroups = new ArrayList<Group>();
        try {
            Group employerGroup = organizationService.getGroupHandler().findGroupById(EMPLOYER_GROUP);
            employerGroups = organizationService.getGroupHandler().findGroups(employerGroup);
        } catch (Exception e) {
            LOG.error("Unable to find employer groups ",e);

        }
        String[] employerValues = new String[employerGroups.size()];
        int currentIndex=0;
        for (Group current : employerGroups) {
            employerValues[currentIndex]=current.getGroupName();
            currentIndex++;
        }

        for (int i=0; i<nbIndependants;i++) {
            try {
                //createUser
                User user = createUser();
                nbCreatedUsers++;
                //fill profile
                Space[] visibleSpaces = getNotHiddenSpaces(user.getUserName());
                fillProfile(user,employerValues, visibleSpaces);
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

    private void fillProfile(User user, String[] employerValues, Space[] visibleSpaces) throws Exception {
        Profile currentProfile = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,user.getUserName()
                ,true).getProfile();

        organizationService.getMembershipHandler().linkMembership(user,
                organizationService.getGroupHandler().findGroupById(ATIS_INDEPENDENTS_GROUP),
                organizationService.getMembershipTypeHandler().findMembershipType(MEMBER_ROLE),true);


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
        currentProfile.setProperty(PROVINCES,new ArrayList<>());
        currentProfile.setProperty(ACTIVITIES,new ArrayList<>());
        identityManager.updateProfile(currentProfile);

        independentService.saveMyIndependentCard(user.getUserName(),companyName,brandName,headOfficeStreetName,headOfficePostalCode,headOfficeCity,null,null,null,establishmentsNumber,null,activityFirstDate,employer,null);
        String presentation =this.getRandomText(50,300,dataFactory);
        independentService.saveMyIndependentActivity(user.getUserName(),presentation,new ArrayList<>(),new ArrayList<>());



        int nbDocumentsToGenerate = dataFactory.getNumberBetween(1,3);
        for (int i=0;i<nbDocumentsToGenerate;i++) {
            String fileName = dataFactory.getRandomText(4,8);
            FileItem file = diskFilefactory.createItem(fileName+".pdf", "application/pdf", false, fileName+".pdf");
            OutputStream out = file.getOutputStream();
            createPDFDocument(out);
            independentService.addAttachmentForUser(file, user.getUserName(), user.getUserName());
        }
        spaceService.addMember(dataFactory.getItem(visibleSpaces),user.getUserName());

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

    public Space[] getNotHiddenSpaces(String userId){
        try {
            ListAccess<Space> allSpaces = new SpaceListAccess(this.spaceStorage, userId, SpaceListAccess.Type.VISIBLE);

            return allSpaces.load(0, allSpaces.getSize());

        } catch (Exception e) {
            LOG.error("Unable to get public spaces",e);
            return new Space[0];
        }

    }

    //override datafactory getRandomText
    //the version in 0.8 create a single word
    //We need a real text
    public String getRandomText(int minLength, int maxLength, DataFactory dataFactory) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(maxLength);
        int length = minLength;
        if (maxLength != minLength) {
            length = length + random.nextInt(maxLength - minLength);
        }
        while (length > 0) {
            if (sb.length() != 0) {
                sb.append(" ");
                length--;
            }
            final double desiredWordLengthNormalDistributed = 1.0 + Math.abs(random.nextGaussian()) * 6;
            int usedWordLength = (int) (Math.min(length, desiredWordLengthNormalDistributed));
            String word = dataFactory.getRandomWord(usedWordLength);
            sb.append(word);
            length = length - word.length();
        }
        return sb.toString();

    }

    // Create PDF (.pdf) document
    public boolean createPDFDocument(OutputStream file) {

        int nbParagraph = dataFactory.getNumberBetween(5,15);

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, file);
            document.open();
            for (int i=0;i<nbParagraph;i++){
                String content = this.getRandomText(100,300,dataFactory);
                document.add(new Paragraph(content+"\n"));
            }
            document.close();
            file.close();
            return true;
        } catch(Exception ex) {
            return false;
        }
    }
}
