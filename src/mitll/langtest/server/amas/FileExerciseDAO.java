package mitll.langtest.server.amas;

import mitll.langtest.server.database.AudioDAO;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.UserExerciseDAO;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.exercise.CommonUserExercise;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Reads plan directly from a CSV.
 * <p>
 * TODO : Ideally we'd load the data into h2, then read it out again.
 * <p>
 * User: GO22670
 * Date: 10/8/12
 * Time: 3:35 PM
 * To change this template use File | Settings | File Templates.
 * hasn't been used in a long time
 */
public class FileExerciseDAO implements ExerciseDAO {
  private static final Logger logger = Logger.getLogger(FileExerciseDAO.class);

  private static final String FILE_PREFIX = "file://";
  private static final int FILE_PREFIX_LENGTH = "file://".length();

  private static final String ENCODING = "UTF8";
  private static final int MAX_ERRORS = 100;
  private static final String MP3 = ".mp3";
  private static final boolean WRITE_ANSWER_KEY = false;
  private final String mediaDir;

  private List<CommonExercise> exercises;
  private final Map<String, CommonExercise> idToExercise = new HashMap<String, CommonExercise>();
  private final SectionHelper sectionHelper = new SectionHelper();
  private final List<String> errors = new ArrayList<String>();
  private final ILRMapping ilrMapping;
  private String configDir;
  private String language;

  /**
   * @param mediaDir
   * @param language
   * @param mappingFile
   * @see mitll.langtest.server.database.DatabaseImpl#makeExerciseDAO
   */
  public FileExerciseDAO(String mediaDir, String language, String configDir, String mappingFile) {
    this.mediaDir = mediaDir;
    this.language = language;
    logger.debug("media dir " + mediaDir);
    ilrMapping = new ILRMapping(configDir, sectionHelper, mappingFile, false); // TODO : correct???
    this.configDir = configDir;
  }

  @Override
  public SectionHelper getSectionHelper() {
    return sectionHelper;
  }

  @Override
  public CommonExercise addOverlay(CommonUserExercise userExercise) {
    return null;
  }

  @Override
  public void add(CommonUserExercise userExercise) {

  }

  @Override
  public boolean remove(String id) {
    return false;
  }

  @Override
  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) {

  }

  @Override
  public void setAddRemoveDAO(AddRemoveDAO addRemoveDAO) {

  }

  public CommonExercise getExercise(String id) {
    if (idToExercise.isEmpty()) logger.warn("huh? couldn't find any exercises..?");
    if (!idToExercise.containsKey(id)) {
      logger.warn("couldn't find " + id + " in " + idToExercise.size() + " exercises...");
    }
    return idToExercise.get(id);
  }

  @Override
  public void setAudioDAO(AudioDAO audioDAO, String mediaDir, String installPath) {

  }

  @Override
  public void attachAudio(Collection<CommonUserExercise> all) {

  }

  private void populateIDToExercise(List<CommonExercise> exercises) {
    for (CommonExercise e : exercises) idToExercise.put(e.getID(), e);
  }

  /**
   * @param installPath
   * @param lessonPlanFile
   * @see mitll.langtest.server.database.DatabaseImpl#getRawExercises
   */
  public synchronized void readFastAndSlowExercises(final String installPath, String configDir, String lessonPlanFile) {
    if (exercises != null) return;
    File file = new File(lessonPlanFile);
    if (!file.exists()) {
      logger.error("can't find '" + file + "'");
      return;
    }

    InputStream resourceAsStream;
    try {
      resourceAsStream = new FileInputStream(lessonPlanFile);
    } catch (FileNotFoundException e) {
      logger.error("Couldn't find " + lessonPlanFile);
      return;
    }

    exercises = readExercises(installPath, configDir, lessonPlanFile, resourceAsStream);
  }

  /**
   * @param installPath
   * @param configDir
   * @param lessonPlanFile
   * @param resourceAsStream
   * @return
   * @see #readFastAndSlowExercises(String, String, String)
   */
  private List<CommonExercise> readExercises(String installPath, String configDir, String lessonPlanFile,
                                             InputStream resourceAsStream) {
    List<AmasExerciseImpl> exercises = new ArrayList<>();

    try {
      BufferedReader reader = getBufferedReader(resourceAsStream);
      String line2;
      int count = 0;

      if (WRITE_ANSWER_KEY) {
        doBuffer();
      }
      String lastID = "";
      int qid = 0;

      AmasExerciseImpl lastAmasExerciseImpl = null;
      while ((line2 = reader.readLine()) != null) {
        count++;

        try {
          String[] split = line2.split("\\t");
          String id = split[0].trim();

          if (id.equals(lastID)) {
            qid++;
          } else {
            qid = 0;
          }

          AmasExerciseImpl amasExerciseImpl = readTSVLine(installPath, configDir, line2, qid);

          if (amasExerciseImpl != null) {
            //   id++;
            if (amasExerciseImpl.getID().equals(lastID)) {
              //logger.debug("readExercises ex " + lastID + " adding " + exercise.getEnglishQuestions());
              if (lastAmasExerciseImpl != null) {
                lastAmasExerciseImpl.addQuestions(AmasExerciseImpl.EN, amasExerciseImpl.getEnglishQuestions());
                lastAmasExerciseImpl.addQuestions(AmasExerciseImpl.FL, amasExerciseImpl.getForeignLanguageQuestions());
              }
            } else {
              exercises.add(amasExerciseImpl);
              ilrMapping.addMappingAssoc(amasExerciseImpl.getID(), amasExerciseImpl);
              lastAmasExerciseImpl = amasExerciseImpl;
            }

            lastID = amasExerciseImpl.getID();
          } else {
            logger.warn("readExercises Skipping line " + line2);
            qid = 0;
          }

        } catch (Exception e) {
          logger.error("Got " + e + ".Skipping line -- couldn't parse line #" + count + " : " + line2);
          errors.add("line #" + count + " : " + line2);
          if (errors.size() > MAX_ERRORS) {
            logger.error("too many errors (" + errors.size() + "), giving up...");
            break;
          }
        }
      }
      ilrMapping.finalStep();
      reader.close();
      if (WRITE_ANSWER_KEY) {
        writer.close();
      }
    } catch (IOException e) {
      logger.error("readExercises reading " + lessonPlanFile + " got " + e, e);
    }
    if (exercises.isEmpty()) {
      logger.error("readExercises no exercises found in " + lessonPlanFile + "?");
    } else {
      logger.debug("readExercises found " + exercises.size() + " exercises, first is " + exercises.iterator().next());
    }
    //  if (CONFIRM_AUDIO_REFS) confirmAudioRefs(exercises);
    populateIDToExercise(exercises);
    ilrMapping.report(idToExercise);
    return exercises;
  }

