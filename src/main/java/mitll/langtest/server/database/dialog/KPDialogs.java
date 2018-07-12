package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.SlickDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

import static mitll.langtest.shared.dialog.IDialog.METADATA.FLTITLE;

/**
 * Dialog data from Paul - 6/20/18
 */
public class KPDialogs implements IDialogReader {
  private static final Logger logger = LogManager.getLogger(KPDialogs.class);
  private static final String DIALOG = "dialog";
  private static final List<String> SPEAKER_LABELS = Arrays.asList("A", "B", "C", "D", "E", "F");
  private static final String UNIT = IDialog.METADATA.UNIT.getLC();
  private static final String CHAPTER = IDialog.METADATA.CHAPTER.getLC();
  private static final String PAGE = IDialog.METADATA.PAGE.getLC();
  private static final String PRESENTATION = IDialog.METADATA.PRESENTATION.getLC();
  private static final String SPEAKER = IDialog.METADATA.SPEAKER.getCap();
  //public static final String FLTITLE = "fltitle";

  private final String docIDS =
      "333815\n" +
          "333816\n" +
          "333817\n" +
          "333818\n" +
          "333819\n" +
          "333821\n" +
          "333822\n" +
          "333823\n" +
          "333824\n" +
          "333825";
  private final String title =
      "Meeting someone for the first time\n" +
          "What time is it?\n" +
          "You dialed the wrong number.\n" +
          "What will you do during the coming school break?\n" +
          "Where should I go to exchange currency?\n" +
          "What do Koreans do in their spare time?\n" +
          "Please give me two tickets for the 10:30 showing?\n" +
          "Please exchange this for a blue tie.\n" +
          "Common Ailments and Symptoms\n" +
          "Medical Emergencies";
  private final String ktitle =
      "처음 만났을 때\n" +
          "지금 몇 시예요?\n" +
          "전화 잘못 거셨어요.\n" +

          "이번 방학에 뭐 할 거야?\n" +
          "환전하려면 어디로 가야 해요?\n" +
          "한국 사람들은 시간이 날 때 뭐 해요?\n" +

          "10시 반 표 두 장 주세요.\n" +
          "파란색 넥타이로 바꿔 주세요.\n" +
          "독감에 걸려서 고생했어.\n" +

          "구급차 좀 빨리 보내주세요.";
  private final String dir =
      "010_C01\n" +
          "001_C05\n" +
          "003_C09\n" +
          "023_C09\n" +
          "036_C13\n" +
          "001_C17\n" +
          "019_C17\n" +
          "001_C18\n" +
          "005_C29\n" +
          "010_C30";
  private final String chapter =
      "1\n" +
          "5\n" +
          "9\n" +
          "9\n" +
          "13\n" +
          "17\n" +
          "17\n" +
          "18\n" +
          "29\n" +
          "30";
  private final String page =
      "12\n" +
          "5\n" +
          "5\n" +
          "15\n" +
          "25\n" +
          "5\n" +
          "26\n" +
          "4\n" +
          "7\n" +
          "12";
  private final String pres =
      "Topic Presentation B\n" +
          "Topic Presentation A\n" +
          "Topic Presentation A\n" +
          "Topic Presentation A\n" +
          "Topic Presentation C\n" +
          "Topic Presentation A\n" +
          "Topic Presentation C\n" +
          "Topic Presentation A\n" +
          "Topic Presentation A\n" +
          "Topic Presentation B";

  private final String unit =
      "1\n" +
          "2\n" +
          "3\n" +
          "3\n" +
          "4\n" +
          "5\n" +
          "5\n" +
          "5\n" +
          "8\n" +
          "8";

