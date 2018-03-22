package mitll.langtest.server.scoring;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class WordAndProns {
  private String word;
  private Set<String> prons = new LinkedHashSet<>();

  WordAndProns(String word, Collection<String> prons) {
    this.word = word;
    this.prons.addAll(prons);
  }

  WordAndProns(String word) {
    this.word = word;
  }

  WordAndProns(String word, String pron) {
    this.word = word;
    this.prons.add(pron);
  }

  public String getWord() {
    return word;
  }

  Set<String> getProns() {
    return prons;
  }

  public boolean addPron(String pron) {
    return prons.add(pron);
  }

  @Override
  public String toString() {
    return word + " : " + prons;
  }
}
