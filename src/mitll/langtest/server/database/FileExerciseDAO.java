package mitll.langtest.server.database;

import audio.imagewriter.AudioConverter;
import audio.tools.FileCopier;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Lesson;
import org.apache.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private static Logger logger = Logger.getLogger(FileExerciseDAO.class);
  public static final List<String> EMPTY_LIST = Collections.emptyList();

  public static final String ENCODING = "UTF8";
  private static final String LESSON_FILE = "lesson-737.csv";
  private static final String FAST = "Fast";
  private static final String SLOW = "Slow";
  private static final boolean TESTING = false;
  private static final int MAX_ERRORS = 100;
  private final String mediaDir;

  private List<Exercise> exercises;
  private final boolean isUrdu;
  private final boolean isFlashcard;
  private final boolean isEnglish;

  @Override
  public SectionHelper getSectionHelper() {
    return null;
  }

  private Map<String,Map<String,Lesson>> typeToUnitToLesson = new HashMap<String,Map<String,Lesson>>();
  // e.g. "week"->"week 5"->[unit->["unit A","unit B"]],[chapter->["chapter 3","chapter 5"]]
  private Map<String,Map<String,Map<String,Set<String>>>> typeToSectionToTypeToSections = new HashMap<String, Map<String,Map<String,Set<String>>>>();
  private boolean isPashto;


  public FileExerciseDAO(boolean isFlashcard) {
    this("","",false,isFlashcard);
  }
  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeExerciseDAO
   * @param mediaDir
   * @param language
   */
  public FileExerciseDAO(String mediaDir, String language, boolean showSections, boolean isFlashcard) {
    this.isUrdu = language.equalsIgnoreCase("Urdu");
    this.isEnglish = language.equalsIgnoreCase("English");
    this.isPashto = language.equalsIgnoreCase("Pashto");
    this.mediaDir = mediaDir;
    this.isFlashcard = isFlashcard;
   // this.showSections = showSections;
  }

  /**
   * TODO : write to h2
   * @see DatabaseImpl#getExercises(boolean, String)
   * @deprecated
   * @param installPath
   */
  public void readExercises(String installPath) {
    if (exercises != null) return;
    String exerciseFile = LESSON_FILE;
    InputStream resourceAsStream = getExerciseListStream(exerciseFile);
    if (resourceAsStream == null) return;

    try {
      exercises = new ArrayList<Exercise>();
      BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,ENCODING));
      String line2;
      int count = 0;
      logger.debug("using install path " + installPath);
      while ((line2 = reader.readLine()) != null) {
        String[] split = line2.split(",");
        String name = split[1];
        String displayName = split[2];
        String arabic = split[3];
        String translit = split[4];
        String english = split[5];

        String content = getContent(arabic, translit, english,"");
        String audioRef =  "ref"+ File.separator+name+File.separator+"reference.wav";
       /* if (installPath.length() > 0) {
          if (!installPath.endsWith(File.separator)) installPath += File.separator;
          audioRef = installPath + audioRef;
        }*/
        File file = new File(audioRef);
        if (!file.exists()) {
          file = new File(installPath,audioRef);
        }
        if (!file.exists()) {
          if (count++ < 5) logger.debug("can't find audio file " + file.getAbsolutePath());
        } else {
          Exercise exercise = new Exercise("repeat", displayName, content, ensureForwardSlashes(audioRef), arabic, english);
          exercises.add(exercise);
        }
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (exercises.isEmpty()) {
      logger.error("no exercises found in " + exerciseFile +"?");
    }
    else {
      logger.debug("found " + exercises.size() + " exercises, first is " + exercises.iterator().next());
    }
  }

  /**
   *
   * @see DatabaseImpl#getExercises(boolean, String)
   * @param lessonPlanFile
   * @param language
   * @param doImages
   */
  public void readWordPairs(String lessonPlanFile, String language, boolean doImages, String configDir) {
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


      while ((line = reader.readLine()) != null) {
        if (!gotHeader && line.trim().startsWith("Word")) {
          logger.info("for " + lessonPlanFile + " got header " + line);
          gotHeader = true;
        } else {
          if (isTSV) {
            id = readTSVLine(language, doImages, line, id);
          } else {
            id = readLine(language, doImages, line, id);
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
  }

  /**
   * Expects the columns to be like this:
   * Word/Expression	Arabic	Transliteration	RefAudio	Unit	Chapter	Week	Weight
   *
   * @param language
   * @param doImages
   * @param line
   * @param id
   * @return
   */
  private int readTSVLine(String language, boolean doImages, String line, int id) {
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
      String flashcardStimulus = doImages ? getImageContent(foreign, language) : english;
      String tooltip = doImages ? foreign : translations.get(0);
      if (doImages) translations.add(foreign);
      Exercise repeat = new Exercise("flashcardStimulus", "" + (id++), flashcardStimulus, translations, tooltip);
      repeat.setTranslitSentence(translit);
      repeat.setEnglishSentence(english);
      String audioRef = (refAudio.length() == 0) ? mediaDir + "/" + english + ".mp3" : mediaDir + "/" + refAudio;
    //  logger.debug("audio ref = " + audioRef);
      repeat.setRefAudio(audioRef); // TODO confirm file exists.

      exercises.add(repeat);
    }
    return id;
  }

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
      String flashcard = doImages ? getImageContent(foreign, language) : getFlashcard(foreign, language);
      String tooltip = doImages ? foreign : translations.get(0);
      if (doImages) translations.add(foreign);
      Exercise repeat = new Exercise("flashcard", "" + (id++), flashcard, translations, tooltip);
      repeat.addQuestion(Exercise.FL, "Please record the sentence above.","", EMPTY_LIST);

      exercises.add(repeat);
    }
    return id;
  }

  /**
   * @see DatabaseImpl#getExercises
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
      if (!typeToUnitToLesson.isEmpty()) {
        logger.debug("sections " + typeToUnitToLesson);
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
  }

  private BufferedReader getReader(String lessonPlanFile) throws FileNotFoundException, UnsupportedEncodingException {
    FileInputStream resourceAsStream = new FileInputStream(lessonPlanFile);
    return new BufferedReader(new InputStreamReader(resourceAsStream,ENCODING));
  }

  /**
   * Read from tsv file that points to other files.
   *
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
    String level = split[i++].trim();
    String type = split[i++].trim();
    String includeFile = split[i++].trim();

    File include = getIncludeFile(configDir, includeFile);

    boolean exists = include.exists();
    if (!exists) {
      include = new File(installPath,includeFile);
      exists = include.exists();
    }
     if (!exists) {
      logger.warn("couldn't open file " + include.getName() + " at " +include.getAbsolutePath() + " for line " + line);
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
    if (includeFile.startsWith("file://")) {
      includeFile = includeFile.substring("file://".length());
    }
    return new File(configDir,includeFile);
  }

  /**
   * If listening, include HTML 5 audio reference, otherwise include text from file.
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

        File file = new File(audioPath);
        boolean exists = file.exists();
        if (!exists) {
          file = new File(installPath,audioPath);
          exists = file.exists();
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

  private Map<String, Lesson> getSectionToLesson( String section) {
    Map<String, Lesson> unit = typeToUnitToLesson.get(section);
    if (unit == null) typeToUnitToLesson.put(section, unit = new HashMap<String, Lesson>());
    return unit;
  }

  private static class Pair {
    String type; String section;

    public Pair(String type, String section) {
      this.type = type;
      this.section = section;
    }
  }

/*  private void addAssociations(List<Pair> pairs) {
    for (Pair p : pairs) {
      List<Pair> others = new ArrayList<Pair>(pairs);
      others.remove(p);
      for (Pair o : others) {
        addAssociation(p, o);
       // addAssociation(o, p);
      }
    }
  }
*/
  private void addAssociation(Pair first, Pair second) {
    addAssociation(first.type, first.section, second.type, second.section);
  }

  private void addAssociation(String type, String unitName, String otherType, String otherSection) {
    Map<String, Map<String, Set<String>>> sectionToTypeToSections = typeToSectionToTypeToSections.get(type);
    if (sectionToTypeToSections == null) {
      typeToSectionToTypeToSections.put(type, sectionToTypeToSections = new HashMap<String, Map<String, Set<String>>>());
    }
    Map<String, Set<String>> subsections = sectionToTypeToSections.get(unitName);
    if (subsections == null) {
      sectionToTypeToSections.put(unitName, subsections = new HashMap<String, Set<String>>());
    }
    Set<String> sections = subsections.get(otherType);
    if (sections == null) subsections.put(otherType, sections = new HashSet<String>());
    sections.add(otherSection);
  }

  /**
   * @see #readFastAndSlowExercises
   * @param contentSentence
   * @param id
   * @return
   */
  private Exercise getWordListExercise(String contentSentence, String id) {
    contentSentence = getRefSentence(contentSentence);
    String content = getArabic(contentSentence);

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
    if (e1.contains(";")) {
      String[] split = e1.split(";");

      String firstPhrase = split[0];
      String lastPhrase = split[split.length - 1];
      if (lastPhrase.contains(firstPhrase)) {
        e1 = lastPhrase;
      }
      else {
        e1 = firstPhrase;
      }
   /*   if (e1.endsWith("-") && split.length > 1) {
        e1 = split[split.length-2];
      }*/
    }
    return e1.trim().replaceAll("-", " ");
  }


  /**
   * Assumes a file that looks like:
   * <br></br>
   *
   * <s> word word word </s> (audio_file_name_without_suffix)
   *
   * @see #readFastAndSlowExercises
   * @param line2
   * @return
   */
  private Exercise getSimpleExerciseForLine(String line2, int id) {
    String[] split = line2.split("\\(");

    String foreignLanguagePhrase = split[0].trim();
    foreignLanguagePhrase = foreignLanguagePhrase.replaceAll("<s>", "").replaceAll("</s>", "").trim();
    String content = getArabic(foreignLanguagePhrase);

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
      logger.debug("made " +exercise);

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

    String content = getContent(arabic, translit, english,"");
    String fastAudioRef = mediaDir+File.separator+name+File.separator+ FAST + ".wav";
    String slowAudioRef = mediaDir+File.separator+name+File.separator+ SLOW + ".wav";

    return new Exercise("repeat", name, content,
        ensureForwardSlashes(fastAudioRef), ensureForwardSlashes(slowAudioRef), arabic, english);
  }

  public String getContent(String arabic, String translit, String english, String meaning) {
    String arabicHTML = getArabic(arabic);
    String translitHTML = translit.length() > 0 ?
      getSpanWrapper("Transliteration:", translit)
      : "";
    String translationHTML = (!isEnglish && english.length() > 0) ?
      getSpanWrapper("Translation:", english) : "";
    String meaningHTML = (isEnglish && meaning.length() > 0) ?
      getSpanWrapper("Meaning:", meaning) : "";
    return arabicHTML +
      translitHTML +
      translationHTML +
      meaningHTML;
  }

  private String getSpanWrapper(String rowTitle, String english) {
    return "<div class=\"Instruction\">\n" +
    "<span class=\"Instruction-title\">" +
      rowTitle +
      "</span>\n" +
    "<span class=\"Instruction-data\"> " + english +
    "</span>\n" +
    "</div>";
  }

  private String getArabic(String arabic) {
    return "<div class=\"Instruction\">\n" +
        "<span class=\"Instruction-title\">Say:</span>\n" +
        "<span class=\"" +
        (isUrdu ? "urdufont" : isPashto ? "pashtofont" : "Instruction-data") +
        "\"> " + arabic +
        "</span>\n" +
        "</div>\n";
  }

  private String getImageContent(String flPhrase, String language) {
    String filePath = mediaDir + "/" + flPhrase + ".png";
    String s = ensureForwardSlashes(filePath);
    return "<img src='" +
      s +
      "'/>";
  }

  private String getFlashcard(String flPhrase, String language) {
    return flPhrase;
  }

  private InputStream getExerciseListStream(String exerciseFile) {
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
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  public List<Exercise> getRawExercises() {
    return exercises;
  }

  private void convertPlan() {
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
  }
}
