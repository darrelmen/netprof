/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
  private final IUserExerciseDAO userExerciseDAO;

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

    Map<Integer, Integer> dominoIDToExID = userExerciseDAO.getDominoIDToExID(projid);
    String unitName = projectInfo.getUnitName();
    String chapterName = projectInfo.getChapterName();

    logger.info("readExercises for project " + projid +
        "\n\tunit    " + unitName +
        "\n\tchapter " + chapterName);

    List<CommonExercise> addedCommonExercises = getAddedCommonExercises(
        projid,
        projectInfo.getCreatorID(),
        unitName,
        chapterName,

        importDocs, dominoIDToExID
    );

    List<CommonExercise> changedCommonExercises =
        getChangedCommonExercises(
            projid,
            projectInfo.getCreatorID(),
            unitName,
            chapterName,

            importDocs, dominoIDToExID
        );


    return new ImportInfo(projectInfo,
        addedCommonExercises,
        changedCommonExercises,
        importDocs.getDeleted2(),
        importDocs.getDeletedNPIDs()
        /*,
        importDocs.getNpidToDominoID()*/
    );
  }

  @NotNull
  private List<CommonExercise> getAddedCommonExercises(int projid, int creator,
                                                       String unitName, String chapterName,
                                                       DominoImport.ChangedAndDeleted changedAndDeleted, Map<Integer, Integer> dominoToExID) {
    return getExerciseFromImport(projid, creator, unitName, chapterName, changedAndDeleted.getAdded(), dominoToExID);
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
                                                         DominoImport.ChangedAndDeleted changedAndDeleted, Map<Integer, Integer> dominoToExID) {
    return getExerciseFromImport(projid, creator, unitName, chapterName, changedAndDeleted.getChanged(), dominoToExID);
  }

  @NotNull
  private List<CommonExercise> getExerciseFromImport(int projid, int creator, String unitName, String chapterName,
                                                     List<ImportDoc> changed, Map<Integer, Integer> dominoToExID) {
    List<CommonExercise> exercises = new ArrayList<>(changed.size());

    changed.forEach(docObj -> exercises.add(getExerciseFromVocab(projid,
        creator,
        unitName, chapterName,
        docObj.getDocID(),
        docObj.getTimestamp(),
        docObj.getVocabularyItem(),
        dominoToExID
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
   * @param dominoToExID
   * @return
   * @see #getChangedCommonExercises
   */
  private Exercise getExerciseFromVocab(int projid,
                                        int creator,
                                        String unitName,
                                        String chapterName,
                                        int docID,
                                        long time,
                                        VocabularyItem vocabularyItem,
                                        Map<Integer, Integer> dominoToExID) {
//    logger.info("getExerciseFromVocab ex for doc " + docID + " term " + vocabularyItem.getTerm());
    //SlickExercise byDominoID = userExerciseDAO.getByDominoID(docID);
    Integer byDominoID = dominoToExID.get(docID);
    int exID = -1;
    if (byDominoID != null) {
      exID = byDominoID;
    }
    String npID = getNPId(vocabularyItem);
    Exercise ex = getExerciseFromVocabularyItem(projid, docID, npID, vocabularyItem, creator, time, exID);
    addAttributes(unitName, chapterName, vocabularyItem, ex);
    logger.info("getExerciseFromVocab ex for netprof id " + npID + " unit and chapter = " + ex.getUnitToValue());
//        logger.info("Got " + ex.getUnitToValue());
    addContextSentences(projid, creator, docID, npID, vocabularyItem.getSamples(), ex);

    return ex;
  }


  private void addAttributes(String unitName, String chapterName, MetadataComponentBase vocabularyItem, Exercise ex) {
    List<IMetadataField> metadataFields = vocabularyItem.getMetadataFields();
    Set<String> skipped = new HashSet<>();
    for (IMetadataField field : metadataFields) {
      String name = field.getName();
      String displayValue = field.getDisplayValue();

      boolean isNPID = name.equals(V_NP_ID);
      if (name.startsWith(PREFIX) && !isNPID) {
        name = name.substring(2);
//        logger.info("addAttributes for ex " + ex.getID() + " unit " + unitName + " chapter " + chapterName +
//            "\n\tfield " + name + " = " + displayValue);

        if (!addAttribute(unitName, chapterName, name, displayValue, ex)) {
          skipped.add(name + "=" + displayValue);
        }
      }
    }
    if (!skipped.isEmpty()) {
      logger.warn("skipped vocab attributes " + skipped + " for " + ex.getID() + " old " + ex.getOldID() + " domino " +ex.getDominoID());
    }
  }

  private boolean addAttribute(String unitName, String chapterName, String name, String displayValue, Exercise ex) {
    if (name.equals(UNIT)) {
      return ex.addUnitToValue(unitName, displayValue);
    } else if (name.equals(CHAPTER)) {
      return ex.addUnitToValue(chapterName, displayValue);
    } else {
      if (!displayValue.trim().isEmpty()) {
//            logger.info("findOrAddAttribute : for " + ex.getID() + " adding " + name + " = " + displayValue);
        ex.addAttribute(new ExerciseAttribute(getNormName(name), displayValue));
        return true;
      } else {
        return false;
      }
    }
  }

  private String getNormName(String name) {
    if (name.equalsIgnoreCase(SectionHelper.SUBTOPIC_LC)) {
      return Facet.SUB_TOPIC.getName();
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
   * @see #getExerciseFromVocab(int, int, String, String, int, long, VocabularyItem, Map)
   */
  private void addContextSentences(int projid,
                                   int creator,
                                   int docID,
                                   String npID,
                                   IDocumentComposite samples,
                                   Exercise parentExercise) {
    for (IDocumentComponent comp : samples.getComponents()) {
      SampleSentence sample = (SampleSentence) comp;
      String contextNPID = npID.equalsIgnoreCase(UNKNOWN) ? UNKNOWN : npID + "_" + sample.getNum();

      String sentenceVal = sample.getSentenceVal();

/*      logger.info("addContextSentences : context" +
          "\n\timport id " + docID +
          "\n\tnpID      " + contextNPID +
          "\n\tsentence  " + sentenceVal);*/

      if (!sentenceVal.trim().isEmpty()) {
        int parentDominoID = parentExercise.getDominoID();
        Map<String, String> unitToValue = parentExercise.getUnitToValue();

        Exercise context =
            getContextExercise(projid, -1, creator, docID, sample, contextNPID, sentenceVal, parentDominoID, unitToValue);

//        logger.info("addContextSentences : parent ex id " + parentExercise.getID() + " dom " + parentDominoID);

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
  //  context.setParentDominoID(parentDominoID);
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
