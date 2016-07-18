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

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.CopyToPostgres;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.npdata.dao.DBConnection;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.File;

public class PostgresTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(PostgresTest.class);

  @Test
  public void testCreate() {
    getDatabaseVeryLight("spanish", false).createTables();
  }

  @Test
  public void testDrop() {
    getConnection("spanish").dropAll();
  }

  @Test
  public void testCopy() {
    testCreate();
    getDatabaseLight("spanish", true).copyToPostgres();
  }

  @Test
  public void testCopyProject() {
    testCreate();
    DatabaseImpl<CommonExercise> spanish = getDatabaseLight("spanish", true);
    new CopyToPostgres().createProjectIfNotExists(spanish);
  }

  @Test
  public void testCopyUserExercises() {
    testCreate();
    DatabaseImpl<CommonExercise> spanish = getDatabaseLight("spanish", true);
   // new CopyToPostgres().copyOnlyUserExercises(spanish);
  }

  @Test
  public void testCopyUserJoinExercises() {
    testCreate();
    DatabaseImpl<CommonExercise> spanish = getDatabaseLight("spanish", true);
 //   new CopyToPostgres().copyUserExListJoin(spanish);
  }

  private static DBConnection getConnection(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParentFile().getAbsolutePath();
    logger.info("path is " + parent);
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    return new DBConnection(serverProps.getDatabaseType(),
        serverProps.getDatabaseHost(), serverProps.getDatabasePort(), serverProps.getDatabaseName(), serverProps.getDatabaseUser(), serverProps.getDatabasePassword());
  }
}
