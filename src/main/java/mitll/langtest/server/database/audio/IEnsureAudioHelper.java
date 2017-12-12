package mitll.langtest.server.database.audio;

import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.ImageOptions;

import java.util.Collection;
import java.util.List;

public interface IEnsureAudioHelper {
  /**
   *
   * @param projectid
   */
  void ensureAudio(int projectid);

  /**
   *
   * @param exercises
   * @param language
   */
  void ensureCompressedAudio(Collection<CommonExercise> exercises, String language);

  /**
   * @see mitll.langtest.server.services.AudioServiceImpl#writeAudioFile
   * @param user
   * @param commonShell
   * @param path
   * @param audioType
   * @param language
   * @return
   */
  String ensureCompressedAudio(int user,
                               CommonExercise commonShell,
                               String path,
                               AudioType audioType,
                               String language);

  /**
   * @see mitll.langtest.server.services.AudioServiceImpl#getImageForAudioFile
   * @param audioFile
   * @param language
   * @return
   */
  String getWavAudioFile(String audioFile, String language);
}
