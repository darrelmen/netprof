/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.audio;

import mitll.langtest.server.scoring.ASR;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.project.Language;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SLFFile {
  // private static final Logger logger = LogManager.getLogger(SLFFile.class);

  private static final String UNKNOWN_MODEL = ASR.UNKNOWN_MODEL;
  //private static final String ENCODING = "UTF8";

  // private static final String LINK_WEIGHT = "-1.00";
  //private static final float EQUAL_LINK_CONSTANT = -1.00f;
  private static final String UNKNOWN_MODEL_BIAS = "-1.20";
  private static final String STANDARD_WEIGHT = "-1.00";
  private static final String SIL = "SIL";
  private final SmallVocabDecoder svd;

  /**
   * Unknown Model Bias Weight balances the likelihood between matching one of the decode words or the unknown model.
   * <p>
   * Writes a file into the temp directory, with name {@linkx mitll.langtest.server.scoring.ASRScoring#SMALL_LM_SLF}
   *
   * @return
   * @paramx lmSentences
   * @paramx tmpDir
   * @paramx unknownModelBiasWeight - a property you can set in the property file
   * @paramx dontRemoveAccents
   * @seex mitll.langtest.server.scoring.ASRScoring#calcScoreForAudio
   */
/*
  public String createSimpleSLFFile(Collection<String> lmSentences, String tmpDir, float unknownModelBiasWeight, boolean dontRemoveAccents) {
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
        Collection<String> tokens = svd.getTokens(sentence, dontRemoveAccents);
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
*/
/*
  private String getSLFPath(String tmpDir) {
    return tmpDir + File.separator + Scoring.SMALL_LM_SLF;
  }
*/
  public SLFFile(Language language) {
    svd = new SmallVocabDecoder(language);

  }

  /**
   * creates string LM for hydra
   *
   * @param lmSentences
   * @param addSil             true usually
   * @param includeUnk         if decode
   * @param includeSelfSILLink only true if trimming
   * @param removeAllAccents
   * @return
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#runHydra
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#getSmallLM
   */
  public String[] createSimpleSLFFile(Collection<String> lmSentences,
                                      boolean addSil,
                                      boolean includeUnk,
                                      boolean includeSelfSILLink,
                                      boolean removeAllAccents) {
    List<String> slf = new ArrayList<>();
    slf.add("VERSION=1.0;");

    int linkCount = 0;
    StringBuilder nodesBuf = new StringBuilder();
    nodesBuf.append("I=0 W=<s>;");
    int finalNodeIndex = 1;
    nodesBuf.append("I=" + finalNodeIndex + " W=</s>;");
    int newNodes = 2;
    StringBuilder linksBuf = new StringBuilder();

    // include the UNKNOWNMODEL
    Collection<String> sentencesToUse = new ArrayList<>(lmSentences);
    if (includeUnk) {
      sentencesToUse.add(UNKNOWN_MODEL);
    }

    String finalSentence = "";

    int ctr = 0;
    for (String sentence : sentencesToUse) {
//      logger.info("createSimpleSLFFile sentence " + sentence);
      Collection<String> tokens = svd.getTokens(sentence, removeAllAccents);

      int prevNode = 0;  // points to initial node
      int currentSil = 0;
      int c = 0;

      if (includeSelfSILLink) {
        linksBuf.append(getLink(linkCount++, prevNode, prevNode, STANDARD_WEIGHT, false));
      }

//      logger.info("tokens " + tokens);
      for (String token : tokens) {
        boolean onLast = ++c == tokens.size();
//        logger.info("createSimpleSLFFile onLast " + onLast + " c " + c + " '" + token + "' tokens length = " + tokens.size());
        String cleanedToken = svd.lcToken(token, removeAllAccents);

        if (!cleanedToken.isEmpty()) {
          int currentNode = newNodes++;
          boolean isUNK = cleanedToken.toUpperCase().equals(UNKNOWN_MODEL);
          String linkWeight = isUNK ? UNKNOWN_MODEL_BIAS : STANDARD_WEIGHT;

          linksBuf.append(getLink(linkCount++, prevNode, currentNode, linkWeight, false));

          String wordtoken = isUNK ? cleanedToken.toUpperCase() : cleanedToken;
          nodesBuf.append(getNode(currentNode, wordtoken));

          if (addSil && !isUNK) {
            if (c > 1) {
              linksBuf.append(getLink(linkCount++, currentSil, currentNode, linkWeight, false));
            }

            if (!onLast) {
              currentSil = newNodes++;
              nodesBuf.append(getNode(currentSil, SIL));
              linksBuf.append(getLink(linkCount++, currentNode, currentSil, linkWeight, false));
            }
          }
          if (!isUNK) {
            finalSentence += cleanedToken + ";";
          }

          prevNode = currentNode;
        }
      }

      //int linkID = linkCount++;
      // String finalLinkWeight = "-1.00";
      boolean isLastLink = ctr == sentencesToUse.size() - 1;
      //linksBuf.append("J=" + linkID + " S=" + prevNode + " E=" + finalNodeIndex + " l=" + finalLinkWeight + (isLastLink ? "" : ";"));
      linksBuf.append(getLink(linkCount++, prevNode, finalNodeIndex, STANDARD_WEIGHT, isLastLink && !includeSelfSILLink));

      if (includeSelfSILLink) {
        linksBuf.append(getLink(linkCount++, finalNodeIndex, finalNodeIndex, STANDARD_WEIGHT, isLastLink));
      }
      ctr += 1;
    }
    slf.add(getNodeAndLinkCount(newNodes, linkCount));
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

  private String getNodeAndLinkCount(int newNodes, int linkCount) {
    return "N=" + newNodes + " L=" + linkCount + ";";
  }

  private String getLink(int linkID, int prevNode, int currentNode, String linkWeight, boolean isLastLink) {
    return "J=" + linkID + " S=" + prevNode + " E=" + currentNode + " l=" + linkWeight + (isLastLink ? "" : ";");
  }

  private String getNode(int currentNode, String wordtoken) {
    return "I=" + currentNode + " W=" + wordtoken + ";";
  }

  /**
   * Special rule for french.
   *
   * @param token
   * @param removeAllPunct
   * @return
   * @see AudioFileHelper#
   */
/*  public String lcToken(String token, boolean removeAllPunct) {
    return removeAllPunct ?
        lcToken(token) :
        svd.getTrimmedLeaveAccents(token).toLowerCase();
  }*/

  /**
   * NOPE don't want to strip accents.
   * Redundant with SmallVocabDecoder...
   *
   * @param token
   * @return
   * @see #createSimpleSLFFile(Collection, boolean, boolean, boolean, boolean)
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#runHydra(String, String, String, Collection, String, boolean, int)
   */
  /*  private String lcToken(String token) {
   *//*    String s = token
        .replaceAll(REMOVE_ME, " ")
        .replaceAll("\\p{Z}+", " ")
        .replaceAll("\\p{P}", "");

    // return StringUtils.stripAccents(s).toLowerCase();*//*

    String s = svd.getTrimmedLeaveLastSpace(token);
    return s.toLowerCase();
  }*/
}
