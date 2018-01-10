package mitll.langtest.server.domino;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import mitll.hlt.domino.server.data.DocumentServiceDelegate;
import mitll.hlt.domino.server.data.IProjectWorkflowDAO;
import mitll.hlt.domino.server.data.ProjectServiceDelegate;
import mitll.hlt.domino.server.util.Mongo;
import mitll.hlt.domino.shared.common.FilterDetail;
import mitll.hlt.domino.shared.common.FindOptions;
import mitll.hlt.domino.shared.model.HeadDocumentRevision;
import mitll.hlt.domino.shared.model.document.DocumentColumn;
import mitll.hlt.domino.shared.model.document.VocabularyItem;
import mitll.hlt.domino.shared.model.project.ClientPMProject;
import mitll.hlt.domino.shared.model.project.ProjectColumn;
import mitll.hlt.domino.shared.model.project.ProjectDescriptor;
import mitll.hlt.domino.shared.model.project.ProjectWorkflow;
import mitll.hlt.domino.shared.model.user.DBUser;
import mitll.langtest.server.database.exercise.DominoExerciseDAO;
import mitll.langtest.server.database.project.ProjectManagement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.include;
import static mitll.langtest.server.domino.ProjectSync.MONGO_TIME;

public class DominoImport implements IDominoImport {
  private static final Logger logger = LogManager.getLogger(DominoImport.class);
  private static final String VOCABULARY = "Vocabulary";
  private static final String V_UNIT = "v-unit";
  private static final String V_CHAPTER = "v-chapter";

  private final ProjectServiceDelegate projectDelegate;
  private final DocumentServiceDelegate documentDelegate;
  private final IProjectWorkflowDAO workflowDelegate;
  private final Mongo pool;

  /**
   * @param projectDelegate
   * @param workflowDelegate
   * @param documentDelegate
   */
  public DominoImport(ProjectServiceDelegate projectDelegate, IProjectWorkflowDAO workflowDelegate,
                      DocumentServiceDelegate documentDelegate,
                      Mongo pool) {
    this.projectDelegate = projectDelegate;
    this.workflowDelegate = workflowDelegate;
    this.documentDelegate = documentDelegate;
    this.pool = pool;
  }

  /**
   * @param projID
   * @param dominoID
   * @param sinceInUTC
   * @param dominoAdminUser
   * @return
   * @see ProjectManagement#getImportFromDomino
   */
  @Override
  public ImportInfo getImportFromDomino(int projID, int dominoID, String sinceInUTC, DBUser dominoAdminUser) {
    List<ImportProjectInfo> matches = getImportProjectInfosByID(dominoID, dominoAdminUser);

    if (matches.isEmpty()) {
      return null;
    } else {
      ClientPMProject next = getClientPMProject(dominoID, dominoAdminUser);

      return new DominoExerciseDAO()
          .readExercises(projID, matches.iterator().next(), getChangedDocs(sinceInUTC, dominoAdminUser, next)
          );
    }
  }

  /**
   * @return
   * @see ProjectManagement#getVocabProjects
   */
  @Override
  @NotNull
  public List<ImportProjectInfo> getImportProjectInfos(DBUser dominoAdminUser) {
    FindOptions<ProjectColumn> options = new FindOptions<>();
    options.addFilter(new FilterDetail<>(ProjectColumn.Skill, VOCABULARY, FilterDetail.Operator.EQ));
    return getImportProjectInfos(options, dominoAdminUser);
  }

  /**
   * @param id
   * @param dominoAdminUser
   * @return
   * @see #getImportFromDomino(int, int, String, DBUser)
   */
  @NotNull
  private List<ImportProjectInfo> getImportProjectInfosByID(int id, DBUser dominoAdminUser) {
    FindOptions<ProjectColumn> options = new FindOptions<>();
    options.addFilter(new FilterDetail<>(ProjectColumn.Id, "" + id, FilterDetail.Operator.EQ));
    return getImportProjectInfos(options, dominoAdminUser);
  }

  @NotNull
  private List<ImportProjectInfo> getImportProjectInfos(FindOptions<ProjectColumn> options, DBUser dominoAdminUser) {
    List<ImportProjectInfo> imported = new ArrayList<>();
    getProjectDescriptors(options, dominoAdminUser)
        .forEach(projectDescriptor -> imported.add(getImportProjectInfo(projectDescriptor)));
    return imported;
  }

  @NotNull
  private ImportProjectInfo getImportProjectInfo(ProjectDescriptor project) {
    int id = project.getId();
    ImportProjectInfo importProjectInfo = new ImportProjectInfo(
        id,
        project.getCreator().getDocumentDBID(),
        project.getName(),
        project.getContent().getLanguageName(),
        project.getCreateTime().getTime()
    );

    setUnitAndChapter(id, importProjectInfo);
    return importProjectInfo;
  }

  /**
   * From workflow.
   *
   * @param id
   * @param importProjectInfo
   */
  private void setUnitAndChapter(int id, ImportProjectInfo importProjectInfo) {
    ProjectWorkflow forProject = workflowDelegate.getForProject(id);

    if (forProject == null) {
      logger.warn("no workflow for project " + id);
    } else {
      forProject
          .getTaskSpecs()
          .forEach(taskSpecification -> {
            taskSpecification
                .getMetadataLists()
                .forEach(metadataList -> {
                  metadataList
                      .getList()
                      .forEach(metadataSpecification -> {
                        String dbName = metadataSpecification.getDBName();
                        String longName = metadataSpecification.getLongName();

                        if (dbName.equalsIgnoreCase(V_UNIT)) {
                          importProjectInfo.setUnitName(longName);
                        } else if (dbName.equalsIgnoreCase(V_CHAPTER)) {
                          importProjectInfo.setChapterName(longName);
                        }
                      });
                });
          });
    }
  }

