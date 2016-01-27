package mitll.langtest.shared.exercise;

import java.util.Collection;

/**
 * Created by go22670 on 1/26/16.
 */
public interface AudioRefExercise {
  boolean hasRefAudio();

  String getRefAudio();

  String getSlowAudioRef();

  Collection<AudioAttribute> getAudioAttributes();

  AudioAttribute getRecordingsBy(long userID, boolean regularSpeed);
}
