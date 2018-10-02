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

/**
 *
 */
package mitll.langtest.server.scoring;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Scores is a simple holder for the combination of scores returned when scoring an utterance.
 * Depending on the function passed in to Audio.sv and Audio.multisv,
 * these scores might just be raw or might be transformed to be in the range 0.0-1.0.
 * Some values may be null if they have not been computed.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class Scores {
  private static final Logger logger = LogManager.getLogger(Scores.class);

  static final String PHONES = "phones";
  static final String WORDS = "words";
  float hydraScore = 0f;
  Map<String, Map<String, Float>> eventScores;
  private int processDur = 0;
  private  JsonObject kaldiJsonObject;

  public Scores() {
    eventScores = Collections.emptyMap();
  }

 /**
   * @param hydecScore
   * @param eventScores
   * @param processDur
   * @see PrecalcScores#getCachedScores
   */
  Scores(float hydecScore, Map<String, Map<String, Float>> eventScores, int processDur) {
    this.hydraScore = hydecScore;
    this.eventScores = eventScores;
    this.processDur = processDur;
  }

  /**
   * TODO : do we need word scores?
   *
   * @param scoreSplit
   * @see ASRWebserviceScoring#runHydra
   */
  Scores(String[] scoreSplit) {
    this.eventScores = new HashMap<>();
    eventScores.put(PHONES, new HashMap<>());

    try {
      float s = Float.parseFloat(scoreSplit[0]);
      this.hydraScore = Float.isNaN(s) ? -1f : s;
      for (int i = 1; i < scoreSplit.length; i += 2) {
        eventScores.get(PHONES).put(scoreSplit[i], Float.parseFloat(scoreSplit[i + 1]));
      }
    } catch (NumberFormatException e) {
      logger.error("Parsing " + Arrays.asList(scoreSplit) + " Got " + e, e);
    }
  }

  public boolean isValid() {
    return hydraScore > -0.01;
  }

  public String toString() {
    return "Scores score " + hydraScore + " events " + eventScores + " took " + processDur + " millis";
  }

  public JsonObject getKaldiJsonObject() {
    return kaldiJsonObject;
  }

  public Scores setKaldiJsonObject(JsonObject kaldiJsonObject) {
    this.kaldiJsonObject = kaldiJsonObject;
    return this;
  }
}