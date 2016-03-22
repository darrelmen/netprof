/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.custom;

import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserExercise extends AudioExercise implements CombinedMutableUserExercise, CommonAnnotatable {
  public static final String CUSTOM_PREFIX = "Custom_";
  private long uniqueID = -1; //set by database

  private long creator;
  private boolean isPredef;
  private boolean isOverride;
  private long modifiedTimestamp = 0;
  private List<CorrectAndScore> scores;
  private float avgScore;
  private transient Collection<String> refSentences;
  private transient List<String> firstPron = new ArrayList<String>();

  private Collection<CommonExercise> directlyRelated = new ArrayList<>();
  private Collection<CommonExercise> mentions = new ArrayList<>();

  public UserExercise() {
  }  // just for serialization

  /**
   * Tooltip is the english phrase, but if it's empty, use the foreign language.
   *
   * @param uniqueID
   * @param exerciseID
   * @param creator
   * @param english
   * @param foreignLanguage
   * @param transliteration
   * @see mitll.langtest.client.custom.dialog.EditItem#createNewItem(long)
   * @see mitll.langtest.client.custom.dialog.EditItem#getNewItem
   */
  public UserExercise(long uniqueID, String exerciseID, long creator, String english, String foreignLanguage,
                      String transliteration) {
    super(exerciseID);
    this.creator = creator;
    this.uniqueID = uniqueID;
    this.english = english;
    this.foreignLanguage = foreignLanguage;
    this.transliteration = transliteration;
    isPredef = checkPredef();//!exerciseID.startsWith(CUSTOM_PREFIX);
  }

  /**
   * @param uniqueID
   * @param exerciseID
   * @param creator
   * @param english
   * @param foreignLanguage
   * @param transliteration
   * @param context
   * @param contextTranslation
   * @param isOverride
   * @param modifiedTimestamp
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#getUserExercises
   */
  public UserExercise(long uniqueID, String exerciseID, long creator, String english, String foreignLanguage,
                      String transliteration, String context, String contextTranslation,
                      boolean isOverride,
                      Map<String, String> unitToValue, long modifiedTimestamp
  ) {
    this(uniqueID, exerciseID, creator, english, foreignLanguage, transliteration);
    setUnitToValue(unitToValue);
    this.isOverride = isOverride;
    this.modifiedTimestamp = modifiedTimestamp;
    addContext(context, contextTranslation);
  }

  /**
   * @param exercise
   * @param creatorID
   * @see FlexListLayout#getFactory(PagingExerciseList)
   */
  public <T extends CommonExercise> UserExercise(T exercise, long creatorID) {
    super(exercise.getID());
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
    for (CommonExercise contextEx : exercise.getMentions()) {
      addMentionedContext(contextEx);
    }
    copyAudio(exercise);
    this.creator = creatorID;
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

  @Override
  public long getCreator() {
    return creator;
  }

  public void setCreator(long id) {
    creator = id;
  }

  /**
   * @param uniqueID
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#add
   */
  public void setUniqueID(long uniqueID) {
    this.uniqueID = uniqueID;
  }

  public long getUniqueID() {
    return uniqueID;
  }

  public void setTransliteration(String transliteration) {
    this.transliteration = transliteration;
  }

  protected void addContext(String context, String contextTranslation) {
    if (context != null && !context.isEmpty()) {
      UserExercise contextExercise = new UserExercise(-1, "c" + id, getCreator(), contextTranslation, english, "");
      addContextExercise(contextExercise);
    }
  }

  @Override
  public boolean isPredefined() {
    return isPredef;
  }

  protected void addContextExercise(CommonExercise contextExercise) {
    directlyRelated.add(contextExercise);
  }

  public void addMentionedContext(CommonExercise exercise) {
    mentions.add(exercise);
  }

  public boolean hasContext() {
    return !getDirectlyRelated().isEmpty();
  }

  public Collection<CommonExercise> getDirectlyRelated() {
    return directlyRelated;
  }

  public Collection<CommonExercise> getMentions() {
    return mentions;
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
    return this;
  }

  @Override
  public CommonAnnotatable getCommonAnnotatable() {
    return this;
  }

  /**
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getCreateButton(UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, com.github.gwtbootstrap.client.ui.ControlGroup)
   */
  public boolean checkPredef() {
    return !getID().startsWith(CUSTOM_PREFIX);
  }

  public List<CorrectAndScore> getScores() {
    return scores;
  }

  @Override
  public void setScores(List<CorrectAndScore> scores) {
    this.scores = scores;
  }

  public float getAvgScore() {
    return avgScore;
  }

  @Override
  public List<String> getFirstPron() {
    return firstPron;
  }

  @Override
  public void setBagOfPhones(Set<String> bagOfPhones) {
  }

  @Override
  public void setFirstPron(List<String> phones) {
    this.firstPron = phones;
  }

  @Override
  public void setRefSentences(Collection<String> orDefault) {
    this.refSentences = orDefault;
  }

  @Override
  public String getRefAudioIndex() {
    return "";
  }

  public void setAvgScore(float avgScore) {
    this.avgScore = avgScore;
  }

  public String toString() {
    return "UserExercise" +
        " #" + uniqueID + "/" + getID() +
        (isPredef ? " <Predef>" : " <User>") +
        (isOverride ? " <Override>" : "") +
        " creator " + getCreator() +
        " : English '" + getEnglish() + "', " +
        "foreign language '" + getForeignLanguage() + "'" +
        " transliteation '" + getTransliteration() + "' " +
        "meaning '" + getMeaning() + "' " +
        "context '" + getDirectlyRelated() + "' " +
        "' audio attr (" + getAudioAttributes().size() +
        ")" +
        " unit/lesson " + getUnitToValue() +
        " state " + getState() + "/" + getSecondState() +
        " modified " + new Date(modifiedTimestamp);
  }

  public long getUpdateTime() {
    return modifiedTimestamp;
  }
}
