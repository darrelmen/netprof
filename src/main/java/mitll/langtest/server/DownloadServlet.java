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

package mitll.langtest.server;

import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.server.audio.AudioExport;
import mitll.langtest.server.audio.AudioExportOptions;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.excel.EventDAOToExcel;
import mitll.langtest.server.database.excel.ResultDAOToExcel;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.result.MonitorResult;
import mitll.langtest.shared.user.User;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class DownloadServlet extends DatabaseServlet {
  private static final Logger logger = LogManager.getLogger(DownloadServlet.class);
  private static final String AUDIO = "audio";
  private static final String LIST = "list";
  private static final String DIALOG = "d";
  private static final String DIALOG_ARG = DIALOG + "=";
  private static final String FILE = "file";

  private static final String COMPRESSED_SUFFIX = "mp3";
  private static final String RESULTS = "results";
  /**
   * @see #returnSpreadsheet(HttpServletResponse, DatabaseImpl, String, int, String)
   */
  private static final String EVENTS = "events";
  private static final String REQUEST = "request";
  private static final String MALE = "male=";
  private static final String REGULAR = "regular=";
  private static final String CONTEXT = "context=";
  /**
   * @see #getAudioExportOptions
   */
  private static final String LISTS = "Lists=[";

  private static final String REGEXAMPERSAND = "&";

  private static final String AMPERSAND = DownloadHelper.AMPERSAND;
  private static final String COMMA = DownloadHelper.COMMA;
  private static final String RESULTS_XLSX = "results.xlsx";
  private static final String EVENTS_XLSX = "events.xlsx";
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final String UNIT = "unit";
  private static final String SEARCH = "search";
  private static final String AUDIO1 = "audio";
  //private static final int BUFFER_SIZE = 4096;
  private static final String STUDENT_AUDIO = "studentAudio";
  private static final String USERID = "userid";
  private static final String CONTENT_DISPOSITION = "Content-Disposition";

  /**
   * This is getting complicated.
   * <p/>
   * Just calling this servlet gives you a zip with one default audio cut per exercise, if available.
   * e.g. https://np.ll.mit.edu/npfClassroomPashto3/downloadAudio
   * <p/>
   * Calling it with an empty argument list, e.g. https://np.ll.mit.edu/npfClassroomPashto3/downloadAudio?{}
   * returns a zip with just an excel spreadsheet of the all the backing data.
   * <p/>
   * Calling it with a list argument, e.g. https://np.ll.mit.edu/npfClassroomPashto3/downloadAudio?list=2
   * returns an excel spreadsheet and audio for that list - one male and one female recording, if available.
   * <p/>
   * Without a list, but without an argument indicates a chapter selection, e.g. https://np.ll.mit.edu/npfClassroomPashto3/downloadAudio?{Chapter=[29%20LC-1]}
   * <p/>
   * Otherwise, either the users, results, or events table is returned as a spreadsheet, e.g. https://np.ll.mit.edu/npfClassroomPashto3/downloadUsers
   * <p/>
   * You can only see these in admin mode: https://np.ll.mit.edu/npfClassroomPashto3/?admin
   *
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DatabaseImpl db = getDatabase();

    if (db != null) {
      try {
        int projid = getProjectIDFromSession(request);

        if (projid == -1) {
          projid = getProjectIDFromUser(request);
        }
        if (projid == -1) {
          handleNoProject(response);
          return;
        }
        //       logger.info("doGet : current session found projid " + project1);
        Project project = getDatabase().getProject(projid);
        //boolean hasProjectSpecificAudio = project.hasProjectSpecificAudio();
        String language = project.getLanguageEnum().toDisplay();

        String requestURI = request.getRequestURI();

        logger.info("doGet :" +
            "\n\tRequest " + request.getQueryString() +
            // "\n\tRequest decode " + URLDecoder.decode(request.getQueryString(), "UTF-8") +
            "\n\tpath    " + request.getPathInfo() +
            "\n\turi     " + requestURI +
            "\n\turl     " + request.getRequestURL() +
            "\n\tpath    " + request.getServletPath());

        if (requestURI.toLowerCase().contains(AUDIO)) {
          String queryString = URLDecoder.decode(request.getQueryString(), "UTF-8");
          if (queryString.startsWith(LIST) || queryString.contains(LISTS)) {
            logger.info("downloadAList " + requestURI);
            downloadAList(response, db, projid, queryString);
          } else if (queryString.startsWith(FILE)) {
            logger.info("returnAudioFile " + requestURI);
            returnAudioFile(response, db, queryString, language, projid);
          } else if (queryString.startsWith(REQUEST + "=" + STUDENT_AUDIO)) {
            writeStudentAudio(request, response, db, projid);
          } else if (queryString.startsWith(REQUEST)) {
            //          logger.info("writeAudioZip " + requestURI);
            writeAudioZip(response, db, queryString, projid);
          } else if (queryString.contains(DIALOG_ARG)) {
            //        logger.info("downloadDialogContent " + requestURI);
            downloadDialogContent(response, db, projid, queryString);
          } else {
            logger.warn("huh? did nothing with " + requestURI);
          }
        } else {
          //    logger.info("file download request " + requestURI);
          returnSpreadsheet(response, db, requestURI, projid, language);
        }
      } catch (Exception e) {
        logger.error("doGet Got " + e, e);
        db.logAndNotify(e);
        throw new IOException("doGet couldn't process request.", e);
      }
    }

    closeOutputStream(response);
  }

  private void writeStudentAudio(HttpServletRequest request, HttpServletResponse response, DatabaseImpl db, int projid) throws DominoSessionException {
    ParamParser invoke = new ParamParser(request.getQueryString().split("&")).invoke(false);

    logger.info("Got " + invoke.getSelection());
    Collection<String> userid = invoke.getSelection().get(USERID);

    int user = getInt(userid);

    Collection<String> sessionid = invoke.getSelection().get("sessionid");
//            logger.info("writeStudentAudioZip " + requestURI);
    String session = sessionid == null || sessionid.isEmpty() ? "-1" : sessionid.iterator().next();
    Collection<String> from = invoke.getSelection().get("from");
    long fromTime = getLong(from);
    Collection<String> to = invoke.getSelection().get("to");
    long toTime = getLong(to);
    logger.info("from " + new Date(fromTime));
    logger.info("to   " + new Date(toTime));
    writeStudentAudioZip(response, db, user, session, projid, fromTime, toTime,
        db.getUserSecurityManager().getUserIDFromSessionLight(request));
  }

  private int getInt(Collection<String> userid) {
    int user = -1;
    try {
      user = userid == null || userid.isEmpty() ? -1 : Integer.parseInt(userid.iterator().next());
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
    return user;
  }

  private long getLong(Collection<String> userid) {
    long user = -1;
    try {
      user = userid == null || userid.isEmpty() ? -1 : Long.parseLong(userid.iterator().next());
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
    return user;
  }

  private void handleNoProject(HttpServletResponse response) throws IOException {
    logger.warn("doGet no current project for request ");
    response.setContentType("text/html");
    response.getOutputStream().write("no project for request".getBytes());
    closeOutputStream(response);
  }

  private void downloadDialogContent(HttpServletResponse response, DatabaseImpl db, int projid, String queryString) {
    String[] split = queryString.split(DIALOG_ARG);

    if (split.length == 2) {
      String[] splitArgs = split[1].split(REGEXAMPERSAND);
      String listid = splitArgs[0];
      if (!listid.isEmpty()) {
        writeDialogList(response, db, listid, projid, getAudioExportOptions(splitArgs));
      }
    }
  }

  private void downloadAList(HttpServletResponse response, DatabaseImpl db, int projid, String queryString) {
    if (queryString.contains(LISTS)) {
      String s = queryString.split("Lists=\\[")[1];
      String[] split = s.split(("\\]"));
      String listid = split[0];
      if (!listid.isEmpty()) {
        String[] splitArgs = split[1].split(REGEXAMPERSAND);
        writeUserList(response, db, listid, projid, getAudioExportOptions(splitArgs));
      }
    } else {
      String[] split = queryString.split("list=");

      if (split.length == 2) {
        String[] splitArgs = split[1].split(REGEXAMPERSAND);
        String listid = splitArgs[0];
        if (!listid.isEmpty()) {
          writeUserList(response, db, listid, projid, getAudioExportOptions(splitArgs));
        }
      }
    }
  }

  /**
   * @param request
   * @return
   * @throws DominoSessionException
   */
  private int getProjectIDFromUser(HttpServletRequest request) throws DominoSessionException {
    logger.info("doGet no project id on session, let's try the security manager");
    int loggedInUser = securityManager.getLoggedInUserID(request);
    int projid = -1;
    if (loggedInUser != -1) {
      logger.debug("doGet found session user " + loggedInUser);
      projid = getMostRecentProjectByUser(loggedInUser);
    } else {
      logger.warn("doGet couldn't find user via request...");
    }
    return projid;
  }

  private void closeOutputStream(HttpServletResponse response) {
    try {
      response.getOutputStream().close();
    } catch (IOException e) {
      logger.warn("got " + e, e);
    }
  }

  /**
   * @param splitArgs
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private AudioExportOptions getAudioExportOptions(String[] splitArgs) {
    AudioExportOptions options = new AudioExportOptions();
    for (String arg : splitArgs) {
      logger.info("getAudioExportOptions arg " + arg);

      if (arg.startsWith(MALE)) options.setJustMale(isTrue(arg));
      else if (arg.startsWith(REGULAR)) options.setJustRegularSpeed(isTrue(arg));
      else if (arg.startsWith(CONTEXT)) options.setJustContext(isTrue(arg));
      else if (arg.startsWith(AUDIO1)) options.setIncludeAudio(isTrue(arg));
      else if (arg.startsWith(SEARCH)) {
        String[] split = arg.split("=");
        String search = split.length > 1 ? split[1] : "";
        options.setSearch(search);
      } else {
        logger.info("getAudioExportOptions : got unexpected arg '" + arg + "'");
      }
    }
    return options;
  }

  private boolean isTrue(String arg) {
    return arg.endsWith("true");
  }

  /**
   * @param response
   * @param db
   * @param queryString
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private void writeAudioZip(HttpServletResponse response,
                             DatabaseImpl db,
                             String queryString,
                             int projid) {
    //logger.debug("writeAudioZip : request " + projid + " " + language + " query " + queryString);
    String[] split1 = queryString.split(REGEXAMPERSAND);
    Project project = db.getProject(projid);
    String unitChapter = getUnitAndChapter(split1);

    Map<String, Collection<String>> typeToSection = getTypeToSelectionFromRequest(unitChapter);
    AudioExportOptions audioExportOptions =
        getAudioExportOptions(split1);

    {
      String zipFileName = getZipFileName(db, typeToSection, projid, project.getLanguageEnum().toDisplay(), audioExportOptions.getInfo());
      //logger.info("writeAudioZip zip file name " + zipFileName);
      setHeader(response, zipFileName);
    }

    writeZip(response, typeToSection, projid, audioExportOptions);
  }

  private void writeStudentAudioZip(HttpServletResponse response,
                                    DatabaseImpl db,
                                    int userid,
                                    String session,
                                    int projid,
                                    long from,
                                    long to,
                                    int callingUser) {
    // logger.debug("writeAudioZip : request " + projid + " " + language + " query " + queryString);
    Project project = db.getProject(projid);

    AudioExportOptions audioExportOptions = new AudioExportOptions();

    String userID = db.getUserDAO().getMiniUser(userid).getUserID();
    audioExportOptions.setInfo("student_" + userID);

    {
      String zipFileName = getZipFileName(db, new HashMap<>(), projid, project.getLanguageEnum().toDisplay(), audioExportOptions.getInfo());
      //logger.info("writeAudioZip zip file name " + zipFileName);
      setHeader(response, zipFileName);
    }

    writeStudentZip(response, projid, userid, session, audioExportOptions, from, to, callingUser);
  }

  /**
   * TODO : uh... don't assume unit
   *
   * @param split1
   * @return
   * @see #writeAudioZip(HttpServletResponse, DatabaseImpl, String, int)
   */
  private String getUnitAndChapter(String[] split1) {
    String unitChapter = "";
    String prefix = UNIT;

    for (String arg : split1) {
      String prefix1 = prefix + "=";
      if (arg.startsWith(prefix1)) {
        unitChapter = arg.split(prefix1)[1];
      }
    }
    return unitChapter;
  }

  /**
   * Make a zip of audio, typically for a chapter or unit.
   *
   * @param response
   * @param typeToSection
   * @see #doGet
   */
  private void writeZip(HttpServletResponse response,
                        Map<String, Collection<String>> typeToSection,
                        int projectid,
                        AudioExportOptions options) {
    try {
      getDatabase().writeZip(response.getOutputStream(), typeToSection, projectid, options, ensureAudioHelper);
    } catch (Exception e) {
      logger.error("couldn't write zip?", e);
    }
  }

  private void writeStudentZip(HttpServletResponse response,
                               int projectid,
                               int userID,
                               String sessionID,
                               AudioExportOptions options,
                               long from, long to, int callingUser) {
    try {
      Timestamp from1 = new Timestamp(from);
      List<MonitorResult> resultsBySession = sessionID.equalsIgnoreCase("-1") ?
          db.getResultDAO().getResultsInTimeRange(userID, projectid, from1, new Timestamp(to)) :
          db.getResultDAO().getResultsBySession(userID, projectid, sessionID);

      logger.info("writeStudentZip for " +
          "\n\tuser  " + userID +
          "\n\tsess  " + sessionID +
          "\n\tfound " + resultsBySession.size());

      Project project = db.getProject(projectid);

      String suffix = getTimestamp(sessionID, from1);
      User userWhere = db.getUserDAO().getUserWhere(userID);
      User caller = db.getUserDAO().getUserWhere(callingUser);

      String base = userWhere.getFirstInitialName() + suffix;
      String baseName = base
          .replaceAll("\\s++", "_")
          .replaceAll("\\.", "_");

      new AudioExport(db.getServerProps(), project.getPathHelper().getContext())
          .writeResultsToStream(
              project,
              resultsBySession,
              baseName,
              project.getTypeOrder(),
              project.getLanguage(),
              response.getOutputStream(),
              options.setIncludeAudio(true),
              project.isEnglish(),
              userWhere, caller.isAdmin());

    } catch (Exception e) {
      logger.error("couldn't write zip?", e);
    }
  }

  @NotNull
  private String getTimestamp(String sessionID, Timestamp from1) {
    long start = getSession(sessionID);
    SimpleDateFormat format = new SimpleDateFormat("MMM d yyyy h mm a");

    return start == -1 ? "_" + format.format(from1) : "_" + format.format(new Date(start));
  }

  private long getSession(String sessionID) {
    long start = -1;
    try {
      start = Long.parseLong(sessionID);
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
    return start;
  }

  private String getZipFileName(DatabaseImpl db,
                                Map<String, Collection<String>> typeToSection,
                                int projectid, String language,

                                String info) {
    String name = getBaseName(db, typeToSection, projectid, language, info);
    name += ".zip";
    return name;
  }

  private String getBaseName(DatabaseImpl db,
                             Map<String, Collection<String>> typeToSection,
                             int projectid,
                             String language,
                             String info) {
    String name = typeToSection.isEmpty() ? AUDIO : db.getPrefix(typeToSection, projectid);
    name = name.replaceAll(",", "_");
    name += info;
    name = language + "_" + name;
    return name;
  }

  /**
   * Option to download an mp3 you've just recorded.
   * <p/>
   * Hack to remove N/A english fields
   *
   * @param response
   * @param db
   * @param queryString
   * @param projid
   * @throws IOException
   * @see #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  private void returnAudioFile(HttpServletResponse response,
                               DatabaseImpl db,
                               String queryString,
                               String language,
                               int projid) throws IOException {
    String[] split = queryString.split(REGEXAMPERSAND);

    String file = split[0].split("=")[1];

    // better be mp3 lying around - see ensureCompressedAudio

    if (file.endsWith(WAV)) file = file.replaceAll(WAV, MP3);

    {
      String exercise = split[1].split("=")[1];
      String useridString = split[2].split("=")[1];

      logger.debug("returnAudioFile download exercise #" + exercise + " for user id=" + useridString + " file " + file);

      String underscores = getFilenameForDownload(db, projid, Integer.parseInt(exercise), useridString, language);

      logger.debug("returnAudioFile query is " + queryString + " exercise " + exercise +
          " user " + useridString + " so name is " + underscores);

      setResponseForAudio(response, underscores);
    }

    File fileRef = pathHelper.getAbsoluteAudioFile(file);
    if (!fileRef.exists()) {
      logger.warn("huh? can't find " + file + " at " + fileRef.getAbsolutePath());
    } else {
      FileInputStream input = new FileInputStream(fileRef);
      int size = (int) input.getChannel().size();
      // logger.debug("copying file " + fileRef + " size  " + size);
      response.setContentLength(size);

      IOUtils.copy(input, response.getOutputStream());
      response.getOutputStream().flush();
    }
  }

  /**
   * try to prevent browser from complaining that it's getting an expected MIME type.
   *
   * @param response
   * @param underscores
   */
  private void setResponseForAudio(HttpServletResponse response, String underscores) {
    response.setContentType("audio/mpeg");
    // response.setCharacterEncoding(UTF_8);
    response.setHeader(CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + underscores);
//    response.setHeader("Content-Disposition", "filename='" + underscores +"'");
  }

  /**
   * Return an attachment that looks like foreign_english_by_user.
   *
   * @param db           to talk to
   * @param exercise     id of the exercise
   * @param useridString missing is OK, although unexpected
   * @return name without spaces
   * @throws UnsupportedEncodingException
   */
  private String getFilenameForDownload(DatabaseImpl db, int projid, int exercise, String useridString, String language)
      throws UnsupportedEncodingException {
    CommonExercise exercise1 = db.getExercise(projid, exercise);

    if (exercise1 == null) {
      logger.error("getFilenameForDownload couldn't find exercise #" + exercise + " for user '" + useridString + "' and language " + language + " for project #" + projid);
      return "Unknown_Exercise";
    } else {
      return getPrettyFileName(db, useridString, language, exercise1);
    }
  }

  @NotNull
  private String getPrettyFileName(DatabaseImpl db, String useridString, String language, CommonShell exercise1) throws UnsupportedEncodingException {
    boolean isEnglish = language.equalsIgnoreCase("isEnglish");

    // foreign part
    String foreignPart = isEnglish ? "" : exercise1.getForeignLanguage().trim();

    // english part
    String englishPart = exercise1.getEnglish().trim();
    if (englishPart.equals("N/A")) englishPart = "";
    if (!englishPart.isEmpty()) englishPart = "_" + englishPart;

    // user part
    String userPart = getUserPart(db, Integer.parseInt(useridString));

    String fileName = foreignPart + englishPart + userPart;
    fileName = fileName.replaceAll("\\.", "");
    fileName += "." + COMPRESSED_SUFFIX;

    //logger.debug("file is '" + fileName + "'");
    String underscores = fileName.replaceAll("\\p{Z}+", "_");  // split on spaces
    underscores = URLEncoder.encode(underscores, "UTF-8");
    return underscores;
  }

  private String getUserPart(DatabaseImpl db, int userid) {
    User userWhere = db.getUserDAO().getUserWhere(userid);
    return userWhere != null ? (userWhere.getUserID().isEmpty() ? "" : "_by_" + userWhere.getUserID()) : "";
  }

  /**
   * @param response
   * @param db
   * @param encodedFileName
   * @param language
   * @throws IOException
   * @see #doGet
   */
  private void returnSpreadsheet(HttpServletResponse response,
                                 DatabaseImpl db,
                                 String encodedFileName,
                                 int projectid,
                                 String language) throws IOException {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    ServletOutputStream outputStream = response.getOutputStream();
    String projectName = getProjectName(projectid);
    String prefix = language + "_" + projectName.replaceAll("\\s++", "_") + "_";

    /*    if (encodedFileName.toLowerCase().contains(USERS)) {
      String filename = prefix + "users.xlsx";
      response.setHeader("Content-Disposition", "attachment; filename=" + filename);
      db.usersToXLSX(response.getOutputStream());
    } else
      */

    logger.info("returnSpreadsheet : (" + projectName + ") req " + encodedFileName);

    if (encodedFileName.toLowerCase().contains(RESULTS)) {
      setFilenameHeader(response, prefix + RESULTS_XLSX);
      new ResultDAOToExcel().writeExcelToStream(db.getMonitorResults(projectid), db.getTypeOrder(projectid), outputStream);
    } else if (encodedFileName.toLowerCase().contains(EVENTS)) {
      setFilenameHeader(response, prefix + EVENTS_XLSX);
      new EventDAOToExcel(db).toXLSX(db.getEventDAO().getAll(projectid), outputStream);
    } else {
      logger.error("returnSpreadsheet huh? can't handle request " + encodedFileName);
    }
  }

  /**
   * @param response
   * @param fileName
   * @see #setHeader(HttpServletResponse, String)
   */
  private void setResponseHeader(HttpServletResponse response, String fileName) {
    setFilenameHeader(response, fileName);
  }

  private void setFilenameHeader(HttpServletResponse response, String filename) {
    response.setHeader(CONTENT_DISPOSITION, "attachment; filename=" + filename);
  }

  /**
   * @param response
   * @param db
   * @param listid
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */

  private void writeUserList(HttpServletResponse response,
                             DatabaseImpl db,
                             String listid,
                             int projectid,
                             AudioExportOptions options) {
    Integer id = null;
    try {
      id = Integer.parseInt(listid);
    } catch (NumberFormatException e) {
      logger.error("couldn't parse " + listid);
    }

    try {
      String name = id == null ? "unknown" : db.getUserListName(id);
      name = name.replaceAll(",", "_").replaceAll(" ", "_");
      name += ".zip";
      setHeader(response, name);

      options.setUserList(true);
      db.writeUserListAudio(response.getOutputStream(),
          id == null ? -1 : id,
          projectid,
          options);
    } catch (Exception e) {
      logger.error("couldn't write zip?", e);
    }
  }

  private void writeDialogList(HttpServletResponse response,
                               DatabaseImpl db,
                               String dialogID,
                               int projectid,
                               AudioExportOptions options) {
    Integer id = null;
    try {
      id = Integer.parseInt(dialogID);
    } catch (NumberFormatException e) {
      logger.error("couldn't parse " + dialogID);
    }

    try {
      final int fid = id == null ? -1 : id;

      List<IDialog> collect = db.getProject(projectid).getDialogs().stream().filter(d -> d.getID() == fid).collect(Collectors.toList());

      String name = id == null | collect.isEmpty() ? "unknown" : collect.get(0).getEnglish();
      name = name.replaceAll(",", "_").replaceAll(" ", "_");
      name += ".zip";
      setHeader(response, name);

      options.setUserList(true);
      db.writeDialogItems(response.getOutputStream(),
          id == null ? -1 : id,
          projectid,
          options);
    } catch (Exception e) {
      logger.error("couldn't write zip?", e);
    }
  }

  /**
   * @param response
   * @param fileName
   * @see #writeAudioZip
   */
  private void setHeader(HttpServletResponse response, String fileName) {
    setResponseHeader(response, fileName);
    response.setContentType("application/zip");
  }

  /**
   * Parse the query string that indicates the unit and chapter selections.
   * <p>
   * So here we remap an encoded ampersand and an encoded comma back to unencoded versions.
   * <p>
   * Encoded in download helper.
   *
   * @param queryString
   * @return map representing unit/chapter selections - what SectionHelper will parse
   * @see mitll.langtest.client.download.DownloadHelper#getURL
   * @see #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  private Map<String, Collection<String>> getTypeToSelectionFromRequest(String queryString) {
    if (queryString.length() > 2) {
      queryString = queryString.substring(1, queryString.length() - 1);
    }
//      logger.info("getTypeToSelectionFromRequest " + queryString);
    queryString = queryString.replaceAll("%20", " ");    // need this for pashto3 which has "29 LC1" as chapters
    //   logger.debug("got " + queryString);

    String[] sections = queryString.split("],");

    // logger.debug("sections " + sections[0]);

    Map<String, Collection<String>> typeToSection = new HashMap<>();
    for (String section : sections) {
      // logger.debug("\tsection " + section);

      String[] split1 = section.split("=");
      if (split1.length > 1) {
        String key = split1[0];
        String s = split1[1];
        //   logger.debug("\ts " + s);

        if (!s.isEmpty()) {
          s = s.substring(1);
        }
        s = s.replaceAll("]", "");
        //   logger.debug("\ts " + s);

        s = s.replaceAll(AMPERSAND, "&");
        List<String> values = Arrays.asList(s.split(","));
        List<String> trimmed = new ArrayList<>();
        values.forEach(v -> trimmed.add(v.replaceAll(COMMA, ",").trim()));

//        logger.debug("\tkey " + key + "=" + trimmed);
        typeToSection.put(key.trim(), trimmed);
      } else {
        logger.debug("\tgetTypeToSelectionFromRequest sections 1" + split1[0]);
      }
    }

    logger.info("getTypeToSelectionFromRequest returning" +
        "\n\ttype->selection '" + typeToSection + "'" +
        "\n\tfor             '" + queryString + "'");

    return typeToSection;
  }

  @Override
  public void init() throws ServletException {
    super.init();
    setPaths(getServletContext());
  }

  /**
   * @param inputStream
   * @param saveFile
   * @throws IOException
   */
/*  void writeToFile(InputStream inputStream, File saveFile) throws IOException {
    // opens an output stream for writing file
    copyToOutput(inputStream, new FileOutputStream(saveFile));
  }*/

  /**
   * TODO replace with commons call
   *
   * @param inputStream
   * @param outputStream
   * @throws IOException
   */
/*  private void copyToOutput(InputStream inputStream, OutputStream outputStream) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;

    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }

    outputStream.close();
    inputStream.close();
  }*/
}