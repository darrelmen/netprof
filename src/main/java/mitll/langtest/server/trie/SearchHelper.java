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

package mitll.langtest.server.trie;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SearchHelper {
  private static final Logger logger = LogManager.getLogger(SearchHelper.class);

/*  @NotNull
  public Collection<CommonExercise> getSearchMatches(Collection<CommonExercise> exercisesForState,
                                                     String prefix,
                                                     String language,
                                                     SmallVocabDecoder smallVocabDecoder) {
    Collection<CommonExercise> originalSet = exercisesForState;
    // logger.info("original set" +originalSet.size());
    long then = System.currentTimeMillis();
    ExerciseTrie<CommonExercise> trie =
        new ExerciseTrie<>(exercisesForState, language, smallVocabDecoder, true);
    long now = System.currentTimeMillis();
    if (now - then > 20)
      logger.info("took " + (now - then) + " millis to build trie for " + exercisesForState.size() + " exercises");
    exercisesForState = trie.getExercises(prefix);

    if (exercisesForState.isEmpty()) {
      String prefix1 = StringUtils.stripAccents(prefix);
      exercisesForState = trie.getExercises(prefix1);
      logger.info("getSearchMatches trying " + prefix1 + " instead of " + prefix + " found " + exercisesForState.size());
    }

    Set<Integer> unique = new HashSet<>();
    exercisesForState.forEach(e -> unique.add(e.getID()));

    {
      then = System.currentTimeMillis();
      trie = new ExerciseTrie<>(originalSet, language, smallVocabDecoder, false);
      now = System.currentTimeMillis();
      if (now - then > 20) {
        logger.info("took " + (now - then) + " millis to build trie for " + originalSet.size() + " context exercises");
      }

      List<CommonExercise> contextExercises = trie.getExercises(prefix);
      if (contextExercises.isEmpty()) {
        contextExercises = trie.getExercises(StringUtils.stripAccents(prefix));
        logger.info("getSearchMatches context trying " + StringUtils.stripAccents(prefix) + " instead of " + prefix + " found " + contextExercises.size());
      }
      exercisesForState.addAll(contextExercises.stream().filter(e -> !unique.contains(e.getID())).collect(Collectors.toList()));
    }
    if (exercisesForState.isEmpty()) {
      logger.info("getSearchMatches neither " + prefix + " nor " + StringUtils.stripAccents(prefix) + " found any matches against " + exercisesForState.size());

    }
    return exercisesForState;
  }*/
}
