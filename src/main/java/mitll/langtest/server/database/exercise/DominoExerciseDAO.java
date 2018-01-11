package mitll.langtest.server.database.exercise;

import mitll.hlt.domino.shared.model.document.*;
import mitll.hlt.domino.shared.model.project.ProjectContentDescriptor;
import mitll.hlt.domino.shared.model.project.ProjectDescriptor;
import mitll.hlt.json.JSONSerializer;
import mitll.langtest.server.database.copy.VocabFactory;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.domino.DominoImport;
import mitll.langtest.server.domino.ImportDoc;
import mitll.langtest.server.domino.ImportInfo;
import mitll.langtest.server.domino.ImportProjectInfo;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static mitll.hlt.domino.shared.model.metadata.MetadataTypes.SkillType.Vocabulary;

/**
 * Read export from domino!
 * <p>
 * Created by go22670 on 5/2/17.
 */
public class DominoExerciseDAO {
  private static final Logger logger = LogManager.getLogger(DominoExerciseDAO.class);

  private static final String UNIT = "unit";
  private static final String CHAPTER = "chapter";
  /**
   * @see #addAttributes
   */
  private static final String V_NP_ID = "v-np-id";
  private static final String PREFIX = "v-";
  public static final String EDIT = "edit";

  private final JSONSerializer ser;

  public DominoExerciseDAO() {
    ser = null;
  }

  /**
   * @param serializer
   */
  public DominoExerciseDAO(JSONSerializer serializer) {
    this.ser = serializer;
  }

  /**
   * TODO : use domino language object
   *
   * @param file        null except for testing
   * @param inputStream null only when testing
   * @param projid      for this project
   * @param importUser  who is doing the importing - marked as the creator of the exercises
   * @return
   * @see mitll.langtest.server.FileUploadHelper#readJSON
   * @deprecated
   */
/*  public ImportInfo readExercises(String file, InputStream inputStream, int projid, int importUser) {
    try {
      JsonObject readObj = getJsonObject(file, inputStream);

      ImportProjectInfo projectInfo = getProjectInfo(readObj);
      setProjectInfo(importUser, readObj, projectInfo);

      List<CommonExercise> exercises =
          getCommonExercises(
              projid,
              projectInfo.getCreatorID(),
              projectInfo.getUnitName(),
              projectInfo.getChapterName(),

              readObj.getJsonArray(DOCUMENTS)
          );

      return new ImportInfo(
          projectInfo,
          exercises
      );

    } catch (FileNotFoundException e) {
      logger.error("Got " + e, e);
    }
    return null;
  }*/

  /**
   * @param projid
   * @param projectInfo
   * @param importDocs
   * @return
   * @see IProjectManagement#getImportFromDomino
   */
  public ImportInfo readExercises(int projid,
                                  ImportProjectInfo projectInfo,
                                  DominoImport.ChangedAndDeleted importDocs
  ) {
    List<CommonExercise> exercises =
        getCommonExercises(
            projid,
            projectInfo.getCreatorID(),
            projectInfo.getUnitName(),
            projectInfo.getChapterName(),

            importDocs
        );

    return new ImportInfo(projectInfo, exercises, importDocs.getDeleted2(), importDocs.getDeletedNPIDs());
  }

 /* private JsonObject getJsonObject(String file, InputStream inputStream) throws FileNotFoundException {
    JsonReader reader = file == null ?
        Json.createReader(inputStream) :
        Json.createReader(new FileReader(file));

    return (JsonObject) reader.read();
  }

  private void setProjectInfo(int importUser, JsonObject readObj, ImportProjectInfo projectInfo) {
    ProjectDescriptor pd = getProjectDescriptor(readObj);
    logger.info("readExercises got ProjectDescriptor " + pd + " : " + projectInfo.getUnitName() + ", " + projectInfo.getChapterName());

    String languageName = getLanguage(pd);
    int creator = getCreator(importUser, pd);
    Date createTime = pd.getCreateTime();
    int id = pd.getId();

    projectInfo.setCreatorID(creator);
    projectInfo.setDominoProjectID(id);
    projectInfo.setCreateTime(createTime.getTime());
    projectInfo.setLanguage(languageName);
  }*/


  /**
   * Get the unit and chapter from the workflow
   *
   * @param readObj
   * @return
   */
/*
  @NotNull
  private ImportProjectInfo getProjectInfo(JsonObject readObj) {
    return getImportProjectInfoFromWorkflow(getProjectWorkflow(readObj));
  }
*/

