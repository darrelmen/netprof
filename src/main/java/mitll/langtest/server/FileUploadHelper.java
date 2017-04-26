package mitll.langtest.server;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.exercise.ExcelImport;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.logging.log4j.LogManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 4/26/17.
 */
public class FileUploadHelper {
  private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(FileUploadHelper.class);
  private static final int MAX_FILE_SIZE = 10000000;
  private static final String UPLOAD_FORM_NAME = "upload";
  DatabaseServices db;
  Map<Integer,Collection<CommonExercise>> idToExercises = new HashMap<>();

  public FileUploadHelper(DatabaseServices db) {
    this.db = db;
  }

  public Site gotFile(HttpServletRequest request) {
    //logger.info("Getting site given config dir " + configDir);
    FileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);
    upload.setSizeMax(MAX_FILE_SIZE);

    try {
      List<FileItem> items = upload.parseRequest(request);
      logger.info("getSite : from http request, got " + items.size() + " items.");

      DiskFileItem rememberedFileItem = null;


      Site site = new Site();
      for (FileItem item : items) {
        if (!item.isFormField() && UPLOAD_FORM_NAME.equals(item.getFieldName())) {
          rememberedFileItem = (DiskFileItem) item;
        } else {
          readFormItemAndStoreInSite(site, item);
        }
      }

      if (rememberedFileItem != null) {
        logger.info("got " + rememberedFileItem);

        readExercises(site, rememberedFileItem);
        site.setValid(true);
      } else {
        site.setValid(false);
      }

      return site;
    } catch (Exception e) {
      logger.error("Got " + e, e);
      return new Site();
    }
  }

  private boolean readFormItemAndStoreInSite(Site site, FileItem item) {
    logger.info("from http request, got " + item);
    String name = item.getFieldName();
    if (name != null) {
      if (name.toLowerCase().endsWith("projectid")) {
        logger.info("-------------> got siteid <----------------\n\n");
        site.id = Integer.parseInt(item.getString().trim());
        logger.info("------------->  siteid" +site.id+
            "  <----------------\n\n");
        return true;
        //  logger.info("User " + item.getString());
        // site.creatorID = Long.parseLong(item.getString().trim());
      } else {
        logger.info("Got " + item);
      }
    }
    return false;
  }

  public class Site implements IsSerializable {
    private int id;
    private int num = 0;

    private boolean valid = false;

    public long getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public boolean isValid() {
      return valid;
    }

    public void setValid(boolean valid) {
      this.valid = valid;
    }

    public int getNum() {
      return num;
    }

    public void setNum(int num) {
      this.num = num;
    }
  }

  private void readExercises(Site site, FileItem item) throws IOException {
    logger.info("got upload " + item);
    String fileName = item.getName();
    InputStream inputStream = item.getInputStream();
    readExercisesPopulateSite(site, fileName, inputStream);
  }

  private void readExercisesPopulateSite(Site site, String fileName, InputStream inputStream) {
    List<CommonExercise> exercises;
 //   ExerciseDAO importer;
    if (fileName.endsWith(".json")) {
//      FileExerciseDAO fileImporter = new FileExerciseDAO("", "", false, "", "");  //TODO fully support this
//      exercises = fileImporter.readExercises(inputStream);
//      importer = fileImporter;
    } else {
      ExcelImport excelImport = new ExcelImport(fileName, db.getServerProps(), db.getUserListManager(), false);
      exercises = excelImport.readExercises(inputStream);
     // importer = excelImport;
      String s = "Read " + exercises.size();
      logger.info("got " + s);
      if (exercises.isEmpty()) {

      }
      else {
        idToExercises.put(site.id,exercises);
        site.setNum(exercises.size());
      }
    }
  }

  /**
   * @paramx db
   * @param response
   * @param site
   * @throws IOException
   * @paramx siteDeployer
   * @see LangTestDatabaseImpl#service
   */
  public void doSiteResponse( HttpServletResponse response, Site site) throws IOException {
    response.setContentType("text/plain");
    if (!site.isValid()) {
      response.getWriter().write("Name in use or invalid.");
    } else {
      response.getWriter().write("Read " + site.getNum() + " items.  Press OK to add them to project.");
    }
  }
}
