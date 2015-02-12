package mitll.langtest.server.audio;

import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.SmallVocabDecoder;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;

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
  private static final String ENCODING = "UTF8";

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
      BufferedWriter writer = new BufferedWriter(
          new OutputStreamWriter(new FileOutputStream(slfFile), ENCODING));
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
  
  // creates string LM for hydra
  // TODO calculate how many entries in the LM and then preallocate space appropriately to be faster
  public String createSimpleSLFFile(Collection<String> lmSentences) {
	  ArrayList<String> slf = new ArrayList<String>();
	  slf.add("VERSION=1.0;");

	  int linkCount = 0;
	  StringBuilder nodesBuf = new StringBuilder();
	  nodesBuf.append("I=0 W=<s>;");
	  nodesBuf.append("I=1 W=</s>;");
	  int newNodes = 2;
	  StringBuilder linksBuf = new StringBuilder();
	  Collection<String> sentencesToUse = new ArrayList<String>(lmSentences);
	  sentencesToUse.add(UNKNOWN_MODEL);

	  SmallVocabDecoder svd = new SmallVocabDecoder();
	  int ctr = 0;
	  for (String sentence : sentencesToUse) {
		  Collection<String> tokens = svd.getTokens(sentence);
		  int start = 0;

		  for (String token : tokens) {
			  int next = newNodes++;
			  linksBuf.append("J=" + (linkCount++) + " S=" + start + " E=" + next +
					  " l=" +
					  (token.equals(UNKNOWN_MODEL) ? UNKNOWN_MODEL_BIAS : "-1.00") + ";");
			  nodesBuf.append("I=" +
					  next +
					  " W=" +
					  token +
					  ";");

			  start = next;
		  }
		  linksBuf.append("J=" + (linkCount++) + " S=" + start + " E=1" + " l=-1.00" + (ctr == sentencesToUse.size() - 1 ? "" : ";"));
		  ctr += 1;
	  }
	  slf.add("N=" + newNodes + " L=" + linkCount + ";");
	  slf.add(nodesBuf.toString());
	  slf.add(linksBuf.toString());

	  StringBuilder slfBuf = new StringBuilder();
	  for(int i = 0; i < slf.size(); i++) {
		  slfBuf.append(slf.get(i));
		  //if(i != (slf.size() - 1))
		//	  slfBuf.append(";");
	  }
	  return slfBuf.toString();
  }
}
