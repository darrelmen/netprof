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

package mitll.langtest.shared.custom;

import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/27/13
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
    this.englishSentence = english;
    this.foreignLanguage = foreignLanguage;
    this.transliteration = transliteration;
    isPredef = !exerciseID.startsWith(CUSTOM_PREFIX);
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
  public UserExercise(long uniqueID, String exerciseID, long creator,
                      String english,
                      String foreignLanguage,
                      String transliteration,
                      String context, String contextTranslation,
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
   * @param creatorID
   * @see FlexListLayout#getFactory(PagingExerciseList)
   */
  public <T extends CommonShell & AnnotationExercise & AudioRefExercise> UserExercise(T exercise, long creatorID) {
    super(exercise.getID());
    this.isPredef = true;
    this.englishSentence = exercise.getEnglish();
    this.foreignLanguage = exercise.getForeignLanguage();
    this.transliteration = exercise.getTransliteration();
    this.meaning = exercise.getMeaning();

    setFieldToAnnotation(exercise.getFieldToAnnotation());
    setUnitToValue(exercise.getUnitToValue());
    setState(exercise.getState());
    setSecondState(exercise.getSecondState());
    setContext(exercise.getContext());
    setContextTranslation(exercise.getContextTranslation());
    copyAudio(exercise);
    this.creator = creatorID;
  }

  public CommonShell getShell() {
    return new ExerciseShell(getID(), englishSentence, meaning, foreignLanguage, transliteration, context, contextTranslation, displayID);
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

  public void setContext(String context) {
    this.context = context;
  }

  public void setContextTranslation(String contextTranslation) {
    this.contextTranslation = contextTranslation;
  }

  @Override
  public boolean isPredefined() {
    return isPredef;
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
        "context '" + getContext() + "' " +
        "contextTranslation '" + getContextTranslation() + "' " +
        "' audio attr (" + getAudioAttributes().size() +
        ")" +
        // " :" + getAudioAttributes() +
        " unit/lesson " + getUnitToValue() +
        " state " + getState() + "/" + getSecondState() +
        " modified " + new Date(modifiedTimestamp);
  }

  public long getUpdateTime() {
    return modifiedTimestamp;
  }
}
