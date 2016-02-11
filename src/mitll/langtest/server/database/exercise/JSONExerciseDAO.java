package mitll.langtest.server.database.exercise;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by go22670 on 2/10/16.
 */
public class JSONExerciseDAO extends BaseExerciseDAO implements ExerciseDAO {
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
    JsonExport jsonExport = new JsonExport(null, sectionHelper, null);

    try {
      byte[] encoded = Files.readAllBytes(Paths.get(jsonFile));
      String asString = new String(encoded, ENCODING);
      logger.info("readExercises reading from " + jsonFile);

      List<CommonExercise> exercises = jsonExport.getExercises(asString);

      for (CommonExercise ex : exercises) {
        Collection<SectionHelper.Pair> pairs = new ArrayList<>();
        for (Map.Entry<String,String> pair : ex.getUnitToValue().entrySet()) {
          pairs.add(getSectionHelper().addExerciseToLesson(ex, pair.getKey(), pair.getValue()));
        }
        getSectionHelper().addAssociations(pairs);
      }

      logger.info("read " +exercises.size() + " from " + jsonFile);
      return exercises;
    } catch (IOException e) {
      logger.error("got " +e,e);
    }
    return Collections.emptyList();
  }
}
