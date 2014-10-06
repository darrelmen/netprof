package mitll.langtest.server.audio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;

import mitll.langtest.server.database.exercise.FileExerciseDAO;
import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.SmallVocabDecoder;

/**
 * Created by go22670 on 1/17/14.
 */
public class SLFFile {
  //private static final Logger logger = Logger.getLogger(SLFFile.class);

  /**
   * Limit on vocabulary size -- too big and dcodr will run out of memory and segfault
   */
  public static final String UNKNOWN_MODEL = "UNKNOWNMODEL";
  private static final String SMALL_LM_SLF = ASRScoring.SMALL_LM_SLF;

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

      SmallVocabDecoder svd = new SmallVocabDecoder();
      for (String sentence : sentencesToUse) {
        Collection<String> tokens = svd.getTokens(sentence);
        //logger.debug("\tfor '" + sentence + "' tokens are " + tokens);
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
}
