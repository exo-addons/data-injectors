package org.exoplatform.services.injection.social;

import com.atisnetwork.util.AtisUtils;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.fluttercode.datafactory.impl.DataFactory;

import java.util.HashMap;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
public class IdentityInjector extends AbstractSocialInjector {
  /** . */
  private static final String NUMBER = "number";
  private static final String PREFIX = "prefix";

  private static Log LOG = ExoLogger.getLogger(IdentityInjector.class);

  private DataFactory dataFactory;
  
  public IdentityInjector(PatternInjectorConfig pattern) {
    super(pattern);
    dataFactory = new DataFactory();
  }
  
  @Override
  public void inject(HashMap<String, String> params) throws Exception {

    //
    int number = param(params, NUMBER);
//    String prefix = params.get(PREFIX);
//    init(prefix, null, userSuffixValue, spaceSuffixValue);

    //
    for(int i = 0; i < number; ++i) {

      //
      String firstName="";
      String lastName="";
      String username="";
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


      if (userHandler.findUserByName(username)==null) {
        User user = userHandler.createUserInstance(username);
        user.setEmail(username + "@" + DOMAIN);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(this.password);

        try {

          //
          userHandler.createUser(user, true);
          identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, false);

          //
          ++userNumber;

        } catch (Exception e) {
          getLog().error(e);
        }

        //
        getLog().info("User " + username + " generated ("+(i+1)+"/"+number+")");
      } else {
        i--;
      }

    }

  }

}
