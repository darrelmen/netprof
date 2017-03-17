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
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.userexercise.UserExerciseDAO;
import mitll.langtest.shared.flashcard.CorrectAndScore;

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
  @Deprecated
  protected String oldid = "";
  private transient Collection<String> refSentences = new ArrayList<String>();
  private List<CorrectAndScore> scores;

  private transient List<String> firstPron = new ArrayList<>();
  private int numPhones;
  private long updateTime = 0;

  private Collection<CommonExercise> directlyRelated = new ArrayList<>();
  //  private Collection<CommonExercise> mentions = new ArrayList<>();
  private boolean safeToDecode;
  private transient long safeToDecodeLastChecked;

  private int creator = UNDEFINED_USER;

  private boolean isPredef;
  private boolean isOverride;

  /**
   * TODO : why do we need to carry this around?
   */
  protected Map<String, String> unitToValue = new HashMap<>(3);

  protected String transliteration = "";

  private int dominoID = -1;

  private transient List<ExerciseAttribute> attributes;

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
    super(-1, projectid);
    this.oldid = id;
    this.meaning = meaning;
    this.updateTime = updateTime;
    this.isPredef = true;
    addContext(context, altcontext, contextTranslation);
    //   this.dominoID = dominoID;
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
   * @see #addContext
   */
  @Deprecated
  private Exercise(String id, String context, String altcontext, String contextTranslation, int projectid) {
    super(-1, projectid);
    this.foreignLanguage = context;
    this.altfl = altcontext;
    this.english = contextTranslation;
    this.oldid = id;
  }


  /**
   * @param exid
   * @param oldid
   * @param creator
   * @param englishSentence
   * @param foreignLanguage
   * @param altFL
   * @param meaning
   * @param transliteration
   * @param projectid
   * @param candecode
   * @param lastChecked
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#fromSlickToExercise
   */
  public Exercise(int exid,
                  String oldid,
                  int creator,
                  String englishSentence,
                  String foreignLanguage,
                  String altFL,
                  String meaning,
                  String transliteration,
                  int projectid,
                  boolean candecode,
                  long lastChecked) {
    super(exid, projectid);
    this.oldid = oldid;
    this.creator = creator;
    setEnglishSentence(englishSentence);
    this.meaning = meaning;
    setForeignLanguage(foreignLanguage);
    setTransliteration(transliteration);
    setAltFL(altFL);
    this.safeToDecode = candecode;
    safeToDecodeLastChecked = lastChecked;
  }

  /**
   * @param uniqueID
   * @param exerciseID
   * @param creator
   * @param english
   * @param foreignLanguage
   * @param altFL
   * @param transliteration
   * @param isOverride
   * @param modifiedTimestamp
   * @param projectid
   * @param candecode
   * @param lastChecked
   * @see UserExerciseDAO#getUserExercise
   */
  public Exercise(int uniqueID,
                  String exerciseID,
                  int creator,
                  String english,
                  String foreignLanguage,
                  String altFL,
                  String transliteration,
                  boolean isOverride,
                  Map<String, String> unitToValue,
                  long modifiedTimestamp,
                  int projectid, boolean candecode, long lastChecked) {
    this(uniqueID, exerciseID, creator, english, foreignLanguage, altFL, "", transliteration, projectid, candecode, lastChecked);
    setUnitToValue(unitToValue);
    this.isOverride = isOverride;
    this.updateTime = modifiedTimestamp;
    this.safeToDecode = candecode;
  }

  /**
   * @param exercise
   * @see FlexListLayout#getFactory(PagingExerciseList)
   */
  public <T extends CommonExercise> Exercise(T exercise) {
    super(exercise.getID(), exercise.getProjectID());
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
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#addAttributeToExercise(Map, Map, CommonExercise)
   * @param exerciseAttributes
   */
  @Override
  public void setAttributes(List<ExerciseAttribute> exerciseAttributes) {
    this.attributes = exerciseAttributes;
  }

  @Override
  public List<ExerciseAttribute> getAttributes() {
    return attributes;
  }

  /**
   * @param context
   * @param altcontext
   * @param contextTranslation
   * @seex mitll.langtest.server.database.exercise.JSONURLExerciseDAO#addContextSentences
   */
  private void addContext(String context, String altcontext, String contextTranslation) {
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
   * @see mitll.langtest.server.database.result.BaseResultDAO#attachScoreHistory(int, CommonExercise, boolean, String)
   */
  @Override
  public void setScores(List<CorrectAndScore> scores) {
    this.scores = scores;
  }

/*
  public float getAvgScore() {
    return avgScore;
  }
*/

  /**
   * @param avgScore
   * @see ResultDAO#attachScoreHistory
   */
/*
  public void setAvgScore(float avgScore) {
    this.avgScore = avgScore;
  }
*/

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
   * @param firstPron
   * @see mitll.langtest.server.audio.AudioFileHelper#countPhones
   */
  @Override
  public void setFirstPron(List<String> firstPron) {
    this.firstPron = firstPron;
  }

  /**
   * @param updateTime
   * @seex mitll.langtest.server.database.exercise.JSONURLExerciseDAO#toExercise
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

  public String getContextTranslation() {
    return hasContext() ? getDirectlyRelated().iterator().next().getEnglish() : "";
  }

  public Collection<CommonExercise> getDirectlyRelated() {
    return directlyRelated;
  }

/*
  public Collection<CommonExercise> getMentions() {
    return mentions;
  }
*/

  /**
   * @return
   * @see mitll.langtest.client.custom.exercise.ContextCommentNPFExercise#getItemContent
   */
  public boolean isSafeToDecode() {
    return safeToDecode;
  }

  public void setSafeToDecode(boolean safeToDecode) {
    this.safeToDecode = safeToDecode;
  }

  /**
   * @return
   * @deprecated we don't do overrides any more
   */
  public boolean isOverride() {
    return isOverride;
  }

  public void setCreator(int creator) {
    this.creator = creator;
  }

  public void setID(int uniqueID) {
    this.id = uniqueID;
  }

  @Override
  public boolean equals(Object other) {
    boolean checkOld = !getOldID().isEmpty();
    return other instanceof Exercise &&
        (checkOld && getOldID().equals(((Exercise) other).getOldID()) ||
            (getID() == ((ExerciseShell) other).getID())
        );
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
        " unit->lesson " + getUnitToValue() +
        " attr " + getAttributes();
  }

  @Deprecated
  public String getOldID() {
    return oldid;
  }

  /**
   * @param id
   * @see IUserExerciseDAO#add(CommonExercise, boolean, boolean)
   */
  @Deprecated
  public void setOldID(String id) {
    this.oldid = id;
  }

  /**
   * @return
   */
  public Map<String, String> getUnitToValue() {
    return unitToValue;
  }

  /**
   * @param unit
   * @param value
   * @see mitll.langtest.server.database.exercise.SectionHelper#addExerciseToLesson
   */
  public void addUnitToValue(String unit, String value) {
    if (value == null) return;
    unitToValue.put(unit, value);
  }

  public void addPair(Pair pair) {
    unitToValue.put(pair.getProperty(), pair.getValue());
  }

  /**
   * @param unitToValue
   * @see mitll.langtest.shared.exercise.Exercise#Exercise
   */
  public void setUnitToValue(Map<String, String> unitToValue) {
    this.unitToValue = unitToValue;
  }

  public void setPairs(List<Pair> pairs) {
    for (Pair pair : pairs) unitToValue.put(pair.getProperty(), pair.getValue());
  }

  @Override
  public String getTransliteration() {
    return transliteration;
  }

  public int getDominoID() {
    return dominoID;
  }

  public long getLastChecked() {
    return safeToDecodeLastChecked;
  }

  public int getNumPhones() {
    return numPhones;
  }

  public void setNumPhones(int numPhones) {
    this.numPhones = numPhones;
  }
}