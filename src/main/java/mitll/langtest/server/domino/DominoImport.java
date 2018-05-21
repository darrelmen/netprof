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
import mitll.hlt.domino.shared.model.document.IMetadataField;
import mitll.hlt.domino.shared.model.document.MetadataComponentBase;
import mitll.hlt.domino.shared.model.document.VocabularyItem;
import mitll.hlt.domino.shared.model.metadata.MetadataTypes;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;
import static mitll.langtest.server.domino.ProjectSync.MONGO_TIME;

public class DominoImport implements IDominoImport {
  private static final Logger logger = LogManager.getLogger(DominoImport.class);
  private static final String VOCABULARY = "Vocabulary";
  private static final String V_UNIT = "v-unit";
  private static final String V_CHAPTER = "v-chapter";
  private static final String ID = "_id";
  private static final String ACTIVE = "active";
  private static final String DELETE_TIME = "deleteTime";
  private static final String PROJ_ID = "projId";
  private static final String V_NP_ID = "v-np-id";
  private static final String METADATA = "metadata";
  private static final String DOC_CONTENT = "docContent";
  private static final String VALUE = "value";

  private final ProjectServiceDelegate projectDelegate;
  private final DocumentServiceDelegate documentDelegate;
  private final IProjectWorkflowDAO workflowDelegate;
  private final Mongo pool;

  /**
   * @param projectDelegate
   * @param workflowDelegate
   * @param documentDelegate
   * @see ProjectManagement#setupDominoProjectImport
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
    logger.info("getImportFromDomino " + projID + " domino " + dominoID + " since " + sinceInUTC);

    List<ImportProjectInfo> matches = getImportProjectInfosByID(dominoID, dominoAdminUser);

    if (matches.isEmpty()) {
      return null;
    } else {
      return new DominoExerciseDAO()
          .readExercises(projID,
              matches.iterator().next(),
              getChangedDocs(
                  sinceInUTC,
                  dominoAdminUser,
                  getClientPMProject(dominoID, dominoAdminUser))
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

    setUnitAndChapter(id, importProjectInfo, project.getContent().getParentId());
    return importProjectInfo;
  }

  /**
   * From workflow.
   *
   * @param id
   * @param importProjectInfo
   * @param parentId
   * @see #getImportProjectInfo(ProjectDescriptor)
   */
  private void setUnitAndChapter(int id, ImportProjectInfo importProjectInfo, int parentId) {
    ProjectWorkflow workflow = workflowDelegate.getForProject(id);


    if (workflow == null && parentId > -1) {
      workflow = workflowDelegate.getForProject(parentId);
      if (workflow != null) {
//        logger.info("found workflow on parent " + parentId);
      }
    }

    if (workflow == null) {
      logger.error("setUnitAndChapter: no workflow for project " + id + " or parent " + parentId);
    } else {
      workflow
          .getTaskSpecs()
          .forEach(taskSpecification -> taskSpecification
              .getMetadataLists()
              .forEach(metadataList -> metadataList
                  .getList()
                  .forEach(metadataSpecification -> {
                    String dbName = metadataSpecification.getDBName();
                    String longName = metadataSpecification.getLongName();

                    if (dbName.equalsIgnoreCase(V_UNIT)) {
                      importProjectInfo.setUnitName(longName);
                    } else if (dbName.equalsIgnoreCase(V_CHAPTER)) {
                      importProjectInfo.setChapterName(longName);
                    }
                  })));

      String unitName = importProjectInfo.getUnitName();
      String chapterName = importProjectInfo.getChapterName();

      if (unitName.isEmpty() && chapterName.isEmpty()) {
        logger.warn("no unit or chapter info on " + id +
            "\n\tworkflow " + workflow +
            "\n\ttasks " + workflow.getTaskSpecs());
      } else {
/*        logger.info("unit/chapter info on " + id +
            "\n\tunitName " + unitName +
            "\n\tchapterName " + chapterName);*/
      }
    }
  }

