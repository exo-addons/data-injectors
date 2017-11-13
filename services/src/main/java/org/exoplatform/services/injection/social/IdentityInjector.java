package org.exoplatform.services.injection.social;

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

      String firstname = dataFactory.getFirstName();
      String lastName = dataFactory.getLastName();
      String username = firstname.toLowerCase().charAt(0)+lastName.toLowerCase().replace("'","");
      if (userHandler.findUserByName(username)==null) {
        User user = userHandler.createUserInstance(username);
        user.setEmail(username + "@" + DOMAIN);
        user.setFirstName(firstname);
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
