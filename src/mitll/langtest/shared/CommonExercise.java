package mitll.langtest.shared;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by GO22670 on 3/20/2014.
 */
public interface CommonExercise extends CommonShell {
  String getPlan();

  String getEnglish();

  String getMeaning();

  String getContext();

  String getForeignLanguage();
  String getRefSentence();

  String getTransliteration();

  String getContent();

  boolean hasRefAudio();

  String getRefAudio();

  String getSlowAudioRef();

  Collection<AudioAttribute> getAudioAttributes();
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
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.FastAndSlowASRScoringAudioPanel#getAfterPlayWidget()
   * @param isMale
   * @return
   */
  Map<MiniUser, List<AudioAttribute>> getMostRecentAudio(boolean isMale);
  void addAudio(AudioAttribute audioAttribute);

  Map<String, String> getUnitToValue();

  CommonShell getShell();

  CommonShell getShellCombinedTooltip();

  Map<String, ExerciseAnnotation> getFieldToAnnotation();

  ExerciseAnnotation getAnnotation(String field);

  Exercise toExercise();
  CommonUserExercise toCommonUserExercise();

  // super nice to remove these... and make read only

  /**
   * @see mitll.langtest.server.database.SectionHelper#addExerciseToLesson(CommonExercise, String, String)
   * @param unit
   * @param value
   */
  void addUnitToValue(String unit, String value);

  void addAnnotation(String field, String status, String comment);

  Date getModifiedDate();

  Collection<String> getFields();
  boolean removeAudio(AudioAttribute audioAttribute);
  String getCombinedTooltip();
  void setTooltip();

  void setScores(Collection<ScoreAndPath> scoreTotal);
  Collection<ScoreAndPath> getScores();

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addAnnotationsAndAudio(long, CommonExercise)
   * @param v
   */
  void setAvgScore(float v);
  float getAvgScore();
}
