/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared;

import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by GO22670 on 3/20/2014.
 */
public interface CommonExercise extends CommonShell {
  String getContext();
  
  String getContextTranslation();

  String getRefSentence();

  Collection<String> getRefSentences();

  String getTransliteration();

  String getContent();

  boolean hasRefAudio();

  String getRefAudio();
  String getRefAudioWithPrefs(Set<Long> prefs);

  String getSlowAudioRef();

  Collection<AudioAttribute> getAudioAttributes();
  AudioAttribute getRegularSpeed();
  AudioAttribute getSlowSpeed();

  /**
   * @see mitll.langtest.server.DatabaseServlet#getJsonForExercise(CommonExercise)
   * @see mitll.langtest.server.ScoreServlet#getJsonArray(java.util.List)
   * @return
   */
  AudioAttribute getLatestContext(boolean isMale);
  AudioAttribute getRecordingsBy(long userID, boolean regularSpeed);
  AudioAttribute getRecordingsBy(long userID, String speed);
  Collection<AudioAttribute> getByGender(boolean isMale);
  Collection<AudioAttribute> getDefaultUserAudio();

  /**
   * Sorted by user age. Gotta choose something...
   * @param malesMap
   * @return
   */
  List<MiniUser> getSortedUsers(Map<MiniUser, List<AudioAttribute>> malesMap);
  Map<String, AudioAttribute> getAudioRefToAttr();
  Map<MiniUser, List<AudioAttribute>> getUserMap(boolean isMale);

  /**
   * @see mitll.langtest.client.scoring.FastAndSlowASRScoringAudioPanel#getAfterPlayWidget()
   * @param isMale
   * @return
   */
  Map<MiniUser, List<AudioAttribute>> getMostRecentAudio(boolean isMale, Set<Long> preferredUsers);
  void addAudio(AudioAttribute audioAttribute);

  CommonShell getShell();

  CommonShell getShellCombinedTooltip();

  Map<String, ExerciseAnnotation> getFieldToAnnotation();

  ExerciseAnnotation getAnnotation(String field);

  /**
   * @see mitll.langtest.server.database.Export#getExports
   * @return
   */
  //Exercise toExercise();

  // super nice to remove these... and make read only

  Map<String, String> getUnitToValue();

  /**
   * @see mitll.langtest.server.database.exercise.SectionHelper#addExerciseToLesson(CommonExercise, String, String)
   * @param unit
   * @param value
   */
  void addUnitToValue(String unit, String value);

  void addAnnotation(String field, String status, String comment);

  long getModifiedDateTimestamp();

  Collection<String> getFields();
  boolean removeAudio(AudioAttribute audioAttribute);
  String getCombinedTooltip();
  void setTooltip();

  /**
   * @see mitll.langtest.server.database.ResultDAO#attachScoreHistory(long, CommonExercise, boolean)
   * @param scoreTotal
   */
  void setScores(List<CorrectAndScore> scoreTotal);
  List<CorrectAndScore> getScores();

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addAnnotationsAndAudio
   * @param v
   */
  void setAvgScore(float v);
  float getAvgScore();

  List<String> getFirstPron();

  void setBagOfPhones(Set<String> bagOfPhones);
  void setFirstPron(List<String> phones);

  void setRefSentences(Collection<String> orDefault);
}
