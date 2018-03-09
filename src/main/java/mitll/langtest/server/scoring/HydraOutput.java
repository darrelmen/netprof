package mitll.langtest.server.scoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;

public class HydraOutput {
  private static final Logger logger = LogManager.getLogger(HydraOutput.class);

  private Scores scores;
  private String wordLab;
  private String phoneLab;
  // private Trie<String> trie;
  //private Trie<String> trie;
  private List<WordAndProns> wordAndProns;

  /**
   * @param scores
   * @param wordLab
   * @param phoneLab
   * @param wordAndProns
   */
  HydraOutput(Scores scores,
              String wordLab, String phoneLab, List<WordAndProns> wordAndProns) {//Trie<String> trie) {
    this.scores = scores;
    this.wordLab = wordLab;
    this.phoneLab = phoneLab;
    this.wordAndProns = wordAndProns;
  }

  public Scores getScores() {
    return scores;
  }

  public String getWordLab() {
    return wordLab;
  }

  public String getPhoneLab() {
    return phoneLab;
  }

/*  public Trie<String> getTrie() {
    return trie;
  }*/

  public void setScores(Scores scores) {
    this.scores = scores;
  }

  public boolean isMatch(List<WordAndProns> reco) {
    if (reco.size() != wordAndProns.size()) {
      logger.warn("isMatch expecting  " + wordAndProns.size() + " words, saw " + reco.size());
      return false;
    } else {
      boolean res = true;
      for (int i = 0; i < reco.size(); i++) {
        WordAndProns recoWordAndProns = reco.get(i);
        WordAndProns expected = wordAndProns.get(i);
        String expectedWord = expected.getWord();
        String recoWord = recoWordAndProns.getWord();
        if (!recoWord.equalsIgnoreCase(expectedWord)) {
          logger.warn("isMatch word : expecting  " + expectedWord + " saw " + recoWord);
        }
        Set<String> prons = expected.getProns();
        String next = recoWordAndProns.getProns().iterator().next();
        boolean contains = prons.contains(next);
        res &= contains;
        if (!contains) {
          logger.warn("isMatch phone : expecting one of " + prons + " saw " + next);
          break;
        }
      }
      return res;
    }
  }

  public String toString() {
    return wordLab + ", " + phoneLab;
  }

  public List<WordAndProns> getWordAndProns() {
    return wordAndProns;
  }

}
