package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseFormatter;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads plan directly from a CSV.
 *
 * TODO : Ideally we'd load the data into h2, then read it out again.
 *
 * User: GO22670
 * Date: 10/8/12
 * Time: 3:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileExerciseDAO implements ExerciseDAO {
  private static final Logger logger = Logger.getLogger(FileExerciseDAO.class);

  private static final String FILE_PREFIX = "file://";
  private static final int FILE_PREFIX_LENGTH = "file://".length();
  private static final List<String> EMPTY_LIST = Collections.emptyList();

  public static final String ENCODING = "UTF8";
  private static final String FAST = "Fast";
  private static final String SLOW = "Slow";
  private static final int MAX_ERRORS = 100;
  private static final boolean CONFIRM_AUDIO_REFS = false;
  private final String mediaDir;

  private List<Exercise> exercises;
  private final Map<String,Exercise> idToExercise = new HashMap<String,Exercise>();
  private boolean isUrdu;
  private final boolean isFlashcard;
  private boolean isEnglish;
  private final boolean processSemicolons = false;
  private boolean isPashto;
  private SectionHelper sectionHelper = new SectionHelper();

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeExerciseDAO
   * @param mediaDir
   * @param language
   */
  public FileExerciseDAO(String mediaDir, String language, boolean isFlashcard) {
    if (language != null) {
      this.isUrdu = language.equalsIgnoreCase("Urdu");
      this.isEnglish = language.equalsIgnoreCase("English");
      this.isPashto = language.equalsIgnoreCase("Pashto");
    }
    this.mediaDir = mediaDir;
    this.isFlashcard = isFlashcard;
  }

  @Override
  public SectionHelper getSectionHelper() {
    return sectionHelper;
  }

  public Exercise getExercise(String id) {
    if (idToExercise.isEmpty()) logger.warn("huh? couldn't find any exercises..?");
    if (!idToExercise.containsKey(id)) {
       logger.warn("couldn't find " +id + " in " +idToExercise.size() + " exercises...");
    }
    return idToExercise.get(id);
  }

  public void readWordPairs(String lessonPlanFile, String language, boolean doImages) {
    readWordPairs(lessonPlanFile, language, doImages, false);
  }

  /**
   *
   * @see DatabaseImpl#getExercises(boolean, String)
   * @param lessonPlanFile
   * @param language
   * @param doImages
   * @param dontExpectHeader
   */
  private void readWordPairs(String lessonPlanFile, String language, boolean doImages, boolean dontExpectHeader) {
    if (exercises != null) return;
    exercises = new ArrayList<Exercise>();
    boolean isTSV =  (lessonPlanFile.endsWith(".tsv"));
    try {
      File file = new File(lessonPlanFile);
      if (!file.exists()) {
        logger.error("can't find '" + file + "'");
        return;
      } /*else {
        // logger.debug("found file at " + file.getAbsolutePath());
      }*/
      BufferedReader reader = getReader(lessonPlanFile);

      String line;
      int id = 1;
      boolean gotHeader = false;

      if (dontExpectHeader) {
        while ((line = reader.readLine()) != null) {
          id = readLine(language, doImages, isTSV, line, id);
        }
      } else {
        while ((line = reader.readLine()) != null) {
          if (!gotHeader && line.trim().toLowerCase().startsWith("word")) { // skip lines until we get the header
            logger.info("for " + lessonPlanFile + " got header " + line);
            gotHeader = true;
          } else {
            id = readLine(language, doImages, isTSV, line, id);
          }
        }
      }
      reader.close();

    } catch (Exception e) {
      logger.error("reading " + lessonPlanFile + " got " + e, e);
    }
    if (exercises.isEmpty()) {
      logger.error("no exercises found in " + lessonPlanFile + "?");
    } else {
      logger.debug("found " + exercises.size() + " exercises, first is " + exercises.iterator().next());
    }
    populateIDToExercise();
  }

  /**
   * @see #readWordPairs(String, String, boolean,boolean)
   * @param language
   * @param doImages
   * @param TSV
   * @param line
   * @param id
   * @return
   */
  private int readLine(String language, boolean doImages, boolean TSV, String line, int id) {
    if (TSV) {
      id = readTSVLine(doImages, line, id);
    } else {
      id = readLine(language, doImages, line, id);
    }
    return id;
  }

  private void populateIDToExercise() {
    for (Exercise e : exercises) idToExercise.put(e.getID(),e);
  }

  /**
   * Expects the columns to be like this:
   * Word/Expression	Arabic	Transliteration	RefAudio	Unit	Chapter	Week	Weight
   *
   *
   * @param doImages
   * @param line
   * @param id
   * @return
   */
  private int readTSVLine(boolean doImages, String line, int id) {
    if (line.trim().length() == 0) {
      logger.debug("skipping empty line");
      return id;
    }
    String[] split = line.split("\\t");
    int len = split.length;
    String english = split[0].trim();

    String foreign = len > 1 ? split[1].trim() : "";
    String translit = len > 2 ? split[2].trim() : "";
    String refAudio = len > 3 ? split[3].trim() : "";

    List<String> translations = new ArrayList<String>();
  /*  for (int i = 1; i < split.length; i++) {
      translations.add(split[i]);
    }*/
    if (foreign.length() > 0) {
      translations.add(foreign);
    }

    if (translations.isEmpty() && !doImages) {
      logger.error("huh? no translations with '" + line + "' and foreign : " + foreign);
    }
    else if (foreign.length() > 0) {
      String flashcardStimulus = doImages ? getImageContent(foreign) : english;
      String tooltip = doImages ? foreign : translations.get(0);
      if (doImages) translations.add(foreign);
      Exercise repeat = new Exercise("flashcardStimulus", "" + (id++), flashcardStimulus, translations, tooltip);
      repeat.setTranslitSentence(translit);
      repeat.setEnglishSentence(english);
      String audioRef = (refAudio.length() == 0) ? mediaDir + "/" + english + ".mp3" : mediaDir + "/" + refAudio;
    //  logger.debug("audio ref = " + audioRef);
      repeat.setRefAudio(audioRef); // TODO confirm file exists. - see confirmAudio

      exercises.add(repeat);
    }
    return id;
  }

  /**
   * @see #readLine(String, boolean, boolean, String, int)
   * @param language
   * @param doImages
   * @param line
   * @param id
   * @return
   */
  private int readLine(String language, boolean doImages, String line, int id) {
    String[] split = line.split(",");
    String foreign = split[0].trim();
    List<String> translations = new ArrayList<String>();
    for (int i = 1; i < split.length; i++) {
      translations.add(split[i]);
    }

    if (translations.isEmpty() && !doImages) {
      logger.error("huh? no translations with '" + line + "' and foreign : " + foreign);
    }
    else if (foreign.length() > 0) {
      String flashcard = doImages ? getImageContent(foreign) : getFlashcard(foreign, language);
      String tooltip = doImages ? foreign : translations.get(0);
      if (doImages) translations.add(foreign);
      Exercise repeat = new Exercise("flashcard", "" + (id++), flashcard, translations, tooltip);
      repeat.addQuestion(Exercise.FL, "Please record the sentence above.","", EMPTY_LIST);

      exercises.add(repeat);
    }
    return id;
  }

  /**
   * @see DatabaseImpl#getExercises(boolean, String)
   * @param installPath
   * @param lessonPlanFile
   */
  public void readFastAndSlowExercises(final String installPath, String configDir, String lessonPlanFile) {
    if (exercises != null) return;

    try {
      File file = new File(lessonPlanFile);
      if (!file.exists()) {
        logger.error("can't find '" + file +"'");
        return;
      }
      else {
       // logger.debug("found file at " + file.getAbsolutePath());
      }
      BufferedReader reader = getReader(lessonPlanFile);
      String line2;
      int count = 0;
      //logger.debug("using install path " + installPath + " lesson " + lessonPlanFile + " isurdu " +isUrdu);
      exercises = new ArrayList<Exercise>();
      Pattern pattern = Pattern.compile("^\\d+\\.(.+)");
      int errors = 0;
      int id = 0;

      String lastID = "";
      Exercise lastExercise = null;
      while ((line2 = reader.readLine()) != null) {
        count++;
     //  if (TESTING && count > 200) break;

        Matcher matcher = pattern.matcher(line2.trim());
        boolean wordListOnly = matcher.matches();

        try {
          Exercise exercise;
          if (wordListOnly) {
            String group = matcher.group(1);
            exercise = getWordListExercise(group,""+count);
            exercises.add(exercise);
          }
          else {
            int length = line2.split("\\(").length;
            boolean simpleFile = length == 2 && line2.startsWith("<s>");
            boolean isTSV = line2.contains("\t");
            exercise = simpleFile ?
              getSimpleExerciseForLine(line2,id) :
              isTSV ? readTSVLine(installPath, configDir, line2) : getExerciseForLine(line2);

            if (exercise != null) {
              id++;
              if (exercise.getID().equals(lastID)) {
                //logger.debug("ex " +lastID+ " adding " + exercise.getEnglishQuestions());
                lastExercise.addQuestions(Exercise.EN, exercise.getEnglishQuestions());
                lastExercise.addQuestions(Exercise.FL, exercise.getForeignLanguageQuestions());
              } else {
                exercises.add(exercise);
                lastExercise = exercise;
              }

              lastID = exercise.getID();
            }
          }
        } catch (Exception e) {
          logger.error("Got " + e + ".Skipping line -- couldn't parse line #"+count + " : " +line2,e);
          errors++;
          if (errors > MAX_ERRORS) {
            logger.error("too many errors, giving up...");
            break;
          }
        }
      }
      reader.close();
    } catch (IOException e) {
     logger.error("reading " +lessonPlanFile+ " got " +e,e);
    }
    if (exercises.isEmpty()) {
      logger.error("no exercises found in " + lessonPlanFile +"?");
    }
    else {
      logger.debug("found " + exercises.size() + " exercises, first is " + exercises.iterator().next());
    }
    if (CONFIRM_AUDIO_REFS) confirmAudioRefs(exercises);
    populateIDToExercise();
  }

  private void confirmAudioRefs(List<Exercise> exercises) {
    int c = 0;
    try {
      FileWriter writer = new FileWriter("missingAudio.txt");
      for (Exercise e : exercises) {
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
  }

  private BufferedReader getReader(String lessonPlanFile) throws FileNotFoundException, UnsupportedEncodingException {
    FileInputStream resourceAsStream = new FileInputStream(lessonPlanFile);
    return new BufferedReader(new InputStreamReader(resourceAsStream,ENCODING));
  }

  /**
   * Read from tsv file that points to other files.
   *
   * @see #readFastAndSlowExercises(String, String, String)
   * @param installPath
   * @param configDir
   * @param line
   * @return
   */
  private Exercise readTSVLine(String installPath, String configDir, String line) {
    if (line.trim().length() == 0) {
      logger.debug("skipping empty line");
      return null;
    }
    String[] split = line.split("\\t");
    int i =0;
    String id = split[i++].trim();
    /*String level =*/ split[i++].trim();
    String type = split[i++].trim();
    String includeFile = split[i++].trim();

    File include = getIncludeFile(configDir, includeFile);

    if (!include.exists()) {
      File configDirFile = new File(installPath, configDir);
/*      logger.warn("couldn't open file " + include.getName() + " at " +include.getAbsolutePath() + " config '" + configDir +"' with install path'" +
        installPath +
        "' new config dir " + configDirFile.getAbsolutePath());*/
      include = getIncludeFile(configDirFile.getAbsolutePath(), includeFile);
    }
     if (!include.exists()) {
      logger.warn("couldn't open file " + include.getName() + " at " +include.getAbsolutePath() + " config " + configDir +
        " for line " + line);
      return null;
    } else {
      boolean listening = type.equalsIgnoreCase("listening");
      String content = getContentFromIncludeFile(installPath, include, listening);
      if (content.isEmpty()) {
        logger.warn("no content for exercise " + id + " type " + type);
        return null;
      } else {
        String arabicQuestion = split[i++].trim();
        String englishQuestion = split[i++].trim();
        String arabicAnswers = split[i++].trim();
        String englishAnswers = split[i++].trim();

        Exercise exercise = new Exercise("plan", id, content, false, false, englishQuestion);

        addQuestion(arabicQuestion, arabicAnswers, exercise, true);
        addQuestion(englishQuestion, englishAnswers, exercise, false);
        return exercise;
      }
    }
  }

  private File getIncludeFile(String configDir, String includeFile) {
    if (includeFile.startsWith(FILE_PREFIX)) {
      includeFile = includeFile.substring(FILE_PREFIX_LENGTH);
  /*    logger.debug("1 after '" +  includeFile+
        "'");*/
    }
    else if (includeFile.startsWith("file:/")) { // weird issue with pashto...
      logger.debug("2 include '" +  includeFile+
        "'");
      includeFile = includeFile.substring("file:/".length());
    }
    return new File(configDir,includeFile);
  }

  /**
   * If listening, include HTML 5 audio reference, otherwise include text from file.
   * @see #readTSVLine(String, String, String)
   * @param include
   * @param isListening
   * @return
   */
  private String getContentFromIncludeFile(String installPath, File include, boolean isListening) {
    StringBuilder builder = new StringBuilder();
    String audioFileEquivalent = include.getName().replace(".html", ".wav");
    try {
      if (isListening) {
        String audioPath = mediaDir + File.separator + "media" + File.separator + audioFileEquivalent;

        if (!audioPath.contains("media")) {
          audioPath = mediaDir + File.separator + "media" + File.separator + audioFileEquivalent;
        }
        File file = new File(audioPath);
        boolean exists = file.exists();
        if (!exists) {
          file = new File(installPath,audioPath);
          exists = file.exists();
        }

        if (!exists) { // hack to remove double media dir
          audioPath = mediaDir + File.separator + audioFileEquivalent;
          file = new File(audioPath);
          exists = file.exists();
          if (!exists) {
            file = new File(installPath,audioPath);
            exists = file.exists();
          }
        }
        if (!exists) {
          logger.warn("couldn't find audio file at " + file.getAbsolutePath());
        } else {
          builder.append(getHTML5Audio(audioPath));
        }
      } else {
        readFromFile(include, builder);
      }
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
    return builder.toString();
  }

  private void readFromFile(File include, StringBuilder builder) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(include), ENCODING));
    String line2;
    while ((line2 = reader.readLine()) != null) {
      line2 = line2.trim();
      if (line2.startsWith("File://") && line2.endsWith(".mp3")) {
        //logger.debug("skipping mp3 include line for now : " + line2);
      } else {
        builder.append(line2);
      }
    }
    reader.close();
  }

  private void addQuestion(String question, String answers, Exercise exercise, boolean isFLQ) {
    List<String> alternateAnswers = Arrays.asList(answers.split("\\|\\|"));
    List<String> objects1 = Collections.emptyList();
    List<String> objects = alternateAnswers.size() > 1 ? alternateAnswers.subList(1, alternateAnswers.size()) : objects1;
    exercise.addQuestion(isFLQ ? Exercise.FL : Exercise.EN, question, alternateAnswers.get(0), new ArrayList<String>(objects));
  }

  /**
   * @see #getContentFromIncludeFile
   * @param audioPath
   * @return
   */
  private String getHTML5Audio(String audioPath) {
    String mp3Ref = audioPath.replace(".wav",".mp3");//"config/pilot/media/bc-L0P-k15/bc-L0P-k15_My_house_door.mp3";
    String oggRef = audioPath.replace(".wav",".ogg");
   // logger.debug("file path " + mp3Ref);
    return "<h4>Listen to this audio and answer the question below</h4>\n"+
    "<audio controls='controls'>"+
    "<source type='audio/mp3' src='" +
      mp3Ref +
      "'></source>"+
    "<source type='audio/ogg' src='" +
      oggRef +
      "'></source>"+
    "  Your browser does not support the audio tag."+
    "</audio>";
  }

  /**
   * @see #readFastAndSlowExercises
   * @param contentSentence
   * @param id
   * @return
   */
  private Exercise getWordListExercise(String contentSentence, String id) {
    contentSentence = getRefSentence(contentSentence);
    String content = ExerciseFormatter.getArabic(contentSentence, isUrdu, isPashto);

    Exercise exercise = new Exercise("repeat", id, content, false, true, contentSentence);
    exercise.setRefSentence(contentSentence);
    exercise.addQuestion(Exercise.FL, "Please record the sentence above.","", EMPTY_LIST);
    return exercise;
  }

  /**
   * Deal with data where it looks like :  put ; put ; put out
   * @param contentSentence
   * @return
   */
  private String getRefSentence(String contentSentence) {
    String e1 = contentSentence.trim();
/*    if (processSemicolons && e1.contains(";")) {
      logger.warn("found semi " + e1);
      String[] split = e1.split(";");

      String firstPhrase = split[0];
      String lastPhrase = split[split.length - 1];
      if (lastPhrase.contains(firstPhrase)) {
        e1 = lastPhrase;
      }
      else {
        e1 = firstPhrase;
      }
    }*/
    return e1.trim().replaceAll("-", " ");
  }


  /**
   * Assumes a file that looks like:
   * <br></br>
   *
   * <s> word word word </s> (audio_file_name_without_suffix)
   *
   * e.g. from MSA : trans.msa.unique
   * @see #readFastAndSlowExercises
   * @param line2
   * @return
   */
  private Exercise getSimpleExerciseForLine(String line2, int id) {
    String[] split = line2.split("\\(");

    String foreignLanguagePhrase = split[0].trim();
    foreignLanguagePhrase = foreignLanguagePhrase.replaceAll("<s>", "").replaceAll("</s>", "").trim();
    String content = ExerciseFormatter.getArabic(foreignLanguagePhrase,isUrdu,isPashto);

    String audioFileName = split[1].trim();
    String english = "";
    String translit = "";
    if (audioFileName.contains("\t")) {
      String[] split1 = audioFileName.split("\\t");
      audioFileName = split1[0].trim();
      english = split1[1].trim();
      if (split1.length == 3) {
        translit = split1[2].trim();
      }
    }
    audioFileName = audioFileName.substring(0, audioFileName.length() - 1); // remove trailing )
    String audioRef = mediaDir + File.separator + audioFileName + ".wav";

    if (isFlashcard) {
      if (english.length() == 0) {
        //logger.warn("huh? english is empty for " + line2);
      }
      Exercise imported = getFlashcardExercise(id, foreignLanguagePhrase, english, translit, audioRef);
      return imported;
    } else {
      Exercise exercise =
        new Exercise("repeat", audioFileName, content, ensureForwardSlashes(audioRef),
          foreignLanguagePhrase, foreignLanguagePhrase);
      exercise.addQuestion(Exercise.EN, "Please record the sentence above.", "", EMPTY_LIST);  // required for grading view
      exercise.addQuestion(Exercise.FL, "Please record the sentence above.", "", EMPTY_LIST);

      return exercise;
    }
  }

  private Exercise getFlashcardExercise(int id, String foreignLanguagePhrase, String english, String translit, String audioRef) {
    List<String> translations = new ArrayList<String>();
    if (foreignLanguagePhrase.length() > 0) {
      translations.addAll(Arrays.asList(foreignLanguagePhrase.split(";")));
      //logger.debug(english + "->" + translations);
    }
    Exercise imported = new Exercise("flashcardStimulus", "" + id, english, translations, english);
    imported.setEnglishSentence(english);
    if (translit.length() > 0) {
      imported.setTranslitSentence(translit);
    }
    imported.setRefAudio(ensureForwardSlashes(audioRef));
    return imported;
  }

  /**
   * Parses file that looks like:
   * <br></br>
   * pronz.MultiRefRepeatExercise$: nl0001_ams, nl0001_ams | reference, nl0001_ams | Female_01, nl0001_ams | Male_01, nl0001_ams | REF_MALE, nl0001_ams | STE-004M, nl0001_ams | STE-006F -> nl0001_ams -> FOREIGN_LANGUAGE_SENTENCE -> marHaba -> Hello.
   *
   * @param line2
   * @return
   */
  private Exercise getExerciseForLine(String line2) {
    String[] split = line2.split("\\|");
    String lastCol = split[6];
    String[] split1 = lastCol.split("->");
    String name = split1[1].trim();
    //String displayName = name;
    String arabic = split1[2];
    String translit = split1[3];
    String english = split1[4];

    String content = ExerciseFormatter.getContent(arabic, translit, english,"",isEnglish,isUrdu,isPashto);
    String fastAudioRef = mediaDir+File.separator+name+File.separator+ FAST + ".wav";
    String slowAudioRef = mediaDir+File.separator+name+File.separator+ SLOW + ".wav";

    return new Exercise("repeat", name, content,
        ensureForwardSlashes(fastAudioRef), ensureForwardSlashes(slowAudioRef), arabic, english);
  }

  private String getImageContent(String flPhrase) {
    String filePath = mediaDir + "/" + flPhrase + ".png";
    String s = ensureForwardSlashes(filePath);
    return "<img src='" +
      s +
      "'/>";
  }

  private String getFlashcard(String flPhrase, String language) {
    return flPhrase;
  }

/*  private InputStream getExerciseListStream(String exerciseFile) {
    InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(exerciseFile);
    if (resourceAsStream == null) {
      logger.error("can't find " + exerciseFile);
      try {
        String n2 = "C:\\Users\\go22670\\DLITest\\clean\\netPron2\\src\\";
        String name = n2+ exerciseFile;
        File file = new File(name);
        boolean exists = file.exists();
        if (!exists) System.err.println("can't find " + file);
        resourceAsStream = new FileInputStream(name);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
      if (resourceAsStream == null) {
        return null;
      }
    }
    return resourceAsStream;
  }*/

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  public List<Exercise> getRawExercises() {
    return exercises;
  }

/*  private void convertPlan() {
    InputStream resourceAsStream = getExerciseListStream("lesson.plan");

    if (resourceAsStream == null) return;

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,ENCODING));
      String line2;
      int count = 0;
      while ((line2 = reader.readLine()) != null) {
       // if (count++ > 3) break;
        String[] split = line2.split("\\|");
        String secondCol = split[1];
        System.out.println("read " + line2 + " second " + secondCol);
        String name = secondCol.split(",")[1].trim();
        String longFile = "reference.wav";
        String shortFile = "REF_MALE.wav";
        File base = new File("C:\\Users\\go22670\\mt_repo\\github\\hs-levantine\\data752\\"+name);
        File newRefDir = new File("C:\\Users\\go22670\\mt_repo\\github\\hs-levantine\\newRef\\"+name);
        newRefDir.mkdirs();
        if (!base.exists()) {
          System.err.println("Can't find " + base.getAbsolutePath());
          continue;
        }
        AudioInputStream shortAIS = AudioSystem.getAudioInputStream(new File(base,shortFile));
        File longFileFile = new File(base, longFile);
        AudioInputStream longAIS = AudioSystem.getAudioInputStream(longFileFile);

        long shortFrames = shortAIS.getFrameLength();
        long longFrames = longAIS.getFrameLength();
        AudioFormat format = shortAIS.getFormat();
        long shortBytes = shortFrames * format.getFrameSize();
        long skip = longAIS.skip(shortBytes);
        if (skip   != shortBytes) System.err.println("skipped " + skip + " but asked for " + shortBytes);

        new AudioConverter().trim("C:\\Users\\go22670\\sox-14-3-2\\sox.exe",
            longFileFile.getAbsolutePath(),
            new File(newRefDir, SLOW + ".wav").getAbsolutePath(),
            shortFrames,
            longFrames-shortFrames);
        shortAIS.close();
        longAIS.close();

        new FileCopier().copy(new File(base, shortFile).getAbsolutePath(), new File(newRefDir, FAST + ".wav").getAbsolutePath());
      }
      reader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }*/
}
