package mitll.langtest.server.audio;

import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.SmallVocabDecoder;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by go22670 on 1/17/14.
 * Writes an HTK SLF file similar to : <a href="http://www1.icsi.berkeley.edu/Speech/docs/HTKBook/node293_mn.html">Example</a>
 * See <a href="http://www1.icsi.berkeley.edu/Speech/docs/HTKBook/node288_ct.html">SLF File Documentation</a>
 */
public class SLFFile {
  //private static final Logger logger = Logger.getLogger(SLFFile.class);

  public static final String UNKNOWN_MODEL = "UNKNOWNMODEL";
  private static final String ENCODING = "UTF8";

  private static final String LINK_WEIGHT = "-1.00";
  public static final float UNKNOWN_MODEL_BIAS_CONSTANT = 1.40f;

  /**
   * Unknown Model Bias Weight balances the likelihood between matching one of the decode words or the unknown model.
   *
   * Writes a file into the temp directory, with name {@link mitll.langtest.server.scoring.ASRScoring#SMALL_LM_SLF}
   * @see mitll.langtest.server.audio.AudioFileHelper#createSLFFile
   * @param lmSentences
   * @param tmpDir
   * @param unknownModelBiasWeight - a property you can set in the property file
   * @return
   */
  public String createSimpleSLFFile(Collection<String> lmSentences, String tmpDir, float unknownModelBiasWeight) {
    String slfFile = tmpDir + File.separator + ASRScoring.SMALL_LM_SLF;

    String unknownModelBias = String.format("%.2f", unknownModelBiasWeight);

    try {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(slfFile), ENCODING));
      writer.write("VERSION=1.0\n");

      int linkCount = 0;
      StringBuilder nodesBuf = new StringBuilder();
      // add silence nodes
      nodesBuf.append("I=0 W=<s>\n");
      nodesBuf.append("I=1 W=</s>\n");
      int newNodes = 2;

      StringBuilder linksBuf = new StringBuilder();
      Collection<String> sentencesToUse = new ArrayList<String>(lmSentences);
      sentencesToUse.add(UNKNOWN_MODEL);

      SmallVocabDecoder svd = new SmallVocabDecoder();
      for (String sentence : sentencesToUse) {
        Collection<String> tokens = svd.getTokens(sentence);
        //logger.debug("\tfor '" + sentence + "' tokens are " + tokens);
        int start = 0;

        for (String token : tokens) {
          int next = newNodes++;
          linksBuf.append("J=" + (linkCount++) + " S=" + start + " E=" + next +
            " l=" +
            (token.equals(UNKNOWN_MODEL) ? unknownModelBias : LINK_WEIGHT) +
            "\n");
          nodesBuf.append("I=" +
            next +
            " W=" +
            token +
            "\n");

          start = next;
        }
        linksBuf.append("J=" + (linkCount++) + " S=" + start + " E=1" + " l=" +
            LINK_WEIGHT +
            "\n");
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
}
