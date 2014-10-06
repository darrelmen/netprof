package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.CommonExercise;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by GO22670 on 3/7/14.
 */
public class ILRMapping {
  private static final Logger logger = Logger.getLogger(ILRMapping.class);
  private static final String ILR_LEVEL = "ILR Level";
  private static final String TEST_TYPE = "Test type";

  private final Map<String,Set<String>> levelToExercises = new HashMap<String,Set<String>>();
  private final Map<String,String> exerciseToLevel = new HashMap<String,String>();
  private final Collection<String> readingExercises = new ArrayList<String>();
  private final Collection<String> listeningExercises = new ArrayList<String>();
  private final SectionHelper sectionHelper;

  public ILRMapping(String configDir, SectionHelper sectionHelper, String mappingFile) {
    this.sectionHelper = sectionHelper;
    File ilrMapping = new File(configDir, mappingFile);

    if (ilrMapping.exists()) {
      readILRMapping2(ilrMapping);
    }
    else logger.debug("can't find " + ilrMapping.getAbsolutePath());
  }

  public void finalStep() {
    if (useMapping()) {
      sectionHelper.setPredefinedTypeOrder(Arrays.asList(ILRMapping.TEST_TYPE, ILRMapping.ILR_LEVEL));
    }
  }

  public void report(Map<String, CommonExercise> idToExercise) {
    int size = idToExercise.keySet().size();
    int size1 = getMappedExercises().size();
    if (size != size1) {
      logger.warn("huh? there are " + size + " ids from reading the database, but " + size1 + " from reading the mapping file" );
      Set<String> strings = new HashSet<String>(idToExercise.keySet());
      strings.removeAll(getMappedExercises());
      if (!strings.isEmpty()) logger.warn("unmapped are these ids " + strings);
    }
  }

  public boolean useMapping() { return !exerciseToLevel.isEmpty(); }

  private void readILRMapping2(File ilrMapping) {
    try {
      BufferedReader reader = getReader(ilrMapping.getAbsolutePath());
      String line;
      while ((line = reader.readLine()) != null) {
        String[] split = line.split("\t");
        if (split.length < 2) continue;
        String ilr = split[1].trim();
        ilr = ilr.split("/")[0];
        String id = split[0].trim();
        Set<String> ids = levelToExercises.get(ilr);
        if (ids == null) {
          levelToExercises.put(ilr, ids = new HashSet<String>());
        }
        exerciseToLevel.put(id,ilr);
        ids.add(id);

        String type = split[2].trim();
        if (type.equals("listening")) {
          listeningExercises.add(id);
        }
        else if (type.equals("reading")) {
          readingExercises.add(id);
        }
      }
      logger.debug("level->exercise map has size " + levelToExercises.size() + " keys " + levelToExercises.keySet());
      logger.debug("listening has size " + listeningExercises.size() + " reading " + readingExercises.size());
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * @see mitll.langtest.server.database.exercise.FileExerciseDAO#readExercises(String, String, String, java.io.InputStream)
   * @param exid
   * @param e
   */
  public void addMappingAssoc(String exid, CommonExercise e) {
    List<SectionHelper.Pair> pairs = new ArrayList<SectionHelper.Pair>();

    String level = exerciseToLevel.get(exid);
    SectionHelper.Pair ilrAssoc = sectionHelper.addExerciseToLesson(e, ILR_LEVEL, level);
    pairs.add(ilrAssoc);

    String type = listeningExercises.contains(exid) ? "Listening" : readingExercises.contains(exid) ? "Reading" : "other";
    SectionHelper.Pair typeAssoc = sectionHelper.addExerciseToLesson(e, TEST_TYPE, type);

    pairs.add(typeAssoc);

    sectionHelper.addAssociations(pairs);
  }

  private BufferedReader getReader(String lessonPlanFile) throws FileNotFoundException, UnsupportedEncodingException {
    FileInputStream resourceAsStream = new FileInputStream(lessonPlanFile);
    return new BufferedReader(new InputStreamReader(resourceAsStream, SQLExerciseDAO.ENCODING));
  }

  private Set<String> getMappedExercises() {
    Set<String> strings = new HashSet<String>();
    for (Set<String> ids : levelToExercises.values()) { strings.addAll(ids); }
    return strings;
  }
}
