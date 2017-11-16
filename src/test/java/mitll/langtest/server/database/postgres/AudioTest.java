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
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.audio.AudioDAO;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.*;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(AudioTest.class);
  //public static final boolean DO_ONE = false;

/*  @Test
  public void testWriteAudio() {
    DatabaseImpl spanish = getDatabase("spanish");

    IAudioDAO dao = spanish.getAudioDAO();

    //if (dao.getAudioAttributes().size() == 0) {
//     spanish.copyOneConfig();
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
    logger.info("getRecorded " + dao.getRecordedBySameGender(1));
    logger.info("getRecorded " + dao.getRecordedBySameGender(3));
  }*/

  @Test
  public void testReadAudio() {
    DatabaseImpl spanish = getH2Database("spanish");

    // TODO : replace with h2 user dao.
    IAudioDAO h2AudioDAO = new AudioDAO(spanish, spanish.getUserDAO());

    Map<Integer, Integer> truth = new HashMap<>();
    for (User user : new UserDAO(spanish).getUsers()) {
//      Collection<String> recordedBy = dao.getRecordedBySameGender(user.getID());
      Collection<Integer> recordedBy = h2AudioDAO.getRecordedExForUser(user.getID());
      if (!recordedBy.isEmpty()) {
        logger.info("h2  for " + user.getUserID() + " recorded\t" + recordedBy.size());
      }
    }

    IAudioDAO dao = spanish.getAudioDAO();
//    for (User user : spanish.getUsers()) {
////      Collection<String> recordedBy = dao.getRecordedBySameGender(user.getID());
//      Collection<Integer> recordedBy = dao.getRecordedExForUser(user.getID());
//      if (!recordedBy.isEmpty()) {
//        logger.info("postgres for " + user.getUserID() + " recorded " + recordedBy.size());
//      }
//    }
  }

/*  @Test
  public void testAllAudio() {
    DatabaseImpl spanish = getDatabase("spanish");

    IAudioDAO dao = spanish.getAudioDAO();
    List<User> users = spanish.getUsers();
    long then = System.currentTimeMillis();
    for (User user : users) {
      Collection<Integer> recordedBy = dao.getWithContext(user.getID());
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
      Collection<Integer> recordedBy = audioDAO.getWithContext(user.getID());
      if (!recordedBy.isEmpty()) {
        logger.info("h2 for " + user.getUserID() + " with context " + recordedBy.size());
      }
    }
    now = System.currentTimeMillis();
    logger.info("took " + (now - then) + " millis");

    Map<String, Float> maleFemaleProgress = spanish.getMaleFemaleProgress(-1);
    logger.info("got " + maleFemaleProgress);
    Map<String, Float> maleFemaleProgressH2 = spanish.getH2MaleFemaleProgress(-1);

    logger.info("got  h2 " + maleFemaleProgressH2);

    for (User user : users) {
      Collection<Integer> recordedBy = dao.getRecordedExampleForUser(user.getID());
      if (!recordedBy.isEmpty()) {
        logger.info("postgres for " + user.getUserID() + " getRecordedExampleForUser " + recordedBy.size());
      }
    }
  }*/

/*  @Test
  public void testDefect() {
    DatabaseImpl spanish = getDatabase("spanish");

    IAudioDAO dao = spanish.getAudioDAO();
    List<AudioAttribute> audioAttributes = dao.getExToAudio(projectid).get("2");
    for (AudioAttribute audioAttribute : audioAttributes) {
      logger.info("found " + audioAttribute);
    }
    for (AudioAttribute audioAttribute : audioAttributes) {
      logger.info("defects " + dao.markDefect(audioAttribute));
    }
    audioAttributes = dao.getExToAudio(projectid).get("2");
    if (audioAttributes != null) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        logger.info("found " + audioAttribute);
      }
    }
  }*/

  /*@Test
  public void testUpdate() {
    DatabaseImpl spanish = getDatabase("spanish");

    UserDAO h2UserDAO = new UserDAO(spanish);
    AudioDAO audioDAO = new AudioDAO(spanish, h2UserDAO);

    audioDAO.getExToAudio(projectid);

    IAudioDAO dao = spanish.getAudioDAO();
    List<AudioAttribute> audioAttributes = dao.getExToAudio(projectid).get(3);
    if (audioAttributes != null) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        logger.info("found " + audioAttribute);
      }
//      for (AudioAttribute audioAttribute : audioAttributes) {
//        dao.updateExerciseID(audioAttribute.getUniqueID(), "dude");
//      }
    } else logger.info("no exercises under 3");


    audioAttributes = dao.getExToAudio(projectid).get(2222222);
    if (audioAttributes != null) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        logger.info("found " + audioAttribute);
      }
    }
  }*/
}
