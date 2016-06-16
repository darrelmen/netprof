package mitll.langtest.server.database;

import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.Collection;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/30/14.
 */
public class PostgresTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(PostgresTest.class);
  //public static final boolean DO_ONE = false;

  @Test
  public void testSpanishEventCopy() {
     getDatabase("spanish");
  }

  @Test
  public void testSudaneseEval() {
    DatabaseImpl<CommonExercise> sudaneseEval = getDatabase("sudaneseEval");
    Collection<CommonExercise> exercises = sudaneseEval.getExercises();
    Collection<CommonExercise> exercisesForSelectionState =
        sudaneseEval.getSectionHelper().getExercisesForSelectionState("Chapter","1");
    logger.info("Got " +exercisesForSelectionState);
    logger.info("first " + exercises.iterator().next());
    logger.info("type order " + sudaneseEval.getTypeOrder());
  }
}
