/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.shared.exercise;

import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.server.database.userexercise.UserExerciseDAO;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.npdata.dao.SlickExercise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static mitll.langtest.server.database.user.BaseUserDAO.UNDEFINED_USER;

/**
 * Representation of a individual item of work the user sees.  Could be a pronunciation exercise or a question(s)
 * based on a prompt.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/8/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Exercise extends AudioExercise implements CommonExercise,
    MutableExercise, MutableAudioExercise, MutableAnnotationExercise, CommonAnnotatable {
  private transient Collection<String> refSentences = new ArrayList<String>();
  private List<CorrectAndScore> scores;
  private float avgScore;

  private transient List<String> firstPron = new ArrayList<String>();
  private long updateTime = 0;

  private Collection<CommonExercise> directlyRelated = new ArrayList<>();
  private Collection<CommonExercise> mentions = new ArrayList<>();
  private boolean safeToDecode;

  private int creator = UNDEFINED_USER;
  private boolean isPredef;
  private boolean isOverride;

  // for serialization
  public Exercise() {
  }

  /**
   * @param id
   * @param altcontext
   * @param projectid
   * @param updateTime
   * @paramx content
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
   */
  public Exercise(String id,
                  String context,
                  String altcontext,
                  String contextTranslation,
                  String meaning,
                  int projectid,
                  long updateTime) {
    super(id, -1, projectid);
    this.meaning = meaning;
    this.updateTime = updateTime;
    addContext(context, altcontext, contextTranslation);
  }


  /**
   * @param exid
   * @param creator
   * @param english
   * @param projectid
   * @see EditItem#getNewItem
   */
  public Exercise(int exid, int creator, String english, int projectid, boolean isPredef) {
    this.id = exid;
    this.creator = creator;
    this.english = english;
    this.projectid = projectid;
    this.isPredef = isPredef;
  }

  /**
   * @param id
   * @param context
   * @param altcontext
   * @param contextTranslation
   * @param projectid
   * @Deprecated - use related exercise join table
   */
  @Deprecated
  private Exercise(String id, String context, String altcontext, String contextTranslation, int projectid) {
    super(id, -1, projectid);
    this.foreignLanguage = context;
    this.altfl = altcontext;
    this.english = contextTranslation;
  }

  /**
   * @param id
   * @param englishSentence
   * @param foreignLanguage
   * @param meaning
   * @param transliteration
   * @param dominoID
   * @param projectid
   * @see mitll.langtest.server.database.exercise.JSONURLExerciseDAO#toExercise
   * @see mitll.langtest.server.json.JsonExport#toExercise
   */
  public Exercise(String id,
                  String englishSentence,
                  String foreignLanguage,
                  String meaning,
                  String transliteration,
                  int dominoID,
                  int projectid) {
    super(id, -1, projectid);
    setEnglishSentence(englishSentence);
    this.meaning = meaning;
    setForeignLanguage(foreignLanguage);
    setTransliteration(transliteration);
    this.dominoID = dominoID;
  }

  /**
   * @param exid
   * @param oldid
   * @param creator
   * @param englishSentence
   * @param foreignLanguage
   * @param meaning
   * @param transliteration
   * @param projectid
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#fromSlickToExercise(SlickExercise, Collection, SectionHelper)
   */
  public Exercise(int exid,
                  String oldid,
                  int creator,
                  String englishSentence,
                  String foreignLanguage,
                  String meaning,
                  String transliteration,
                  int projectid) {
    super(oldid, exid, projectid);
    this.creator = creator;
    setEnglishSentence(englishSentence);
    this.meaning = meaning;
    setForeignLanguage(foreignLanguage);
    setTransliteration(transliteration);
  }

