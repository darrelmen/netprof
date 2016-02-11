package mitll.langtest.shared.exercise;

import mitll.langtest.shared.MiniUser;
import net.sf.json.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * Created by go22670 on 1/5/16.
 */
public interface AudioAttributeExercise extends AudioRefExercise {
  String getRefAudioWithPrefs(Collection<Long> prefs);

  /**
   * @see mitll.langtest.server.DatabaseServlet#getJsonForExercise(CommonExercise)
   * @see mitll.langtest.server.json.JsonExport#addContextAudioRefs(AudioAttributeExercise, JSONObject)
   * @return
   */
  AudioAttribute getLatestContext(boolean isMale);

  AudioAttribute getRecordingsBy(long userID, String speed);

  Collection<AudioAttribute> getByGender(boolean isMale);

  Collection<AudioAttribute> getDefaultUserAudio();

  Map<String, AudioAttribute> getAudioRefToAttr();

  /**
   * @see mitll.langtest.client.scoring.FastAndSlowASRScoringAudioPanel#getAfterPlayWidget()
   * @param isMale
   * @return
   */
  Map<MiniUser, List<AudioAttribute>> getMostRecentAudio(boolean isMale, Collection<Long> preferredUsers);

}
