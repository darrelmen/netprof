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

package mitll.langtest.server.audio.tools;

import mitll.langtest.server.database.project.Project;
import mitll.langtest.server.scoring.IPronunciationLookup;
import mitll.langtest.server.services.AudioServiceImpl;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ModelType;
import mitll.langtest.shared.project.OOVWordsAndUpdate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class OOVWordsHelper {
  private static final Logger logger = LogManager.getLogger(OOVWordsHelper.class);

  private static final String REGEX = " ";  // no break space!
  private static final String TIC_REGEX = "&#39;";

  private static final boolean DEBUG_VALID = true;

  public OOVWordsAndUpdate get(CommonExercise exerciseByID, String text, Project englishProject, Project project) {
    Exercise exercise = new Exercise(exerciseByID);
    String trim = getTrim(text);

    if (DEBUG_VALID) logger.info("isValid " + text + " = '" + trim + "'");

    exercise.getMutable().setForeignLanguage(trim);

    boolean hasEnglishAttr = exercise.hasEnglishAttr();
    boolean allEnglish = hasEnglishAttr;
    boolean allOOV = false;
    boolean someAreOOV = false;
    boolean noEnglish = !hasEnglishAttr;

    IPronunciationLookup.InDictStat englishTokenStats = null;
    IPronunciationLookup engPronLookup = null;
    if (englishProject == null) {
      logger.warn("isValid : no english project?");
    } else {
      engPronLookup = englishProject.getAudioFileHelper().getASR().getPronunciationLookup();
      englishTokenStats = engPronLookup.getTokenStats(trim);
    }

    if (englishTokenStats != null) {
      if (hasEnglishAttr) {      // expecting english, but there is none there, probably got swapped content
        if (DEBUG_VALID) logger.info("isValid, expecting english, got " + englishTokenStats);
        if (englishTokenStats.getNumInDict() == 0 && englishTokenStats.getNumTokens() > 0) {
          noEnglish = true;
        }
      } else { // expecting chinese (say) but it's all english? probably a swap
        if (DEBUG_VALID)
          logger.info("isValid, expecting " + project.getLanguage() + ", got " + englishTokenStats + " for english");
        boolean allTokensInDict = englishTokenStats.getNumTokens() == englishTokenStats.getNumInDict();
        if (allTokensInDict && englishTokenStats.getNumTokens() > 0) {
          IPronunciationLookup.InDictStat flTokenStats = project.getAudioFileHelper().getASR().getPronunciationLookup().getTokenStats(trim, true);
          if (DEBUG_VALID) logger.info("isValid, checking against " + project.getLanguage() + ", got " + flTokenStats);

          // so if all the words are english, and the in dict words are english, then the word appears in both dictionaries...
          // oov are unk
          // if all in dict are english, figure it's bogus
          boolean allInDictAreEnglish = true;
          if (engPronLookup != null) {
            final IPronunciationLookup fengPronLookup = engPronLookup;

            Set<String> flInDict = flTokenStats.getInDictTokens();
            flInDict.forEach(t -> logger.info("isValid fl dict in dict token " + t));
            List<String> collect = flInDict.stream().filter(t -> fengPronLookup.getTokenStats(t).getNumInDict() == 1).collect(Collectors.toList());
            if (DEBUG_VALID) logger.info("isValid : found " + collect + " as english tokens from set " + flInDict);
            allInDictAreEnglish = collect.size() == flInDict.size();
            someAreOOV = flTokenStats.areSomeOOV();
          }

          allEnglish = allInDictAreEnglish;  // all the tokens are english
          allOOV = (flTokenStats.getNumInDict() == 0);  //
        }
      }
    }

    Map<Integer, String> idToNorm = new HashMap<>();
    Set<String> oovTokens = project.getAudioFileHelper().isValid(exercise, idToNorm);
    Collection<String> values = idToNorm.values();

    String normText = values.isEmpty() ? trim : values.iterator().next();

    OOVWordsAndUpdate oovWordsAndUpdate =
        new OOVWordsAndUpdate(false, oovTokens, project.getModelType() == ModelType.HYDRA, normText);
    oovWordsAndUpdate.setAllEnglish(allEnglish);
    oovWordsAndUpdate.setAllOOV(allOOV);
    oovWordsAndUpdate.setSomeOOV(someAreOOV);
    oovWordsAndUpdate.setNoEnglish(noEnglish);
    oovWordsAndUpdate.setCheckValid(true);
    oovWordsAndUpdate.setPossible(oovTokens.isEmpty());

    if (DEBUG_VALID) logger.info("isValid : Sending " + oovWordsAndUpdate);

    return oovWordsAndUpdate;
  }

  private String getTrim(String part) {
    return part.replaceAll(REGEX, " ").replaceAll(TIC_REGEX, "'").trim();
  }
}
