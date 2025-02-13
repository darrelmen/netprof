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

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/10/16.
 */
public class JSONExerciseDAO extends BaseExerciseDAO implements ExerciseDAO<CommonExercise> {
  private static final Logger logger = Logger.getLogger(JSONExerciseDAO.class);

  private static final String ENCODING = "UTF8";

  private final String jsonFile;

  /**
   * @param file
   * @param userListManager
   * @param addDefects
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  public JSONExerciseDAO(String file,
                         ServerProperties serverProps,
                         UserListManager userListManager,
                         boolean addDefects) {
    super(serverProps, userListManager, addDefects);
    this.jsonFile = file;
  }

  @Override
  List<CommonExercise> readExercises() {
    JsonExport jsonExport = new JsonExport(null, sectionHelper, null, serverProps.getLanguage().equalsIgnoreCase("english"));

    try {
      Path path = Paths.get(jsonFile);
      byte[] encoded = Files.readAllBytes(path);
      String asString = new String(encoded, ENCODING);
      logger.info("readExercises reading from " + path.toFile().getAbsolutePath());

      List<CommonExercise> exercises = jsonExport.getExercises(asString);
      populateSections(exercises);

      logger.info("read " +exercises.size() + " from " + jsonFile);
      return exercises;
    } catch (IOException e) {
      logger.error("got " +e,e);
    }
    return Collections.emptyList();
  }

  protected void populateSections(List<CommonExercise> exercises) {
    for (CommonExercise ex : exercises) {
      Collection<SectionHelper.Pair> pairs = new ArrayList<>();
      for (Map.Entry<String,String> pair : ex.getUnitToValue().entrySet()) {
        pairs.add(getSectionHelper().addExerciseToLesson(ex, pair.getKey(), pair.getValue()));
      }
      getSectionHelper().addAssociations(pairs);
    }
  }
}
