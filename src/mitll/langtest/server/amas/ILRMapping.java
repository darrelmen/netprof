package mitll.langtest.server.amas;

import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.shared.exercise.Shell;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Created by GO22670 on 3/7/14.
 */
public class ILRMapping<T extends Shell> {
  private static final Logger logger = Logger.getLogger(ILRMapping.class);
  public static final String ILR_LEVEL = "ILR Level";
  private static final String TEST_TYPE = "Test type";
  private static final String LISTENING = "Listening";
  private static final String READING = "Reading";
  private static final String QUIZ = "Quiz";

  private final Map<String,Set<String>> levelToExercises = new HashMap<String,Set<String>>();
  private final Map<String,String> exerciseToLevel = new HashMap<String,String>();
  private final Collection<String> readingExercises = new ArrayList<String>();
  private final Collection<String> listeningExercises = new ArrayList<String>();
  private final Map<String,String> exerciseToQuiz = new HashMap<String,String>();

  private final SectionHelper<T> sectionHelper;
  private static final String ENCODING = "UTF8";
  private final boolean usePredefOrder;
  /**
   * @see mitll.langtest.server.amas.FileExerciseDAO#FileExerciseDAO
   * @param configDir
   * @param sectionHelper
   * @param mappingFile
   * @param usePredefOrder
   */
  public ILRMapping(String configDir, SectionHelper<T> sectionHelper, String mappingFile, boolean usePredefOrder) {
    this.sectionHelper = sectionHelper;
    this.usePredefOrder = usePredefOrder;
    File ilrMapping = new File(configDir, mappingFile);
//    logger.debug("config " + configDir + " " + new File(configDir).getAbsolutePath());
    if (ilrMapping.exists()) {
      readILRMapping2(ilrMapping);
    }
    else {
      logger.debug("can't find " + ilrMapping.getAbsolutePath());
    }
    sectionHelper.report();
  }

  /**
   * For now we only have listening at level 0+
   * @see mitll.langtest.server.amas.FileExerciseDAO#readExercises
   */
  public void finalStep() {
    if (useMapping()) {
      sectionHelper.setPredefinedTypeOrder(Arrays.asList(QUIZ, ILRMapping.TEST_TYPE, ILRMapping.ILR_LEVEL));
    }
  }

  private boolean useMapping() { return !exerciseToLevel.isEmpty(); }

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
        if (usePredefOrder) {
          exerciseToQuiz.put(id,"1");
        }
        else {
          String quiz = split[split.length - 1];
          exerciseToQuiz.put(id, quiz.trim());
        }
      }
      logger.debug("level->exercise map has size " + levelToExercises.size() + " keys " + levelToExercises.keySet());
      //logger.debug("listening has size " + listeningExercises.size() + " reading " + readingExercises.size());
      //logger.debug("ex->quiz " +exerciseToQuiz.size() + " " +exerciseToQuiz.values());
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * @see mitll.langtest.server.amas.FileExerciseDAO#readExercises(String, String, String, InputStream)
   * @param exid
   * @param e
   */
  public void addMappingAssoc(String exid, T e) {
    List<SectionHelper.Pair> pairs = new ArrayList<SectionHelper.Pair>();

    SectionHelper.Pair ilrAssoc = sectionHelper.addExerciseToLesson(e, ILR_LEVEL, exerciseToLevel.get(exid));
    pairs.add(ilrAssoc);

    String type = listeningExercises.contains(exid) ? LISTENING : readingExercises.contains(exid) ? READING : "other";
    SectionHelper.Pair typeAssoc = sectionHelper.addExerciseToLesson(e, TEST_TYPE, type);
    pairs.add(typeAssoc);

    List<SectionHelper.Pair> pairs2 = new ArrayList<SectionHelper.Pair>(pairs);

    SectionHelper.Pair quiz = sectionHelper.addExerciseToLesson(e, QUIZ, exerciseToQuiz.get(exid));
    pairs.add(quiz);

 //   SectionHelper.Pair quiz2 = sectionHelper.addExerciseToLesson(e, QUIZ, "Test");
 //   pairs2.add(quiz2);

    sectionHelper.addAssociations(pairs);
    sectionHelper.addAssociations(pairs2);
  }

  private BufferedReader getReader(String lessonPlanFile) throws FileNotFoundException, UnsupportedEncodingException {
    FileInputStream resourceAsStream = new FileInputStream(lessonPlanFile);
    return new BufferedReader(new InputStreamReader(resourceAsStream, ENCODING));
  }

  public void report(Map<String, T> idToExercise) {
    int size = idToExercise.keySet().size();
    Set<String> mappedExercises = getMappedExercises();
    int size1 = mappedExercises.size();
    if (size != size1) {
      logger.warn("report huh? there are " + size + " ids from reading the database, but " + size1 + " from reading the mapping file" );
      Set<String> strings = new HashSet<String>(idToExercise.keySet());
      strings.removeAll(mappedExercises);
      if (!strings.isEmpty()) logger.warn("unmapped are these ids " + strings);

      Set<String> mapped = new TreeSet<String>(mappedExercises);
      mapped.removeAll(idToExercise.keySet());
      if (!mapped.isEmpty()) {
        logger.warn("mapped that don't appear in exercise ids " + mapped);
      }

    }
  }

  private Set<String> getMappedExercises() {
    Set<String> strings = new HashSet<String>();
    for (Set<String> ids : levelToExercises.values()) { strings.addAll(ids); }
    return strings;
  }
}
