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

package mitll.langtest.server.database;

import mitll.langtest.client.user.Md5Hash;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.audio.AudioDAO;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.instrumentation.IEventDAO;
import mitll.langtest.server.database.result.IAnswerDAO;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.shared.AudioType;
import mitll.langtest.shared.User;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickSlimEvent;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.util.*;

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
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");

    IEventDAO eventDAO = spanish.getEventDAO();
    List<Event> all = eventDAO.getAll("spanish");
    for (Event event : all.subList(0, getMin(all))) logger.info("Got " + event);

    List<SlickSlimEvent> spanish1 = eventDAO.getAllSlim("spanish");
    for (SlickSlimEvent event : spanish1.subList(0, getMin(spanish1))) logger.info("Got " + event);

    List<SlickSlimEvent> allDevicesSlim = eventDAO.getAllDevicesSlim("spanish");
    for (SlickSlimEvent event : allDevicesSlim.subList(0, getMin(allDevicesSlim))) logger.info("Got " + event);

    eventDAO.addPlayedMarkings(1, spanish.getExercises().iterator().next());

    logger.info("Got " + eventDAO.getFirstSlim("spanish"));
    //  spanish.doReport(new PathHelper("war"));
  }

  int getMin(List<?> all) {
    return Math.min(all.size(), 10);
  }

  @Test
  public void testEvent() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");

    IEventDAO eventDAO = spanish.getEventDAO();

    eventDAO.add(new Event("123", "button", "2334", "testing", 1, System.currentTimeMillis(), "device"), "spanish");
    logger.info("Got " + eventDAO.getFirstSlim("spanish"));

    //  spanish.doReport(new PathHelper("war"));
  }

  /**
   * TODO : add option to get mock db to use
   */
  @Test
  public void testUser() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");

    IUserDAO dao = spanish.getUserDAO();

    List<User> users = dao.getUsers();

    logger.info("got " + users);

    String userid = "userid";
    String hash = Md5Hash.getHash("g@gmail.com");
    String password = Md5Hash.getHash("password");
    String bueller = Md5Hash.getHash("bueller");
    User user = dao.addUser(userid, password, hash, User.Kind.STUDENT, "128.0.0.1", true, 89, "midest", "browser");
    User user2 = dao.addUser(userid, password, hash, User.Kind.STUDENT, "128.0.0.1", true, 89, "midest", "iPad");

    logger.info("made " + user);
    logger.info("made " + user2);
    User user3 = dao.addUser(userid + "ipad", password, hash, User.Kind.STUDENT, "128.0.0.1", true, 89, "midest", "iPad");

    if (user == null) {
      user = dao.getUserByID(userid);
      logger.info("found " + user);
    }

    logger.info("before " + user.isEnabled());

    int id = user.getId();
    boolean b = dao.enableUser(id);

    logger.info("enable user " + b);

    logger.info("change enabled " + dao.changeEnabled(id, true) + " : " + dao.getUserWhere(id).isEnabled());
    logger.info("change enabled " + dao.changeEnabled(id, false) + " : " + dao.getUserWhere(id).isEnabled());
    int bogusID = 123456789;
    logger.info("change enabled " + dao.changeEnabled(bogusID, false) + " : " + dao.getUserWhere(id).isEnabled());


    logger.info("user " + dao.getIDForUserAndEmail(userid, hash));
    logger.info("user " + dao.getIDForUserAndEmail(userid, password));
    logger.info("user id " + dao.getIdForUserID(userid));
    logger.info("user " + dao.getUser(userid, password));
    logger.info("user " + dao.getUser(userid, hash));
    logger.info("user " + dao.getUserWithPass(userid, password));
    logger.info("user " + dao.getUserWithPass(userid, hash));
    logger.info("user devices " + dao.getUsersDevices());
    logger.info("mini " + dao.getMiniUsers());
    logger.info("mini first " + dao.getMiniUser(dao.getMiniUsers().keySet().iterator().next()));
    logger.info("males " + dao.getUserMap(true));
    logger.info("females " + dao.getUserMap(false));
    logger.info("all " + dao.getUserMap());

    logger.info("valid email " + dao.isValidEmail(hash));
    logger.info("valid email " + dao.isValidEmail(password));
    logger.info("change password\n" +
        dao.getUserWithPass(userid, password) + " :\n" +
        dao.changePassword(id, bueller) + "\n" +
        dao.getUserWithPass(userid, password) + " :\n" +
        dao.getUserWithPass(userid, bueller));

    logger.info("reset key " +
        dao.getUserWithResetKey("reset") +
        ":\n" + dao.updateKey(id, true, "reset") +
        " :\n" + dao.getUserWithResetKey("reset") +
        ":\n" + dao.clearKey(id, true) +
        " :\n" + dao.getUserWithResetKey("reset")
    );

    logger.info("enabled key " +
        dao.getUserWithResetKey("enabled") +
        ":\n" + dao.updateKey(id, false, "enabled") +
        " :\n" + dao.getUserWithEnabledKey("enabled") +
        ":\n" + dao.clearKey(id, false) +
        ":\n" + dao.clearKey(bogusID, false) +
        " :\n" + dao.getUserWithEnabledKey("enabled")
    );
    //  spanish.doReport(new PathHelper("war"));
  }

  @Test
  public void testWriteAudio() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");

    IAudioDAO dao = spanish.getAudioDAO();

    //if (dao.getAudioAttributes().size() == 0) {
