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

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.User;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
  private static final Logger logger = Logger.getLogger(DownloadServlet.class);
  private static final String AUDIO = "audio";
  private static final String LIST = "list";
  private static final String FILE = "file";
  private static final String CONTEXT = "context";
  private static final String COMPRESSED_SUFFIX = "mp3";

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
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    DatabaseImpl db = getDatabase();

    if (db != null) {
      try {
        String requestURI = request.getRequestURI();
        if (requestURI.toLowerCase().contains(AUDIO)) {
          String pathInfo = request.getPathInfo();
          String queryString = request.getQueryString();
          logger.debug("DownloadServlet.doGet : Request " + queryString + " path " + pathInfo +
              " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());

          if (queryString == null) {
            setHeader(response, "allAudio.zip");
            writeAllAudio(response);
          } else if (queryString.startsWith(LIST)) {
            String[] split = queryString.split("list=");
            if (split.length == 2) {
              String listid = split[1];
              if (!listid.isEmpty()) {
                writeUserList(response, db, listid);
              }
            }
          } else if (queryString.startsWith(FILE)) {
            returnAudioFile(response, db, queryString);
          } else if (queryString.startsWith("request")) {
            String[] split1 = queryString.split("&");
            String requestCommand = split1[0];

            queryString = split1.length == 1 ? split1[0] : split1[1];

            logger.debug("request " + requestCommand + " query " +queryString);
            if (requestCommand.contains(CONTEXT)) {
              Map<String, Collection<String>> typeToSection = getTypeToSelectionFromRequest(queryString);
              setHeader(response, getZipFileName(db, typeToSection));
              writeContextZip(response, typeToSection);
            } else {
              Map<String, Collection<String>> typeToSection = getTypeToSelectionFromRequest(queryString);
              setHeader(response, getZipFileName(db, typeToSection));
              writeZip(response, typeToSection);
            }
          }
        } else {
          logger.warn("file download request " + requestURI);

          returnSpreadsheet(response, db, requestURI);
        }
      } catch (Exception e) {
        logger.error("Got " + e, e);
        db.logAndNotify(e);
        throw new IOException("couldn't process request.", e);
      }
    }

    try {
      response.getOutputStream().close();
    } catch (IOException e) {
      logger.warn("got " + e, e);
    }
  }

  /**
   * @see #doGet
   * @param response
   * @param typeToSection
   */
  private void writeZip(HttpServletResponse response, Map<String, Collection<String>> typeToSection) {
    try {
      getDatabase().writeZip(response.getOutputStream(), typeToSection);
    } catch (Exception e) {
      logger.error("couldn't write zip?", e);
    }
  }

  private void writeContextZip(HttpServletResponse response, Map<String, Collection<String>> typeToSection) {
    try {
      getDatabase().writeContextZip(response.getOutputStream(), typeToSection);
    } catch (Exception e) {
      logger.error("couldn't write zip?", e);
    }
  }

  private String getZipFileName(DatabaseImpl db, Map<String, Collection<String>> typeToSection) {
    String name = typeToSection.isEmpty() ? AUDIO : db.getPrefix(typeToSection);
    name = name.replaceAll("\\,", "_");
    name += ".zip";
    return db.getServerProps().getLanguage() + "_" + name;
  }

  /**
   * Option to download an mp3 you've just recorded.
   * <p/>
   * Hack to remove N/A english fields
   *
   * @param response
   * @param db
   * @param queryString
   * @throws IOException
   * @see #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  private void returnAudioFile(HttpServletResponse response, DatabaseImpl db, String queryString) throws IOException {
    String[] split = queryString.split("&");

    String file = split[0].split("=")[1];

    // better be mp3 lying around - see ensureCompressedAudio

    if (file.endsWith(".wav")) file = file.replaceAll(".wav",".mp3");

    String exercise = split[1].split("=")[1];
    String useridString = split[2].split("=")[1];

    String underscores = getFilenameForDownload(db, exercise, useridString);

    logger.debug("returnAudioFile query is " + queryString + " file " + file + " exercise " + exercise + " user " + useridString + " so name is " + underscores);

    response.setContentType("audio/mpeg");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + underscores);

    File fileRef = pathHelper.getAbsoluteFile(file);
    if (!fileRef.exists()) {
      logger.warn("huh? can't find " + file);
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
   * Return an attachment that looks like foreign_english_by_user.
   * 
   * @param db to talk to
   * @param exercise id of the exercise
   * @param useridString missing is OK, although unexpected
   * @return name without spaces
   * @throws UnsupportedEncodingException
   */
  private String getFilenameForDownload(DatabaseImpl db, String exercise, String useridString) throws UnsupportedEncodingException {
    CommonExercise exercise1 = db.getExercise(exercise);
    boolean english = db.getServerProps().getLanguage().equalsIgnoreCase("english");

    // foreign part
    String foreignPart = english ? "" : exercise1.getForeignLanguage().trim();

    // english part
    String englishPart = exercise1.getEnglish().trim();
    if (englishPart.equals("N/A")) englishPart = "";
    if (!englishPart.isEmpty()) englishPart = "_" + englishPart;

    // user part
    String userPart = getUserPart(db, Long.parseLong(useridString));

    String fileName = foreignPart + englishPart + userPart;
    fileName = fileName.replaceAll("\\.","");
    fileName += "." + COMPRESSED_SUFFIX;

    //logger.debug("file is '" + fileName + "'");
    String underscores = fileName.replaceAll("\\p{Z}+", "_");  // split on spaces
    underscores = URLEncoder.encode(underscores, "UTF-8");
    return underscores;
  }

  private String getUserPart(DatabaseImpl db, long userid) {
    User userWhere = db.getUserDAO().getUserWhere(userid);
    return userWhere != null ? (userWhere.getUserID().isEmpty() ? "":"_by_" + userWhere.getUserID()) : "";
  }

  /**
   * @see #doGet
   * @param response
   * @param db
   * @param encodedFileName
   * @throws IOException
   */
  private void returnSpreadsheet(HttpServletResponse response, DatabaseImpl db, String encodedFileName) throws IOException {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    if (encodedFileName.toLowerCase().contains("users")) {
      response.setHeader("Content-Disposition", "attachment; filename=users.xlsx");
      db.usersToXLSX(response.getOutputStream());
    } else if (encodedFileName.toLowerCase().contains("results")) {
      response.setHeader("Content-Disposition", "attachment; filename=results.xlsx");
      db.getResultDAO().writeExcelToStream(db.getMonitorResults(), db.getTypeOrder(), response.getOutputStream());
    } else if (encodedFileName.toLowerCase().contains("events")) {
      response.setHeader("Content-Disposition", "attachment; filename=events.xlsx");
      db.getEventDAO().toXLSX(response.getOutputStream());
    } else {
      logger.warn("huh? can't handle request " + encodedFileName);
    }
  }

  /**
   * @see #doGet
   * @param response
   */
  private void writeAllAudio(HttpServletResponse response) {
    try {
      getDatabase().writeZip(response.getOutputStream());
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }
  }

  /**
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   * @param response
   * @param db
   * @param listid
   */
  private void writeUserList(HttpServletResponse response, DatabaseImpl db, String listid) {
    Integer id = null;
    try {
      id = Integer.parseInt(listid);
    } catch (NumberFormatException e) {
      logger.error("couldn't parse " + listid);
    }

    try {
      String name = id == null ? "unknown" : db.getUserListName(id);
      name = name.replaceAll("\\,", "_").replaceAll(" ", "_");
      name += ".zip";
      setHeader(response, name);

      db.writeZip(response.getOutputStream(), id == null ? -1 : id, new PathHelper(getServletContext()));
    } catch (Exception e) {
      logger.error("couldn't write zip?", e);
    }
  }

  private void setHeader(HttpServletResponse response, String fileName) {
    response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
    response.setContentType("application/zip");
  }

  private DatabaseImpl getDatabase() {
    DatabaseImpl db = null;

    Object databaseReference = getServletContext().getAttribute(LangTestDatabaseImpl.DATABASE_REFERENCE);
    if (databaseReference != null) {
      db = (DatabaseImpl) databaseReference;
      // logger.debug("found existing database reference " + db + " under " +getServletContext());
    } else {
      logger.error("huh? no existing db reference?");
    }
    return db;
  }

  /**
   * Parse the query string that indicates the unit and chapter selections.
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

        List<String> values = Arrays.asList(s.split(","));
        List<String> trimmed = new ArrayList<>();
        for (String v : values) {
          trimmed.add(v.trim());
        }
//        logger.debug("\tkey " + key + "=" + trimmed);
        typeToSection.put(key.trim(), trimmed);
      } else {
        logger.debug("\tsections 1" + split1[0]);
      }
    }
    logger.debug("returning " + typeToSection + " for " + queryString);
    return typeToSection;
  }

  @Override
  public void init() throws ServletException {
    super.init();
    setPaths();
  }
}