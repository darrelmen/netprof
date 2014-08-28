package mitll.langtest.server;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.CommonExercise;
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
 * User: GO22670
 * Date: 12/17/13
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("serial")
public class DownloadServlet extends DatabaseServlet {
  private static final Logger logger = Logger.getLogger(DownloadServlet.class);

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
      String encodedFileName = request.getRequestURI();
      if (encodedFileName.toLowerCase().contains("audio")) {
        String pathInfo = request.getPathInfo();
        String queryString = request.getQueryString();
        logger.debug("DownloadServlet.doGet : Request " + queryString + " path " + pathInfo +
            " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());

        if (queryString == null) {
          setHeader(response, "allAudio.zip");
          writeAllAudio(response);
        } else if (queryString.startsWith("list")) {
          String[] split = queryString.split("list=");
          if (split.length == 2) {
            String listid = split[1];
            if (!listid.isEmpty()) {
              writeUserList(response, db, listid);
            }
          }
        } else if (queryString.startsWith("file")) {
          returnAudioFile(response, db, queryString);
        } else {
          Map<String, Collection<String>> typeToSection = getTypeToSelectionFromRequest(queryString);
          String name = typeToSection.isEmpty() ? "audio" : db.getPrefix(typeToSection);
          name = name.replaceAll("\\,", "_");
          name += ".zip";
          String fileName = db.getServerProps().getLanguage() + "_" + name;

          setHeader(response, fileName);
          try {
            db.writeZip(response.getOutputStream(), typeToSection);
          } catch (Exception e) {
            logger.error("couldn't write zip?", e);
          }
        }
      } else {
        returnSpreadsheet(response, db, encodedFileName);
      }
    }

    try {
      response.getOutputStream().close();
    } catch (IOException e) {
      logger.warn("got " + e, e);
    }
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
    String exercise = split[1].split("=")[1];
    String useridString = split[2].split("=")[1];

    String underscores = getFilenameForDownload(db, exercise, useridString);

    logger.debug("returnAudioFile query is " + queryString + " file " + file + " ex " + exercise + " user " + useridString + " so name is " + underscores);

    response.setContentType("application/octet-stream");
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
    fileName += ".mp3";

    //logger.debug("file is '" + fileName + "'");
    String underscores = fileName.replaceAll("\\p{Z}+", "_");  // split on spaces
    underscores = URLEncoder.encode(underscores, "UTF-8");
    return underscores;
  }

  private String getUserPart(DatabaseImpl db, long userid) {
    User userWhere = db.getUserDAO().getUserWhere(userid);
    return userWhere != null ? (userWhere.getUserID().isEmpty() ? "":"_by_" + userWhere.getUserID()) : "";
  }

  private void returnSpreadsheet(HttpServletResponse response, DatabaseImpl db, String encodedFileName) throws IOException {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    if (encodedFileName.toLowerCase().contains("users")) {
      response.setHeader("Content-Disposition", "attachment; filename=users");
      db.usersToXLSX(response.getOutputStream());
    } else if (encodedFileName.toLowerCase().contains("results")) {
      response.setHeader("Content-Disposition", "attachment; filename=results");
      db.getResultDAO().writeExcelToStream(db.getResultsWithGrades(), response.getOutputStream());
    } else if (encodedFileName.toLowerCase().contains("events")) {
      response.setHeader("Content-Disposition", "attachment; filename=events");
      db.getEventDAO().toXLSX(response.getOutputStream());
    } else {
      logger.warn("huh? can't handle request " + encodedFileName);
    }
  }

  private void writeAllAudio(HttpServletResponse response) {
    DatabaseImpl db = getDatabase();

    try {
      db.writeZip(response.getOutputStream());
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }

  }

  private void writeUserList(HttpServletResponse response, DatabaseImpl db, String listid) {
    Integer id = Integer.parseInt(listid);

    try {
      String name = db.getUserListName(id);
      name = name.replaceAll("\\,", "_").replaceAll(" ", "_");
      name += ".zip";
      setHeader(response, name);

      db.writeZip(response.getOutputStream(), id);
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

    Object databaseReference = getServletContext().getAttribute("databaseReference");
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

    Map<String, Collection<String>> typeToSection = new HashMap<String, Collection<String>>();
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
        List<String> trimmed = new ArrayList<String>();
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