package mitll.langtest.server.database.exercise;

import mitll.hlt.domino.shared.model.document.*;
import mitll.langtest.server.database.copy.VocabFactory;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.domino.DominoImport;
import mitll.langtest.server.domino.ImportDoc;
import mitll.langtest.server.domino.ImportInfo;
import mitll.langtest.server.domino.ImportProjectInfo;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.SlickExercise;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
  public static final String UNKNOWN = "unknown";
  private boolean shouldSwap;
  private IUserExerciseDAO userExerciseDAO;

  public DominoExerciseDAO(IUserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
  }

  /**
   * @param projid
   * @param projectInfo
   * @param importDocs
   * @param shouldSwap
   * @return
   * @see IProjectManagement#getImportFromDomino
   */
  public ImportInfo readExercises(int projid,
                                  ImportProjectInfo projectInfo,
                                  DominoImport.ChangedAndDeleted importDocs,
                                  boolean shouldSwap) {
    this.shouldSwap = shouldSwap;
    List<CommonExercise> addedCommonExercises = getAddedCommonExercises(
        projid,
        projectInfo.getCreatorID(),
        projectInfo.getUnitName(),
        projectInfo.getChapterName(),

        importDocs
    );

    List<CommonExercise> changedCommonExercises =
        getChangedCommonExercises(
            projid,
            projectInfo.getCreatorID(),
            projectInfo.getUnitName(),
            projectInfo.getChapterName(),

            importDocs
        );


    return new ImportInfo(projectInfo,
        addedCommonExercises,
        changedCommonExercises,
        importDocs.getDeleted2(),
        importDocs.getDeletedNPIDs());
  }

  @NotNull
  private List<CommonExercise> getAddedCommonExercises(int projid, int creator, String unitName, String chapterName,
                                                       DominoImport.ChangedAndDeleted changedAndDeleted) {
    return getExerciseFromImport(projid, creator, unitName, chapterName, changedAndDeleted.getAdded());
  }

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
  private List<CommonExercise> getChangedCommonExercises(int projid, int creator, String unitName, String chapterName,
                                                         DominoImport.ChangedAndDeleted changedAndDeleted) {
    return getExerciseFromImport(projid, creator, unitName, chapterName, changedAndDeleted.getChanged());
  }

  @NotNull
  private List<CommonExercise> getExerciseFromImport(int projid, int creator, String unitName, String chapterName,
                                                     List<ImportDoc> changed) {
    List<CommonExercise> exercises = new ArrayList<>(changed.size());

    changed.forEach(docObj -> exercises.add(getExerciseFromVocab(projid,
        creator, unitName, chapterName,
        docObj.getDocID(),
        docObj.getTimestamp(),
        docObj.getVocabularyItem()
    )));
    return exercises;
  }

  /**
   * @param projid
   * @param creator
   * @param unitName
   * @param chapterName
   * @param docID
   * @param time
   * @param vocabularyItem
   * @return
   * @see #getChangedCommonExercises
   */
  private Exercise getExerciseFromVocab(int projid,
                                        int creator,
                                        String unitName,
                                        String chapterName,
                                        int docID,
                                        long time,
                                        VocabularyItem vocabularyItem) {
    logger.info("getExerciseFromVocab ex for doc " + docID + " term " + vocabularyItem.getTerm());
    SlickExercise byDominoID = userExerciseDAO.getByDominoID(docID);
    int exID = -1;
    if (byDominoID != null) {
      exID = byDominoID.id();

    }
    String npID = getNPId(vocabularyItem);
    Exercise ex = getExerciseFromVocabularyItem(projid, docID, npID, vocabularyItem, creator, time, exID);
    addAttributes(unitName, chapterName, vocabularyItem, ex);
//        logger.info("Got " + ex.getUnitToValue());
    addContextSentences(projid, creator, docID, npID, vocabularyItem.getSamples(), ex, shouldSwap);

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
//        logger.info("addAttributes for ex " + ex.getID() + " unit " + unitName + " chapter " + chapterName +
//            "\n\tfield " + name + " = " + displayValue);

        addAttribute(unitName, chapterName, name, displayValue, ex);
      }
    }
  }

  private void addAttribute(String unitName, String chapterName, String name, String displayValue, Exercise ex) {
    if (name.equals(UNIT)) {
      ex.addUnitToValue(unitName, displayValue);
    } else if (name.equals(CHAPTER)) {
      ex.addUnitToValue(chapterName, displayValue);
    } else {
      if (!displayValue.trim().isEmpty()) {
//            logger.info("addAttribute : for " + ex.getID() + " adding " + name + " = " + displayValue);
        ex.addAttribute(new ExerciseAttribute(getNormName(name), displayValue));
      }
    }
  }

  private String getNormName(String name) {
    if (name.equalsIgnoreCase(SectionHelper.SUBTOPIC_LC)) {
      return SectionHelper.Facet.SUB_TOPIC.getName();
    } else {
      return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
  }

  /**
   * @param vocabularyItem
   * @return
   * @see #getExerciseFromVocab
   */
  private String getNPId(MetadataComponentBase vocabularyItem) {
    List<IMetadataField> metadataFields = vocabularyItem.getMetadataFields();
    for (IMetadataField field : metadataFields) {
      String name = field.getName();
      String displayValue = field.getDisplayValue();

      if (name.equals(V_NP_ID) && !displayValue.isEmpty()) {
        return displayValue;
      }
    }
    return UNKNOWN;
  }

  /**
   * @param projid
   * @param creator
   * @param docID
   * @param samples
   * @param parentExercise
   * @param shouldSwap
   * @see #getExerciseFromVocab(int, int, String, String, int, long, VocabularyItem)
   */
  private void addContextSentences(int projid,
                                   int creator,
                                   int docID,
                                   String npID,
                                   IDocumentComposite samples,
                                   Exercise parentExercise,
                                   boolean shouldSwap) {
    for (IDocumentComponent comp : samples.getComponents()) {
      SampleSentence sample = (SampleSentence) comp;
      String contextNPID = npID.equalsIgnoreCase(UNKNOWN) ? UNKNOWN : npID + "_" + sample.getNum();

      String sentenceVal = sample.getSentenceVal();
      logger.info("addContextSentences : context" +
          "\n\timport id " + docID +
          "\n\tnpID      " + contextNPID +
          "\n\tsentence  " + sentenceVal);

      if (!sentenceVal.trim().isEmpty()) {
        int parentDominoID = parentExercise.getDominoID();
        Map<String, String> unitToValue = parentExercise.getUnitToValue();

        Exercise context =
            getContextExercise(projid, -1, creator, docID, sample, contextNPID, sentenceVal, parentDominoID, unitToValue);

        logger.info("addContextSentences : parent ex id " + parentExercise.getID() + " dom " + parentDominoID);

        parentExercise.getDirectlyRelated().add(context);
      }
    }
  }

  /**
   * So at the moment of import - we know the parent domino id and it's sample num, but nothing else about it.
   *
   * @param projid
   * @param exID
   * @param creator
   * @param docID
   * @param sample
   * @param contextNPID
   * @param sentenceVal
   * @param parentDominoID
   * @param unitToValue
   * @return
   */
  @NotNull
  private Exercise getContextExercise(int projid, int exID, int creator, int docID, SampleSentence sample, String contextNPID,
                                      String sentenceVal, int parentDominoID, Map<String, String> unitToValue) {
    Exercise context = getExerciseFromVocabularyItem(
        projid,
        exID,
        contextNPID,
        docID, // parent domino id
        creator,

        removeMarkup(sentenceVal),
        removeMarkup(sample.getAlternateFormVal()),
        removeMarkup(sample.getTransliterationVal()),
        removeMarkup(sample.getTranslationVal()),

        true, shouldSwap);

    context.setDominoContextIndex(sample.getNum());

    context.setUnitToValue(unitToValue);
    context.setParentDominoID(parentDominoID);
    return context;
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
   * @param exID
   * @return
   */
  @NotNull
  private Exercise getExerciseFromVocabularyItem(int projid,
                                                 int oldid,
                                                 String npID,
                                                 VocabularyItem vocabularyItem,
                                                 int creatorID,
                                                 long createTime, int exID) {
    String termVal = vocabularyItem.getTermVal();
    String alternateFormVal = vocabularyItem.getAlternateFormVal();
    String transliterationVal = vocabularyItem.getTransliterationVal();
    String meaning = vocabularyItem.getMeaningVal();

    Exercise exerciseFromVocabularyItem = getExerciseFromVocabularyItem(
        projid,
        exID,
        npID,
        oldid,
        creatorID,
        removeMarkup(termVal),
        removeMarkup(alternateFormVal),
        removeMarkup(transliterationVal),
        removeMarkup(meaning), false, shouldSwap);
    exerciseFromVocabularyItem.setUpdateTime(createTime);

    return exerciseFromVocabularyItem;
  }

  private String removeMarkup(String termVal) {
    return termVal.replaceAll(VocabFactory.HTML_TAG_PATTERN, "").replaceAll("&#xa0;", "").trim();
  }

  /**
   * @param projid
   * @param exID
   * @param npID
   * @param dominoID
   * @param creatorID
   * @param termVal
   * @param alternateFormVal
   * @param transliterationVal
   * @param meaning
   * @param isContext
   * @param shouldSwap
   * @return
   * @see #addContextSentences
   */
  @NotNull
  private Exercise getExerciseFromVocabularyItem(int projid,
                                                 int exID,
                                                 String npID,
                                                 int dominoID,
                                                 int creatorID,
                                                 String termVal,
                                                 String alternateFormVal,
                                                 String transliterationVal,
                                                 String meaning,
                                                 boolean isContext,
                                                 boolean shouldSwap) {
    String trim = termVal.trim();

    Exercise exercise = new Exercise(exID,
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
        dominoID, shouldSwap);

    logger.info("getExerciseFromVocabularyItem : made ex" +
        "\n\tdominoID " + exercise.getDominoID() +
        "\n\tnpID     " + exercise.getOldID() +
        "\n\tex id    " + exercise.getID() +
        "\n\teng      '" + exercise.getEnglish() + "'" +
        "\n\tfl       '" + exercise.getForeignLanguage() + "'" +
        "\n\tcontext  " + isContext);

    exercise.setPredef(true);

    return exercise;
  }
}
