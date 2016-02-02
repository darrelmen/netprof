package mitll.langtest.shared.exercise;

import mitll.langtest.shared.MiniUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 1/26/16.
 */
public interface AudioRefExercise {
  boolean hasRefAudio();

  String getRefAudio();

  String getSlowAudioRef();

  AudioAttribute getRegularSpeed();

  AudioAttribute getSlowSpeed();

  Collection<AudioAttribute> getAudioAttributes();

  AudioAttribute getRecordingsBy(long userID, boolean regularSpeed);

  /**
   * Sorted by user age. Gotta choose something...
   * @param malesMap
   * @return
   */
  List<MiniUser> getSortedUsers(Map<MiniUser, List<AudioAttribute>> malesMap);

  Map<MiniUser, List<AudioAttribute>> getUserMap(boolean isMale);
}
