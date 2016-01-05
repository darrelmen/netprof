package mitll.langtest.shared.exercise;

import mitll.langtest.shared.MiniUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by go22670 on 1/5/16.
 */
public interface AudioAttributeExercise {
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

  boolean removeAudio(AudioAttribute audioAttribute);
}
