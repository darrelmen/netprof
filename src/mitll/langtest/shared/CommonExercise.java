package mitll.langtest.shared;

import java.util.Collection;
import java.util.Date;
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

  Map<String, String> getUnitToValue();

  CommonShell getShell();

  CommonShell getShellCombinedTooltip();

  Map<String, ExerciseAnnotation> getFieldToAnnotation();

  ExerciseAnnotation getAnnotation(String field);

  Exercise toExercise();
  CommonUserExercise toCommonUserExercise();

  // super nice to remove these... and make read only
  void addUnitToValue(String unit, String value);

  void addAnnotation(String field, String status, String comment);

  Date getModifiedDate();

  Collection<String> getFields();

}
