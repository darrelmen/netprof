/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server;

import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.TrackInfo;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.security.IUserSecurityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.*;

/**
 * TODO : consider how to partition audio services...
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/8/2014.
 */
public class DatabaseServlet extends HttpServlet {
  private static final Logger logger = LogManager.getLogger(DatabaseServlet.class);
  private static final int BUFFER_SIZE = 4096;
  private DatabaseImpl db = null;
  protected ServerProperties serverProps;
  private String configDir;
  protected PathHelper pathHelper;
  private IUserSecurityManager securityManager;

  /**
   * @param wavFile
   * @param trackInfo
   * @see mitll.langtest.server.ScoreServlet#ensureMP3Later
   */
  void ensureMP3(String wavFile, TrackInfo trackInfo) {
    ensureMP3(wavFile, configDir, trackInfo);
  }

  private boolean ensureMP3(String wavFile, String configDir, TrackInfo trackInfo) {
    if (wavFile != null) {
      //  String mediaDir = pathHelper.getInstallPath();
      String mediaDir = serverProps.getMediaDir();
      //logger.debug("ensureMP3 : wav " + wavFile + " under " + mediaDir);

      AudioConversion audioConversion = new AudioConversion(serverProps);
      if (!audioConversion.exists(mediaDir, wavFile)) {
        logger.warn("ensureMP3 can't find " + wavFile + " under " + mediaDir + " trying config... ");
        mediaDir = configDir;
      }
      if (!audioConversion.exists(mediaDir, wavFile)) {
        logger.error("ensureMP3 huh? can't find " + wavFile + " under " + mediaDir);
      }
      String filePath = audioConversion.ensureWriteMP3(mediaDir, wavFile, false, trackInfo);
      return new File(filePath).exists();
    } else {
      return false;
    }
  }

  void setPaths() {
    pathHelper = getPathHelper();
    // String config = getServletContext().getInitParameter("config");
    //this.relativeConfigDir = "config" + File.separator + config;
    // logger.debug("setPaths rel " + relativeConfigDir  + " pathHelper " + pathHelper);
    this.configDir = getConfigDir();
  }

  private PathHelper getPathHelper() {
    serverProps = getDatabase().getServerProps();
    if (serverProps == null) throw new IllegalArgumentException("huh? props is null?");
    return new PathHelper(getServletContext(), serverProps);
  }

  private String getConfigDir() {
    return getConfigDir(pathHelper);
  }

  private String getConfigDir(PathHelper pathHelper) {
    String config = getServletContext().getInitParameter("config");
    return pathHelper.getInstallPath() + File.separator + "config" + File.separator + config;
  }

  protected void writeToFile(InputStream inputStream, File saveFile) throws IOException {
    // opens an output stream for writing file
    copyToOutput(inputStream, new FileOutputStream(saveFile));
  }

  private void copyToOutput(InputStream inputStream, OutputStream outputStream) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;

    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }

    outputStream.close();
    inputStream.close();
  }

  /**
   * @param request
   * @return
   * @see #doGet
   */
  int getProject(HttpServletRequest request) {
    int userIDFromRequest = securityManager.getUserIDFromRequest(request);
    if (userIDFromRequest == -1) {
      return -1;
    } else {
      return getMostRecentProjectByUser(userIDFromRequest);
    }
  }

  /**
   * @param id
   * @return
   * @see #getProject(HttpServletRequest)
   */
  int getMostRecentProjectByUser(int id) {
    return getDatabase().getUserProjectDAO().mostRecentByUser(id);
  }


  protected DatabaseImpl getDatabase() {
    if (db == null) {
      Object databaseReference = getServletContext().getAttribute(LangTestDatabaseImpl.DATABASE_REFERENCE);
      if (databaseReference != null) {
        db = (DatabaseImpl) databaseReference;
        securityManager = db.getUserSecurityManager();
        //securityManager = new UserSecurityManager(db.getUserDAO(), db.getUserSessionDAO(), this);
        // logger.debug("found existing database reference " + db + " under " +getServletContext());
      } else {
        logger.error("huh? no existing db reference?");
      }
    }
    return db;
  }

}
