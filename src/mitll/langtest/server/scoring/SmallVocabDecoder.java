package mitll.langtest.server.scoring;

import mitll.langtest.server.database.FileExerciseDAO;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates the LM for a small vocab decoder from foreground and background sentences.
 *
 * User: GO22670
 * Date: 2/7/13
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class SmallVocabDecoder {
  private static final Logger logger = Logger.getLogger(ASRScoring.class);

  private static final String SMALL_LM_SLF = ASRScoring.SMALL_LM_SLF;
  /**
   * Limit on vocabulary size -- too big and dcodr will run out of memory and segfault
   */
  public static final String UNKNOWN_MODEL = "UNKNOWNMODEL";
  private static final String UNKNOWN_MODEL_BIAS = "-1.20";
  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#createSLFFile
   * @param lmSentences
   * @param tmpDir
   * @return
   */
  public String createSimpleSLFFile(Collection<String> lmSentences, String tmpDir) {
    String slfFile = tmpDir + File.separator + SMALL_LM_SLF;

    try {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(slfFile), FileExerciseDAO.ENCODING));
      writer.write("VERSION=1.0\n");

      int linkCount = 0;
      StringBuilder nodesBuf = new StringBuilder();
      nodesBuf.append("I=0 W=<s>\n");
      nodesBuf.append("I=1 W=</s>\n");
      int newNodes = 2;
      StringBuilder linksBuf = new StringBuilder();
      Collection<String> sentencesToUse = new ArrayList<String>(lmSentences);
      sentencesToUse.add(UNKNOWN_MODEL);
      for (String sentence : sentencesToUse) {
        Collection<String> tokens = getTokens(sentence);
        //logger.debug("\tfor " + sentence + " tokens are " + tokens);
        int start = 0;

        for (String token : tokens) {
          int next = newNodes++;
          linksBuf.append("J=" + (linkCount++) + " S=" + start + " E=" + next +
            " l=" +
            (token.equals(UNKNOWN_MODEL) ? UNKNOWN_MODEL_BIAS : "-1.00") +
            "\n");
          nodesBuf.append("I=" +
            next +
            " W=" +
            token +
            "\n");

          start = next;
        }
        linksBuf.append("J=" + (linkCount++) + " S=" + start + " E=1" + " l=-1.00\n");
      }
      writer.write("N=" + newNodes + " L=" + linkCount + "\n");

      writer.write(nodesBuf.toString());
      writer.write(linksBuf.toString());

      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    //logger.debug("wrote " + slfFile + " exists " + new File(slfFile).exists());
    return slfFile;
  }



  /**
   * Get the vocabulary to use when generating a language model. <br></br>
   * Very important to limit the vocabulary (less than 300 words) or else the small vocab dcodr will run out of
   * memory and segfault! <br></br>
   * Remember to add special tokens like silence, pause, and unk
   * @see ASRScoring#getUsedTokens
   * @param background sentences
   * @return most frequent vocabulary words
   */
  public List<String> getVocab(List<String> background, int vocabSizeLimit) {
    return getSimpleVocab(background, vocabSizeLimit);
  }

  /**
   * @see ASRScoring#getUniqueTokensInLM
   * @param sentences
   * @param vocabSizeLimit
   * @return
   */
  public List<String> getSimpleVocab(Collection<String> sentences, int vocabSizeLimit) {
    // count the tokens
    final Map<String, Integer> sc = new HashMap<String, Integer>();
    for (String sentence : sentences) {
      for (String token : getTokens(sentence)) {
      //  if (isValid(scoring, token)) {
          Integer c = sc.get(token);
          sc.put(token, (c == null) ? 1 : c + 1);
     /*   } else {
          logger.warn("getSimpleVocab : skipping '" + token + "' which is not in dictionary.");
        }*/
      }
    }

    // sort by frequency
    List<String> vocab = new ArrayList<String>(sc.keySet());
    Collections.sort(vocab, new Comparator<String>() {
      public int compare(String s, String s2) {
        Integer first = sc.get(s);
        Integer second = sc.get(s2);
        return first < second ? +1 : first > second ? -1 : 0;
      }
    });

    // take top n most frequent
    List<String> all = new ArrayList<String>(); // copy list b/c sublist not serializable ???
    if (vocab.size() > vocabSizeLimit) logger.warn("truncating vocab size from " + vocab.size() + " to " + vocabSizeLimit);
    all.addAll(vocab.subList(0, Math.min(vocab.size(), vocabSizeLimit)));
    return all;
  }

  public Collection<String> getTokens(String sentence) {
    List<String> all = new ArrayList<String>();

    for (String untrimedToken : sentence.split("\\p{Z}+")) { // split on spaces
      String tt = untrimedToken.replaceAll("\\p{P}", ""); // remove all punct
      String token = tt.trim();  // necessary?
      if (token.length() > 0) {
        all.add(token);
      }
    }

    return all;
  }
}
