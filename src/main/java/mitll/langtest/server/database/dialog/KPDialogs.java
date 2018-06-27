package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.SlickDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

/**
 * Dialog data from Paul - 6/20/18
 */
public class KPDialogs {
  private static final Logger logger = LogManager.getLogger(KPDialogs.class);

  String docIDS = "333815\n" +
      "333816\n" +
      "333817\n" +
      "333818\n" +
      "333819\n" +
      "333821\n" +
      "333822\n" +
      "333823\n" +
      "333824\n" +
      "333825";
  String title = "Meeting someone for the first time\n" +
      "What time is it?\n" +
      "You dialed the wrong number.\n" +
      "What will you do during the coming school break?\n" +
      "Where should I go to exchange currency?\n" +
      "What do Koreans do in their spare time?\n" +
      "Please give me two tickets for the 10:30 showing?\n" +
      "Please exchange this for a blue tie.\n" +
      "Common Ailments and Symptoms\n" +
      "Medical Emergencies";
  String dir = "010_C01\n" +
      "001_C05\n" +
      "003_C09\n" +
      "023_C09\n" +
      "036_C13\n" +
      "001_C17\n" +
      "019_C17\n" +
      "001_C18\n" +
      "005_C29\n" +
      "010_C30";
  String chapter = "1\n" +
      "5\n" +
      "9\n" +
      "9\n" +
      "13\n" +
      "17\n" +
      "17\n" +
      "18\n" +
      "29\n" +
      "30";
  String page = "12\n" +
      "5\n" +
      "5\n" +
      "15\n" +
      "25\n" +
      "5\n" +
      "26\n" +
      "4\n" +
      "7\n" +
      "12";
  String pres = "Topic Presentation B\n" +
      "Topic Presentation A\n" +
      "Topic Presentation A\n" +
      "Topic Presentation A\n" +
      "Topic Presentation C\n" +
      "Topic Presentation A\n" +
      "Topic Presentation C\n" +
      "Topic Presentation A\n" +
      "Topic Presentation A\n" +
      "Topic Presentation B";

  String unit = "1\n" +
      "2\n" +
      "3\n" +
      "3\n" +
      "4\n" +
      "5\n" +
      "5\n" +
      "5\n" +
      "8\n" +
      "8";

  public List<IDialog> getDialogs(int defaultUser, int projID) {
    String[] docs = docIDS.split("\n");
    String[] titles = title.split("\n");

    String[] units = unit.split("\n");
    String[] chapters = chapter.split("\n");
    String[] pages = page.split("\n");
    String[] topics = pres.split("\n");
    String[] dirs = dir.split("\n");

    List<IDialog> dialogs = new ArrayList<>();
    long time = System.currentTimeMillis();
    Timestamp modified = new Timestamp(time);
    for (int i = 0; i < docs.length; i++) {
      String dir = dirs[i];
      String imageRef = "/opt/netprof/images/" + dir + File.separator + dir + ".jpg";
      List<ExerciseAttribute> attributes = new ArrayList<>();
      attributes.add(new ExerciseAttribute("unit", units[i]));
      attributes.add(new ExerciseAttribute("chapter", chapters[i]));
      attributes.add(new ExerciseAttribute("page", pages[i]));
      attributes.add(new ExerciseAttribute("topic", topics[i]));

      List<CommonExercise> exercises = new ArrayList<>();

      String dirPath = "/opt/netprof/dialog/" + dir;
      File loc = new File(dirPath);
      boolean directory = loc.isDirectory();
      if (!directory) logger.warn("huh? not a dir");

      //   List<String> images=new ArrayList<>();
      List<String> sentences = new ArrayList<>();
      //List<Path> sentenceFiles = new ArrayList<>();
      List<String> audio = new ArrayList<>();
      Map<CommonExercise, String> exToAudio = new HashMap<>();
      Map<String, Path> sentenceToFile = new HashMap<>();
      try {
        String absolutePath = loc.getAbsolutePath();
        logger.info("looking in " + absolutePath);

        List<String> audioFileNames=new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(absolutePath))) {
          paths
              .filter(Files::isRegularFile)
              .forEach(file -> {

                logger.info("found " +file);
                String fileName = file.getFileName().toString();
                logger.info("fileName " +fileName);
                if (fileName.endsWith("jpg")) {
                  logger.info("skip " + fileName);
                } else if (fileName.endsWith(".wav")) {
               //   audio.add(fileName);
                  audioFileNames.add(fileName);
                } else if (fileName.endsWith(".txt")) {
                  String e = fileName.toString();
                  sentences.add(e);
                  //    sentenceFiles.add(file);
                  sentenceToFile.put(e, file);
                }
              });
        }

        audio.sort(Comparator.comparing(String::toString));
        sentences.sort(Comparator.comparing(String::toString));

        logger.info("found audio  " +audio);
        logger.info("found sentences  " +sentences);

        sentences.forEach(file -> {
          Path path = sentenceToFile.get(file);
          StringBuilder builder = new StringBuilder();

          try (Stream<String> stream = Files.lines(path)) {
            stream.forEach(builder::append);
          } catch (IOException e) {
            e.printStackTrace();
          }

          {
            Exercise exercise = new Exercise();
            exercise.getMutable().setForeignLanguage(builder.toString());
            String pathAudio = dirPath + File.separator + audio.get(exercises.size());
            exToAudio.put(exercise, pathAudio);
            exercises.add(exercise);
          }
        });
      } catch (IOException e) {
        logger.error("got " + e, e);
      }

      SlickDialog e = new SlickDialog(-1,
          defaultUser,
          projID,
          -1,
          -1,
          modified,
          modified,
          DialogType.DIALOG.toString(),
          DialogStatus.DEFAULT.toString(),
          titles[i],
          ""
      );
      Dialog dialog = new Dialog(-1, defaultUser, projID, -1, time,
          "", imageRef,
          attributes,
          exercises);
      dialog.setSlickDialog(e);
      dialogs.add(dialog);

      logger.info("read " + dialog);
      logger.info("\tex   " + dialog.getExercises());
      logger.info("\tattr " + dialog.getAttributes());
    }
    return dialogs;
  }
}
