package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.instrumentation.IEventDAO;
import mitll.langtest.shared.instrumentation.Event;
import org.apache.logging.log4j.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

/**
 * Created by go22670 on 2/17/16.
 */
public class EventReaderTest {
  private static final Logger logger = LogManager.getLogger(EventReaderTest.class);
  private static DatabaseImpl database;

  @BeforeClass
  public static void setup() {
    getDatabase("spanish");
  }

  private static void getDatabase(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    String name = file.getName();

    logger.debug("config dir " + parent + " config     " + name);
    ServerProperties serverProps = new ServerProperties(parent, name);
    database = new DatabaseImpl(parent, name, serverProps.getH2Database(), serverProps, new PathHelper("war", serverProps), false, null, false);
    // logger.debug("made " + database);
    database.setInstallPath("war", parent + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());
  }

//  public StartupInfo getProjectStartupInfo() {
//    return new StartupInfo(database.getServerProps().getProperties(), database.getTypeOrder(projectid), database.getSectionNodes());
//  }

  @Test
  public void testReadOne() {
    IEventDAO eventDAO = database.getEventDAO();
    // boolean empty = ((HEventDAO) eventDAO).isEmpty();

    //logger.info("is empty " +empty);

    Number numRows = eventDAO.getNumRows(0);//"spanish");
    logger.info("num rows " + numRows);

    Event next = eventDAO.getAll().iterator().next();
    logger.info("first event " + next);
/*
    List<Event> all = eventDAO.getAllDevices();
    logger.info("first device " + all.iterator().next());*/
  }

/*
  @Test
  public void testReadOnePhone() {
    HPhoneDAO hPhoneDAO = new HPhoneDAO(database);
    boolean phone = hPhoneDAO.isEmpty("Phone");
    logger.info("is empty " +phone);

    boolean phone2 = hPhoneDAO.isEmptyForClass(mitll.langtest.server.database.phone.Phone.class);
    logger.info("is empty " +phone2);
    Number numRows = hPhoneDAO.getNumRowsByClass(mitll.langtest.server.database.phone.Phone.class);
    logger.info("num rows " + numRows);

    HDAO<Word> wordHDAO = new HDAO<>(database);
    boolean empty = wordHDAO.isEmpty("Word");
    logger.info("is empty " +empty);

  }

  @Test
  public void testMe() {
    List<Event> all = database.getEventDAO().getAllPredef();

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

  @Test
  public void testEventCopy() {
    SessionManagement sessionManagement = database.getSessionManagement();

    HEventDAO heventDAO = new HEventDAO(database, database.getUserDAO().getDefectDetector());
    heventDAO.addAll(database.getEventDAO().getAllPredef());

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
  }*/
}
