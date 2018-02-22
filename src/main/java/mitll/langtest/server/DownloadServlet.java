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

import com.google.gwt.http.client.URL;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.server.audio.AudioExportOptions;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.excel.EventDAOToExcel;
import mitll.langtest.server.database.excel.ResultDAOToExcel;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.user.User;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

/**
 * Deals with downloads from site -- for excel spreadsheets and zips of audio.
 * <p/>
 * Can download a user list too.
 * <p/>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/17/13
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("serial")
public class DownloadServlet extends DatabaseServlet {
  private static final Logger logger = LogManager.getLogger(DownloadServlet.class);
  private static final String AUDIO = "audio";
  private static final String LIST = "list";
  private static final String FILE = "file";

  private static final String COMPRESSED_SUFFIX = "mp3";
  // private static final String USERS = "users";
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
//  private static final String ALLCONTEXT = "allcontext";
  private static final String LISTS = "Lists=[";

  private static final String REGEXAMPERSAND = "&";

  private static final String AMPERSAND = DownloadHelper.AMPERSAND;//"___AMPERSAND___";
  private static final String COMMA = DownloadHelper.COMMA;//"___COMMA___";
  private static final String RESULTS_XLSX = "results.xlsx";
  private static final String EVENTS_XLSX = "events.xlsx";

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
        int projid = getProjectID(request);

        if (projid == -1) {
          projid = getProjectIDFromUser(request);
        }
        if (projid == -1) {
          logger.warn("doGet no current project for request ");
          response.setContentType("text/html");
          response.getOutputStream().write("no project for request".getBytes());
          closeOutputStream(response);
          return;
        }
        //       logger.info("doGet : current session found projid " + project1);
        Project project = getDatabase().getProject(projid);
        boolean hasProjectSpecificAudio = project.hasProjectSpecificAudio();
        String language = project.getLanguage();

        String requestURI = request.getRequestURI();
        if (requestURI.toLowerCase().contains(AUDIO)) {
          String queryString = request.getQueryString();

          String decode = URLDecoder.decode(queryString, "UTF-8");

          logger.debug("doGet :" +
              "\n\tRequest " + queryString +
              "\n\tRequest decode " + decode +
              "\n\tpath    " + request.getPathInfo() +
              "\n\turi     " + requestURI +
              "\n\turl     " + request.getRequestURL() +
              "\n\tpath    " + request.getServletPath());

          queryString = decode;

          if (queryString.startsWith(LIST) || queryString.contains(LISTS)) {
            if (queryString.contains(LISTS)) {
              String s = queryString.split("Lists=\\[")[1];
              String[] split = s.split(("\\]"));
              String listid = split[0];
              if (!listid.isEmpty()) {
                String[] splitArgs = split[1].split(REGEXAMPERSAND);
                writeUserList(response, db, listid, projid, getAudioExportOptions(splitArgs, hasProjectSpecificAudio));
              }
            } else {
              String[] split = queryString.split("list=");

              if (split.length == 2) {
                String[] splitArgs = split[1].split(REGEXAMPERSAND);
                String listid = splitArgs[0];
                if (!listid.isEmpty()) {
                  writeUserList(response, db, listid, projid, getAudioExportOptions(splitArgs, hasProjectSpecificAudio));
                }
              }
            }
          } else if (queryString.startsWith(FILE)) {
            returnAudioFile(response, db, queryString, language, projid);
          } else if (queryString.startsWith(REQUEST)) {
            writeAudioZip(response, db, queryString, projid, language);
          }
        } else {
//          logger.debug("file download request " + requestURI);
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
   * @param hasProjectSpecificAudio
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private AudioExportOptions getAudioExportOptions(String[] splitArgs, boolean hasProjectSpecificAudio) {
    AudioExportOptions options = new AudioExportOptions(hasProjectSpecificAudio);
    for (String arg : splitArgs) {
      logger.info("getAudioExportOptions arg " + arg);

      if (arg.startsWith(MALE)) options.setJustMale(isTrue(arg));
      else if (arg.startsWith(REGULAR)) options.setJustRegularSpeed(isTrue(arg));
      else if (arg.startsWith(CONTEXT)) options.setJustContext(isTrue(arg));
      //else if (arg.startsWith(ALLCONTEXT)) options.setAllContext(isTrue(arg));
      else {
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
                             int projid,
                             String language) {
    logger.debug("writeAudioZip : request " + projid + " " + language+ " query " + queryString);
    String[] split1 = queryString.split(REGEXAMPERSAND);

    // TODO : super buggy - what if we don't have a unit/chapter hierarchy???
    String unitChapter = "";
    for (String arg : split1) {
      if (arg.startsWith("unit=")) unitChapter = arg.split("unit=")[1];
    }

    Map<String, Collection<String>> typeToSection = getTypeToSelectionFromRequest(unitChapter);
    AudioExportOptions audioExportOptions =
        getAudioExportOptions(split1, db.getProject(projid).hasProjectSpecificAudio());

    audioExportOptions.setSkip(typeToSection.isEmpty());// && !audioExportOptions.isAllContext());

    {
      String zipFileName = getZipFileName(db, typeToSection, projid, language, audioExportOptions);
      //logger.info("writeAudioZip zip file name " + zipFileName);
      setHeader(response, zipFileName);
    }

    writeZip(response, typeToSection, projid, audioExportOptions);
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


  private String getZipFileName(DatabaseImpl db,
                                Map<String, Collection<String>> typeToSection,
                                int projectid, String language,

                                AudioExportOptions audioExportOptions) {
    String name = getBaseName(db, typeToSection, projectid, language, audioExportOptions);
    name += ".zip";
    return name;
  }

  private String getBaseName(DatabaseImpl db,
                             Map<String, Collection<String>> typeToSection,
                             int projectid,
                             String language,
                             AudioExportOptions audioExportOptions) {
    String name = typeToSection.isEmpty() ? AUDIO : db.getPrefix(typeToSection, projectid);
    name = name.replaceAll(",", "_");

    name += audioExportOptions.getInfo();
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

    if (file.endsWith(".wav")) file = file.replaceAll(".wav", ".mp3");

    String exercise = split[1].split("=")[1];
    String useridString = split[2].split("=")[1];

    logger.debug("returnAudioFile download exercise #" + exercise + " for user id=" + useridString + " file " + file);

    String underscores = getFilenameForDownload(db, projid, Integer.parseInt(exercise), useridString, language);

    logger.debug("returnAudioFile query is " + queryString + " exercise " + exercise +
        " user " + useridString + " so name is " + underscores);

    setResponse(response, underscores);

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

  private void setResponse(HttpServletResponse response, String underscores) {
    response.setContentType("audio/mpeg");
    // response.setCharacterEncoding(UTF_8);
    response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + underscores);
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
      logger.error("getFilenameForDownload couldn't find exercise #" + exercise + " for user '" + useridString + "' and language " + language + " for project #"+projid);
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
    String prefix = language + "_" + projectName.replaceAll("\\s++","_") + "_";
/*    if (encodedFileName.toLowerCase().contains(USERS)) {
      String filename = prefix + "users.xlsx";
      response.setHeader("Content-Disposition", "attachment; filename=" + filename);
      db.usersToXLSX(response.getOutputStream());
    } else
      */
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

  private void setResponseHeader(HttpServletResponse response, String fileName) {
    setFilenameHeader(response, fileName);
  }

  private void setFilenameHeader(HttpServletResponse response, String filename) {
    response.setHeader("Content-Disposition", "attachment; filename=" + filename);
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
   *
   * So here we remap an encoded ampersand and an encoded comma back to unencoded versions.
   *
   * Encoded in download helper.
   *
   * @see mitll.langtest.client.download.DownloadHelper#getURL
   *
   * @param queryString
   * @return map representing unit/chapter selections - what SectionHelper will parse
   * @see #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  private Map<String, Collection<String>> getTypeToSelectionFromRequest(String queryString) {
    if (queryString.length() > 2) {
      queryString = queryString.substring(1, queryString.length() - 1);
    }
    //  logger.debug("got " + queryString);
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

        s = s
            .replaceAll(AMPERSAND, "&")
        ;
        List<String> values = Arrays.asList(s.split(","));
        List<String> trimmed = new ArrayList<>();
        values.forEach(v -> trimmed.add(v.replaceAll(COMMA, ",").trim()));

//        logger.debug("\tkey " + key + "=" + trimmed);
        typeToSection.put(key.trim(), trimmed);
      } else {
        logger.debug("\tgetTypeToSelectionFromRequest sections 1" + split1[0]);
      }
    }
    logger.debug("getTypeToSelectionFromRequest returning type->selection '" + typeToSection + "' for '" + queryString + "'");
    return typeToSection;
  }

  @Override
  public void init() throws ServletException {
    super.init();
    setPaths();
  }
}