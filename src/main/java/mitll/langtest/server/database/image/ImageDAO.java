/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.image;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickImage;
import mitll.npdata.dao.image.ImageDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class ImageDAO extends DAO implements IImageDAO {
  private static final Logger logger = LogManager.getLogger(ImageDAO.class);
  //public static final int DEFAULT_IMAGE_ID = 1;
  private final boolean doCheckOnStartup;
  private final ServerProperties serverProps;
  private final boolean hasMediaDir;
  private final long now = System.currentTimeMillis();
  private final long before = now - (24 * 60 * 60 * 1000);
  private int defImageID = -1;
  private final ImageDAOWrapper dao;

  /**
   * @param database
   * @param dbConnection
   * @see DatabaseImpl#makeDialogDAOs
   */
  public ImageDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new ImageDAOWrapper(dbConnection);
    serverProps = database.getServerProps();
    doCheckOnStartup = serverProps.doAudioCheckOnStartup();
    String mediaDir = serverProps.getMediaDir();
    hasMediaDir = new File(mediaDir).exists();
  }

  @Override
  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return false;
  }

  @Override
  public int insert(SlickImage image) {
    return dao.insert(image);
  }

  @Override
  public List<SlickImage> getAll(int projid) {
    return dao.getAll(projid);
  }

  @Override
  public List<SlickImage> getAllNoExistsCheck(int projid) {
    return dao.getAllNoCheck(projid);
  }

  @Override
  public int getDefault() {
    return defImageID;
  }

  @Override
  public int ensureDefault(int defProjectID) {
    if (defImageID > 0) return defImageID;
    else {
      Collection<SlickImage> byID = dao.getDefault();

      if (byID.isEmpty()) {
        return defImageID = addDefault(defProjectID);
      } else {
        return defImageID = byID.iterator().next().id();
      }
    }
  }

  private int addDefault(int defProjectID) {

    logger.info("addDefault for " + defProjectID);
    long now = System.currentTimeMillis();

    Timestamp modified = new Timestamp(now);
    return insert(new SlickImage(-1,
        defProjectID,
        -1,
        modified,
        modified,
        "default", "/opt/netprof/image/default.jpg", 0, 0, false, false, modified
    ));
  }


  public void makeSureImagesAreThere(int projectID, String language, boolean validateAll) {
    if (hasMediaDir) {
      logger.info("makeSureImagesAreThere " + projectID + " " + language + " validate all = " + validateAll);
      //logger.debug("makeSureImagesAreThere media dir " + file + " exists ");
      String mediaDir = serverProps.getMediaDir();
      File file = new File(mediaDir);
      if (file.isDirectory()) {
        String[] list = file.list();
        if (list == null) {
          logger.error("setAudioDAO configuration error - can't get files from media directory " + mediaDir);
        } else if (list.length > 0) { // only on pnetprof (behind firewall), znetprof has no audio, might have a directory.
          //logger.debug("setAudioDAO validating files under " + file.getAbsolutePath());
          boolean foundFiles = didFindAnyAudioFiles(projectID);
          if (validateAll ||
              (serverProps.doAudioChecksInProduction() &&
                  (serverProps.doAudioFileExistsCheck() || !foundFiles))) {
            logger.info("makeSureImagesAreThere validateFileExists ");
            validateFileExists(projectID, mediaDir, language, validateAll);
          }
        }
      } else {
        logger.error("configuration error - (" + projectID + " " + language +
            ") expecting media directory " + mediaDir + " to be directory.");
      }
    } /*else {
      logger.warn("makeSureImagesAreThere : (" + projectID + " " + language +
          ") configuration error? - expecting a media directory " + mediaDir);
    }*/
  }


  private boolean didFindAnyAudioFiles(int projectid) {
    return dao.getCountExists(projectid) > 0;
  }

  private void validateFileExists(int projid, String installPath, String language, boolean checkAll) {
    long pastTime = doCheckOnStartup || checkAll ? now : before;
    logger.info("validateFileExists before " + new Date(pastTime));
    dao.validateFileExists(projid, new Timestamp(pastTime), installPath, language.toLowerCase());
  }
}
