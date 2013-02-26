package mitll.langtest.server;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.ExcelImport;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Lesson;
import mitll.langtest.shared.Site;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Copies website as needed.
 * User: GO22670
 * Date: 2/14/13
 * Time: 6:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class SiteDeployer {
  public static final int MAX_FILE_SIZE = 10000000;
  public static final String UPLOAD_FORM_NAME = "upload";
  private static Logger logger = Logger.getLogger(SiteDeployer.class);

  /**
   * @see LangTestDatabaseImpl#deploySite(long, String, String, String)
   * @param site
   * @param installPath
   * @return
   */
  public boolean isValidName(Site site, String installPath) {
    boolean valid = checkName(site);
    if (!valid) {
      logger.warn("Site name " + site.name + " not good in a url");
      return valid;
    }
    File installDir = new File(installPath);

    String newSiteLoc = getWebappsInstallLoc(site, installDir);
    boolean exists = new File(newSiteLoc).exists();
    if (exists) logger.warn("site " + newSiteLoc + " exists already.");
    else logger.info("OK " +newSiteLoc + " doesn't exist.");
    return !exists;
  }

  private String getWebappsInstallLoc(Site site, File installDir) {
    return installDir.getParent()+ File.separator+site.name;
  }

  public boolean checkName(Site site) {
    String validPattern = "\\w+";

    return Pattern.matches(validPattern, site.name);
  }

  public void doSiteResponse(DatabaseImpl db, HttpServletResponse response, SiteDeployer siteDeployer, Site site) throws IOException {
    response.setContentType("text/plain");
    if (!siteDeployer.checkName(site) || db.siteExists(site)) {
      response.getWriter().write("Name in use or invalid.");
    }
    else if (!site.getExercises().isEmpty()) {
      Site site1 = db.addSite(site);
      if (site1 != null) {
        response.getWriter().write("" + site1.id);
      } else {
        response.getWriter().write("Invalid file");
      }
    } else {
      response.getWriter().write("Invalid file");
    }
  }


  /**
   * Use apache commons fileupload utilities to handle uploading a exercise file and read the form fields.<br></br>
   * Note max file size is {@link #MAX_FILE_SIZE}
   *
   * @see DiskFileItemFactory
   * @see ServletFileUpload
   * @see LangTestDatabaseImpl#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   *
   * @param request
   * @param configDir
   * @return  site
   */
  public Site getSite(HttpServletRequest request, String configDir) {
    logger.debug("Getting site given config dir " + configDir);
    FileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);
    upload.setSizeMax(MAX_FILE_SIZE);

    try {
      List<FileItem> items = upload.parseRequest(request);
      Site site = new Site();
      for (FileItem item : items) {
        if (!item.isFormField() && UPLOAD_FORM_NAME.equals(item.getFieldName())) {
          readExercises(site, item);
          storeUploadedFile(configDir, site, (DiskFileItem) item);
        }
        else {
          readFormItemAndStoreInSite(site, item);
        }
      }
      logger.info("made " +site);


      return site;
    } catch (Exception e) {
      logger.error("Got " +e,e);
      return null;
    }
  }

  private void readFormItemAndStoreInSite(Site site, FileItem item) {
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

  private void readExercises(Site site, FileItem item) throws IOException {
    ExcelImport importer = new ExcelImport();
    logger.info("got upload " +item);
    List<Exercise> exercises = importer.readExercises(item.getInputStream());
    site.setExercises(exercises);
    StringBuilder builder = new StringBuilder();
    for (String section : importer.getSections()) {
      Map<String,Lesson> section1 = importer.getSection(section);
      if (!section1.isEmpty()) {
        builder.append(section1.keySet().size()).append(" ").append(section).append("s, ");
      }
    }
    String sectionInfo = builder.toString();
    if (sectionInfo.endsWith(", ")) sectionInfo = sectionInfo.substring(0,sectionInfo.length()-2);

    List<String> errors = importer.getErrors();
    site.setFeedback("Read " + exercises.size() + " expressions in " + sectionInfo +
        (errors.isEmpty() ? "" :"<br></br> and found " + errors.size() + " errors, e.g. : " + errors.iterator().next()));
    site.exerciseFile = item.getName();
  }

  private void storeUploadedFile(String configDir, Site site, DiskFileItem item) {
    File uploadsDir = new File(configDir + File.separator + "uploads");
    if (!uploadsDir.exists()) uploadsDir.mkdir();
    File dest = new File(configDir + File.separator + "uploads" + File.separator + System.currentTimeMillis() + "_" + site.exerciseFile);
    boolean b = item.getStoreLocation().renameTo(dest);
    if (!b) {
      logger.error("couldn't rename tmp file " + item.getStoreLocation());
    }
    else {
      logger.info("copied file to " + dest.getAbsolutePath());
      site.savedExerciseFile = dest.getAbsolutePath();
      if (!new File(site.savedExerciseFile).exists()) logger.error("huh? " +site.savedExerciseFile + " doesn't exist?");
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
   * @param toDeploy
   * @param configDir
   * @param installPath
   * @see LangTestDatabaseImpl#deploySite
   * @return true if successful
   */
  public boolean deploySite( Site toDeploy, String configDir, String installPath) {
    logger.info("deploying " + toDeploy + " given config " + configDir + " and install path " + installPath);
    try {
      long then = System.currentTimeMillis();
      File destDir = new File(configDir + File.separator + toDeploy.name);
      destDir.mkdir();
      File destConfigDir = new File(destDir, "config");
      logger.info("sandbox loc for new site " +destDir);
      // copy template config
      File srcTemplateConfigDir = new File(configDir + File.separator + "template" + File.separator + "config");
      copyDir(srcTemplateConfigDir,destConfigDir);

      // copy exercise file
      File realPath = new File(toDeploy.savedExerciseFile);
      File destFile = new File(destConfigDir, "template" + File.separator + toDeploy.exerciseFile);
      logger.info("sandbox loc for new file " +destFile);
      copyFile(realPath, destFile);

      File installDir = new File(installPath);

      copyWebsiteComponents(destDir, installDir);

      // copy web.xml file from templates
      File fromWeb = new File(configDir + File.separator + "template" + File.separator + "WEB-INF" + File.separator + "web.xml");
      File toWeb = new File(destDir, "WEB-INF" + File.separator + "web.xml");
      copyFile(fromWeb, toWeb);

      // write the properties file
      writeNewPropertyFile(toDeploy, destConfigDir);

      // deploy!
      File installLoc = deployToWebappsDir(toDeploy, destDir, installDir);
      long now = System.currentTimeMillis();

      logger.info("copied to install dir " + installLoc + " took " + (now-then) + " millis.");
    } catch (Exception e) {
      logger.error("Got "+e,e);
      return false;
    }

    return true;
  }

  private void copyWebsiteComponents(File destDir, File installDir) throws IOException {
    List<String> toCopy = java.util.Arrays.asList("js", "langtest", "swf", "WEB-INF", "favicon.ico", "LangTest.css", "LangTest.html", "soundmanager2.js");
    for (String dir : toCopy) {
      File file = new File(installDir,dir);
      if (!file.exists()) logger.error("huh? "+ file + "doesn't exist?");
      else {
        File destLoc = new File(destDir, dir);
        if (file.isDirectory()) {
          if (dir.equals("WEB-INF")) {
            FileUtils.copyDirectory(file, destLoc, new FileFilter() {
              @Override
              public boolean accept(File pathname) {
                return !pathname.getName().equals("pronz.jar");
              }
            });

          }
          else {
            copyDir(file, destLoc);
          }
        }
        else {
          copyFile(file, destLoc);
        }
      }
    }
  }

  private File deployToWebappsDir(Site toDeploy, File destDir, File installDir) throws IOException {
    String newSiteLoc = getWebappsInstallLoc(toDeploy, installDir);
    File installLoc = new File(newSiteLoc);
    logger.info("copying to install dir " + installLoc);

    if (!destDir.renameTo(installLoc)) {
      logger.debug("had to do copy dir to " +installLoc);
      copyDir(destDir, installLoc);
    }
    return installLoc;
  }

  private void writeNewPropertyFile(Site toDeploy, File destConfigDir) throws IOException {
    Properties copy = new Properties();
    File propFile = new File(destConfigDir, "template" + File.separator + "config.properties");
    FileInputStream inStream = new FileInputStream(propFile);
    copy.load(inStream);
    inStream.close();

    copy.setProperty("appTitle", toDeploy.name);
    SimpleDateFormat df = new SimpleDateFormat("MM/dd");
    copy.setProperty("releaseDate", df.format(new Date()));
    copy.setProperty("lessonPlanFile",toDeploy.exerciseFile);
    copy.setProperty("readFromFile","true");
    copy.setProperty("dataCollect","true");
    copy.setProperty("dataCollectAdminView","false");
    copy.setProperty("h2Database", "template");
    copy.setProperty("urdu", ""+toDeploy.language.equalsIgnoreCase("urdu"));
    copy.store(new FileWriter(propFile), "");
    logger.info("copied prop file " + propFile);
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
