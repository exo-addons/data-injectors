package org.exoplatform.services.injection.custom;

import com.atisnetwork.search.independent.IndependentSearchConnector;
import com.atisnetwork.services.administration.utils.MembershipManagement;
import com.atisnetwork.services.independent.IndependentService;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.injection.social.AbstractSocialInjector;
import org.exoplatform.services.injection.social.PatternInjectorConfig;
import org.exoplatform.services.listing.DataListingRESTService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.fluttercode.datafactory.impl.DataFactory;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Romain Dénarié (romain.denarie@exoplatform.com) on 13/11/17.
 */
public class ProfileInjector extends AbstractSocialInjector {

    private static final Log LOG = ExoLogger.getLogger(ProfileInjector.class);

    private OrganizationService organizationService;

    /** . */
    private static final String FROM_USER = "fromUser";

    public static final String PLATFORM_USERS_GROUP = "/platform/users";

    private static final String ATIS_INDEPENDENTS_GROUP = "/Atis/Independents";
    public static final String PREMIUM_ROLE = "premium";
    public static final String MEMBER_ROLE = "member";

    public static final String COLLAB_TVA_NUM = "collabTvaNum";
    private static final String ATIS_CHARTER_CHECKED = "atisCharterChecked";



    /** . */
    private static final String TO_USER = "toUser";

    private DataFactory dataFactory;
    private String EMPLOYER_GROUP="/Atis/Employer";

    public ProfileInjector(PatternInjectorConfig config, OrganizationService organizationService) {
        super(config);
        this.organizationService=organizationService;

        dataFactory = new DataFactory();
    }

    @Override
    public void inject(HashMap<String, String> params) throws Exception {


        int fromUser = param(params, FROM_USER);
        int toUser = param(params, TO_USER);


        int numberToInject = toUser-fromUser+1;
        int totalInjected = 1;

        LOG.info("Inject profile data for user n°"+fromUser+" to user n°"+toUser);

        Date minDate = dataFactory.getDate(2010, 1, 1);
        Date maxDate = new Date();


        Group employerGroup = organizationService.getGroupHandler().findGroupById(EMPLOYER_GROUP);
        Collection<Group> employerGroups= organizationService.getGroupHandler().findGroups(employerGroup);

        String[] employerValues = new String[employerGroups.size()];
        int currentIndex=0;
        for (Group current : employerGroups) {
            employerValues[currentIndex]=current.getGroupName();
            currentIndex++;
        }

        int limit =numberToInject;
        if (limit > 100) {
            limit=100;
        }
        int offset=fromUser;
        try {
            ListAccess<User> users = userHandler.findAllUsers();
            if (toUser==0) {
                toUser=users.getSize()-1;
            }
            int total = limit + offset;
            if (total > users.getSize())
                total = users.getSize();
            for (int i = offset; i < total && i<=toUser; i=i+limit) {

                User[] usersArray =users.load(i, limit);
                for (User user:usersArray) {
                    Profile currentProfile = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,user.getUserName()
                            ,true).getProfile();
                    if (!user.getUserName().equals("root") &&
                            organizationService.getMembershipHandler().findMembershipByUserGroupAndType(user.getUserName(), PLATFORM_USERS_GROUP, PREMIUM_ROLE)==null &&
                            currentProfile.getProperty(COLLAB_TVA_NUM)==null &&
                            organizationService.getMembershipHandler().findMembershipByUserGroupAndType(user.getUserName(),ATIS_INDEPENDENTS_GROUP, MEMBER_ROLE)==null) {
                        //user is not root, and not already premium

                        organizationService.getMembershipHandler().linkMembership(user,
                                organizationService.getGroupHandler().findGroupById(ATIS_INDEPENDENTS_GROUP),
                                organizationService.getMembershipTypeHandler().findMembershipType(MEMBER_ROLE),true);

                        IndependentService independentService = new IndependentService();

                        String companyName = dataFactory.getBusinessName();
                        String brandName = companyName;
                        Calendar startDate = Calendar.getInstance();
                        startDate.setTime(dataFactory.getDate(2000, 1, 1));
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

                        //save tvaNum

                        currentProfile.setProperty(IndependentService.TVA_NUM, dataFactory.getNumberText(10));
                        currentProfile.setProperty(ATIS_CHARTER_CHECKED,true);
                        identityManager.updateProfile(currentProfile);

                        independentService.setUserName(user.getUserName());
                        independentService.saveMyIndependentCard(companyName,brandName,headOfficeStreetName,headOfficePostalCode,headOfficeCity,null,null,null,establishmentsNumber,null,activityFirstDate,employer,null);

                        LOG.info("Data profile injected for user "+user.getUserName() +"("+totalInjected+"/"+numberToInject+")");

                    } else {
                        LOG.info("Do not inject data for user "+user.getUserName()+" (he is root, or is profile is already premium, or he is collaborator)");
                    }
                    totalInjected++;
                }
                offset+=limit;
            }
        } catch (Exception e) {
            LOG.error("Error when getting user list",e);
        }


    }
}