  /**
   * @param defaultUser
   * @param projID
   * @param exToAudio
   * @return
   * @see mitll.langtest.server.database.project.DialogPopulate#addDialogInfo
   */
  @Override
  public Map<Dialog, SlickDialog> getDialogs(int defaultUser, int projID,
                                             Map<ClientExercise, String> exToAudio,
                                             Project project) {
    String[] docs = docIDS.split("\n");
    String[] titles = title.split("\n");
    String[] ktitles = ktitle.split("\n");

    String[] units = unit.split("\n");
    String[] chapters = chapter.split("\n");
    String[] pages = page.split("\n");
    String[] topics = pres.split("\n");
    String[] dirs = dir.split("\n");

    //List<Dialog> dialogs = new ArrayList<>();
    long time = System.currentTimeMillis();
    Timestamp modified = new Timestamp(time);

    Map<Dialog, SlickDialog> dialogToSlick = new HashMap<>();
    for (int i = 0; i < docs.length; i++) {
      String dir = dirs[i];
      //  logger.info("Dir " + dir);
      String imageRef = //"/opt/netprof/" +
          "images/" + dir + File.separator + dir + ".jpg";
      String unit = units[i];
      String chapter = chapters[i];
      List<ExerciseAttribute> attributes = getExerciseAttributes(pages[i], topics[i]);

      List<ClientExercise> exercises = new ArrayList<>();
      List<ClientExercise> coreExercises = new ArrayList<>();

      String dirPath = "/opt/netprof/dialog/" + dir;
      File loc = new File(dirPath);
      boolean directory = loc.isDirectory();
      if (!directory) logger.warn("huh? not a dir");

      List<String> sentences = new ArrayList<>();
      List<String> audio = new ArrayList<>();

      //List<Path> passageTextFiles = new ArrayList<>();
      //    Map<CommonExercise, String> exToAudio = new HashMap<>();
      Map<String, Path> sentenceToFile = new HashMap<>();
      List<String> orientations = new ArrayList<>();
      try {
        String absolutePath = loc.getAbsolutePath();
        logger.info("looking in " + absolutePath);

        try (Stream<Path> paths = Files.walk(Paths.get(absolutePath))) {
          paths
              .filter(Files::isRegularFile)
              .forEach(file -> {

                logger.info(dir + " found " + file);
                String fileName = file.getFileName().toString();
                String[] parts = fileName.split("_");
                //        if (parts.length == 2) passageTextFiles.add(file);
                //              logger.info("fileName " + fileName);
                if (fileName.endsWith("jpg")) {
//                  logger.info("skip dialog image " + fileName);
                } else if (fileName.endsWith(".wav")) {
                  //              logger.info("audio " + fileName);
                  audio.add(dir + File.separator + fileName);
                  //    audioFileNames.add(fileName);
                } else if (fileName.endsWith(".txt") && parts.length == 3) { // slickDialog.g. 010_C01_00.txt
                  String e = fileName;
                  logger.info(dir + " text " + fileName);
                  sentences.add(e);
                  sentenceToFile.put(e, file);
                }
              });
        }

        audio.sort(Comparator.comparingInt(this::getIndex));
        sentences.sort(Comparator.comparingInt(this::getIndex));

        logger.info(dir + " found audio      " + audio.size());
        logger.info(dir + " found sentences  " + sentences.size());

        Set<String> speakers = new LinkedHashSet<>();

        sentences.forEach(file -> {
          Path path = sentenceToFile.get(file);
          int index = getIndex(path.toString());
          // logger.info("sentence " + file);
          // logger.info("path     " + path.toString());
          // logger.info("index    " + index);

          String fileText = getTextFromFile(path);

          if (index == 0) {
            orientations.add(fileText);
          } else {
            ClientExercise exercise = getExercise(attributes, speakers, path, fileText, unit, chapter);
            exToAudio.put(exercise, DIALOG + File.separator + audio.get(exercises.size()));

            while (!project.isTrieBuilt()) {
              logger.info("wait for trie...");
              try {
                Thread.sleep(2000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
            //   logger.info(" trie ready...");

            addCoreWords(project, coreExercises, exercise);
            exercises.add(exercise);
//            logger.info("Ex " + exercise.getOldID() + " " + exercise.getUnitToValue());
          }
        });

        // add speaker attributes
        addSpeakerAttrbutes(attributes, speakers);
      } catch (IOException e) {
        logger.error("got " + e, e);
      }

      String orientation = orientations.get(0);
      String title = titles[i];

      List<ExerciseAttribute> dialogAttr = new ArrayList<>(attributes);
      dialogAttr.add(new ExerciseAttribute(FLTITLE.toString().toLowerCase(), ktitles[i], false));

      SlickDialog slickDialog = new SlickDialog(-1,
          defaultUser,
          projID,
          -1,
          -1,
          modified,
          modified,
          unit, chapter,
          DialogType.DIALOG.toString(),
          DialogStatus.DEFAULT.toString(),
          title,
          orientation
      );

      Dialog dialog = new Dialog(-1, defaultUser, projID, -1, -1, time,
          unit, chapter,
          orientation,
          imageRef,
          "",
          title,

          dialogAttr,
          exercises,
          coreExercises);

      //dialog.setSlickDialog(slickDialog);

      dialogToSlick.put(dialog, slickDialog);
      // dialogs.add(dialog);

      // logger.info("read " + dialog);
      //    dialog.getExercises().forEach(logger::info);
      //logger.info("\tex   " + dialog.getExercises());
      // logger.info("\tattr " + dialog.getAttributes());
      //  dialog.getAttributes().forEach(logger::info);
    }

    logger.info("ex to audio now " + exToAudio.size());
    return dialogToSlick;
  }

  private void addSpeakerAttrbutes(List<ExerciseAttribute> attributes, Set<String> speakers) {
    List<String> speakersList = new ArrayList<>(speakers);
    speakersList
        .forEach(s -> attributes
            .add(new ExerciseAttribute(SPEAKER +
                " " + SPEAKER_LABELS.get(speakersList.indexOf(s)), s, false)));
  }

  private void addCoreWords(Project project, List<ClientExercise> coreExercises, ClientExercise exercise) {
    String[] tokens = exercise.getForeignLanguage().split(" ");
    Set<String> uniq = new HashSet<>(Arrays.asList(tokens));
    uniq.forEach(token -> {
      CommonExercise exerciseBySearch = project.getExerciseBySearch(token);
      if (exerciseBySearch != null) {
        coreExercises.add(exerciseBySearch);
      }
    });
  }

  /**
   * @param attributes
   * @param speakers
   * @param path
   * @param fileText
   * @param unit
   * @param chapter
   * @return
   * @see #getDialogs
   */
  @NotNull
  private Exercise getExercise(List<ExerciseAttribute> attributes,
                               Set<String> speakers,
                               Path path,
                               String fileText, String unit, String chapter) {
    Exercise exercise = new Exercise();
    Map<String, String> unitToValue = new HashMap<>();
    unitToValue.put(UNIT, unit);
    unitToValue.put(CHAPTER, chapter);
    exercise.setUnitToValue(unitToValue);
    attributes.forEach(exercise::addAttribute);

    int i1 = fileText.indexOf(":");
    String speaker = fileText.substring(0, i1).trim();
    speakers.add(speaker);
    //  logger.info("file name " + path.getFileName());
    String[] split = path.getFileName().toString().split("\\.");
    String oldID = split[0];
    //  logger.info("ex " + oldID);
    exercise.getMutable().setOldID(oldID);
    exercise.addAttribute(new ExerciseAttribute(SPEAKER, speaker, false));
    // logger.info("speaker " + speaker);
    String turn = fileText.substring(i1 + 1).trim();

    exercise.getMutable().setForeignLanguage(turn);

    return exercise;
  }

  @NotNull
  private String getTextFromFile(Path path) {
    StringBuilder builder = new StringBuilder();

    try (Stream<String> stream = Files.lines(path)) {
      stream.forEach(builder::append);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return builder.toString();
  }

  /**
   * @param page
   * @param topic
   * @return
   */
  @NotNull
  private List<ExerciseAttribute> getExerciseAttributes(String page, String topic) {
    List<ExerciseAttribute> attributes = new ArrayList<>();
    attributes.add(new ExerciseAttribute(PAGE, page));
    attributes.add(new ExerciseAttribute(PRESENTATION, topic));
    return attributes;
  }

  private int getIndex(String o1) {
    String[] split = o1.split("/");
    String name = split[split.length - 1];
    // logger.info("index " + name );
    String[] parts = name.split("_");
    String count = parts[2];
    //  logger.info("count " + count );
    String index = count.split("\\.")[0];
    //  logger.info("index " + index );
    return Integer.parseInt(index);
  }
}
