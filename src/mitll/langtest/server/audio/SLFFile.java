/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.audio;

import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.SmallVocabDecoder;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/17/14.
 * Writes an HTK SLF file similar to : <a href="http://www1.icsi.berkeley.edu/Speech/docs/HTKBook/node293_mn.html">Example</a>
 * See <a href="http://www1.icsi.berkeley.edu/Speech/docs/HTKBook/node288_ct.html">SLF File Documentation</a>
 */
public class SLFFile {
  private static final Logger logger = Logger.getLogger("SLFFile");

  public static final String UNKNOWN_MODEL = "UNKNOWNMODEL";
  private static final String ENCODING = "UTF8";

  // private static final String LINK_WEIGHT = "-1.00";
  private static final float EQUAL_LINK_CONSTANT = -1.00f;
  // public static final float UNKNOWN_MODEL_BIAS_CONSTANT = -1.20f;
  private static final String UNKNOWN_MODEL_BIAS = "-1.20";
  private static final String STANDARD_WEIGHT = "-1.00";
  private static final String SIL = "SIL";

  /**
   * Unknown Model Bias Weight balances the likelihood between matching one of the decode words or the unknown model.
   * <p>
   * Writes a file into the temp directory, with name {@link mitll.langtest.server.scoring.ASRScoring#SMALL_LM_SLF}
   *
   * @param lmSentences
   * @param tmpDir
   * @param unknownModelBiasWeight - a property you can set in the property file
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#createSLFFile
   */
  public String createSimpleSLFFile(Collection<String> lmSentences, String tmpDir, float unknownModelBiasWeight) {
    String slfFile = getSLFPath(tmpDir);

    String unknownModelBias = String.format("%.2f", unknownModelBiasWeight);
    String linkWeight = String.format("%.2f", EQUAL_LINK_CONSTANT);

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
      Collection<String> sentencesToUse = new ArrayList<>(lmSentences);
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
              (token.equals(UNKNOWN_MODEL) ? unknownModelBias : linkWeight) +
              "\n");
          nodesBuf.append("I=" +
              next +
              " W=" +
              token +
              "\n");

          start = next;
        }
        linksBuf.append("J=" + (linkCount++) + " S=" + start + " E=1" + " l=" +
            linkWeight +
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

  private String getSLFPath(String tmpDir) {
    return tmpDir + File.separator + ASRScoring.SMALL_LM_SLF;
  }

  /**
   * @param lmSentences
   * @param addSil
   * @param includeUnk
   * @return
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#runHydra
   */
  // creates string LM for hydra
  public String[] createSimpleSLFFile(Collection<String> lmSentences, boolean addSil, boolean includeUnk) {
    List<String> slf = new ArrayList<>();
    slf.add("VERSION=1.0;");

    int linkCount = 0;
    StringBuilder nodesBuf = new StringBuilder();
    nodesBuf.append("I=0 W=<s>;");
    nodesBuf.append("I=1 W=</s>;");
    int newNodes = 2;
    StringBuilder linksBuf = new StringBuilder();

    // include the UNKNOWNMODEL
    Collection<String> sentencesToUse = new ArrayList<>(lmSentences);
    if (includeUnk) {
      sentencesToUse.add(UNKNOWN_MODEL);
    }

    String finalSentence = "";

    SmallVocabDecoder svd = new SmallVocabDecoder();
    int ctr = 0;
    for (String sentence : sentencesToUse) {
      Collection<String> tokens = svd.getTokens(sentence);

      int prevNode = 0;
      int currentSil = 0;
      int c = 0;

//      logger.info("tokens " + tokens);
      for (String token : tokens) {
        boolean onLast = ++c == tokens.size();
  //      logger.info("onLast " + onLast + " c " + c + " " + token + " tokens " + tokens.size());
        String cleanedToken = cleanToken(token);

        if (!cleanedToken.isEmpty()) {
          int currentNode = newNodes++;
          boolean isUNK = cleanedToken.toUpperCase().equals(UNKNOWN_MODEL);
          String linkWeight = isUNK ? UNKNOWN_MODEL_BIAS : STANDARD_WEIGHT;

          linksBuf.append("J=" + (linkCount++) + " S=" + prevNode + " E=" + currentNode + " l=" + linkWeight + ";");

          String wordtoken = isUNK ? cleanedToken.toUpperCase() : cleanedToken;
          nodesBuf.append("I=" + currentNode + " W=" + wordtoken + ";");

          if (addSil && !isUNK) {
            if (c > 1) {
              linksBuf.append("J=" + (linkCount++) + " S=" + currentSil + " E=" + currentNode + " l=" + linkWeight + ";");
            }

            if (!onLast) {
              currentSil = newNodes++;
              nodesBuf.append("I=" + currentSil + " W=" + SIL + ";");
              linksBuf.append("J=" + (linkCount++) + " S=" + currentNode + " E=" + currentSil + " l=" + linkWeight + ";");
            }

          }
          if (!isUNK)
            finalSentence += cleanedToken + ";";

          prevNode = currentNode;
        }
      }

      linksBuf.append("J=" + (linkCount++) + " S=" + prevNode + " E=1" + " l=-1.00" + (ctr == sentencesToUse.size() - 1 ? "" : ";"));
      ctr += 1;
    }
    slf.add("N=" + newNodes + " L=" + linkCount + ";");
    slf.add(nodesBuf.toString());
    slf.add(linksBuf.toString());

    StringBuilder slfBuf = new StringBuilder();
    for (String aSlf : slf) {
      slfBuf.append(aSlf);
      //if(i != (slf.size() - 1))
      //	  slfBuf.append(";");
    }
    return new String[]{slfBuf.toString(), finalSentence};
  }

  public String cleanToken(String token) {
    return token.replaceAll("\\u2022", " ").replaceAll("\\p{Z}+", " ").replaceAll(";", " ").replaceAll("~", " ").replaceAll("\\u2191", " ").replaceAll("\\u2193", " ").replaceAll("\\p{P}", "").toLowerCase();
  }
}
