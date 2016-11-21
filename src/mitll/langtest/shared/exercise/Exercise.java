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

import mitll.langtest.server.database.ResultDAO;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Representation of a individual item of work the user sees.  Could be a pronunciation exercise or a question(s)
 * based on a prompt.
 * <p>
 * TODO : consider subclass for pronunciation exercises?
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/8/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Exercise extends AudioExercise implements CommonExercise,
    MutableExercise, MutableAudioExercise, MutableAnnotationExercise, CommonAnnotatable {
  private transient String refAudioIndex;
  private transient Collection<String> refSentences = new ArrayList<String>();
  private List<CorrectAndScore> scores;
  private float avgScore;

  private transient List<String> firstPron = new ArrayList<String>();
  private long updateTime = 0;

  public Exercise() {}

  /**
   * @param id
   * @param context
   * @paramx content
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
   */
  public Exercise(String id, String context, String contextTranslation, String meaning, String refAudioIndex) {
    super(id);
    this.context = context;
    this.contextTranslation = contextTranslation;
    this.meaning = meaning;
    this.refAudioIndex = refAudioIndex;
  }

  public Exercise(String id,
                  String englishSentence,
                  String foreignLanguage,
                  String meaning,
                  String transliteration,
                  String context,
                  String contextTranslation,
                  String displayID) {
    super(id);
    setEnglishSentence(englishSentence);
    this.meaning = meaning;
    setForeignLanguage(foreignLanguage);
    setTransliteration(transliteration);
    this.context = context;
    this.contextTranslation = contextTranslation;
    this.displayID = displayID;
  }

  @Override
  public Collection<String> getRefSentences() {
    return refSentences;
  }

  public CommonShell getShell() {
    return new ExerciseShell(getID(), englishSentence, meaning, foreignLanguage, transliteration, context, contextTranslation, displayID);
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
  public void setContext(String context) {
    this.context = context;
  }

  @Override
  public void setContextTranslation(String context) {
    this.contextTranslation = context;
  }

/*  @Override
  public String getRefAudioIndex() {
    return refAudioIndex;
  }*/

  @Override
  public boolean isPredefined() {
    return true;
  }

  /**
   * @see mitll.langtest.server.database.UserDAO#DEFAULT_USER_ID
   * @return
   */
  @Override
  public long getCreator() {
    return -5;
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

  @Override
  public CombinedMutableUserExercise getCombinedMutableUserExercise() {
    new Exception("shouldn't call this method.");
    return null;
  }

  public CommonAnnotatable getCommonAnnotatable() { return this; }

  @Override
  public void setTransliteration(String transliteration) {
    this.transliteration = transliteration;
  }

  /**
   * @param englishSentence
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
   */
  public void setEnglishSentence(String englishSentence) {
    this.englishSentence = englishSentence;
  }

  public List<CorrectAndScore> getScores() {
    return scores;
  }

  /**
   * @see ResultDAO#attachScoreHistory
   * @param scores
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
   * @param bagOfPhones
   * @see mitll.langtest.server.audio.AudioFileHelper#countPhones
   */
  @Override
  public void setBagOfPhones(Set<String> bagOfPhones) {
  }

  @Override
  public List<String> getFirstPron() {
    return firstPron;
  }

  @Override
  public void setFirstPron(List<String> firstPron) {
    this.firstPron = firstPron;
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
        //Integer.toHexString(hashCode()) + " " +
        getID() + "/"+getDisplayID()+
        " english '" + getEnglish() +
        "'/'" + getForeignLanguage() + "' " +
        "meaning '" + getMeaning() +
        "' transliteration '" + getTransliteration() +
        "' context " + getContext() + "/" + getContextTranslation() +
        " audio count = " + audioAttributes1.size() +
        (builder.toString().isEmpty() ? "" : " \n\tmissing user audio " + builder.toString()) +
        " unit->lesson " + getUnitToValue();
  }

  /**
   * @see mitll.langtest.server.database.exercise.JSONURLExerciseDAO#toExercise
   * @param updateTime
   */
  public void setUpdateTime(long updateTime) {
    this.updateTime = updateTime;
  }

  public long getUpdateTime() {
    return updateTime;
  }
}