  private List<ProjectDescriptor> getProjectDescriptors(FindOptions<ProjectColumn> options, DBUser dominoAdminUser) {
    return projectDelegate.getProjects(dominoAdminUser,
        null,
        options,
        false, false, false
    );
  }

  private ClientPMProject getClientPMProject(int dominoID, DBUser dominoAdminUser) {
    FindOptions<ProjectColumn> options = new FindOptions<>();
    options.addFilter(new FilterDetail<>(ProjectColumn.Id, "" + dominoID, FilterDetail.Operator.EQ));
    List<ClientPMProject> projectDescriptor = projectDelegate.getHeavyProjects(dominoAdminUser, options);

    return projectDescriptor.iterator().next();
  }

  /**
   * @param sinceInUTC
   * @param dominoAdminUser
   * @param next
   * @return
   * @see #getImportFromDomino
   */
  @NotNull
  private ChangedAndDeleted getChangedDocs(String sinceInUTC, DBUser dominoAdminUser, ClientPMProject next) {
    long then = System.currentTimeMillis();

    FindOptions<DocumentColumn> options1 = getSince(sinceInUTC);

    List<HeadDocumentRevision> documents1 = documentDelegate.getHeavyDocuments(next, dominoAdminUser, false, false, options1);

    List<ImportDoc> docs = new ArrayList<>(documents1.size());
    List<ImportDoc> deleted = new ArrayList<>();

    documents1.forEach(doc -> {
      VocabularyItem vocabularyItem = (VocabularyItem) doc.getDocument();
      docs.add(new ImportDoc(doc.getId(), doc.getUpdateTime().getTime(), vocabularyItem));
      logger.info("\t getChangedDocs : found changed " + vocabularyItem);
    });

    long now = System.currentTimeMillis();

    logger.info("getChangedDocs : took " + (now - then) + " to get " + docs.size());

    return new ChangedAndDeleted(docs, deleted, getDeletedDocsSince(sinceInUTC, next.getId()));
  }

  public class ChangedAndDeleted {
    private final List<ImportDoc> changed;
    private final List<ImportDoc> deleted;
    private Collection<Integer> deleted2;

    /**
     * @param changed
     * @param deleted
     * @param deleted2
     */
    ChangedAndDeleted(List<ImportDoc> changed, List<ImportDoc> deleted,
                      Collection<Integer> deleted2) {
      this.changed = changed;
      this.deleted = deleted;
      this.deleted2 = deleted2;
    }

    /**
     * @see DominoExerciseDAO#getCommonExercises(int, int, String, String, ChangedAndDeleted)
     * @return
     */
    public List<ImportDoc> getChanged() {
      return changed;
    }

    public List<ImportDoc> getDeleted() {
      return deleted;
    }

    public Collection<Integer> getDeleted2() {
      return deleted2;
    }

    public void setDeleted2(List<Integer> deleted2) {
      this.deleted2 = deleted2;
    }

  }

  @NotNull
  private FindOptions<DocumentColumn> getSince(String sinceInUTC) {
    FindOptions<DocumentColumn> options1 = new FindOptions<>();
    options1.addFilter(new FilterDetail<>(DocumentColumn.RevisionTime, sinceInUTC, FilterDetail.Operator.GT));
    return options1;
  }

  /**
   * @param sinceInUTC
   * @param projid
   * @return
   * @see #getChangedDocs(String, DBUser, ClientPMProject)
   */
  private Collection<Integer> getDeletedDocsSince(String sinceInUTC, int projid) {
    logger.info("getDeletedDocsSince since " + sinceInUTC);

    LocalDate sinceThen = getModifiedTime(sinceInUTC);

    Bson query = and(
        eq("projId", projid)
//        ,
//        eq("active", "false")
        //,
        //gt("updateTime", sinceInUTC)
    );

    FindIterable<Document> projection = pool
        .getMongoCollection("document_heads")
        .find(query)
        .projection(include("_id", "deleteTime", "active"));

    List<Integer> ids = new ArrayList<>();

    int total = 0;
    MongoCursor<Document> cursor = projection.iterator();
    try {
      while (cursor.hasNext()) {
        Document doc = cursor.next();
        Integer id = doc.getInteger("_id");
        Boolean active = doc.getBoolean("active");
        if (!active) {
          String updateTime = doc.getString("deleteTime");
          LocalDate update = getModifiedTime(updateTime);
          if (update.isAfter(sinceThen)) {
            logger.info("getDeletedDocsSince for " + id + " = " + updateTime);
            ids.add(id);
          } else {
            logger.info("getDeletedDocsSince for " + id + " = " + updateTime + " or " + update + " not after " + sinceThen);

            total++;
          }
        } else total++;
      }
    } finally {
      cursor.close();
    }

    logger.info("getDeletedDocsSince : found " + ids.size() + " deleted from " + total);
    return ids;
  }

  @NotNull
  private LocalDate getModifiedTime(String toParse) {
    return LocalDate.parse(toParse, DateTimeFormatter.ofPattern(MONGO_TIME));
  }
}
