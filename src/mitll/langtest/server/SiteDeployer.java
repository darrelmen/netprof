package mitll.langtest.server;

import mitll.langtest.server.database.ExcelImport;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Site;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Copies website as needed.
 * User: GO22670
 * Date: 2/14/13
 * Time: 6:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class SiteDeployer {
  private static Logger logger = Logger.getLogger(SiteDeployer.class);

  public Site getSite(HttpServletRequest request, String configDir) {
    logger.debug("Getting site given config dir " + configDir);
    FileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);
    upload.setSizeMax(10000000);

    try {
      List<FileItem> items = upload.parseRequest(request);
      Site site = new Site();
      for (FileItem item : items) {
        if (!item.isFormField()
            && "upload".equals(item.getFieldName())) {

          ExcelImport importer = new ExcelImport();
          logger.info("got upload " +item);
          List<Exercise> exercises = importer.readExercises(item.getInputStream());
          site.setExercises(exercises);
          site.setFeedback("Collection contains " +exercises.size() + " exercises and " + importer.getLessons().size()+ " lessons.");
          site.exerciseFile = item.getName();
          File uploadsDir = new File(configDir + File.separator + "uploads");
          if (!uploadsDir.exists()) uploadsDir.mkdir();
          File dest = new File(configDir + File.separator + "uploads" + File.separator + System.currentTimeMillis() + "_" + site.exerciseFile);
          boolean b = ((DiskFileItem) item).getStoreLocation().renameTo(dest);
          if (!b) logger.error("couldn't rename tmp file " + ((DiskFileItem) item).getStoreLocation());
          else {
            logger.info("copied file to " + dest.getAbsolutePath());
            site.savedExerciseFile = dest.getAbsolutePath();
            if (!new File(site.savedExerciseFile).exists()) logger.error("huh? " +site.savedExerciseFile + " doesn't exist?");
          }
        }
        else {
          logger.info("got " + item);
          String name = item.getFieldName();
          if (name != null) {
            if (name.endsWith("Name")) {
              logger.info("name " + item.getString());
              site.name = item.getString().trim();
            } else if (name.endsWith("Language")) {
              logger.info("Language " + item.getString());
              site.language = item.getString().trim();
            } else if (name.endsWith("Notes")) {
              logger.info("Notes " + item.getString());
              site.notes = item.getString().trim();
            } else if (name.toLowerCase().endsWith("user")) {
              logger.info("User " + item.getString());
              site.creatorID = Long.parseLong(item.getString().trim());
            }
            else {
              logger.info("Got " + item);
            }
          }
        }
      }
      logger.info("made " +site);
      return site;
    } catch (Exception e) {
      logger.error("Got " +e,e);
      return null;
    }
  }

  /**
   * Copy template to something named by site name
   * copy exercise file from media to site/config/template
   * set fields in config.properties
   *   - apptitle
   *   - release date
   *    - lesson plan file
   *
   *    then copy to install path/../name
   * @paramx xid
   * @return
   */
  public boolean deploySite( Site siteByID, String configDir, String installPath) {
    logger.info("deploying " + siteByID + " given config " + configDir + " and install path " + installPath);
    try {
      File destDir = new File(configDir + File.separator + siteByID.name);
      destDir.mkdir();
      File destConfigDir = new File(destDir, "config");
      logger.info("sandbox loc for new site " +destDir);
      File srcTemplateConfigDir = new File(configDir + File.separator + "template" + File.separator + "config");
     // logger.info("Copying from " +srcTemplateConfigDir + " to " +destConfigDir);
      copyDir(srcTemplateConfigDir,destConfigDir);
      File realPath = new File(siteByID.savedExerciseFile);
      File destFile = new File(destConfigDir, "template" + File.separator + siteByID.exerciseFile);
      logger.info("sandbox loc for new file " +destFile);
      copyFile(realPath, destFile);
      File installDir = new File(installPath);

      List<String> toCopy = java.util.Arrays.asList("js", "langtest", "swf", "WEB-INF", "favicon.ico", "LangTest.css", "LangTest.html", "soundmanager2.js");
      for (String dir : toCopy) {
        File file = new File(installDir,dir);
        if (!file.exists()) logger.error("huh? "+ file + "doesn't exist?");
        else {
          File destLoc = new File(destDir, dir);
          if (file.isDirectory()) {
            copyDir(file, destLoc);
          }
          else {
            copyFile(file, destLoc);
          }
        }
      }
      File fromWeb = new File(configDir + File.separator + "template" + File.separator + "WEB-INF" + File.separator + "web.xml");
      File toWeb = new File(destDir, "WEB-INF" + File.separator + "web.xml");
      copyFile(fromWeb, toWeb);

      Properties copy = new Properties();
      File propFile = new File(destConfigDir, "template" + File.separator + "config.properties");
      FileInputStream inStream = new FileInputStream(propFile);
      copy.load(inStream);
      inStream.close();

      copy.setProperty("appTitle",siteByID.name);
      SimpleDateFormat df = new SimpleDateFormat("MM/dd");
      copy.setProperty("releaseDate",df.format(new Date()));
      copy.setProperty("lessonPlanFile",siteByID.exerciseFile);
      copy.setProperty("readFromFile","true");
      copy.setProperty("dataCollect","true");
      copy.setProperty("dataCollectAdminView","false");
      copy.setProperty("h2Database", "template");
      copy.store(new FileWriter(propFile), "");
      logger.info("copied prop file " + propFile);

      String newSiteLoc = installDir.getParent()+File.separator+siteByID.name;
      File installLoc = new File(newSiteLoc);
      logger.info("copying to install dir " + installLoc);

      copyDir(destDir, installLoc);
      logger.info("copied to install dir " + installLoc);
    } catch (Exception e) {
      logger.error("Got "+e,e);
      return false;
    }

    return true;
  }

  private void copyDir(File srcDir, File destDir) throws IOException {
    logger.debug("dir : copying from " +srcDir + " to " +destDir);

    FileUtils.copyDirectory(srcDir, destDir);
  }

  private void copyFile(File srcFile, File destFile) throws IOException {
    logger.debug("copying from " +srcFile + " to " +destFile);
    FileUtils.copyFile(srcFile, destFile);
  }

  public static void main(String [] arg) {
   // new SiteDeployer().deploySite(new Site())
  }
}
