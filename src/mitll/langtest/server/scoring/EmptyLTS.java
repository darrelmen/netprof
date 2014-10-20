package mitll.langtest.server.scoring;

import corpus.LTS;
import mitll.langtest.server.audio.SLFFile;

/**
* Created by go22670 on 10/19/14.
*/
class EmptyLTS extends LTS {
  @Override
  public String[][] process(String word) {
    //System.out.println("EmptyLTS.process " + word );

    if (word.equals(SLFFile.UNKNOWN_MODEL)) {
      String[][] strings = new String[1][1];
      strings[1][1] = "+UNK+";
      System.out.println("For " + word + " returning " + strings);
      return strings;
    }
    else {
      return new String[0][];
    }
  }
}
