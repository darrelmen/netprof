package mitll.langtest.server.database;

import mitll.langtest.server.AudioConversion;
import mitll.langtest.shared.Exercise;

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
  private static final String ENCODING = "UTF8";
  public static final String LESSON_FILE = "lesson-737.csv";

  private List<Exercise> exercises;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeExerciseDAO()
   */
  public FileExerciseDAO() {}

  /**
   * TODO : write to h2
   * @see mitll.langtest.server.database.DatabaseImpl#getExercises()
   * @paramx installPath
   */
  public void readExercises(String installPath) {
    if (exercises != null) return;
    String exerciseFile = LESSON_FILE;
    InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(exerciseFile);
    if (resourceAsStream == null) {
      System.err.println("can't find " + exerciseFile);
      try {
        resourceAsStream = new FileInputStream("C:\\Users\\go22670\\DLITest\\LangTest\\src\\" +LESSON_FILE);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
      if (resourceAsStream == null) {
        return;
      }
    }

    try {
      AudioConversion audioConversion = new AudioConversion();
      exercises = new ArrayList<Exercise>();
      BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,ENCODING));
      String line2;
      int count = 0;
      System.out.println("using install path " + installPath);
      while ((line2 = reader.readLine()) != null) {
        String[] split = line2.split(",");
        String name = split[1];
        String displayName = split[2];
        String arabic = split[3];
        String translit = split[4];
        String english = split[5];

        String content = "<div class=\"Instruction\">\n" +
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
          if (count++ < 5) System.err.println("can't find audio file " + file.getAbsolutePath());
        } else {
          audioConversion.ensureWriteMP3(audioRef,installPath);
          if (count++ < 20) System.out.println("name to use " + displayName);
          Exercise exercise = new Exercise("repeat", displayName, content, ensureForwardSlashes(audioRef), arabic);
          exercises.add(exercise);
        }
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (exercises.isEmpty()) {
      System.err.println("no exercises found in " + exerciseFile +"?");
    }
    else {
      System.out.println("found " + exercises.size() + " exercises, first is " + exercises.iterator().next());
    }
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  public List<Exercise> getRawExercises() {
    return exercises;
  }

/*  public static void main(String [] arg) {
    List<Exercise> rawExercises = new FileExerciseDAO(*//*"war"*//*).getRawExercises();
    System.out.println("first is " + rawExercises.iterator().next());
  }*/
}
