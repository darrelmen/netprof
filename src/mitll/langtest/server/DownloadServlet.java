package mitll.langtest.server;

import mitll.langtest.server.database.DatabaseImpl;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deals with downloads from site -- for excel spreadsheets and zips of audio.
 *
 * Can download a user list too.
 *
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
        }
        else if (queryString.startsWith("list")) {
          String[] split = queryString.split("list=");
          if (split.length == 2) {
            String listid = split[1];
            if (!listid.isEmpty()) {
              writeUserList(response, db, listid);
            }
          }
        }
        else {
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
      logger.warn("got " +e,e);
    }
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
      logger.error("Got " +e,e);
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
      logger.debug("found existing database reference " + db + " under " +getServletContext());
    } else {
      logger.error("huh? no existing db reference?");
    }
    return db;
  }

  /**
   * Parse the query string that indicates the unit and chapter selections.
   * @see #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   * @param queryString
   * @return map representing unit/chapter selections - what SectionHelper will parse
   */
  private Map<String, Collection<String>> getTypeToSelectionFromRequest(String queryString) {
    if (queryString.length() > 2) {
      queryString = queryString.substring(1, queryString.length() - 1);
    }
   //  logger.debug("got " + queryString);
    queryString = queryString.replaceAll("%20"," ");    // need this for pashto3 which has "29 LC1" as chapters
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
       s = s.replaceAll("]","");

    //   logger.debug("\ts " + s);

        List<String> values = Arrays.asList(s.split(","));
        List<String> trimmed = new ArrayList<String>();
        for (String v : values) {
          trimmed.add(v.trim());
        }
//        logger.debug("\tkey " + key + "=" + trimmed);
        typeToSection.put(key.trim(), trimmed);
      }
      else {
        logger.debug("\tsections 1" + split1[0]);
      }
    }
    logger.debug("returning " + typeToSection + " for " + queryString);
    return typeToSection;
  }
}