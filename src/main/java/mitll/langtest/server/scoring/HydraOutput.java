package mitll.langtest.server.scoring;

import mitll.langtest.server.trie.Trie;

public class HydraOutput {
  private Scores scores;
  private String wordLab;
  private String phoneLab;
  private Trie<String> trie;

  HydraOutput(Scores scores,
              String wordLab, String phoneLab, Trie<String> trie) {
    this.scores = scores;
    this.wordLab = wordLab;
    this.phoneLab = phoneLab;
    this.trie = trie;
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

  public Trie<String> getTrie() {
    return trie;
  }

  public void setScores(Scores scores) {
    this.scores = scores;
  }

  public String toString() {
    return wordLab + ", " + phoneLab;
  }
}
