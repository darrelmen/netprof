/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import mitll.langtest.server.database.ResultDAO;
import mitll.langtest.shared.flashcard.CorrectAndScore;

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
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Exercise extends AudioExercise implements CommonExercise, MutableExercise, MutableAudioExercise, MutableAnnotationExercise {
  private transient String refAudioIndex;
  private transient Collection<String> refSentences = new ArrayList<String>();
  //private List<String> translitSentences = new ArrayList<String>();
  private List<CorrectAndScore> scores;
  private float avgScore;

  private transient List<String> firstPron = new ArrayList<String>();

  public Exercise() {
  }

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
  public String getRefAudioIndex() {
    return refAudioIndex;
  }

  @Override
  public boolean isPredefined() {
    return true;
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
//    throw new IllegalAccessException("shouldn't call this method.");
    return null;
  }

  public void setForeignLanguage(String foreignLanguage) {
    this.foreignLanguage = foreignLanguage;
  }

  @Override
  public void setTransliteration(String transliteration) {
   setTranslitSentence(transliteration);
  }

  /**
   * @param translitSentence
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise(String, String, String, String, String, boolean)
   */
  public void setTranslitSentence(String translitSentence) {
//    translitSentences.clear();
//    translitSentences.add(translitSentence);
    this.transliteration = translitSentence;
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

  @Override
  public void setEnglish(String english) {
    this.englishSentence = english;
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

    return "Exercise " + getID() + //" content bytes = " + content.length() +
        " english '" + getEnglish() +
        "'/'" + getForeignLanguage() + "' " +
        "meaning '" + getMeaning() +
        "' transliteration '" + getTransliteration() +
        "' context " + getContext() + "/" + getContextTranslation() +
        " audio count = " + audioAttributes1.size() +
        (builder.toString().isEmpty() ? "" : " \n\tmissing user audio " + builder.toString()) +
        " unit->lesson " + getUnitToValue();
  }
}
