package org.exoplatform.services.injection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.commons.utils.StringCommonUtils;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.injection.module.SpaceModule;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.image.ImageUtils;
import org.exoplatform.social.core.model.AvatarAttachment;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.URLDecoder;
import java.util.Calendar;

/**
 * Created by kmenzli on 6/30/17.
 */
public class InjectorUtils {

    /** The log. */
    private final Log LOG = ExoLogger.getLogger(InjectorUtils.class);

    /** Content Location */
    public final static String DATA_INJECTOR_CONTENT_PATH = "/medias/contents/";
    /** User's avatar and Space's icon Location */
    public static final String DATA_INJECTOR_IMAGE_PATH = "/medias/images/";
    /** Fake data Location */
    public static final String DATA_INJECTOR_DEFAULT_FOLDER_PATH = "data";


    /**
     * Get Default path folder to load fake data
     * @param defaultDataPath
     * @return the dafault fake data path
     * @throws Exception
     */
    public static String getConfigPath (String defaultDataPath) throws Exception {

        String injectorDataFolder = null;
        ConfigurationManager confManager = (ConfigurationManager) RootContainer.getInstance().getComponentInstanceOfType(ConfigurationManager.class);
        try {
            if ((defaultDataPath != null) && (defaultDataPath.length() > 0)) {
                injectorDataFolder = confManager.getResource(defaultDataPath).getPath();
            }

        } catch (Exception ex) {
            injectorDataFolder = confManager.getResource(DATA_INJECTOR_DEFAULT_FOLDER_PATH).getPath();
        }
        try {
            injectorDataFolder = URLDecoder.decode(injectorDataFolder, "UTF-8");
        } catch (UnsupportedEncodingException e) {

        }
        return injectorDataFolder;
    }
    /**
     * Gets the avatar attachment.
     *
     * @param fileName the file name
     * @param defaultFolder the data folder path
     * @return the avatar attachment
     * @throws Exception the exception
     */
    public static AvatarAttachment getAvatarAttachment(String fileName, String defaultFolder) throws Exception {
        String mimeType = "image/png";
        int WIDTH = 120;
        InputStream inputStream = FileUtils.openInputStream(new File(getConfigPath(defaultFolder)+DATA_INJECTOR_IMAGE_PATH + fileName));
        // Resize avatar to fixed width if can't(avatarAttachment == null) keep
        // origin avatar
        AvatarAttachment avatarAttachment = ImageUtils.createResizedAvatarAttachment(inputStream,
                WIDTH,
                0,
                null,
                fileName,
                mimeType,
                null);
        if (avatarAttachment == null) {
            avatarAttachment = new AvatarAttachment(null, fileName, mimeType, inputStream, null, System.currentTimeMillis());
        }
        return avatarAttachment;
    }


    /**
     * Gets the wiki page.
     *
     * @param fileName the file name
     * @param defaultFolder the data folder path
     * @return the wiki page
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static String getWikiPage(String fileName, String defaultFolder) throws IOException {
        StringWriter writer =null;
        try {
            if (fileName.equals("")) {
                return "";
            }
            InputStream inputStream = FileUtils.openInputStream(new File(getConfigPath(defaultFolder)+DATA_INJECTOR_CONTENT_PATH + fileName));
            writer = new StringWriter();
            IOUtils.copy(inputStream, writer);
        } catch (Exception e) {

        }



        return writer.toString();
    }

    /**
     * Gets the file.
     *
     * @param fileName the file name
     * @param fileType the file type
     * @param defaultFolder the data folder path
     * @return the file
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static InputStream getFile(String fileName, String fileType, String defaultFolder) throws IOException,Exception {

        if (fileName.equals("")) {
            return null;
        }
        InputStream inputStream = FileUtils.openInputStream(new File(getConfigPath(defaultFolder)+"/medias/" + fileType + "/" + fileName));
        return inputStream;
    }

    /**
     * Gets the day as int.
     *
     * @param day the day
     * @return the day as int
     */
    public static int getDayAsInt(String day) {
        if ("monday".equals(day))
            return Calendar.MONDAY;
        else if ("tuesday".equals(day))
            return Calendar.TUESDAY;
        else if ("wednesday".equals(day))
            return Calendar.WEDNESDAY;
        else if ("thursday".equals(day))
            return Calendar.THURSDAY;
        else if ("friday".equals(day))
            return Calendar.FRIDAY;
        else if ("saturday".equals(day))
            return Calendar.SATURDAY;
        else if ("sunday".equals(day))
            return Calendar.SUNDAY;
        return Calendar.MONDAY;
    }

    /**
     * Gets the hour as int.
     *
     * @param hourString the hour string
     * @return the hour as int
     */
    public static int getHourAsInt(String hourString) {
        String[] start = hourString.split(":");
        Integer hour = Integer.parseInt(start[0]);
        return hour;
    }

    /**
     * Gets the minute as int.
     *
     * @param hourString the hour string
     * @return the minute as int
     */
    public static int getMinuteAsInt(String hourString) {
        String[] start = hourString.split(":");
        Integer minutes = Integer.parseInt(start[1]);
        return minutes;
    }
}
