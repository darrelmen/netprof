package mitll.langtest.server.database;

import audio.imagewriter.AudioConverter;
import audio.tools.FileCopier;
import mitll.langtest.server.AudioConversion;
import mitll.langtest.shared.Exercise;
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
import java.util.List;

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
  private static final String ENCODING = "UTF8";
  private static final String LESSON_FILE = "lesson-737.csv";
  private static final String FAST_AND_SLOW_REF = "fastAndSlowRef";
  private static final String LESSON_PLAN = "lesson.plan";
  private static final String FAST = "fast";
  private static final String SLOW = "slow";

  private List<Exercise> exercises;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeExerciseDAO
   */
  public FileExerciseDAO() {}

  /**
   * TODO : write to h2
   * @see DatabaseImpl#getExercises(boolean)
   * @param installPath
   */
  public void readExercises(String installPath) {
    if (exercises != null) return;
    String exerciseFile = LESSON_FILE;
    InputStream resourceAsStream = getExerciseListStream(exerciseFile);
    if (resourceAsStream == null) return;

    try {
      AudioConversion audioConversion = new AudioConversion();
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
          audioConversion.ensureWriteMP3(audioRef,installPath);
          Exercise exercise = new Exercise("repeat", displayName, content, ensureForwardSlashes(audioRef), arabic);
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

  public void readFastAndSlowExercises(String installPath) {
    if (exercises != null) return;
    String exerciseFile = LESSON_PLAN;
    InputStream resourceAsStream = getExerciseListStream(exerciseFile);
    if (resourceAsStream == null) return;
    try {
     // BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("lessonPlan752.txt"),ENCODING));

      AudioConversion audioConversion = new AudioConversion();
      exercises = new ArrayList<Exercise>();
      BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,ENCODING));
      String line2;
      int count = 0;
      logger.debug("using install path " + installPath);
      while ((line2 = reader.readLine()) != null) {
       // if (count++ > 55) break;
        String[] split = line2.split("\\|");
        String lastCol = split[6];
        String[] split1 = lastCol.split("->");
        String name = split1[1].trim();
        String displayName = name;
        String arabic = split1[2];
       /* w.write(name);
        w.write(",");
        w.write(arabic);
        w.write("\n");*/
        String translit = split1[3];
        String english = split1[4];

        String content = getContent(arabic, translit, english);
        String fastAudioRef = FAST_AND_SLOW_REF + File.separator+name+File.separator+ FAST + ".wav";
        String slowAudioRef = FAST_AND_SLOW_REF+ File.separator+name+File.separator+ SLOW + ".wav";

        for (String audioRef : new String[]{fastAudioRef,slowAudioRef}) {
          File file = new File(audioRef);
          if (!file.exists()) {
            file = new File(installPath,audioRef);
          }
          if (!file.exists()) {
            if (count++ < 5) logger.debug("can't find audio file " + file.getAbsolutePath());
          } else {
            audioConversion.ensureWriteMP3(audioRef,installPath);
          }
        }

          Exercise exercise = new Exercise("repeat", displayName, content,
              ensureForwardSlashes(fastAudioRef), ensureForwardSlashes(slowAudioRef), arabic);
          exercises.add(exercise);
      }
     // w.close();
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

  private String getContent(String arabic, String translit, String english) {
    return "<div class=\"Instruction\">\n" +
        "<span class=\"Instruction-title\">Say:</span>\n" +
        "<span class=\"Instruction-data\"> " + arabic +
        "</span>\n" +
        "</div>\n" +
        "<div class=\"Instruction\">\n" +
        "<span class=\"Instruction-title\">Transliteration:</span>\n" +
        "<span class=\"Instruction-data\"> " + translit+
        "</span>\n" +
        "</div>\n" +
        "<div class=\"Instruction\">\n" +
        "<span class=\"Instruction-title\">Translation:</span>\n" +
        "<span class=\"Instruction-data\"> " + english+
        "</span>\n" +
        "</div>";
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

  public void convertPlan() {
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
    new FileExerciseDAO().convertPlan();
    /*List<Exercise> rawExercises = new FileExerciseDAO(*//**//*"war"*//**//*).getRawExercises();
    System.out.println("first is " + rawExercises.iterator().next());*/
  }
}
