package mitll.langtest.server.scoring;

import mitll.langtest.shared.scoring.PretestScore;

import java.io.File;
import java.util.List;

/**
 * What the {@link mitll.langtest.server.AutoCRT} object sees of {@link mitll.langtest.server.LangTestDatabaseImpl}
 * User: GO22670
 * Date: 1/10/13
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
public interface AutoCRTScoring {
  PretestScore getASRScoreForAudio(File testAudioFile, List<String> lmSentences,
                                   List<String> background, List<String> vocab);
}