  /**
   * @param pw
   * @return
   * @see mitll.langtest.server.database.project.ProjectManagement#getImportProjectInfos
   */
/*
  @NotNull
  private ImportProjectInfo getImportProjectInfoFromWorkflow(ProjectWorkflow pw) {
    TaskSpecification taskSpec = pw.getTaskSpec(EDIT);
    Collection<MetadataList> metadataLists = taskSpec.getMetadataLists();
    String unitName = "";
    String chapterName = "";
    for (MetadataList metadataList : metadataLists) {
      MetadataSpecification metadata = metadataList.getMetadata(V_UNIT);
      unitName = metadata.getShortName();
      MetadataSpecification metadata2 = metadataList.getMetadata(V_CHAPTER);
      chapterName = metadata2.getShortName();


      if (!unitName.isEmpty()) break;
    }

    return new ImportProjectInfo(unitName, chapterName);
  }

  private ProjectDescriptor getProjectDescriptor(JsonObject readObj) {
    return ser.deserialize(ProjectDescriptor.class, readObj.getJsonObject(PROJECT).toString());
  }

  private ProjectWorkflow getProjectWorkflow(JsonObject readObj) {
    return ser.deserialize(ProjectWorkflow.class, readObj.getJsonObject(WORKFLOW).toString());
  }
*/


  /**
   * Get the language from the content descriptor.
   *
   * @param pd
   * @return
   */
  private String getLanguage(ProjectDescriptor pd) {
    ProjectContentDescriptor content = pd.getContent();
    if (content.getSkill() != Vocabulary) {
      logger.error("readExercises huh? skill type is " + content.getSkill());
    }

    return content.getLanguageName();
  }

  /**
   * Get the creator from the project descriptor
   *
   * @param importUser
   * @param pd
   * @return
   */
/*  private int getCreator(int importUser, ProjectDescriptor pd) {
    UserDescriptor creator1 = pd.getCreator();
    return creator1 != null ? creator1.getDocumentDBID() : importUser;
  }

  @NotNull
  private List<CommonExercise> getCommonExercises(int projid, int creator, String unitName, String chapterName, JsonArray docArr) {
    List<CommonExercise> exercises = new ArrayList<>();
    docArr.forEach(docObj -> exercises.add(getExerciseFromVocabularyItem(projid, creator, unitName, chapterName, docObj)));
    return exercises;
  }*/

  /**
   * @param projid
   * @param creator
   * @param unitName
   * @param chapterName
   * @param changedAndDeleted
   * @return
   * @see #readExercises
   */
  @NotNull
  private List<CommonExercise> getCommonExercises(int projid, int creator, String unitName, String chapterName,
                                                  DominoImport.ChangedAndDeleted changedAndDeleted) {
    List<ImportDoc> changed = changedAndDeleted.getChanged();
    List<CommonExercise> exercises = new ArrayList<>(changed.size());

    changed.forEach(docObj -> exercises.add(getExerciseFromVocab(projid,
        creator, unitName, chapterName, docObj.getDocID(), docObj.getTimestamp(),
        docObj.getVocabularyItem()
    )));
    return exercises;
  }

  /**
   * @see #getCommonExercises
   * @param projid
   * @param creator
   * @param unitName
   * @param chapterName
   * @param docID
   * @param time
   * @param vocabularyItem
   * @return
   */
  private Exercise getExerciseFromVocab(int projid,
                                        int creator,
                                        String unitName,
                                        String chapterName,
                                        int docID,
                                        long time,
                                        VocabularyItem vocabularyItem) {
    logger.info("getExerciseFromVocab ex for doc " + docID + " term " + vocabularyItem.getTerm());
    String npID = getNPId(vocabularyItem);
    Exercise ex = getExerciseFromVocabularyItem(projid, docID, npID, vocabularyItem, creator, time);
    addAttributes(unitName, chapterName, vocabularyItem, ex);
//        logger.info("Got " + ex.getUnitToValue());
    addContextSentences(projid, creator, docID, npID, vocabularyItem.getSamples(), ex);

    return ex;
  }


  private void addAttributes(String unitName, String chapterName, MetadataComponentBase vocabularyItem, Exercise ex) {
    List<IMetadataField> metadataFields = vocabularyItem.getMetadataFields();
    for (IMetadataField field : metadataFields) {
      String name = field.getName();
      String displayValue = field.getDisplayValue();

      boolean isNPID = name.equals(V_NP_ID);
      if (name.startsWith(PREFIX) && !isNPID) {
        name = name.substring(2);
        addAttribute(unitName, chapterName, name, displayValue, ex);
      }
    }
  }

  private String getNPId(MetadataComponentBase vocabularyItem) {
    List<IMetadataField> metadataFields = vocabularyItem.getMetadataFields();
    for (IMetadataField field : metadataFields) {
      String name = field.getName();
      String displayValue = field.getDisplayValue();

      if (name.equals(V_NP_ID) && !displayValue.isEmpty()) {
        return displayValue;
      }
    }
    return "unknown";
  }