/*  public CommonShell getShell() {
    ExerciseShell exerciseShell = new ExerciseShell(getID(), english, meaning, foreignLanguage, transliteration, context, contextTranslation, displayID);
//    exerciseShell.setState(getState());
//    exerciseShell.setSecondState(getSecondState());
    return exerciseShell;
  }*/

  /**
   * @param uniqueID
   * @param exerciseID
   * @param creator
   * @param english
   * @param foreignLanguage
   * @param transliteration
   * @param isOverride
   * @param modifiedTimestamp
   * @param projectid
   * @see UserExerciseDAO#getUserExercise
   */
  public Exercise(int uniqueID,
                  String exerciseID,
                  int creator,
                  String english,
                  String foreignLanguage,
                  String transliteration,
                  boolean isOverride,
                  Map<String, String> unitToValue,
                  long modifiedTimestamp,
                  int projectid) {
    this(uniqueID, exerciseID, creator, english, foreignLanguage, "", transliteration, projectid);
    setUnitToValue(unitToValue);
    this.isOverride = isOverride;
    this.updateTime = modifiedTimestamp;
  }

  /**
   * @param exercise
   * @see FlexListLayout#getFactory(PagingExerciseList)
   */
  public <T extends CommonExercise> Exercise(T exercise) {
    super(exercise.getOldID(), exercise.getID(), exercise.getProjectID());
    this.isPredef = true;
    this.english = exercise.getEnglish();
    this.foreignLanguage = exercise.getForeignLanguage();
    this.transliteration = exercise.getTransliteration();
    this.meaning = exercise.getMeaning();

    setFieldToAnnotation(exercise.getFieldToAnnotation());
    setUnitToValue(exercise.getUnitToValue());
    setState(exercise.getState());
    setSecondState(exercise.getSecondState());

    for (CommonExercise contextEx : exercise.getDirectlyRelated()) {
      addContextExercise(contextEx);
    }
//    for (CommonExercise contextEx : exercise.getMentions()) {
//      addMentionedContext(contextEx);
//    }
    copyAudio(exercise);
    this.creator = exercise.getCreator();
  }

  private void copyAudio(AudioRefExercise exercise) {
    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      addAudio(audioAttribute);
    }
  }

  @Override
  public Collection<String> getRefSentences() {
    return refSentences;
  }

  /**
   * @param sentenceRefs
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
   */
  @Override
  public void setRefSentences(Collection<String> sentenceRefs) {
    this.refSentences = sentenceRefs;
  }

  @Override
  public boolean isPredefined() {
    return isPredef;
  }

  /**
   * @return
   * @see UserDAO#DEFAULT_USER_ID
   */
  @Override
  public int getCreator() {
    return creator;
  }

  @Override
  public MutableExercise getMutable() {
    return this;
  }

  @Override
  public MutableAudioExercise getMutableAudio() {
    return this;
  }

  @Override
  public MutableAnnotationExercise getMutableAnnotation() {
    return this;
  }

  public CommonAnnotatable getCommonAnnotatable() {
    return this;
  }

  /**
   * @param context
   * @param altcontext
   * @param contextTranslation
   * @see mitll.langtest.server.database.exercise.JSONURLExerciseDAO#addContextSentences
   */
  public void addContext(String context, String altcontext, String contextTranslation) {
    if (!context.isEmpty()) {
      Exercise contextExercise = new Exercise("c" + getID(), context, altcontext, contextTranslation, getProjectID());
      contextExercise.setUpdateTime(getUpdateTime());
      contextExercise.setUnitToValue(getUnitToValue());
      addContextExercise(contextExercise);
    }
  }

  @Override
  public void setTransliteration(String transliteration) {
    this.transliteration = transliteration;
  }

  /**
   * @param englishSentence
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
   */
  public void setEnglishSentence(String englishSentence) {
    this.english = englishSentence;
  }

  public List<CorrectAndScore> getScores() {
    return scores;
  }

  /**
   * @param scores
   * @see mitll.langtest.server.database.result.BaseResultDAO#attachScoreHistory(int, CommonExercise, boolean)
   */
  @Override
  public void setScores(List<CorrectAndScore> scores) {
    this.scores = scores;
  }

  public float getAvgScore() {
    return avgScore;
  }

  /**
   * @param avgScore
   * @see ResultDAO#attachScoreHistory
   */
  public void setAvgScore(float avgScore) {
    this.avgScore = avgScore;
  }

  /**
   * @paramx bagOfPhones
   * @see mitll.langtest.server.audio.AudioFileHelper#countPhones
   */
//  @Override
//  public void setBagOfPhones(Set<String> bagOfPhones) {
//  }
  @Override
  public List<String> getFirstPron() {
    return firstPron;
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#countPhones
   * @param firstPron
   */
  @Override
  public void setFirstPron(List<String> firstPron) {
    this.firstPron = firstPron;
  }

  /**
   * @param updateTime
   * @see mitll.langtest.server.database.exercise.JSONURLExerciseDAO#toExercise
   */
  public void setUpdateTime(long updateTime) {
    this.updateTime = updateTime;
  }

  public long getUpdateTime() {
    return updateTime;
  }

  /**
   * @param contextExercise
   * @see #addContext(String, String, String)
   */
  public void addContextExercise(CommonExercise contextExercise) {
    directlyRelated.add(contextExercise);
  }

/*
  public void addMentionedContext(CommonExercise exercise) {
    mentions.add(exercise);
  }
*/

  public boolean hasContext() {
    return !getDirectlyRelated().isEmpty();
  }

  public String getContext() {
    return hasContext() ? getDirectlyRelated().iterator().next().getForeignLanguage() : "";
  }

  public Collection<CommonExercise> getDirectlyRelated() {
    return directlyRelated;
  }

/*
  public Collection<CommonExercise> getMentions() {
    return mentions;
  }
*/

  public boolean isSafeToDecode() {
    return safeToDecode;
  }

  public void setSafeToDecode(boolean safeToDecode) {
    this.safeToDecode = safeToDecode;
  }

  public boolean isOverride() {
    return isOverride;
  }

  public void setCreator(int creator) {
    this.creator = creator;
  }

  public void setID(int uniqueID) {
    this.id = uniqueID;
  }

  public String toString() {
    Collection<AudioAttribute> audioAttributes1 = getAudioAttributes();

    // warn about attr that have no user
    StringBuilder builder = new StringBuilder();
    for (AudioAttribute attr : audioAttributes1) {
      if (attr.getUser() == null) {
        builder.append("\t").append(attr.toString()).append("\n");
      }
    }

    return "Exercise " +
        //Integer.toHexString(hashCode()) +
        //" " +
        getID() +
        "/" + getDominoID() +
        " old '" + getOldID() +
        "'" +
        " project " + projectid +
        " english '" + getEnglish() +
        "'/'" + getForeignLanguage() + "' " +
        (getAltFL().isEmpty() ? "" : getAltFL()) +
        "meaning '" + getMeaning() +
        "' transliteration '" + getTransliteration() +
        "' context " + getDirectlyRelated() +
        " audio count = " + audioAttributes1.size() +
        (builder.toString().isEmpty() ? "" : " \n\tmissing user audio " + builder.toString()) +
        " unit->lesson " + getUnitToValue();
  }
}
