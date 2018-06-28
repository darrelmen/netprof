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
import org.jetbrains.annotations.NotNull;

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

  public List<IDialog> getDialogs(int defaultUser, int projID,  Map<CommonExercise, String> exToAudio) {
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
    //  logger.info("Dir " + dir);
      String imageRef = "/opt/netprof/images/" + dir + File.separator + dir + ".jpg";
      List<ExerciseAttribute> attributes = getExerciseAttributes(units[i], chapters[i], pages[i], topics[i]);

      List<CommonExercise> exercises = new ArrayList<>();

      String dirPath = "/opt/netprof/dialog/" + dir;
      File loc = new File(dirPath);
      boolean directory = loc.isDirectory();
      if (!directory) logger.warn("huh? not a dir");

      List<String> sentences = new ArrayList<>();
      List<String> audio = new ArrayList<>();

      List<Path> passageTextFiles = new ArrayList<>();
      //    Map<CommonExercise, String> exToAudio = new HashMap<>();
      Map<String, Path> sentenceToFile = new HashMap<>();
      List<String> orientations=new ArrayList<>();
      try {
        String absolutePath = loc.getAbsolutePath();
        logger.info("looking in " + absolutePath);

        try (Stream<Path> paths = Files.walk(Paths.get(absolutePath))) {
          paths
              .filter(Files::isRegularFile)
              .forEach(file -> {

//                logger.info("found " + file);
                String fileName = file.getFileName().toString();
                String[] parts = fileName.split("_");
                if (parts.length == 2) passageTextFiles.add(file);
  //              logger.info("fileName " + fileName);
                if (fileName.endsWith("jpg")) {
                  logger.info("skip " + fileName);
                } else if (fileName.endsWith(".wav")) {
    //              logger.info("audio " + fileName);
                  audio.add(fileName);
                  //    audioFileNames.add(fileName);
                } else if (fileName.endsWith(".txt") && parts.length == 3) { // e.g. 010_C01_00.txt
                  String e = fileName;
      //            logger.info("text " + fileName);
                  sentences.add(e);
                  sentenceToFile.put(e, file);
                }
              });
        }

        audio.sort(Comparator.comparingInt(this::getIndex));
        sentences.sort(Comparator.comparingInt(this::getIndex));

        //logger.info("found audio      " + audio);
       // logger.info("found sentences  " + sentences);

        sentences.forEach(file -> {
          Path path = sentenceToFile.get(file);
          int index = getIndex(path.toString());
         // logger.info("sentence " + file);
         // logger.info("path     " + path.toString());
         // logger.info("index    " + index);

          String fileText = getTextFromFile(path);

          if (index == 0) {
            orientations.add(fileText);
          }
          else
          {
            Exercise exercise = new Exercise();
            attributes.forEach(exercise::addAttribute);
            exercise.getMutable().setForeignLanguage(fileText);

            String pathAudio = dirPath + File.separator + audio.get(exercises.size());
            exToAudio.put(exercise, pathAudio);

            exercises.add(exercise);
          }
        });
      } catch (IOException e) {
        logger.error("got " + e, e);
      }

      String orientation = orientations.get(0);
      String title = titles[i];
      SlickDialog e = new SlickDialog(-1,
          defaultUser,
          projID,
          -1,
          -1,
          modified,
          modified,
          DialogType.DIALOG.toString(),
          DialogStatus.DEFAULT.toString(),
          title,
          orientation
      );

      Dialog dialog = new Dialog(-1, defaultUser, projID, -1, time,
          orientation,
          imageRef,
          "",
          title,

          attributes,
          exercises);
      dialog.setSlickDialog(e);
      dialogs.add(dialog);

     // logger.info("read " + dialog);
      dialog.getExercises().forEach(logger::info);
      //logger.info("\tex   " + dialog.getExercises());
     // logger.info("\tattr " + dialog.getAttributes());
      dialog.getAttributes().forEach(logger::info);
    }
    return dialogs;
  }

  @NotNull
  private String  getTextFromFile(Path path) {
    StringBuilder builder = new StringBuilder();

    try (Stream<String> stream = Files.lines(path)) {
      stream.forEach(builder::append);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return builder.toString();
  }

  @NotNull
  private List<ExerciseAttribute> getExerciseAttributes(String unit, String chapter, String page, String topic) {
    List<ExerciseAttribute> attributes = new ArrayList<>();
    attributes.add(new ExerciseAttribute("unit", unit));
    attributes.add(new ExerciseAttribute("chapter", chapter));
    attributes.add(new ExerciseAttribute("page", page));
    attributes.add(new ExerciseAttribute("topic", topic));
    return attributes;
  }

  private int getIndex(String o1) {
    String[] split = o1.split("/");
    String name =split[split.length-1];
   // logger.info("index " + name );
    String[] parts = name.split("_");
    String count = parts[2];
  //  logger.info("count " + count );
    String index = count.split("\\.")[0];
  //  logger.info("index " + index );
    return Integer.parseInt(index);
  }
}
