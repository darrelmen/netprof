package mitll.langtest.server.scoring;

import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.scoring.PretestScore;

import java.io.File;
import java.util.Collection;

/**
 * What the {@link mitll.langtest.server.autocrt.AutoCRT} object sees of {@link mitll.langtest.server.LangTestDatabaseImpl}
 * User: GO22670
 * Date: 1/10/13
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
public interface AutoCRTScoring {

  /**
   * @see mitll.langtest.server.autocrt.AutoCRT#getFlashcardAnswer(File, Collection, AudioAnswer, boolean, boolean)
   * @param testAudioFile
   * @param lmSentences
   * @param canUseCache
   * @param useOldSchool
   * @return
   */
  PretestScore getASRScoreForAudio(File testAudioFile, Collection<String> lmSentences, boolean canUseCache, boolean useOldSchool);
}
