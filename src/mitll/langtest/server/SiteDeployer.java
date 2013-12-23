package mitll.langtest.server;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.ExcelImport;
import mitll.langtest.server.database.Lesson;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Site;
import mitll.langtest.shared.User;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
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
  private static final int MAX_FILE_SIZE = 10000000;
  private static final String UPLOAD_FORM_NAME = "upload";
  private static Logger logger = Logger.getLogger(SiteDeployer.class);

  /**
   *
   * @see LangTestDatabaseImpl#deploySite(long, String, String, String)
   * @param db
   * @param mailSupport
   * @param request
   * @param configDir
   * @param installPath
   * @param siteID
   * @param name
   * @param language
   * @param notes
   * @return
   */
  public boolean deploySite(DatabaseImpl db, MailSupport mailSupport,
                            HttpServletRequest request, String configDir, String installPath,
                            long siteID, String name, String language, String notes) {
    Site siteByID = db.getSiteByID(siteID);

    SiteDeployer siteDeployer = new SiteDeployer();

    siteByID = db.updateSite(siteByID, name, language, notes);
    if (!siteDeployer.isValidName(siteByID, installPath)) {
      return false;
    }
    boolean b = siteDeployer.deploySite(siteByID, configDir, installPath);
    if (b) {
      db.deploy(siteByID);
    } else {
      logger.warn("didn't deploy " + siteByID);
    }

    if (!b) return b;
    waitUntilDeployed(request,siteByID);

    // String firstName = "Unknown user #" + siteByID.creatorID;
    // String lastName = "Unk.";
    String userid = "";
    for (User user : db.getUsers())
      if (user.id == siteByID.creatorID) {
        //    firstName = user.firstName;
        //    lastName = user.lastName;
        userid = user.userID;
      }

    String subject = "Site " + name + " deployed";
    String message = "Hi,\n" +
      "At site " + getBaseUrl(request) + "" +
      " User '" +
      userid+
      //firstName + " " + lastName +
      "' deployed site " + name + ".\n" +
      "Thought you might want to know.\n" +
      "Thanks,\n" +
      "Your friendly web admin.";
   // MailSupport mailSupport = new MailSupport(props);
    mailSupport.email(subject, message);

    return true;
  }

  private void waitUntilDeployed(HttpServletRequest request, Site siteByID) {
    int tries = 3;
    boolean valid = false;     // todo improve
    while (tries-- > 0) {
      String baseUrl = "";
      try {
        baseUrl = getBaseUrl(request,siteByID.name);
        URL oracle = new URL(baseUrl);
        URLConnection yc = oracle.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
          yc.getInputStream()));
        in.close();

        valid = true;
      } catch (Exception e) {
        logger.info("reading " + baseUrl + " got " + e);
      }
      if (!valid) {
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public String getBaseUrl(HttpServletRequest request) {
    return getBaseUrl(request, request.getContextPath());
  }

  private String getBaseUrl(HttpServletRequest request, String name) {
    if ((request.getServerPort() == 80) ||
      (request.getServerPort() == 443)) {
      return request.getScheme() + "://" +
        request.getServerName() + "/" + name;
    } else {
      return request.getScheme() + "://" +
        request.getServerName() + ":" + request.getServerPort() +
        "/" + name;
    }
  }

  public void sendNewUserEmail(MailSupport mailSupport, HttpServletRequest request,  String userID) {
    String subject = "User " + userID + " registered";
    String message = "Hi,\n" +
      "At site " + getBaseUrl(request) + "\n" +
      " got new user " + userID + ".\n" +
      "Should this person be enabled?\n" +
      "Thanks,\n" +
      "Your friendly web admin.";
    mailSupport.email(subject, message);
  }

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

  private boolean checkName(Site site) {
    String validPattern = "\\w+";

    return Pattern.matches(validPattern, site.name);
  }

  /**
   * @see LangTestDatabaseImpl#service
   * @param db
   * @param response
   * @param siteDeployer
   * @param site
   * @throws IOException
   */
  public void doSiteResponse(DatabaseImpl db, HttpServletResponse response, SiteDeployer siteDeployer, Site site) throws IOException {
    response.setContentType("text/plain");
    if (!site.isValid() || !siteDeployer.checkName(site) || db.siteExists(site)) {
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
  public SiteInfo getSite(HttpServletRequest request, String configDir, DatabaseImpl db, String installPath) {
    logger.debug("Getting site given config dir " + configDir);
    FileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);
    upload.setSizeMax(MAX_FILE_SIZE);

    try {
      List<FileItem> items = upload.parseRequest(request);
      logger.info("getSite : from http request, got " + items.size() + " items.");

      DiskFileItem rememberedFileItem = null;

      boolean isUpdate = false;
      Site site = new Site();
      for (FileItem item : items) {
        if (!item.isFormField() && UPLOAD_FORM_NAME.equals(item.getFieldName())) {
          rememberedFileItem = (DiskFileItem) item;
        }
        else {
          isUpdate |= readFormItemAndStoreInSite(site, item);
        }
      }

      if (isUpdate) {
        Site siteByID = db.getSiteByID(site.id);
        if (siteByID != null) {
          logger.info("found existing " + siteByID);
          String notes = site.notes;
          site = siteByID;
          site.notes = notes;
        }
        else {
          logger.error("huh? couldn't find site with id = "+site.id);
        }
      } else {
        logger.info("made new " + site);
      }
      if (rememberedFileItem != null) {
        readExercises(site, rememberedFileItem);
        storeUploadedFile(configDir, site, rememberedFileItem);
        logger.info("site now " + site);
        if (isUpdate) {
          logger.info("\t updating site " + site + " under " + installPath);

          updateExerciseFile(site, installPath,db);
        }
      }

      return new SiteInfo(site,isUpdate);
    } catch (Exception e) {
      logger.error("Got " +e,e);
      return null;
    }
  }

  public static class SiteInfo {
    Site site;
    boolean isUpdate = false;

    public SiteInfo(Site site, boolean isUpdate) { this.site = site; this.isUpdate = isUpdate;}

  }

  private boolean readFormItemAndStoreInSite(Site site, FileItem item) {
    logger.info("from http request, got " + item);
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
      } else if (name.toLowerCase().endsWith("siteid")) {
        logger.info("-------------> got siteid <----------------\n\n");
        site.id = Long.parseLong(item.getString().trim());
        return true;
        //  logger.info("User " + item.getString());
       // site.creatorID = Long.parseLong(item.getString().trim());
      }
      else {
        logger.info("Got " + item);
      }
    }
    return false;
  }

  private void readExercises(Site site, FileItem item) throws IOException {
    logger.info("got upload " +item);
    String fileName = item.getName();
    InputStream inputStream = item.getInputStream();
    readExercisesPopulateSite(site, fileName, inputStream);
  }

  private void readExercisesPopulateSite(Site site, String fileName, InputStream inputStream) {
    ExcelImport importer = new ExcelImport();
    List<Exercise> exercises = importer.readExercises(inputStream);
    site.setExercises(exercises);
    String sectionInfo = getSectionInfo(importer);

    List<String> errors = importer.getErrors();
    site.setFeedback("Read " + exercises.size() + " expressions in " + sectionInfo +
        (errors.isEmpty() ? "" :"<br></br> and found " + errors.size() + " errors, e.g. : " + errors.iterator().next()));
    site.setExerciseFile(fileName);
  }

  private String getSectionInfo(ExcelImport importer) {
    StringBuilder builder = new StringBuilder();
    for (String section : importer.getSections()) {
      Map<String,Lesson> section1 = importer.getSection(section);
      if (!section1.isEmpty()) {
        builder.append(section1.keySet().size()).append(" ").append(section).append("s, ");
      }
    }
    String sectionInfo = builder.toString();
    if (sectionInfo.endsWith(", ")) sectionInfo = sectionInfo.substring(0,sectionInfo.length()-2);
    return sectionInfo;
  }

  /**
   * @see #getSite
   * @param configDir
   * @param site
   * @param item
   */
  private void storeUploadedFile(String configDir, Site site, DiskFileItem item) {
    String uploadsDirPath = configDir + File.separator + "uploads";
    File uploadsDir = new File(uploadsDirPath);
    if (!uploadsDir.exists()) uploadsDir.mkdir();
    File dest = new File(uploadsDirPath + File.separator + System.currentTimeMillis() + "_" + site.getExerciseFile());
    File uploadedFile = item.getStoreLocation();
    boolean b = uploadedFile.renameTo(dest);
    if (!b) {
      logger.warn("storeUploadedFile : couldn't rename tmp file " + uploadedFile + " to " +dest.getAbsolutePath());
      try {
        FileUtils.copyFile(uploadedFile, dest);
        b = dest.exists();
      } catch (IOException e) {
        logger.error("storeUploadedFile : couldn't copy tmp file " + uploadedFile + " to " +dest.getAbsolutePath());
      }
    }
    if (!b) {
      logger.error("storeUploadedFile : couldn't rename tmp file " + uploadedFile + " to " +dest.getAbsolutePath());
      site.setValid(false);
    }
    else {
      logger.info("storeUploadedFile : copied file to " + dest.getAbsolutePath());
      site.setSavedExerciseFile(dest.getAbsolutePath());
      if (!new File(site.getSavedExerciseFile()).exists()) {
        logger.error("storeUploadedFile : huh? " + site.getSavedExerciseFile() + " doesn't exist?");
      }
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
      String templateDir = configDir + File.separator + "template";
      File srcTemplateConfigDir = new File(templateDir + File.separator + "config");
      copyDir(srcTemplateConfigDir,destConfigDir);

      // copy exercise file
      copyExerciseFile(toDeploy, destConfigDir);

      File installDir = new File(installPath);

      copyWebsiteComponents(destDir, installDir);

      // copy web.xml file from templates
      String webXMLPath = "WEB-INF" + File.separator + "web.xml";
      File fromWeb = new File(templateDir, webXMLPath);
      File toWeb   = new File(destDir, webXMLPath);
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

  private void updateExerciseFile(Site toDeploy, String installPath, DatabaseImpl db) throws IOException {
    File installDir = new File(installPath);

    String newSiteLoc = getWebappsInstallLoc(toDeploy, installDir);
    File installConfigDir = new File(newSiteLoc, "config");
    logger.info("copying to install dir " + installConfigDir);
    copyExerciseFile(toDeploy, installConfigDir);

    writeNewPropertyFile(toDeploy, installConfigDir);
    String webXMLPath = "WEB-INF" + File.separator + "web.xml";

    File toWeb = new File(newSiteLoc, webXMLPath);
    if (!toWeb.setLastModified(System.currentTimeMillis())) logger.warn("huh? couldn't touch " + toWeb);

    db.updateSiteFile(toDeploy);
  }

  /**
   * Put exercise file under (config/)template
   * @param toDeploy
   * @param destConfigDir
   * @throws IOException
   */
  private void copyExerciseFile(Site toDeploy, File destConfigDir) throws IOException {
    File realPath = new File(toDeploy.getSavedExerciseFile());
    File destFile = new File(destConfigDir, "template" + File.separator + toDeploy.getExerciseFile());
    logger.info("copy from " +realPath + " to (sandbox) loc for new file " +destFile);
    copyFile(realPath, destFile);
  }

  private void copyWebsiteComponents(File destDir, File installDir) throws IOException {
    List<String> toCopy = java.util.Arrays.asList("js", "langtest", "swf", "WEB-INF", "favicon.ico", "LangTest.css", "LangTest.html"/*, "soundmanager2.js"*/);
    for (String dir : toCopy) {
      File file = new File(installDir,dir);
      if (!file.exists()) logger.error("huh? "+ file + "doesn't exist?");
      else {
        File destLoc = new File(destDir, dir);
        if (file.isDirectory()) {
/*          if (dir.equals("WEB-INF")) {
            FileUtils.copyDirectory(file, destLoc, new FileFilter() {
              @Override
              public boolean accept(File pathname) {
                return !pathname.getName().equals("pronz.jar");
              }
            });

          }
          else {*/
            copyDir(file, destLoc);
  //        }
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
    File propFile = new File(destConfigDir, "template" + File.separator + "config.properties");

    Properties copy = readFromFile(propFile);

    copy.setProperty("appTitle", toDeploy.name);
    SimpleDateFormat df = new SimpleDateFormat("MM/dd");
    copy.setProperty("releaseDate", df.format(new Date()));
    copy.setProperty("lessonPlanFile", toDeploy.getExerciseFile());
    copy.setProperty("readFromFile","true");
    copy.setProperty("dataCollect","true");
    copy.setProperty("dataCollectAdminView","false");
    copy.setProperty("h2Database", "template");
    copy.setProperty("language", toDeploy.language);
    copy.setProperty("urdu", ""+toDeploy.language.equalsIgnoreCase("urdu"));


    copy.store(new FileWriter(propFile), "");
    logger.info("copied prop file " + propFile);
  }

  private Properties readFromFile(File propFile) throws IOException {
    Properties copy = new Properties();

    FileInputStream inStream = new FileInputStream(propFile);
    copy.load(inStream);
    inStream.close();
    return copy;
  }

  private void copyDir(File srcDir, File destDir) throws IOException {
    logger.debug("dir : copying from " +srcDir + " to " +destDir);
    FileUtils.copyDirectory(srcDir, destDir);
  }

  private void copyFile(File srcFile, File destFile) throws IOException {
    logger.debug("copying from " +srcFile + " to " +destFile);
    FileUtils.copyFile(srcFile, destFile);
  }
}
