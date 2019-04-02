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

package mitll.langtest.server.database;

import mitll.langtest.server.database.exercise.BulkImport;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BulkImportTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(BulkImportTest.class);

  @Test
  public void test() {
    DatabaseImpl spanish = getDatabase();
    spanish.getProject(2);
    Project project = spanish.getProjectManagement().getProductionByLanguage(Language.FRENCH);
    Project english = spanish.getProjectManagement().getProductionByLanguage(Language.ENGLISH);

    String[] lines = new String[2];
    lines[0] = "La tuberculose est une maladie contagieuse.";
    lines[1] = "J'ai  mal au cœur, j'ai des nausées. Il faut appeler un docteur.";

    Set<CommonExercise> knownAlready = new HashSet<>();

    waitUntilTrieReady(project);
    List<CommonExercise> exercises =
        new BulkImport(spanish, spanish).convertTextToExercises(lines, knownAlready, new HashSet<>(), new HashSet<>(), project, english, 6);

    assert(exercises.size() == lines.length);
    exercises.forEach(ex -> logger.info("ex fl " + ex.getForeignLanguage() + " = " + ex.getEnglish()));
  }

  private void waitUntilTrieReady(Project project) {
    for (int i = 0; i < 20; i++) {
      if (project.getFullTrie() == null) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } else break;
    }
  }

}
