package mitll.langtest.server;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.exercise.DominoExerciseDAO;
import mitll.langtest.server.database.exercise.ImportInfo;
import mitll.langtest.server.database.exercise.Project;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 4/26/17.
 */
public class FileUploadHelper {
  private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(FileUploadHelper.class);
  private static final int MAX_FILE_SIZE = 52428800;
  private static final String UPLOAD_FORM_NAME = "upload";
  private final DatabaseServices db;
  private final Map<Integer, ImportInfo> idToExercises = new HashMap<>();

  private final DominoExerciseDAO dominoExerciseDAO;

  public FileUploadHelper(DatabaseServices db, DominoExerciseDAO dominoExerciseDAO) {
    this.db = db;
    this.dominoExerciseDAO = dominoExerciseDAO;
  }

  /**
   * @see LangTestDatabaseImpl#service
   * @param request
   * @return
   */
  UploadInfo gotFile(HttpServletRequest request) {
    FileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);
    upload.setSizeMax(MAX_FILE_SIZE);

    try {
      List<FileItem> items = upload.parseRequest(request);
      logger.info("gotFile : from http request, got " + items.size() + " items.");

      DiskFileItem rememberedFileItem = null;

      UploadInfo UploadInfo = new UploadInfo();
      for (FileItem item : items) {
        if (!item.isFormField() && UPLOAD_FORM_NAME.equals(item.getFieldName())) {
          rememberedFileItem = (DiskFileItem) item;
        } else {
          readFormItemAndStoreInUploadInfo(UploadInfo, item);
        }
      }

      if (rememberedFileItem != null) {
        logger.info("gotFile " + rememberedFileItem);
        readExercises(UploadInfo, rememberedFileItem);
      }

      UploadInfo.setValid(rememberedFileItem != null);

      return UploadInfo;
    } catch (Exception e) {
      logger.error("Got " + e, e);
      return new UploadInfo();
    }
  }

  private void readFormItemAndStoreInUploadInfo(UploadInfo UploadInfo, FileItem item) {
    //logger.info("from http request, got " + item);
    String name = item.getFieldName();
    if (name != null) {
      if (name.toLowerCase().endsWith("projectid")) {
//        logger.info("-------------> got UploadInfoid <----------------\n\n");
        UploadInfo.id = Integer.parseInt(item.getString().trim());
      //  logger.info("------------->  UploadInfoid " + UploadInfo.id + "  <----------------");
       // return true;
      } else {
        logger.info("Got " + item);
      }
    }
//    return false;
  }

  public static class UploadInfo implements IsSerializable {
    private int id;
    private int num = 0;
    private boolean valid = false;

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

    public int getId() {
      return id;
    }
  }

  /**
   * @see #gotFile
   * @param UploadInfo
   * @param item
   * @throws IOException
   */
  private void readExercises(UploadInfo UploadInfo, FileItem item) throws IOException {
    logger.info("readExercises upload " + item);
    readExercisesPopulateUploadInfo(UploadInfo, item.getName(), item.getInputStream());
  }

  /**
   * NO : Technically we can still load excel, but it's turned off in the UI.
   * @param UploadInfo
   * @param fileName
   * @param inputStream
   */
  private void readExercisesPopulateUploadInfo(UploadInfo UploadInfo, String fileName, InputStream inputStream) {
    if (fileName.endsWith(".json")) {
      readJSON(UploadInfo, inputStream);
    }
    else {
      logger.warn("somehow got a non-json file " + fileName);
    }
    //else {
    //  readExcel(UploadInfo, fileName, inputStream);
   // }
  }

/*  private void readExcel(UploadInfo UploadInfo, String fileName, InputStream inputStream) {
    int id = UploadInfo.id;

    List<String> types = getTypes(db.getProject(id));
    ExcelImport excelImport = new ExcelImport(fileName, db.getServerProps(), db.getUserListManager(), false) {
      @Override
      public List<String> getTypeOrder() {
        return types;
      }
    };

    rememberExercises(UploadInfo, id, excelImport.readExercises(inputStream));
  }*/

  private void readJSON(UploadInfo UploadInfo, InputStream inputStream) {
    int id = UploadInfo.id;
    Project project = db.getProject(id);
    ImportInfo info =
        dominoExerciseDAO.readExercises(null, inputStream, project.getID(), db.getUserDAO().getImportUser());
//    logger.info("Got " +info);
    rememberExercises(UploadInfo, id, info);
  }

/*  @NotNull
  private List<String> getTypes(Project project) {
    List<String> types = new ArrayList<>();
    String first = project.getProject().first();
    String second = project.getProject().second();
    if (!first.isEmpty()) types.add(first);
    if (!second.isEmpty()) types.add(second);
    return types;
  }*/

  /**
   * @param response
   * @param UploadInfo
   * @throws IOException
   * @see LangTestDatabaseImpl#service
   */
   void doUploadInfoResponse(HttpServletResponse response, UploadInfo UploadInfo) throws IOException {
    response.setContentType("text/plain");
    if (!UploadInfo.isValid()) {
      response.getWriter().write("Name in use or invalid.");
    } else {
      response.getWriter().write("Read " + UploadInfo.getNum() + " items.  Press OK to add them to project.");
    }
  }

  /**
   * @see #readJSON(UploadInfo, InputStream)
   * @param UploadInfo
   * @param id
   * @param info
   */
  private void rememberExercises(UploadInfo UploadInfo, int id, ImportInfo info) {
    List<CommonExercise> exercises = info.getExercises();
    logger.info("rememberExercises Read " + exercises.size());
//    exercises.forEach(exercise->logger.info("ex  "+exercise.getID() + " " + exercise.getDirectlyRelated()));
    if (exercises.isEmpty()) {
      logger.warn("rememberExercises Read zero? " + exercises.size());
    } else {
      idToExercises.put(id, info);
      UploadInfo.setNum(exercises.size());
      logger.info("rememberExercises UploadInfo " + id + " : " + idToExercises.get(id).getExercises().size());
    }
  }

  /**
   * @see mitll.langtest.server.services.ProjectServiceImpl#addPending
   * @param projid
   * @return
   */
  public ImportInfo getExercises(int projid) {
    return idToExercises.get(projid);
  }
}