//     spanish.copyToPostgres();
    // }

    Collection<AudioAttribute> audio = dao.getAudioAttributes();

    logger.info("Got back " + audio.size());

    AudioAttribute audioAttribute = dao.addOrUpdate(1, "1", AudioType.REGULAR, "file.wav", System.currentTimeMillis(), 1000, "dude");
    logger.info("Got audioAttribute " + audioAttribute);
    logger.info("Got now " + dao.getAudioAttributes().size());

    audioAttribute = dao.addOrUpdate(1, "2", AudioType.SLOW, "file2.wav", System.currentTimeMillis(), 1000, "dude");

    audioAttribute = dao.addOrUpdate(2, "1", AudioType.REGULAR, "file3.wav", System.currentTimeMillis(), 1000, "dude");

    logger.info("Got now " + dao.getAudioAttributes().size());
    logger.info("Got ex to audio " + dao.getExToAudio().size());

    CommonExercise exercise = spanish.getExercise("1");
    logger.info("before attach " + exercise.getAudioAttributes().size());
    logger.info("attach " + dao.attachAudio(exercise, ".", "."));
    logger.info("after  attach  " + exercise.getAudioAttributes().size());
    logger.info("getRecorded " + dao.getRecordedBy(1));
    logger.info("getRecorded " + dao.getRecordedBy(3));
  }

  @Test
  public void testCopy() {   getDatabaseLight("spanish").copyToPostgres();  }

  @Test
  public void testDrop() {
    getConnection("spanish").dropAll();
  }

  protected static DBConnection getConnection(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    String name = file.getName();

    parent = file.getParentFile().getAbsolutePath();

    logger.info("path is "+ parent);
    ServerProperties serverProps = new ServerProperties(parent, name);
    return new DBConnection(serverProps.getDatabaseType(),
        serverProps.getDatabaseHost(), serverProps.getDatabasePort(), serverProps.getDatabaseName());
  }
  @Test
  public void testCreate() {
    getDatabaseVeryLight("spanish").createTables();
  }

  @Test
  public void testReadAudio() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");

    IAudioDAO h2AudioDAO = spanish.getH2AudioDAO();

    Map<Integer, Integer> truth = new HashMap<>();
    for (User user : new UserDAO(spanish).getUsers()) {
//      Collection<String> recordedBy = dao.getRecordedBy(user.getId());
      Collection<String> recordedBy = h2AudioDAO.getRecordedForUser(user.getId());
      if (!recordedBy.isEmpty()) {
        logger.info("h2  for " + user.getUserID() + " recorded\t" + recordedBy.size());
      }
    }

    IAudioDAO dao = spanish.getAudioDAO();
    for (User user : spanish.getUsers()) {
//      Collection<String> recordedBy = dao.getRecordedBy(user.getId());
      Collection<String> recordedBy = dao.getRecordedForUser(user.getId());
      if (!recordedBy.isEmpty()) {
        logger.info("postgres for " + user.getUserID() + " recorded " + recordedBy.size());
      }
    }
  }

  @Test
  public void testAllAudio() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");

    IAudioDAO dao = spanish.getAudioDAO();
    List<User> users = spanish.getUsers();
    long then = System.currentTimeMillis();
    for (User user : users) {
      Collection<String> recordedBy = dao.getWithContext(user.getId());
      if (!recordedBy.isEmpty()) {
        logger.info("postgres for " + user.getUserID() + " with context " + recordedBy.size());
      }
    }
    long now = System.currentTimeMillis();

    logger.info("took " + (now - then) + " millis");

    UserDAO h2UserDAO = new UserDAO(spanish);
    AudioDAO audioDAO = new AudioDAO(spanish, h2UserDAO);
    then = System.currentTimeMillis();
    for (User user : h2UserDAO.getUsers()) {
      Collection<String> recordedBy = audioDAO.getWithContext(user.getId());
      if (!recordedBy.isEmpty()) {
        logger.info("h2 for " + user.getUserID() + " with context " + recordedBy.size());
      }
    }
    now = System.currentTimeMillis();
    logger.info("took " + (now - then) + " millis");

    Map<String, Float> maleFemaleProgress = spanish.getMaleFemaleProgress();
    logger.info("got " + maleFemaleProgress);
    Map<String, Float> maleFemaleProgressH2 = spanish.getH2MaleFemaleProgress();

    logger.info("got  h2 " + maleFemaleProgressH2);

    for (User user : users) {
      Collection<String> recordedBy = dao.getRecordedExampleForUser(user.getId());
      if (!recordedBy.isEmpty()) {
        logger.info("postgres for " + user.getUserID() + " getRecordedExampleForUser " + recordedBy.size());
      }
    }
  }

  @Test
  public void testDefect() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");

    IAudioDAO dao = spanish.getAudioDAO();
    List<AudioAttribute> audioAttributes = dao.getExToAudio().get("2");
    for (AudioAttribute audioAttribute : audioAttributes) {
      logger.info("found " + audioAttribute);
    }
    for (AudioAttribute audioAttribute : audioAttributes) {
      logger.info("defects " + dao.markDefect(audioAttribute));
    }
    audioAttributes = dao.getExToAudio().get("2");
    if (audioAttributes != null) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        logger.info("found " + audioAttribute);
      }
    }
  }

  @Test
  public void testUpdate() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");

    UserDAO h2UserDAO = new UserDAO(spanish);
    AudioDAO audioDAO = new AudioDAO(spanish, h2UserDAO);

    audioDAO.getExToAudio();

    IAudioDAO dao = spanish.getAudioDAO();
    List<AudioAttribute> audioAttributes = dao.getExToAudio().get("3");
    if (audioAttributes != null) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        logger.info("found " + audioAttribute);
      }
      for (AudioAttribute audioAttribute : audioAttributes) {
        dao.updateExerciseID(audioAttribute.getUniqueID(), "dude");
      }
    } else logger.info("no exercises under 3");


    audioAttributes = dao.getExToAudio().get("dude");
    if (audioAttributes != null) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        logger.info("found " + audioAttribute);
      }
    }
  }

  @Test
  public void testResult() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");

    IResultDAO resultDAO = spanish.getResultDAO();

    List<Result> results = resultDAO.getResults();
    int size = results.size();
    Result first = results.get(0);
    logger.info("got " + size + " first " + first);

    Collection<Result> resultsDevices = resultDAO.getResultsDevices();
    logger.info("got " + resultsDevices.size() + " first " + resultsDevices.iterator().next());

    int uniqueID = first.getUniqueID();
    logger.info("Got " + resultDAO.getResultByID(uniqueID));
    logger.info("Got " + resultDAO.getMonitorResults().size());
    String exid = first.getExid();
    logger.info("Got " + resultDAO.getMonitorResultsByID(exid));
    Collection<UserAndTime> userAndTimes = resultDAO.getUserAndTimes();
    logger.info("Got " + userAndTimes.size() + " " + userAndTimes.iterator().next());

    logger.info("Got " + resultDAO.getSessions());
    logger.info("Got " + resultDAO.getResultsForExIDInForUser(Collections.singleton(exid),1,""));
    logger.info("Got " + resultDAO.getNumResults());
  }

  @Test
  public void testAnswerDAO() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");
  }


}