  private String getNPId(MetadataComponentBase vocabularyItem) {
    List<IMetadataField> metadataFields = vocabularyItem.getMetadataFields();
    for (IMetadataField field : metadataFields) {
      String name = field.getName();
      String displayValue = field.getDisplayValue();

      if (name.equals(MetadataTypes.VocabularyMetadata.V_NP_ID) && !displayValue.isEmpty()) {
        return displayValue;
      } else {
        logger.warn("not np id '" + name +
            "' vs " + MetadataTypes.VocabularyMetadata.V_NP_ID);
      }
    }
    return "unknown";
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
   * @param dominoProject
   * @return
   * @see #getImportFromDomino
   */
  @NotNull
  private ChangedAndDeleted getChangedDocs(String sinceInUTC, DBUser dominoAdminUser, ClientPMProject dominoProject) {
    Set<Integer> added = new HashSet<>();
    List<ImportDoc> addedImports = getAddedImports(sinceInUTC, dominoAdminUser, dominoProject, added);

    List<ImportDoc> changedImports = getChangedImports(sinceInUTC, dominoAdminUser, dominoProject, added);

    Set<String> deletedNPIDs = new TreeSet<>();

    //Map<String, Integer> npToDomino = new HashMap<>();

    Collection<Integer> deletedDocsSince = getDeletedDocsSince(sinceInUTC, dominoProject.getId(), deletedNPIDs);

    // Set<String> deletedNPIDs = getDeletedIDs(dominoAdminUser, dominoProject, deletedDocsSince);

    logger.info("getChangedDocs : added " + addedImports.size() + " change " + changedImports.size() + " deleted " + deletedNPIDs.size());
    return new ChangedAndDeleted(changedImports, new ArrayList<>(), deletedDocsSince, deletedNPIDs, addedImports);
  }

  @NotNull
  private List<ImportDoc> getAddedImports(String sinceInUTC, DBUser dominoAdminUser, ClientPMProject next, Set<Integer> added) {
    long then = System.currentTimeMillis();
    List<HeadDocumentRevision> docsSince =
        documentDelegate.getHeavyDocuments(next, dominoAdminUser, false, false,
            getAddedSince(sinceInUTC));
    docsSince.forEach(headDocumentRevision -> added.add(headDocumentRevision.getId()));
    return getImportDocs(then, docsSince);
  }

  @NotNull
  private List<ImportDoc> getChangedImports(String sinceInUTC, DBUser dominoAdminUser, ClientPMProject next, Set<Integer> added) {
    long then = System.currentTimeMillis();
    List<HeadDocumentRevision> changedSince =
        documentDelegate.getHeavyDocuments(next, dominoAdminUser, false, false, getChangedSince(sinceInUTC));

    List<HeadDocumentRevision> changedNotAdded = changedSince.stream().filter(headDocumentRevision -> !added.contains(headDocumentRevision.getId())).collect(Collectors.toList());
    return getImportDocs(then, changedNotAdded);
  }

  @NotNull
  private List<ImportDoc> getImportDocs(long then, List<HeadDocumentRevision> changedSince) {
    List<ImportDoc> importDocs = new ArrayList<>(changedSince.size());

    changedSince.forEach(doc -> {
      //VocabularyItem vocabularyItem = (VocabularyItem) doc.getDocument();
      importDocs.add(new ImportDoc(doc.getId(), doc.getUpdateTime().getTime(), (VocabularyItem) doc.getDocument()));
//      logger.info("\t getImportDocs : found changed " + vocabularyItem);
    });

    long now = System.currentTimeMillis();

    logger.info("getImportDocs : took " + (now - then) + " to get " + importDocs.size() + " document.");
    return importDocs;
  }

  /**
   * Get netprof ids for deleted items.
   *
   * @param dominoAdminUser
   * @param next
   * @param deletedDocsSince
   * @return
   * @see #getChangedDocs(String, DBUser, ClientPMProject)
   */
/*
  @NotNull
  private Set<String> getDeletedIDs(DBUser dominoAdminUser, ClientPMProject next, Collection<Integer> deletedDocsSince) {
    Set<String> deletedNPIDs = new TreeSet<>();

    deletedDocsSince.forEach(id -> {
      logger.info("\tgetDeletedIDs check " + id);
      FindOptions<DocumentColumn> options1 = new FindOptions<>();
      options1.addFilter(new FilterDetail<>(DocumentColumn.Id, "" + id, FilterDetail.Operator.EQ));

      List<HeadDocumentRevision> documents2 =
          documentDelegate.getHeavyDocuments(next, dominoAdminUser, false, false, options1);

      if (!documents2.isEmpty()) {
        HeadDocumentRevision headDocumentRevision = documents2.get(0);
        IDocument document = headDocumentRevision.getDocument();
        VocabularyItem vocabularyItem = (VocabularyItem) document;
        deletedNPIDs.add(getNPId(vocabularyItem));
      } else {
        logger.warn("\tgetDeletedIDs no heavy for " + id + " in " + next);
      }
    });

    logger.info("getDeletedIDs got " + deletedNPIDs + " deleted np items, given " + deletedDocsSince.size());
    return deletedNPIDs;
  }
*/

  /**
   *
   */
  public class ChangedAndDeleted {
    private final List<ImportDoc> added;
    private final List<ImportDoc> changed;
    private final List<ImportDoc> deleted;
    private final Collection<Integer> deleted2;
    private final Set<String> deletedNPIDs;

    /**
     * @param changed
     * @param deleted
     * @param deleted2
     * @see DominoImport#getChangedDocs
     */
    ChangedAndDeleted(List<ImportDoc> changed,
                      List<ImportDoc> deleted,
                      Collection<Integer> deleted2,
                      Set<String> deletedNPIDs,
                      List<ImportDoc> added) {
      this.added = added;
      this.changed = changed;
      this.deleted = deleted;
      this.deleted2 = deleted2;
      this.deletedNPIDs = deletedNPIDs;
    }

    /**
     * @return
     * @seex DominoExerciseDAO#getCommonExercises
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

    /**
     * @return
     * @see DominoExerciseDAO#readExercises
     */
    public Set<String> getDeletedNPIDs() {
      return deletedNPIDs;
    }

    public List<ImportDoc> getAdded() {
      return added;
    }
  }

  @NotNull
  private FindOptions<DocumentColumn> getChangedSince(String sinceInUTC) {
    FindOptions<DocumentColumn> options1 = new FindOptions<>();
    options1.addFilter(new FilterDetail<>(DocumentColumn.RevisionTime, sinceInUTC, FilterDetail.Operator.GT));
    return options1;
  }

  @NotNull
  private FindOptions<DocumentColumn> getAddedSince(String sinceInUTC) {
    FindOptions<DocumentColumn> options1 = new FindOptions<>();
    options1.addFilter(new FilterDetail<>(DocumentColumn.CreateTime, sinceInUTC, FilterDetail.Operator.GT));
    return options1;
  }

  /**
   * TODO : try with normal query again...
   *
   * @param sinceInUTC
   * @param projid
   * @return
   * @see #getChangedDocs(String, DBUser, ClientPMProject)
   */
  private Collection<Integer> getDeletedDocsSince(String sinceInUTC, int projid, Set<String> npIDs) {
    LocalDateTime sinceThen = getModifiedTime(sinceInUTC);
    logger.info("getDeletedDocsSince since " + sinceInUTC + " or " + sinceThen);

    Bson query = and(
        eq(PROJ_ID, projid),
        eq(ACTIVE, false),
        gt(DELETE_TIME, sinceInUTC)
    );

    FindIterable<Document> projection = pool
        .getMongoCollection("document_heads")
        .find(query)
        .projection(include(ID, DELETE_TIME, /*ACTIVE,*/ DOC_CONTENT));

    List<Integer> ids = new ArrayList<>();

    //Map<String, Integer> npToDomino = new HashMap<>();

    try (MongoCursor<Document> cursor = projection.iterator()) {
      while (cursor.hasNext()) {
        Document doc = cursor.next();
        Integer id = doc.getInteger(ID);
        // Boolean active = doc.getBoolean(ACTIVE);
        //  if (!active) {
        String updateTime = doc.getString(DELETE_TIME);
        logger.info("getDeletedDocsSince for " + id + " = " + updateTime);

        ids.add(id);

        Object docContent = doc.get(DOC_CONTENT);
        // logger.info("getDeletedDocsSince docContent " + docContent);

        if (docContent instanceof Document) {
          Document dd = (Document) docContent;
          Object metadata = dd.get(METADATA);

          //logger.info("\tmeta " + metadata);
          if (metadata != null && metadata instanceof List) {
            //logger.info("\tmeta " + metadata.getClass());
            List<Document> collect = getNPMetadata(metadata);
            if (!collect.isEmpty()) {
              Document matchingMeta = collect.iterator().next();
              logger.info("\tgot " + matchingMeta);
              String npID = matchingMeta.getString(VALUE);
              if (npID != null) {
                npIDs.add(npID);
             //   npToDomino.put(npID,id);
              }
            } else {
              logger.warn("no metadata on " + metadata);
            }
          }
        } else {
          logger.warn("docContent not a document " + docContent);
        }

      }
    }

    logger.info("getDeletedDocsSince : found " + ids.size() +
        // " deleted from " + total + " known documents," +
        "\n\n\n\n" +
        "\n\tfound      : " + npIDs
        //+
        //"\n\tnpToDomino : " + npToDomino
    );
    return ids;
  }

  private List<Document> getNPMetadata(Object metadataObj) {
    List<Document> metadata = (List<Document>) metadataObj;
    return metadata.stream().filter(document -> document.values().contains(V_NP_ID)).collect(Collectors.toList());
  }

  @NotNull
  private LocalDateTime getModifiedTime(String toParse) {
    return LocalDateTime.parse(toParse, DateTimeFormatter.ofPattern(MONGO_TIME));
  }


  public static void main(String[] arg) {
    String toParse = "2018-01-30T18:43:36.719Z";
    //  LocalDateTime parse = LocalDateTime.parse(toParse, DateTimeFormatter.ISO_INSTANT);
    LocalDateTime parse = LocalDateTime.parse(toParse, DateTimeFormatter.ofPattern(MONGO_TIME));
    System.err.println("got " + parse + " " + parse.toLocalTime() + " ");
  }
}
