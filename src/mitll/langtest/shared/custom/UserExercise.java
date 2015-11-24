/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.custom;

import mitll.langtest.shared.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserExercise extends AudioExercise implements CommonUserExercise {
  public static final String CUSTOM_PREFIX = "Custom_";
  private long uniqueID = -1; //set by database

  private String transliteration;
  private String context;
  private String contextTranslation;

  private long creator;
  private boolean isPredef;
  private boolean isOverride;
  private long modifiedTimestamp;
  private List<CorrectAndScore> scores;
  private float avgScore;
  private transient Collection<String> refSentences;
  private transient List<String> firstPron = new ArrayList<String>();
  private static final int MAX_TOOLTIP_LENGTH = 15;

  public UserExercise() {
  }  // just for serialization

  /**
   * @param shell
   * @param creator
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   */
  public UserExercise(CommonShell shell, long creator) {
    super(shell.getID());
    isPredef = !shell.getID().startsWith(CUSTOM_PREFIX);
    this.creator = creator;
  }

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
   * @see mitll.langtest.client.custom.dialog.EditItem#getNewItem() (long)
   */
  public UserExercise(long uniqueID, String exerciseID, long creator, String english, String foreignLanguage,
                      String transliteration) {
    super(exerciseID);
    this.creator = creator;
    this.uniqueID = uniqueID;
    this.englishSentence = english;
    this.foreignLanguage = foreignLanguage;
    this.transliteration = transliteration;
    isPredef = !exerciseID.startsWith(CUSTOM_PREFIX);
    setTooltip();
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
    this.context = context;
    this.contextTranslation = contextTranslation;
  }

  /**
   * @param exercise
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   */
  public UserExercise(CommonExercise exercise) {
    super(exercise.getID());
    this.isPredef = true;
    this.englishSentence = exercise.getEnglish();
    this.foreignLanguage = exercise.getRefSentence();
    this.transliteration = exercise.getTransliteration();
    setTooltip();
    setFieldToAnnotation(exercise.getFieldToAnnotation());
    setUnitToValue(exercise.getUnitToValue());
    setState(exercise.getState());
    setSecondState(exercise.getSecondState());
    setContext(exercise.getContext());
    setContextTranslation(exercise.getContextTranslation());
    copyAudio(exercise);
  }

  private void copyAudio(CommonExercise exercise) {
    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      addAudio(audioAttribute);
    }
  }

  /**
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#addNew
   * @deprecated ideally we shouldn't have to do this
   */
  public Exercise toExercise() {
    Exercise exercise = new Exercise(getID(), getEnglish(), getForeignLanguage(), getTooltip());
    copyFields(exercise);
    copyAudio(exercise);

    return exercise;
  }

  private void copyFields(Exercise imported) {
    AudioAttribute slowSpeed = getSlowSpeed();
    if (slowSpeed != null) {
      imported.addAudio(slowSpeed);
    }
    imported.setEnglishSentence(getEnglish());
    imported.setTranslitSentence(getTransliteration());
    imported.setUnitToValue(getUnitToValue());
    imported.setFieldToAnnotation(getFieldToAnnotation());
    imported.setContext(getContext());
    imported.setContextTranslation(getContextTranslation());
  }

  @Override
  public String getRefSentence() {
    return foreignLanguage;
  }

  @Override
  public Collection<String> getRefSentences() {
    return refSentences;
  }

  @Override
  public String getTransliteration() {
    return transliteration;
  }

  /**
   * Consider how to do this better -- not consistent with Exercise meaning...
   *
   * @return
   */
  @Override
  public String getMeaning() {
    return "";
  }

  /**
   * @return
   * @deprecated - nobody set this
   */
  @Override
  public String getContent() {
    return "";
  }

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseShellsCombined(Collection)
   */
  public CommonShell getShellCombinedTooltip() {
    String combined = getCombinedTooltip();

    return new ExerciseShell(getID(), combined, englishSentence, meaning, foreignLanguage);
  }

  public String getCombinedTooltip() {
    String refSentence = getForeignLanguage();
    if (refSentence.length() > MAX_TOOLTIP_LENGTH) {
      refSentence = refSentence.substring(0, MAX_TOOLTIP_LENGTH);
    }
    // boolean refSentenceEqualsTooltip = getTooltip().trim().equals(getForeignLanguage().trim());
    boolean englishSameAsForeign = getEnglish().trim().equals(getForeignLanguage().trim());
    String combined = englishSameAsForeign ? getEnglish() : getEnglish() + (refSentence.isEmpty() ? "" : " / " + refSentence);
    if (combined.isEmpty()) combined = refSentence;
    return combined;
  }

  public void setTooltip() {
    setTooltip(getCombinedTooltip());
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

  @Override
  public long getUniqueID() {
    return uniqueID;
  }

  /**
   * @param english
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#grabInfoFromFormAndStuffInfoExercise()
   */
  public void setEnglish(String english) {  this.englishSentence = english;  }

  public void setForeignLanguage(String foreignLanguage) {
    this.foreignLanguage = foreignLanguage;
  }

  public void setTransliteration(String transliteration) {
    this.transliteration = transliteration;
  }

  @Override
  public String getContext() {
    return context;
  }

  private void setContext(String context) {
    this.context = context;
  }

  @Override
  public String getContextTranslation() {
    return contextTranslation;
  }

  private void setContextTranslation(String contextTranslation) {
    this.contextTranslation = contextTranslation;
  }

  @Override
  public boolean isPredefined() {
    return isPredef;
  }

  @Override
  public UserExercise toUserExercise() {
    return this;
  }

  /**
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getCreateButton(UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, com.github.gwtbootstrap.client.ui.ControlGroup)
   */
  public boolean checkPredef() {
    return !getID().startsWith(CUSTOM_PREFIX);
  }

  @Override
  public long getModifiedDateTimestamp() {
    return modifiedTimestamp;
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
        "foreign language '" + getForeignLanguage() + "'" + " (" + getTransliteration() + ") " +
        "meaning " + getMeaning() +
        "context '" + getContext() + "' " +
        "contextTranslation '" + getContextTranslation() + "' " +
        "tooltip '" + getTooltip() +
        "' audio attr (" + getAudioAttributes().size() +
        ")" +
        // " :" + getAudioAttributes() +
        " unit/lesson " + getUnitToValue() +
        " state " + getState() + "/" + getSecondState() +
        " modified " + modifiedTimestamp;
  }
}
