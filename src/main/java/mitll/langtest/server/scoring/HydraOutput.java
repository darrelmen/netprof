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

package mitll.langtest.server.scoring;

import com.google.gson.JsonObject;
import mitll.langtest.shared.scoring.ImageOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class HydraOutput {
  private static final Logger logger = LogManager.getLogger(HydraOutput.class);

  enum STATUS_CODES {SUCCESS, OOV_IN_TRANS, FAILURE, ERROR}

  private Scores scores;
  private final String wordLab;
  private final String phoneLab;
  private final List<WordAndProns> wordAndProns;
  private final TransNormDict transNormDict;

  /**
   * DEFAULT status is success!
   */
  private STATUS_CODES status = STATUS_CODES.SUCCESS;
  private String message = "";
  private String log;

  /**
   * @param scores
   * @param wordLab
   * @param phoneLab
   * @param wordAndProns
   * @see ASRWebserviceScoring#scoreRepeatExercise
   */
  HydraOutput(Scores scores,
              String wordLab,
              String phoneLab,
              List<WordAndProns> wordAndProns,
              TransNormDict transNormDict) {
    this.scores = scores;
    this.wordLab = wordLab;
    this.phoneLab = phoneLab;
    this.wordAndProns = wordAndProns;
    this.transNormDict = transNormDict;
  }

  public Scores getScores() {
    return scores;
  }

  public void setScores(Scores scores) {
    this.scores = scores;
  }

  /**
   * @see ASRWebserviceScoring#getPretestScore(String, ImageOptions, String, String, HydraOutput, double, int, boolean, JsonObject, boolean)
   * @return
   */
  String getWordLab() {
    return wordLab;
  }

  String getPhoneLab() {
    return phoneLab;
  }

  /**
   * Does the reco word and phone sequence match any of the possible complete sequences
   * Only make sure the number of words is correct for now...
   *
   * @param reco
   * @return
   * @see ASRWebserviceScoring#isMatch(HydraOutput, Map)
   */
  boolean isMatch(List<WordAndProns> reco) {
    if (reco.size() != wordAndProns.size()) {
      logger.warn("isMatch " +
          "\n\texpecting " + wordAndProns.size() + " words : " + wordAndProns +
          "\n\tsaw       " + reco.size() + " : " + reco);
      return false;
    } else {
//      boolean res = doPhoneComparison(reco, wordAndProns); // this is just for fun - it doesn't actually work reliably
      return true; // don't use the result of the phone comparison!
    }
  }

/*  private boolean doPhoneComparison(List<WordAndProns> reco, List<WordAndProns> expectedSeq) {
    boolean res = true;
    for (int i = 0; i < reco.size(); i++) {
      WordAndProns recoWordAndProns = reco.get(i);
      WordAndProns expected = expectedSeq.get(i);

      String expectedWord = expected.getWord();
      String recoWord = recoWordAndProns.getWord();

      if (!recoWord.equalsIgnoreCase(expectedWord)) {
        logger.warn("doPhoneComparison word :" +
            "\n\texpecting '" + expectedWord + "'" +
            "\n\tsaw       '" + recoWord + "'");
      }

      Set<String> prons = expected.getProns();
      String next = recoWordAndProns.getProns().iterator().next();
      boolean contains = prons.contains(next);
      res &= contains;
      if (!contains) {
        logger.warn("doPhoneComparison phone : expecting one of " + prons + " saw " + next);
        break;
      }
    }
    return res;
  }*/

  List<WordAndProns> getWordAndProns() {
    return wordAndProns;
  }

//  public TransNormDict getTransNormDict() {
//    return transNormDict;
//  }

  public STATUS_CODES getStatus() {
    return status;
  }

  public HydraOutput setStatus(STATUS_CODES status) {
    this.status = status;
    return this;
  }

  public HydraOutput setLog(String log) {
    this.log = log;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public HydraOutput setMessage(String message) {
    this.message = message;
    return this;
  }

  public String toString() {
    return "status " + status + "\n\tlog " + log + "\n\twords " + wordLab + "\n\t phones" + phoneLab;
  }
}
