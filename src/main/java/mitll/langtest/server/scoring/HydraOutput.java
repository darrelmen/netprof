package mitll.langtest.server.scoring;

import mitll.langtest.shared.scoring.ImageOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class HydraOutput {
  private static final Logger logger = LogManager.getLogger(HydraOutput.class);

  private Scores scores;
  private String wordLab;
  private String phoneLab;
  private List<WordAndProns> wordAndProns;
  private TransNormDict transNormDict;

  /**
   * @param scores
   * @param wordLab
   * @param phoneLab
   * @param wordAndProns
   * @see ASRWebserviceScoring#scoreRepeatExercise
   */
  HydraOutput(Scores scores,
              String wordLab,
              String phoneLab,
              List<WordAndProns> wordAndProns,
               TransNormDict transNormDict) {
    this.scores = scores;
    this.wordLab = wordLab;
    this.phoneLab = phoneLab;
    this.wordAndProns = wordAndProns;
    this.transNormDict=transNormDict;
  }

  public Scores getScores() {
    return scores;
  }

  public void setScores(Scores scores) {
    this.scores = scores;
  }

  String getWordLab() {
    return wordLab;
  }

  String getPhoneLab() {
    return phoneLab;
  }

  /**
   * Does the reco word and phone sequence match any of the possible complete sequences
   * Only make sure the number of words is correct for now...
   * @param reco
   * @return
   */
  boolean isMatch(List<WordAndProns> reco) {
    if (reco.size() != wordAndProns.size()) {
      logger.warn("isMatch " +
          "\n\texpecting " + wordAndProns.size() + " words," +
          "\n\tsaw       " + reco.size());
      return false;
    } else {
      boolean res = doPhoneComparison(reco, wordAndProns); // this is just for fun - it doesn't actually work reliably


      return true; // don't use the result of the phone comparison!
    }
  }

  private boolean doPhoneComparison(List<WordAndProns> reco, List<WordAndProns> expectedSeq) {
    boolean res = true;
    for (int i = 0; i < reco.size(); i++) {
      WordAndProns recoWordAndProns = reco.get(i);
      WordAndProns expected = expectedSeq.get(i);

      String expectedWord = expected.getWord();
      String recoWord = recoWordAndProns.getWord();

      if (!recoWord.equalsIgnoreCase(expectedWord)) {
        logger.warn("doPhoneComparison word :" +
            "\n\texpecting '" + expectedWord + "'" +
            "\n\tsaw       '" + recoWord + "'");
      }

      Set<String> prons = expected.getProns();
      String next = recoWordAndProns.getProns().iterator().next();
      boolean contains = prons.contains(next);
      res &= contains;
      if (!contains) {
        logger.warn("doPhoneComparison phone : expecting one of " + prons + " saw " + next);
        break;
      }
    }
    return res;
  }

  List<WordAndProns> getWordAndProns() {
    return wordAndProns;
  }

  public TransNormDict getTransNormDict() {
    return transNormDict;
  }

  public String toString() {
    return wordLab + ", " + phoneLab;
  }
}