  private void addAttribute(String unitName, String chapterName, String name, String displayValue, Exercise ex) {
    if (name.equals(UNIT)) {
      ex.addUnitToValue(unitName, displayValue);
    } else if (name.equals(CHAPTER)) {
      ex.addUnitToValue(chapterName, displayValue);
    } else {
      if (!displayValue.trim().isEmpty()) {
//            logger.info("addAttribute : for " + ex.getID() + " adding " + name + " = " + displayValue);
        ex.addAttribute(new ExerciseAttribute(name, displayValue));
      }
    }
  }

  /**
   * @param projid
   * @param creator
   * @param docID
   * @param samples
   * @param parentExercise
   * @see #getExerciseFromVocab(int, int, String, String, int, long, VocabularyItem)
   */
  private void addContextSentences(int projid,
                                   int creator,
                                   int docID,
                                   String npID,
                                   IDocumentComposite samples,
                                   Exercise parentExercise) {
   // IDocumentComposite samples = vocabularyItem.getSamples();
/*
    boolean isInt = false;
    int npInt = -1;
    try {
      npInt = Integer.parseInt(npID);
      isInt = true;
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }*/
    for (IDocumentComponent comp : samples.getComponents()) {
      SampleSentence sample = (SampleSentence) comp;
      //int compid = docID * 10 + sample.getNum();  // NO NO NO
      String contextNPID = /*isInt ? "" + npInt * 10 + sample.getNum() :*/ (npID + "_" + sample.getNum());

      String sentenceVal = sample.getSentenceVal();
      logger.info("addContextSentences : context" +
          "\n\timport id " + docID +
          "\n\tnpID      " + contextNPID +
          " " + sentenceVal);

      if (!sentenceVal.trim().isEmpty()) {
        Exercise context = getExerciseFromVocabularyItem(
            projid,
            contextNPID,
            docID, // parent domino id
            creator,

            removeMarkup(sentenceVal),
            removeMarkup(sample.getAlternateFormVal()),
            removeMarkup(sample.getTransliterationVal()),
            removeMarkup(sample.getTranslationVal()),

            true);

        context.setDominoContextIndex(sample.getNum());

        context.setUnitToValue(parentExercise.getUnitToValue());
        parentExercise.getDirectlyRelated().add(context);
        logger.info("addContextSentences : parent ex id " + parentExercise.getID() + " dom " + parentExercise.getDominoID());
        context.setParentDominoID(parentExercise.getDominoID());
      }
    }
  }

  /**
   * For now we remove markup.
   * TODO : Consider later actually passing it through.
   *
   * @param projid
   * @param oldid
   * @param vocabularyItem
   * @param creatorID
   * @param createTime
   * @return
   */
  @NotNull
  private Exercise getExerciseFromVocabularyItem(int projid,
                                                 int oldid,
                                                 String npID,
                                                 VocabularyItem vocabularyItem,
                                                 int creatorID,
                                                 long createTime) {
    String termVal = vocabularyItem.getTermVal();
    String alternateFormVal = vocabularyItem.getAlternateFormVal();
    String transliterationVal = vocabularyItem.getTransliterationVal();
    String meaning = vocabularyItem.getMeaningVal();

    Exercise exerciseFromVocabularyItem = getExerciseFromVocabularyItem(
        projid,
        npID,
        oldid,
        creatorID,
        removeMarkup(termVal),
        removeMarkup(alternateFormVal),
        removeMarkup(transliterationVal),
        removeMarkup(meaning), false);
    exerciseFromVocabularyItem.setUpdateTime(createTime);

    return exerciseFromVocabularyItem;
  }

  private String removeMarkup(String termVal) {
    return termVal.replaceAll(VocabFactory.HTML_TAG_PATTERN, "").replaceAll("&#xa0;", "").trim();
  }

  /**
   * @param projid
   * @param npID
   * @param dominoID
   * @param creatorID
   * @param termVal
   * @param alternateFormVal
   * @param transliterationVal
   * @param meaning
   * @param isContext
   * @return
   * @see #addContextSentences
   */
  @NotNull
  private Exercise getExerciseFromVocabularyItem(int projid,
                                                 String npID,
                                                 int dominoID,
                                                 int creatorID,
                                                 String termVal,
                                                 String alternateFormVal,
                                                 String transliterationVal,
                                                 String meaning,
                                                 boolean isContext) {
    String trim = termVal.trim();
    Exercise exercise = new Exercise(-1,
        npID,
        creatorID,
        meaning.trim(),
        trim,
        StringUtils.stripAccents(trim),
        alternateFormVal.trim(),
        "",
        transliterationVal.trim(),
        projid,
        false,
        0,
        isContext,
        0,
        dominoID);

    logger.info("made new ex" +
        "\n\tdominoID " + exercise.getDominoID() +
        "\n\tnpID     " + exercise.getOldID() +
        "\n\tex id    " + exercise.getID() +
        "\n\teng      " + exercise.getEnglish() +
        "\n\tfl       " + exercise.getForeignLanguage() +
        "\n\tcontext  " + isContext);

    exercise.setPredef(true);

    return exercise;
  }
}
