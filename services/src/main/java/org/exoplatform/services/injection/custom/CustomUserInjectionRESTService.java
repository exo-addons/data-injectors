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
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.image.ImageUtils;
import org.exoplatform.social.core.manager.IdentityManager;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.space.SpaceListAccess;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.service.TaskService;
import org.fluttercode.datafactory.impl.DataFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;


import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    private TaskService taskService;

    private static final String DOMAIN = "exoplatform.int";
    public static final String DEFAULT_PASSWORD = "!%b@Ti5%!";
    private static final String PLATFORM_USERS_GROUP = "/platform/users";
    private static final String ATIS_INDEPENDENTS_GROUP = "/Atis/Independents";
    private static final String PREMIUM_ROLE = "premium";
    private static final String MEMBER_ROLE = "member";
    private static final String COLLAB_TVA_NUM = "collabTvaNum";
    private String EMPLOYER_GROUP="/Atis/Employer";
    private static final String ATIS_CHARTER_CHECKED = "atisCharterChecked";
    public static final String ACTIVITIES = "activities";
    public static final String PROVINCES = "provinces";


    private int batchSize = 1;

    public CustomUserInjectionRESTService(PortalContainer portalContainer, OrganizationService organizationService, IdentityManager identityManager,
                                          CollaboratorService collaboratorService, InitParams initParams,
                                          SpaceStorage spaceStorage, SpaceService spaceService, IndependentService
                                                  independentService, TaskService taskService) {
        this.organizationService = organizationService;
        this.identityManager = identityManager;
        this.collaboratorService = collaboratorService;
        this.portalContainer = portalContainer;
        this.spaceStorage = spaceStorage;
        this.spaceService = spaceService;
        this.independentService = independentService;
        this.taskService = taskService;
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
    @Path("/communitySpaces")
    @RolesAllowed({"administrators"})
    public Response createCommunitySpaces(@QueryParam("nbSpaces") int nbSpaces) {
        LOG.info("Start the creation of {} community spaces.", nbSpaces);
        int nbCreatedSpaces=0;
        try {

            for (int i=0;i<nbSpaces;i++) {

                String spaceName = "Communauté "+getRandomText(8, 20, dataFactory);

                Space space = spaceService.getSpaceByDisplayName(spaceName);
                while (space!=null) {
                    spaceName = "Communauté "+getRandomText(8, 20, dataFactory);
                    space = spaceService.getSpaceByDisplayName(spaceName);
                }

                String creator = ConversationState.getCurrent().getIdentity().getUserId();

                space = new Space();
                space.setPrettyName(spaceName);
                space.setDisplayName(spaceName);
                space.setRegistration(Space.VALIDATION);
                space.setDescription(spaceName);
                space.setType(DefaultSpaceApplicationHandler.NAME);
                space.setVisibility(Space.PRIVATE);
                String[] managers = new String[] {creator};
                String[] members = new String[] {creator};
                String[] invitedUsers = new String[] {};
                String[] pendingUsers = new String[] {};
                space.setInvitedUsers(invitedUsers);
                space.setPendingUsers(pendingUsers);
                space.setManagers(managers);
                space.setMembers(members);

                setAvatarForSpace(space,"http://avatar.3sd.me/100");
                spaceService.createSpace(space, ConversationState.getCurrent().getIdentity().getUserId());
                spaceService.updateSpaceAvatar(space);

                nbCreatedSpaces++;
                LOG.info("{}/{} spaces created.", nbCreatedSpaces,nbSpaces);

            }


        } catch (Exception e) {
            LOG.error("Error when getting user list",e);
        }


        LOG.info("End of data injection");
        return Response.ok().build();


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
        setAvatarForUser(currentProfile,"https://api.adorable.io/avatars/200/"+collaborator.getEmail());
        identityManager.updateProfile(currentProfile);

        //create tasks for user
        addPersonalTasksByUser(collaborator.getUserName(),dataFactory.getNumberUpTo(3));

    }

    private void setAvatarForUser(Profile currentProfile, String url) {

        try {
            InputStream input = new URL(url).openStream();
            AvatarAttachment avatarAttachment = ImageUtils.createResizedAvatarAttachment(input, 200, 200, null,
                    "avatar.png", "image/png", null);
            currentProfile.setProperty(Profile.AVATAR, avatarAttachment);
            input.close();
        } catch (Exception e) {
            LOG.error("Error when getting avatar image for url {}",url,e);
        }
    }

    private void setAvatarForSpace(Space space, String url) {

        try {
            InputStream input = new URL(url).openStream();
            AvatarAttachment avatarAttachment = ImageUtils.createResizedAvatarAttachment(input, 200, 200, null,
                    "avatar.png", "image/png", null);
            space.setAvatarAttachment(avatarAttachment);
            input.close();
        } catch (Exception e) {
            LOG.error("Error when getting avatar image for url {}",url,e);
        }
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
        setAvatarForUser(currentProfile,"https://api.adorable.io/avatars/200/"+user.getEmail());
        identityManager.updateProfile(currentProfile);

        independentService.saveMyIndependentCard(user.getUserName(),companyName,brandName,headOfficeStreetName,headOfficePostalCode,headOfficeCity,null,null,null,establishmentsNumber,null,activityFirstDate,employer,null,null);
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

        //add user in a community space
        //all users are in one community space
        spaceService.addMember(dataFactory.getItem(visibleSpaces),user.getUserName());

        //25% in at least 2 community spaces
        Space communitySpace = dataFactory.getItem(visibleSpaces,25,null);
        if (communitySpace!=null) {
            spaceService.addMember(dataFactory.getItem(visibleSpaces),user.getUserName());
            //10% in at least 3 community spaces
            communitySpace = dataFactory.getItem(visibleSpaces,10,null);
            if (communitySpace!=null) {
                spaceService.addMember(dataFactory.getItem(visibleSpaces),user.getUserName());
            }
        }



        //create tasks for user
        addPersonalTasksByUser(user.getUserName(),dataFactory.getNumberUpTo(3));

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
            firstName=dataFactory.getFirstName().replaceAll("'","").replaceAll("-","").replaceAll(" ","");
            lastName=dataFactory.getLastName().replaceAll("'","").replaceAll("-","").replaceAll(" ","");
            username = firstName.toLowerCase()+"."+lastName.toLowerCase();
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

    @GET
    @Path("/collaborativesSpaces")
    @RolesAllowed({"administrators"})
    public Response createCollaborativesSpaces(@QueryParam("nbSpaces") int nbSpaces, @QueryParam("nbUsersBySpaces") int nbUsersBySpaces, @QueryParam("usersOffset") int usersOffset) {
        LOG.info("Start the creation of {} spaces with {} users by spaces.",nbSpaces,nbUsersBySpaces);

        //we use AtomicInteger to be able to modify the value in getNextUser()
        AtomicInteger offset=new AtomicInteger(usersOffset);
        int nbCreatedSpaces=0;
        try {
            ListAccess<User> users= organizationService.getUserHandler().findAllUsers();
            for (int i=0;i<nbSpaces;i++) {
                User creator = getNextUser(users, offset);
                User collaborator = getNextUser(users, offset);
                String spaceName = getRandomText(8, 20, dataFactory);
                String messageSubject = getRandomText(8, 20, dataFactory);
                String message = getRandomText(20, 100, dataFactory);

                Space space = spaceService.getSpaceByDisplayName(spaceName);
                while (space!=null) {
                    spaceName = getRandomText(8, 20, dataFactory);
                    space = spaceService.getSpaceByDisplayName(spaceName);
                }

                //create the space
                collaboratorService.createSpaceAndInviteCollaborator(spaceName, creator.getUserName(), collaborator.getUserName(), messageSubject, message);
                space = spaceService.getSpaceByDisplayName(spaceName);

                //accept the invitation
                spaceService.addMember(space, collaborator.getUserName());

                if (space != null){
                    for (int j = 2; j < nbUsersBySpaces; j++) {
                        User newCollaborator = getNextUser(users, offset);
                        //invite a user
                        collaboratorService.inviteCollaboratorToSpace(space.getGroupId(), creator.getUserName(), newCollaborator.getUserName(), messageSubject, message);

                        //accept the invitation
                        spaceService.addMember(space, newCollaborator.getUserName());

                    }
                }
                setAvatarForSpace(space,"http://avatar.3sd.me/100");
                spaceService.updateSpace(space);
                spaceService.updateSpaceAvatar(space);
                nbCreatedSpaces++;
                LOG.info("{}/{} spaces created with {} users in each.", nbCreatedSpaces,nbSpaces,nbUsersBySpaces);

            }


        } catch (Exception e) {
            LOG.error("Error when getting user list",e);
        }


        LOG.info("End of data injection");
        return Response.ok().build();

    }

    private User getNextUser(ListAccess<User> users, AtomicInteger currentOffset) {

        int limit = 1;
        try {
            int total = users.getSize();
            while (currentOffset.intValue()<total) {

                User[] usersArray;
                usersArray = users.load(currentOffset.intValue(), limit);
                currentOffset.set(currentOffset.addAndGet(limit));
                User userToReturn = usersArray [0];

                return userToReturn.getUserName().equals("root") ? getNextUser(users,currentOffset) : usersArray[0];
            }
            //on est à currentOffset==total. On reset offset et on recommence
            currentOffset.set(0);
            return getNextUser(users,currentOffset);
        } catch (Exception e) {
            LOG.error("Unable to get User in list access");
        }
        return null;
    }


    private void addPersonalTasksByUser(String username, int nbTasks) {

        //Create tasks
        //Create tasks associated to personal project of the user
        //Loop on number of projects
        for (int i = 0; i < nbTasks; i++) {

            String taskTitle=getRandomText(8,15,dataFactory);
            String taskDescription=getRandomText(20,50,dataFactory);

            Task task = new Task();
            task.setTitle(taskTitle);
            task.setDescription(taskDescription);
            task.setCreatedBy(username);
            task.setAssignee(username);
            task.setCreatedTime(new Date());
            taskService.createTask(task);
        }

    }

}
