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

package mitll.langtest.server.database.postgres;

import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TrieTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(TrieTest.class);

  @Test
  public void testTrie() {
    Collection<CommonExercise> exercises = new ArrayList<>();
    String test = "habit; custom";
    Exercise e = new Exercise(1, 1, test, 1, false);
    e.setForeignLanguage("soemthing korean");
    exercises.add(e);
    SmallVocabDecoder smallVocabDecoder = new SmallVocabDecoder();
    ExerciseTrie<CommonExercise> korean = new ExerciseTrie<>(exercises, "korean", smallVocabDecoder, true, false);

    testAll(test,korean);
    testAll("habit custom", korean);
    testAll("habitcustom", korean);
    testAll("habit;; ;;custom", korean);

    /*logger.info("got " + korean.getExercises("habit"));
    logger.info("got " + korean.getExercises("habit;"));

    logger.info("got " + korean.getExercises(test));
    logger.info("got " + korean.getExercises("habit; c"));
    logger.info("got " + korean.getExercises("habit ; custom"));


    logger.info("2 got " + korean.getMatches("habit"));
    logger.info("2 got " + korean.getMatches("habit;"));

    logger.info("2 got " + korean.getMatches(test));
    logger.info("2 got " + korean.getMatches("habit; c"));
    logger.info("2 got " + korean.getMatches("habit ; custom"));*/
    // korean.addEntryToTrie()
  }

  private void testAll(String test, ExerciseTrie<CommonExercise> korean) {
    StringBuilder sofar;
    sofar = new StringBuilder();
    for (int i = 0; i < test.length(); ++i) {
      sofar.append(test.charAt(i));
      List<CommonExercise> exercises1 = korean.getExercises(sofar.toString());
      if (exercises1.isEmpty()) {
        logger.info("got " + exercises1);
      }
      else logger.debug(sofar.toString() + " : " + exercises1.size());
    }
  }


}
