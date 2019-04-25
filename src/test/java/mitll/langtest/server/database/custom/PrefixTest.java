package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.project.Project;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.*;

public class PrefixTest extends BaseTest {

  private static final Logger logger = LogManager.getLogger(PrefixTest.class);


  @Test
  public void test() {

    DatabaseImpl french = getDatabase();
    int projectid = 15;
    Project project = french.getProject(projectid, true);
    Map<String, Set<String>> wordToNeighbors = project.getAudioFileHelper().makeOneEditMap(5);
    Set<String> thing = wordToNeighbors.get("thing");
    logger.info("got " + thing);

    Set<String> thing2 = wordToNeighbors.get("sing");
    logger.info("got " + thing2);

    Set<String> thing3 = wordToNeighbors.get("ring");
    logger.info("got " + thing2);
  }

  @Test
  public void testSpanish() {
    String s = "deber\n" +
        "entonces\n" +
        "poner\n" +
        "cosa\n" +
        "tanto\n" +
        "hombre\n" +
        "parecer\n" +
        "nuestro\n" +
        "tan\n" +
        "donde\n" +
        "ahora\n" +
        "parte\n" +
        "después\n" +
        "vida\n" +
        "quedar\n" +
        "siempre\n" +
        "creer\n" +
        "hablar\n" +
        "llevar\n" +
        "dejar\n" +
        "nada\n" +
        "cada\n" +
        "seguir\n" +
        "menos\n" +
        "nuevo\n" +
        "encontrar";
    s.split("\n");
    List<String> spanish = Arrays.asList(s.split("\n"));
    List<String> s2 = new ArrayList<>();
    for (String spanish1 : spanish) {
      String trim = spanish1.trim();
      s2.add(trim);

    }


    logger.info("got " + s2);

    DatabaseImpl french = getDatabase();
    int projectid = 3;
    Project project = french.getProject(projectid, true);
    Map<String, Set<String>> wordToNeighbors = project.getAudioFileHelper().makeOneEditMap(5);
    spanish.forEach(w -> logger.info(w + " = " + wordToNeighbors.get(w)));
  }
}
