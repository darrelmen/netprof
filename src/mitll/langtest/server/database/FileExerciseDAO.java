package mitll.langtest.server.database;

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
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/8/12
 * Time: 3:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileExerciseDAO implements ExerciseDAO {
  private static final String ENCODING = "UTF8";

  private List<Exercise> exercises;

  public FileExerciseDAO() {}
  public FileExerciseDAO(String installPath) {
    readExercises(installPath);
  }

  public void readExercises(String installPath) {
    if (exercises != null) return;
    String exerciseFile = "lesson-737.csv";
    InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(exerciseFile);
    if (resourceAsStream == null) {
      System.err.println("can't find " + exerciseFile);
      try {
        resourceAsStream = new FileInputStream("C:\\Users\\go22670\\DLITest\\LangTest\\src\\lesson-737.csv");
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
      if (resourceAsStream == null) {
        return;
      }
    }
    try {
      exercises = new ArrayList<Exercise>();
      BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,ENCODING));
      String line2;
      while ((line2 = reader.readLine()) != null) {
        String[] split = line2.split(",");
        String name = split[1];
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
      /*  if (installPath.length() > 0) {
          audioRef = installPath +audioRef;
        }*/
        File file = new File(audioRef);
        if (!file.exists()) {
         // System.err.println("can't find audio file " + file.getAbsolutePath());
        } else {
          Exercise exercise = new Exercise("repeat", name, content, audioRef, arabic);
          exercises.add(exercise);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (exercises.isEmpty()) {
      System.err.println("no exercises found in " + exerciseFile +"?");
    }
    else {
      System.out.println("found " + exercises.size() + " exercises");
    }
  }

  public List<Exercise> getRawExercises() {
    return exercises;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public static void main(String [] arg) {
    List<Exercise> rawExercises = new FileExerciseDAO("war").getRawExercises();
    System.out.println("first is " + rawExercises.iterator().next());
  }
}
