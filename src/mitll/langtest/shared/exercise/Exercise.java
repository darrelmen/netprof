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

import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import net.sf.json.JSONObject;

import java.util.*;

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
//  private transient String refAudioIndex;
  private transient Collection<String> refSentences = new ArrayList<String>();
  private List<CorrectAndScore> scores;
  private float avgScore;

  private transient List<String> firstPron = new ArrayList<String>();
  private long updateTime = 0;

  private Collection<CommonExercise> directlyRelated = new ArrayList<>();
  private Collection<CommonExercise> mentions = new ArrayList<>();
  private boolean safeToDecode;

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
                  String context, String altcontext, String contextTranslation,
                  String meaning, String refAudioIndex,
                  int projectid, long updateTime) {
    super(id, -1, projectid);
    this.meaning = meaning;
    //this.refAudioIndex = refAudioIndex;
    this.updateTime = updateTime;
    addContext(context, altcontext, contextTranslation);
  }

  /**
   * @Deprecated - use related exercise join table
   * @param id
   * @param context
   * @param altcontext
   * @param contextTranslation
   * @param projectid
   */
  @Deprecated public Exercise(String id, String context, String altcontext, String contextTranslation, int projectid) {
    super(id, -1, projectid);
    this.foreignLanguage = context;
    this.altfl           = altcontext;
    this.english         = contextTranslation;
  }

  /**
   * @param id
   * @param englishSentence
   * @param foreignLanguage
   * @param meaning
   * @param transliteration
   * @param dominoID
   * @param projectid
   * @see mitll.langtest.server.database.exercise.JSONURLExerciseDAO#toExercise(JSONObject)
   * @see mitll.langtest.server.json.JsonExport#toExercise(JSONObject, Collection)
   */
  public Exercise(String id,
                  String englishSentence,
                  String foreignLanguage,
                  String meaning,
                  String transliteration,
                  int dominoID, int projectid) {
    super(id, -1, projectid);
    setEnglishSentence(englishSentence);
    this.meaning = meaning;
    setForeignLanguage(foreignLanguage);
    setTransliteration(transliteration);
    this.dominoID = dominoID;
  }

  public Exercise(int exid,
                  String oldid,
                  String englishSentence,
                  String foreignLanguage,
                  String meaning,
                  String transliteration, int projectid) {
    super(oldid, exid, projectid);
    setEnglishSentence(englishSentence);
    this.meaning = meaning;
    setForeignLanguage(foreignLanguage);
    setTransliteration(transliteration);
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

  /**
   * @return
   * @deprecated we should convert these into audio table entries
   */
/*  @Override
  public String getRefAudioIndex() {
    return refAudioIndex;
  }*/

  @Override
  public boolean isPredefined() {
    return true;
  }

  /**
   * @return
   * @see UserDAO#DEFAULT_USER_ID
   */
  @Override
  public int getCreator() {
    return UNDEFINED_USER;
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

  public CommonAnnotatable getCommonAnnotatable() {
    return this;
  }

  /**
   * @see mitll.langtest.server.database.exercise.JSONURLExerciseDAO#addContextSentences(JSONObject, Exercise)
   * @param context
   * @param altcontext
   * @param contextTranslation
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
   * @see mitll.langtest.server.database.result.BaseResultDAO#attachScoreHistory(int, CommonExercise, boolean)
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

  /**
   * @see #addContext(String, String, String)
   * @param contextExercise
   */
  public void addContextExercise(CommonExercise contextExercise) {
    directlyRelated.add(contextExercise);
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
        " " + getID() +
        "/" + getDominoID() +
        " old '" + getOldID() +
        "' english '" + getEnglish() +
        "'/'" + getForeignLanguage() + "' " +
        (getAltFL().isEmpty() ? "" : getAltFL())+
        "meaning '" + getMeaning() +
        "' transliteration '" + getTransliteration() +
        "' context " + getDirectlyRelated() +
        " audio count = " + audioAttributes1.size() +
        (builder.toString().isEmpty() ? "" : " \n\tmissing user audio " + builder.toString()) +
        " unit->lesson " + getUnitToValue();
  }

/*
  public void addMentionedContext(CommonExercise exercise) {
    mentions.add(exercise);
  }
*/

  public boolean hasContext() {
    return !getDirectlyRelated().isEmpty();
  }

  public String getContext() { return getDirectlyRelated().iterator().next().getForeignLanguage(); }

  public Collection<CommonExercise> getDirectlyRelated() {
    return directlyRelated;
  }

  public Collection<CommonExercise> getMentions() {
    return mentions;
  }

  public boolean isSafeToDecode() {
    return safeToDecode;
  }

  public void setSafeToDecode(boolean safeToDecode) {
    this.safeToDecode = safeToDecode;
  }
}
