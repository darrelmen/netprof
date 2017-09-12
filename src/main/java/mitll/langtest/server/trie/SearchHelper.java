package mitll.langtest.server.trie;

import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchHelper {
  private static final Logger logger = LogManager.getLogger(SearchHelper.class);

  @NotNull
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
  }
}
