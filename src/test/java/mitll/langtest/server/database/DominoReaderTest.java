package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import org.apache.logging.log4j.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/17/16.
 */
public class DominoReaderTest {
  private static final Logger logger = LogManager.getLogger(DominoReaderTest.class);
  private static DatabaseImpl database;

  @BeforeClass
  public static void setup() {
    getDatabase("dominoSpanish");
  }

  private static void getDatabase(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    String name = file.getName();

    logger.debug("config dir " + parent + " config     " + name);
    ServerProperties serverProps = new ServerProperties(parent, name);
    database = new DatabaseImpl(parent, name, serverProps.getH2Database(), serverProps, new PathHelper("war", serverProps), false, null, false);
    // logger.debug("made " + database);
    database.setInstallPath("war", parent + File.separator + database.getServerProps().getLessonPlan());
  }

//  public StartupInfo getProjectStartupInfo() {
//    return new StartupInfo(database.getServerProps().getProperties(), database.getTypeOrder(projectid), database.getSectionNodesForTypes());
//  }

  @Test
  public void testMe() {
  //  Collection<?> exercises = database.getExercises();
  //  Iterator<?> iterator = exercises.iterator();
  //  Object next2 = iterator.next();

//    logger.info("\n\ngot " + exercises.size());
//    logger.info("e.g. " + next2);
//    logger.info("e.g. " + iterator.next());
//    logger.info("e.g. " + iterator.next());
//    logger.info("e.g. " + iterator.next());
//    logger.info("e.g. " + iterator.next());

//    StartupInfo startupInfo = getProjectStartupInfo();
  //  logger.info("Got " + startupInfo);
   // SectionNode next = startupInfo.getSectionNodesForTypes().iterator().next();
  //  Map<String, Collection<String>> typeToSection = new HashMap<>();
  //  typeToSection.put(next.getProperty(), Collections.singleton(next.getName()));
  //  Collection exercisesForSelectionState = database.getSectionHelper().getExercisesForSelectionState(typeToSection);

   // logger.info("got " + exercisesForSelectionState.size() + " e.g. " + exercisesForSelectionState.iterator().next());
  }
}
