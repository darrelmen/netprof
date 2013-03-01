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
import java.util.ArrayList;
import java.util.Collection;
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
  public static final String ENCODING = "UTF8";
  private static final String LESSON_FILE = "lesson-737.csv";
  private static final String FAST = "fast";
  private static final String SLOW = "slow";
  private static final boolean TESTING = false;
  private static final int MAX_ERRORS = 100;

  private List<Exercise> exercises;
  private final String mediaDir;
  private final boolean isUrdu;
  private final boolean showSections;
  private Map<String,Map<String,Lesson>> typeToUnitToLesson = new HashMap<String,Map<String,Lesson>>();
  // e.g. "week"->"week 5"->[unit->["unit A","unit B"]],[chapter->["chapter 3","chapter 5"]]
  private Map<String,Map<String,Map<String,Set<String>>>> typeToSectionToTypeToSections = new HashMap<String, Map<String,Map<String,Set<String>>>>();

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeExerciseDAO
   * @param mediaDir
   * @param isUrdu
   */
  public FileExerciseDAO(String mediaDir, boolean isUrdu, boolean showSections) {
    this.mediaDir = mediaDir;
    this.isUrdu = isUrdu;
    this.showSections = showSections;
    //logger.debug("is urdu " + isUrdu);
  }

  public Map<String,List<String>> getTypeToSectionsForTypeAndSection(String type, String section) {
    Map<String, Map<String, Set<String>>> sectionToSub = typeToSectionToTypeToSections.get(type);
    if (sectionToSub == null) return Collections.emptyMap();
    Map<String, Set<String>> typeToSections = sectionToSub.get(section);
    if (typeToSections == null) return Collections.emptyMap();
    Map<String,List<String>> retval = new HashMap<String, List<String>>();
    for (Map.Entry<String,Set<String>> pair : typeToSections.entrySet()) {
      retval.put(pair.getKey(),new ArrayList<String>(pair.getValue()));
    }
    return retval;
  }

    @Override
  public Map<String, Collection<String>> getTypeToSections() {
    Map<String,Collection<String>> typeToSection = new HashMap<String, Collection<String>>();
    for (String key : typeToUnitToLesson.keySet()) {
      Map<String, Lesson> stringLessonMap = typeToUnitToLesson.get(key);
      typeToSection.put(key, new ArrayList<String>(stringLessonMap.keySet()));
    }
    return typeToSection;
  }

  @Override
  public Collection<Exercise> getExercisesForSection(String type, String section) {
    Map<String, Lesson> sectionToLesson = typeToUnitToLesson.get(type);
    if (sectionToLesson == null) {
      return Collections.emptyList();
    }
    else {
      Lesson lesson = sectionToLesson.get(section);
      if (lesson == null) {
        logger.error("Couldn't find section " + section);
        return Collections.emptyList();
      } else {
        return lesson.getExercises();
      }
    }
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

        String content = getContent(arabic, translit, english);
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
   * @see DatabaseImpl#getExercises
   * @param installPath
   * @param lessonPlanFile
   */
  public void readFastAndSlowExercises(final String installPath, String lessonPlanFile) {
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
      FileInputStream resourceAsStream = new FileInputStream(lessonPlanFile);
      BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,ENCODING));
      String line2;
      int count = 0;
      logger.debug("using install path " + installPath + " lesson " + lessonPlanFile + " isurdu " +isUrdu);
      exercises = new ArrayList<Exercise>();
      Pattern pattern = Pattern.compile("^\\d+\\.(.+)");
      int errors = 0;
      while ((line2 = reader.readLine()) != null) {
        count++;
       if (TESTING && count > 200) break;

        Matcher matcher = pattern.matcher(line2.trim());
        boolean wordListOnly = matcher.matches();

        try {
          Exercise exercise;
          if (wordListOnly) {
            String group = matcher.group(1);
            exercise = getWordListExercise(group,""+count);
          }
          else {
            int length = line2.split("\\(").length;
            boolean simpleFile = length == 2 && line2.split("\\(")[1].trim().endsWith(")");
            exercise = simpleFile ?
                getSimpleExerciseForLine(line2) :
                getExerciseForLine(line2);
          }
          if (showSections) {
            addSectionTest(count, exercise);
          }

          // if (count < 10) logger.info("Got " + exercise);
          exercises.add(exercise);
        } catch (Exception e) {
          logger.error("Got " + e + ".Skipping line -- couldn't parse line #"+count + " : " +line2,e);
          errors++;
          if (errors > MAX_ERRORS) {
            logger.error("too many errors, giving up...");
            break;
          }
        }
      }
     // w.close();

      logger.debug("sections " + typeToUnitToLesson);
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (exercises.isEmpty()) {
      logger.error("no exercises found in " + lessonPlanFile +"?");
    }
    else {
      logger.debug("found " + exercises.size() + " exercises, first is " + exercises.iterator().next());
    }
  }

  private void addSectionTest(int count, Exercise exercise) {
    // testing unit/chapter/week stuff
    String unitType = "unit";
    String chapterType = "chapter";
    String weekType = "week";
    String unitSection = (count % 2 == 0) ? "even" : "odd";
    List<Pair> pairs = new ArrayList<Pair>();
    pairs.add(addExerciseToLesson(exercise, unitType, unitSection));

    String id = "" +count;
    //Map<String, Lesson> chapter = getSectionToLesson(chapterType);
    String digit = id.substring(id.length() - 1);
    String chapterName = "Chapter " + digit;
          /*Set<String> chapter1 = */
    pairs.add(addExerciseToLesson(exercise, chapterType, chapterName));
    // subSections.add(chapterName);
/*          pairs.add(addAssociation(unitType,unitSection,chapterType,chapterName));
          addAssociation(chapterType,chapterName,unitType,unitSection);*/

    Integer chapterID = Integer.parseInt(digit);
    if (chapterID < 3) {
      pairs.add(addExerciseToLesson(exercise, weekType, "Week 1"));
/*
      addAssociation(chapterType, chapterName, unitType, unitSection);
      addAssociation(unitType,unitSection,chapterType,chapterName);
      addAssociation(weekType,"Week 1",chapterType,chapterName);
      addAssociation(weekType,"Week 1",chapterType,chapterName);
*/

    } else if (chapterID < 6) {
      pairs.add(addExerciseToLesson(exercise, weekType, "Week 2"));
    }
    addAssociations(pairs);
  }

  private Map<String, Lesson> getSectionToLesson( String section) {
    Map<String, Lesson> unit = typeToUnitToLesson.get(section);
    if (unit == null) typeToUnitToLesson.put(section, unit = new HashMap<String, Lesson>());
    return unit;
  }

  private Pair addExerciseToLesson(Exercise exercise, String type, String unitName) {

    Map<String, Lesson> unit = getSectionToLesson(type);

    Lesson even = unit.get(unitName);
    if (even == null) unit.put(unitName, even = new Lesson(unitName, "", ""));
    even.addExercise(exercise);

   return new Pair(type,unitName);
  }

  private static class Pair {
    String type; String section;

    public Pair(String type, String section) {
      this.type = type;
      this.section = section;
    }
  }

  private void addAssociations(List<Pair> pairs) {
    for (Pair p : pairs) {
      List<Pair> others = new ArrayList<Pair>(pairs);
      others.remove(p);
      for (Pair o : others) {
        addAssociation(p, o);
       // addAssociation(o, p);
      }
    }
  }

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

  private Exercise getWordListExercise(String arabic, String id) {
    String content = getArabic(arabic);

    Exercise exercise = new Exercise("repeat", id, content, false, true, arabic);
    exercise.addQuestion(Exercise.FL, "Please record the sentence above.","", Collections.EMPTY_LIST);
    return exercise;
  }
  /**
   * Assumes a file that looks like:
   * <br></br>
   *
   * <s> word word word </s> (audio_file_name_without_suffix)
   *
   * @param line2
   * @return
   */
  private Exercise getSimpleExerciseForLine(String line2) {
    String[] split = line2.split("\\(");
    String name = split[1].trim();
    name = name.substring(0,name.length()-1); // remove trailing )
    String displayName = name;
    String arabic = split[0].trim();
    arabic = arabic.replaceAll("<s>","").replaceAll("</s>","").trim();
    String content = getArabic(arabic);
    String audioRef = mediaDir + File.separator+"media"+File.separator+name+".wav";

    Exercise repeat = new Exercise("repeat", displayName, content, ensureForwardSlashes(audioRef), arabic, arabic);
    //logger.debug("got " +repeat);
    return repeat;
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
    String displayName = name;
    String arabic = split1[2];
    String translit = split1[3];
    String english = split1[4];

    String content = getContent(arabic, translit, english);
    String fastAudioRef = mediaDir + File.separator+"media"+File.separator+name+File.separator+ FAST + ".wav";
    String slowAudioRef = mediaDir + File.separator+"media"+File.separator+name+File.separator+ SLOW + ".wav";

    return new Exercise("repeat", displayName, content,
        ensureForwardSlashes(fastAudioRef), ensureForwardSlashes(slowAudioRef), arabic, english);
  }

  public String getContent(String arabic, String translit, String english) {
    return getArabic(arabic) +
        (translit.length() > 0?
        "<div class=\"Instruction\">\n" +
        "<span class=\"Instruction-title\">Transliteration:</span>\n" +
        "<span class=\"Instruction-data\"> " + translit +
        "</span>\n" +
        "</div>\n" : "")+
        (english.length() > 0 ?
        "<div class=\"Instruction\">\n" +
        "<span class=\"Instruction-title\">Translation:</span>\n" +
        "<span class=\"Instruction-data\"> " + english +
        "</span>\n" +
        "</div>" : "");
  }

  private String getArabic(String arabic) {
    return "<div class=\"Instruction\">\n" +
        "<span class=\"Instruction-title\">Say:</span>\n" +
        "<span class=\"" +
        (isUrdu ? "urdufont" : "Instruction-data") +
        "\"> " + arabic +
        "</span>\n" +
        "</div>\n";
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

  public static void main(String [] arg) {
    Pattern pattern = Pattern.compile("^\\d+\\.(.+)");
    Matcher matcher = pattern.matcher("1. ????????? ????????");
    System.out.println(matcher.matches());
  //  System.out.println(" group " + matcher.find());
    System.out.println(" match " + matcher.group(1));
    //new FileExerciseDAO(mediaDir).convertPlan();
    /*List<Exercise> rawExercises = new FileExerciseDAO(*//**//*"war"*//**//*).getRawExercises();
    System.out.println("first is " + rawExercises.iterator().next());*/
  }
}