/*  private void confirmAudioRefs(List<CommonExercise> exercises) {
    int c = 0;
    try {
      FileWriter writer = new FileWriter("missingAudio.txt");
      for (CommonExercise e : exercises) {
        String refAudio = e.getRefAudio();
        File file = new File(refAudio);
        if (!file.exists()) {
          writer.write(e.getID() + "\n");
          if (c++ < 10) logger.warn("missing audio " + e.getID() + " at " + file.getAbsolutePath());
        }
      }
      writer.close();
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
    if (c > 0) logger.warn("there were " + c + " items with missing audio");
  }*/

  private BufferedReader getBufferedReader(InputStream resourceAsStream) throws UnsupportedEncodingException {
    return new BufferedReader(new InputStreamReader(resourceAsStream, ENCODING));
  }

  /**
   * Read from tsv file that points to other files.
   *
   * @param installPath
   * @param configDir
   * @param line
   * @return
   * @see #readExercises(String, String, String, InputStream)
   */
  private AmasExerciseImpl readTSVLine(String installPath, String configDir, String line, int qid) {
    if (line.trim().length() == 0) {
      logger.debug("readTSVLine skipping empty line");
      return null;
    }
    String[] split = line.split("\\t");
    int i = 0;
    String id = split[i++].trim();
    /*String level =*/
    split[i++].trim();
    String type = split[i++].trim();
    String includeFile = split[i++].trim();

    File include = getIncludeFile(configDir, includeFile);

//    logger.debug("config      " + configDir);
//    logger.debug("this.config " + this.configDir);
//    logger.debug("include     " + include.getAbsolutePath());
//    logger.debug("installPath " + installPath);

    if (!include.exists()) {
      File configDirFile = new File(installPath, configDir);
/*      logger.warn("couldn't open file " + include.getName() + " at " +include.getAbsolutePath() + " config '" + configDir +"' with install path'" +
        installPath +
        "' new config dir " + configDirFile.getAbsolutePath());*/
      //   logger.debug("1 config " + configDirFile.getAbsolutePath());
      include = getIncludeFile(configDirFile.getAbsolutePath(), includeFile);
    }

    if (!include.exists()) {
      File configDirFile = new File(installPath);
/*      logger.warn("couldn't open file " + include.getName() + " at " +include.getAbsolutePath() + " config '" + configDir +"' with install path'" +
        installPath +
        "' new config dir " + configDirFile.getAbsolutePath());*/
      // logger.debug("2 config " + configDirFile.getAbsolutePath());
      include = getIncludeFile(configDirFile.getAbsolutePath(), includeFile);
      //  logger.debug("path " +include.getAbsolutePath());
    }

    if (!include.exists()) {
      File configDirFile = new File(installPath, this.configDir);
/*      logger.warn("couldn't open file " + include.getName() + " at " +include.getAbsolutePath() + " config '" + configDir +"' with install path'" +
        installPath +
        "' new config dir " + configDirFile.getAbsolutePath());*/
      // logger.debug("3 config " + configDirFile.getAbsolutePath());
      include = getIncludeFile(configDirFile.getAbsolutePath(), includeFile);
    }

    boolean exists = include.exists();
    if (!exists) {
      //  if (false) {
      logger.error("couldn't open file " + include.getName() + " at " + include.getAbsolutePath() + " config " + configDir +
          " for line " + line);
      //   }
      // return null;
    }
    //else {
    boolean listening = type.equalsIgnoreCase("listening");
    String content = exists ? getContentFromIncludeFile(installPath, include, listening) : "";
    if (content.isEmpty()) {
      logger.error("no content for exercise " + id + " type " + type);
      //  return null;
    }

    String arabicQuestion = split[i++].trim();
    String englishQuestion = split[i++].trim();
    String arabicAnswers = split[i++].trim();
    String englishAnswers = split[i++].trim();
    String altID = i < split.length ? split[i++].trim() : "";

    AmasExerciseImpl amasExerciseImpl = new AmasExerciseImpl(id, content, altID);

    addQuestion(arabicQuestion, arabicAnswers, amasExerciseImpl, true, qid);
    addQuestion(englishQuestion, englishAnswers, amasExerciseImpl, false, qid);
    return amasExerciseImpl;
  }

  private File getIncludeFile(String configDir, String includeFile) {
    if (includeFile.startsWith(FILE_PREFIX)) {
      includeFile = includeFile.substring(FILE_PREFIX_LENGTH);
  /*    logger.debug("1 after '" +  includeFile+
        "'");*/
    } else if (includeFile.startsWith("file:/")) { // weird issue with pashto...
      logger.debug("2 include '" + includeFile + "'");
      includeFile = includeFile.substring("file:/".length());
    }
    return new File(configDir, includeFile);
  }

  /**
   * If listening, include HTML 5 audio reference, otherwise include text from file.
   *
   * @param include
   * @param isListening
   * @return
   * @see #readTSVLine
   */
  private String getContentFromIncludeFile(String installPath, File include, boolean isListening) {
    StringBuilder builder = new StringBuilder();
    try {
      readFromFile(installPath, include, builder);

      if (isListening) {
        String audioFileEquivalent = include.getName().replace(".html", ".wav");
        String audioPath = getFilePath(installPath, audioFileEquivalent);
        if (audioPath == null) {
          //         logger.warn("for " + id + " couldn't find audio file " + audioFileEquivalent);
        } else {
          //   logger.debug("audio " + audioFileEquivalent + " is " +audioPath);
          builder.append(getHTML5Audio(audioPath));
        }
      }
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
    return builder.toString();
  }

  /**
   * @param installPath
   * @param audioFileEquivalent
   * @return
   * @see #readFromFile
   */
  private String getFilePath(String installPath, String audioFileEquivalent) {
    String audioPath = audioFileEquivalent;
    File file = new File(audioPath);
    boolean exists = file.exists();
    if (!exists) {
      // logger.debug("1 not at " +file.getAbsolutePath());
      file = new File(installPath, audioPath);
      exists = file.exists();
    }

    if (!exists) { // hack to remove double media dir
      // logger.debug("2 not at " +file.getAbsolutePath());

      if (audioFileEquivalent.contains("media")) { // TODO : horrible hack
        audioFileEquivalent = audioFileEquivalent.replace("media", "");
      }
      audioPath = mediaDir + File.separator + audioFileEquivalent;
      file = new File(audioPath);
      exists = file.exists();
      if (!exists) {
        // logger.debug("3 not at " +file.getAbsolutePath());
        file = new File(installPath, audioPath);
        exists = file.exists();
      }
    }
    if (!exists) {
//      logger.warn("couldn't find file at " + file.getAbsolutePath());
      return null;
    } else {
      String s = ensureForwardSlashes(audioPath);
      return s;
    }
  }

  /**
   * Does some font formatting (via h4) on orientation and table.
   *
   * @param installPath
   * @param include
   * @param builder
   * @throws IOException
   * @see #getContentFromIncludeFile
   */
  private void readFromFile(String installPath, File include, StringBuilder builder) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(include), ENCODING));
    String line2;
    while ((line2 = reader.readLine()) != null) {
      line2 = line2.trim();
      if (line2.startsWith("File://")) {
        if (line2.endsWith(MP3)) {
          //logger.debug("skipping mp3 include line for now : " + line2);
        } else if (line2.endsWith(".png")) {
          String path = line2.substring("File://".length());
          String audioPath = getFilePath(installPath, path);

          if (audioPath == null) {
            //logger.warn(" couldn't find png file " + path);
          } else {
            String str = "<img src='" + audioPath + "'/>";
            builder.append(str);
          }
        } else {
          logger.warn("skipping " + line2);
        }
      } else {
        line2 =
            line2.replace("Orientation:", "<h4>Orientation:</h4>").
                replace("<table>", "<h4><table>").
                replace("</table>", "</table></h4>").
                replaceAll("<td dir=\"rtl\">([^<]*)</td>", "<td dir=\"rtl\"><h4>$1</h4></td>").replaceAll(" width=\"20%\"", "");
        if (line2.contains("width")) {
          logger.warn("line has width " + line2);
        }
        builder.append(line2);
      }
    }
    reader.close();
  }

  /**
   * @param audioPath
   * @return
   * @see #getContentFromIncludeFile
   */
  private String getHTML5Audio(String audioPath) {
    String mp3Ref = audioPath.replace(".wav", MP3);
    String oggRef = audioPath.replace(".wav", ".ogg");
    // logger.debug("file path " + mp3Ref);
    return "<h4>Listen to this audio and answer the question below</h4>\n" +
        "<audio controls='controls'>" +
        "<source type='audio/mp3' src='" +
        mp3Ref +
        "'></source>" +
        "<source type='audio/ogg' src='" +
        oggRef +
        "'></source>" +
        "  Your browser does not support the audio tag." +
        "</audio>";
  }

  /**
   * Does NOT expand slash permutations.
   *
   * @param question
   * @param answers
   * @param amasExerciseImpl
   * @param isFLQ
   * @see #readTSVLine
   */
  private void addQuestion(String question, String answers, AmasExerciseImpl amasExerciseImpl, boolean isFLQ, int qid) {
    List<String> alternateAnswers = Arrays.asList(answers.split("\\|\\|"));
    List<String> copy = new ArrayList<String>();
    for (String answer : alternateAnswers) {
      List<String> c = Arrays.asList(answer.split(";"));
      for (String ans : c) {
        dealWithAnswer(amasExerciseImpl.getID(), qid, copy, ans, isFLQ);
      }
    }
    alternateAnswers = copy;
    //  logger.info("For " + exercise.getID() + " found " + alternateAnswers.size() + " alt answers");
    //List<String> objects = alternateAnswers.size() > 1 ? alternateAnswers.subList(1, alternateAnswers.size()) : EMPTY_LIST;

    // logger.debug("to " + exercise.getID() + " adding " + question + " lang " + lang);
    List<String> serializableCollection = new ArrayList<String>(alternateAnswers);

    String lang = isFLQ ? AmasExerciseImpl.FL : AmasExerciseImpl.EN;
    amasExerciseImpl.addQuestion(lang, question, serializableCollection);
  }

  private BufferedWriter writer = null;

  private void doBuffer() {
    try {
      writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("expandedAnswerKey_" + language +
          ".csv"), "UTF8"));
      writer.write("id,qid,orig,expansion\n");
    } catch (IOException e) {
      logger.error("got " + e.getMessage(), e);
    }
  }

  private void dealWithAnswer(String exid, int qid, List<String> copy, String ans, boolean isFLQ) {
/*    String orig = ans;

    ans = ans.replaceAll("\\s+", " ");
    ans = ans.replaceAll("\\s/\\s", "/");
    ans = ans.replaceAll("/\\s", "/");
    ans = ans.replaceAll("\\s/", "/");

    String[] tokens = ans.split("\\s++");

    List<String> current = new ArrayList<>();


    for (String t : tokens) {
      List<String> newc = new ArrayList<>();
      if (t.contains("/")) {
        String[] opts = t.split("/");
        if (current.isEmpty()) {
          for (String opt : opts) {
            newc.add(opt);
          }
        } else {
          for (String opt : opts) {
            for (String c : current) {
              c += " " + opt;
              newc.add(c);
            }
          }

        }
      } else {
        if (current.isEmpty()) {
          newc.add(t);
        } else {
          for (String c : current) {
            c += " " + t;
            newc.add(c);
          }
        }
      }

      current = newc;
    }*/
    copy.add(ans);
    //for (String sentence : copy) {
    if (isFLQ) {
      try {
        if (WRITE_ANSWER_KEY) {
          writer.write(exid + "," + qid + "," + ans.trim() + "\n");
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    //}
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  public List<CommonExercise> getRawExercises() {
    return exercises;
  }
}
