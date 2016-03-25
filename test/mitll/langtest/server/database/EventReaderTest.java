package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.hibernate.AnnotationEvent;
import mitll.langtest.server.database.hibernate.SessionManagement;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.instrumentation.Event;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.*;

/**
 * Created by go22670 on 2/17/16.
 */
public class EventReaderTest {
  private static final Logger logger = Logger.getLogger(EventReaderTest.class);
  private static DatabaseImpl database;

  @BeforeClass
  public static void setup() {
    getDatabase("farsi");
  }

  private static void getDatabase(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    String name = file.getName();

    logger.debug("config dir " + parent + " config     " + name);
    ServerProperties serverProps = new ServerProperties(parent, name);
    database = new DatabaseImpl(parent, name, serverProps.getH2Database(), serverProps, new PathHelper("war"), false, null);
    // logger.debug("made " + database);
    database.setInstallPath("war", parent + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());
  }

  public StartupInfo getStartupInfo() {
    return new StartupInfo(database.getServerProps().getProperties(), database.getTypeOrder(), database.getSectionNodes());
  }

  @Test
  public void testMe() {
//    database.createDatabase();

    List<Event> all = database.getEventDAO().getAll();

    SessionManagement sessionManagement = database.getSessionManagement();

    Event next1 = all.iterator().next();

    logger.info("original " + next1);

    sessionManagement.doInTranscation((session)->{
      session.save( next1 );
    });

    Event ret = sessionManagement.getFromTransaction(new SessionManagement.SessionSupplier<Event>() {
      @Override
      public Event get() {
        List<Event> result = session.createQuery( "from Event" ).list();
        return result.isEmpty() ? null : result.get(0);
      }
    });

    try {
      sessionManagement.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    logger.info("\n\ngot " + ret);
  }
}
