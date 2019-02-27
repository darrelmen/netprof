package mitll.langtest.server.scoring;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class WordAndProns {
  private final String word;
  private boolean fromLTS = false;
  private boolean isUNK = false;
  private final Set<String> prons = new LinkedHashSet<>();

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

  WordAndProns setFromLTS(boolean fromLTS) {
    this.fromLTS = fromLTS;
    return this;
  }

  public boolean isFromLTS() {
    return fromLTS;
  }

  public boolean isUNK() {
    return isUNK;
  }

  public WordAndProns setUNK(boolean UNK) {
    isUNK = UNK;
    return this;
  }

  @Override
  public String toString() {
    return word + " : " + prons + (fromLTS ? " (LTS)" : "")+ (isUNK ? " (UNK)" : "");
  }
}
