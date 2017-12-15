package mitll.langtest.server.domino;

import mitll.hlt.domino.server.data.DocumentServiceDelegate;
import mitll.hlt.domino.server.data.IProjectWorkflowDAO;
import mitll.hlt.domino.server.data.ProjectServiceDelegate;
import mitll.hlt.domino.shared.common.FilterDetail;
import mitll.hlt.domino.shared.common.FindOptions;
import mitll.hlt.domino.shared.model.HeadDocumentRevision;
import mitll.hlt.domino.shared.model.document.DocumentColumn;
import mitll.hlt.domino.shared.model.document.VocabularyItem;
import mitll.hlt.domino.shared.model.metadata.MetadataList;
import mitll.hlt.domino.shared.model.metadata.MetadataSpecification;
import mitll.hlt.domino.shared.model.project.ClientPMProject;
import mitll.hlt.domino.shared.model.project.ProjectColumn;
import mitll.hlt.domino.shared.model.project.ProjectDescriptor;
import mitll.hlt.domino.shared.model.project.ProjectWorkflow;
import mitll.hlt.domino.shared.model.taskspec.TaskSpecification;
import mitll.hlt.domino.shared.model.user.DBUser;
import mitll.langtest.server.database.exercise.DominoExerciseDAO;
import mitll.langtest.server.database.project.IProjectManagement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class DominoImport implements IDominoImport {
  private static final Logger logger = LogManager.getLogger(DominoImport.class);

  private static final String CREATED = "Created";
  public static final String MODIFIED = "Modified";
  public static final String NUM_ITEMS = "Num Items";
  public static final String CREATOR_ID = "creatorId";
  public static final String DOMINO_ID = "Domino ID";
  public static final String VOCABULARY = "Vocabulary";


  private ProjectServiceDelegate projectDelegate;
  private DocumentServiceDelegate documentDelegate;
  private final IProjectWorkflowDAO workflowDelegate;

  public DominoImport(ProjectServiceDelegate projectDelegate, IProjectWorkflowDAO workflowDelegate, DocumentServiceDelegate documentDelegate) {
    this.projectDelegate = projectDelegate;
    this.workflowDelegate = workflowDelegate;
    this.documentDelegate = documentDelegate;
  }

//  @Override
  @Override
  public ImportInfo getImportFromDomino(int projID, int dominoID, String sinceInUTC, DBUser dominoAdminUser) {
    List<ImportProjectInfo> matches = getImportProjectInfosByID(dominoID,dominoAdminUser);

    if (matches.isEmpty()) {
      return null;
    } else {
   //   DBUser dominoAdminUser = db.getUserDAO().getDominoAdminUser();
      ClientPMProject next = getClientPMProject(dominoID, dominoAdminUser);

      return new DominoExerciseDAO()
          .readExercises(projID, matches.iterator().next(),
              getChangedDocs(sinceInUTC, dominoAdminUser, next)
          );
    }
  }

  /**
   * @return
   * @see IProjectManagement#getImportFromDomino
   * @see #getVocabProjects
   */
  @Override
  @NotNull
  public List<ImportProjectInfo> getImportProjectInfos(DBUser dominoAdminUser) {
    FindOptions<ProjectColumn> options = new FindOptions<>();
    options.addFilter(new FilterDetail<>(ProjectColumn.Skill, VOCABULARY, FilterDetail.Operator.EQ));
    return getImportProjectInfos(options,dominoAdminUser);
  }

  @NotNull
  private List<ImportProjectInfo> getImportProjectInfosByID(int id, DBUser dominoAdminUser) {
    FindOptions<ProjectColumn> options = new FindOptions<>();
    options.addFilter(new FilterDetail<>(ProjectColumn.Id, "" + id, FilterDetail.Operator.EQ));
    return getImportProjectInfos(options,dominoAdminUser);
  }

  @NotNull
  private List<ImportProjectInfo> getImportProjectInfos(FindOptions<ProjectColumn> options, DBUser dominoAdminUser) {
    List<ProjectDescriptor> projects1 = projectDelegate.getProjects(dominoAdminUser,
        null,
        options,
        false, false, false
    );

    List<ImportProjectInfo> imported = new ArrayList<>();

    for (ProjectDescriptor project : projects1) {
//      logger.info("Got " + project);
      Date now = project.getCreateTime();

      int documentDBID = project.getCreator().getDocumentDBID();

      project.getContent().getLanguageName();

      int id = project.getId();
      ImportProjectInfo creatorId = new ImportProjectInfo(
          id,
          documentDBID,
          project.getName(),
          project.getContent().getLanguageName(),
          now.getTime()
      );

      imported.add(creatorId);

      ProjectWorkflow forProject = workflowDelegate.getForProject(id);

      if (forProject == null) {
        logger.warn("no workflow for project " + id);
      } else {
        List<TaskSpecification> taskSpecs = forProject.getTaskSpecs();

        for (TaskSpecification specification : taskSpecs) {
          Collection<MetadataList> metadataLists = specification.getMetadataLists();

          for (MetadataList list : metadataLists) {
//            logger.info("got " + list);

            List<MetadataSpecification> list1 = list.getList();
            for (MetadataSpecification specification1 : list1) {
              if (specification1.getDBName().equalsIgnoreCase("v-unit")) {
                creatorId.setUnitName(specification1.getLongName());
              } else if (specification1.getDBName().equalsIgnoreCase("v-chapter")) {
                creatorId.setChapterName(specification1.getLongName());
              }
            }
          }
        }
      }
    }

    return imported;
  }

  private ClientPMProject getClientPMProject(int dominoID, DBUser dominoAdminUser) {
    FindOptions<ProjectColumn> options = new FindOptions<>();
    options.addFilter(new FilterDetail<>(ProjectColumn.Id, "" + dominoID, FilterDetail.Operator.EQ));
    List<ClientPMProject> projectDescriptor = projectDelegate.getHeavyProjects(dominoAdminUser, options);

    return projectDescriptor.iterator().next();
  }

  @NotNull
  private ChangedAndDeleted getChangedDocs(String sinceInUTC, DBUser dominoAdminUser, ClientPMProject next) {
    long then = System.currentTimeMillis();

    FindOptions<DocumentColumn> options1 = getSince(sinceInUTC);

    List<HeadDocumentRevision> documents1 = documentDelegate.getHeavyDocuments(next, dominoAdminUser, false, false, options1);

    List<ImportDoc> docs = new ArrayList<>();
    List<ImportDoc> deleted = new ArrayList<>();
    for (HeadDocumentRevision doc : documents1) {
      Integer id1 = doc.getId();
      VocabularyItem vocabularyItem = (VocabularyItem) doc.getDocument();
      docs.add(new ImportDoc(id1, doc.getUpdateTime().getTime(), vocabularyItem));
      logger.info("\t found changed " + vocabularyItem);
    }
    long now = System.currentTimeMillis();

    logger.info("getDocs : took " + (now - then) + " to get " + docs.size());

    return new ChangedAndDeleted(docs, deleted);
  }

  public class ChangedAndDeleted {
    private List<ImportDoc> changed;
    private List<ImportDoc> deleted;

    public ChangedAndDeleted(List<ImportDoc> changed, List<ImportDoc> deleted) {
      this.changed = changed;
      this.deleted = deleted;
    }

    public List<ImportDoc> getChanged() {
      return changed;
    }

    public List<ImportDoc> getDeleted() {
      return deleted;
    }
  }

  @NotNull
  private FindOptions<DocumentColumn> getSince(String sinceInUTC) {
    FindOptions<DocumentColumn> options1 = new FindOptions<>();
    options1.addFilter(new FilterDetail<>(DocumentColumn.RevisionTime, sinceInUTC, FilterDetail.Operator.GT));
    return options1;
  }

}
