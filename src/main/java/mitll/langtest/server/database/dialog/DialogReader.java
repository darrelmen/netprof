/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.project.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.DialogType;
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

/**
 *
 */
class DialogReader extends BaseDialogReader {
  private static final Logger logger = LogManager.getLogger(DialogReader.class);

  private static final String DIALOG = "dialog";

  DialogReader(String unit, String chapter) {
    super(unit, chapter);
  }

  public Map<Dialog, SlickDialog> getInterpreterDialogs(int defaultUser, Project project, Project englishProject, String excelFile) {
    return Collections.emptyMap();
  }

  /**
   * @param defaultUser
   * @param exToAudio
   * @param project
   * @param dialogProps
   * @param coreVocabs
   * @return
   * @see EnglishDialog#getDialogs
   */
  @NotNull
  Map<Dialog, SlickDialog> getDialogsByProp(int defaultUser,
                                            Map<ClientExercise, String> exToAudio,
                                            Project project,
                                            DialogProps dialogProps,
                                            List<String> coreVocabs) {
    int projID = project.getID();
    String[] docs = dialogProps.docIDS.split("\n");
    String[] titles = dialogProps.title.split("\n");
    String[] ktitles = dialogProps.fltitle.split("\n");

    String[] units = dialogProps.unit.split("\n");
    String[] chapters = dialogProps.chapter.split("\n");
    String[] pages = dialogProps.page.split("\n");
    String[] topics = dialogProps.pres.split("\n");
    String[] dirs = dialogProps.dir.split("\n");

    Timestamp modified = new Timestamp(System.currentTimeMillis());

    Map<Dialog, SlickDialog> dialogToSlick = new HashMap<>();
    String dialogDataDir = getDialogDataDir(project);
    String projectLanguage = project.getLanguage().toLowerCase();
    String imageBaseDir = getImageBaseDir(project);

    List<CVMatch> cvs = getCVs(coreVocabs, project);
    for (int i = 0; i < docs.length; i++) {
      String dir = dirs[i];

      CVMatch cvMatch = i < cvs.size() ? cvs.get(i) : null;
      //  logger.info("Dir " + dir);
      String imageRef = getImageRef(imageBaseDir, dir);
      String unit = units[i];
      String chapter = chapters[i];
      List<ExerciseAttribute> attributes = getExerciseAttributes(pages[i], topics[i]);

      List<ClientExercise> exercises = new ArrayList<>();
      Set<ClientExercise> coreExercises = new TreeSet<>();

      String dirPath = dialogDataDir + dir;
      File loc = new File(dirPath);
      boolean directory = loc.isDirectory();
      if (!directory) logger.warn("huh? not a dir");

      List<String> sentences = new ArrayList<>();
      List<String> audio = new ArrayList<>();

      Map<String, Path> sentenceToFile = new HashMap<>();
      List<String> orientations = new ArrayList<>();
      try {
        String absolutePath = loc.getAbsolutePath();
        logger.info("looking in " + absolutePath);

        getFilesInDirectory(projectLanguage, dir, absolutePath, sentences, audio, sentenceToFile);

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
            ClientExercise exercise = getExercise(attributes, speakers, path, fileText, unit, chapter, project.getTypeOrder());

            addExToAudioEntry(exToAudio, exercises, audio, exercise);
            waitForTrie(project);
            //   logger.info(" trie ready...");

            if (cvMatch == null) {
              // if we don't have core vocab defined for a dialog, try to find matches in netprof
              addCoreWords(project, coreExercises, exercise);
            } else {
              coreExercises.addAll(cvMatch.getNetprofEntries());
              logger.info("for dialog " + dir + " adding " + cvMatch.getNetprofEntries().size() + " : ");
            }

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
      String fltitle = ktitles[i];

      addDialogPair(defaultUser, projID, modified,
          imageRef,

          unit, chapter,

          attributes,

          exercises, coreExercises,

          orientation, title, fltitle,
          dialogToSlick,
          DialogType.INTERPRETER, project.getProject().countrycode());
      // logger.info("read " + dialog);
      //    dialog.getExercises().forEach(logger::info);
      //logger.info("\tex   " + dialog.getExercises());
      // logger.info("\tattr " + dialog.getAttributes());
      //  dialog.getAttributes().forEach(logger::info);
    }

    logger.info("ex to audio now " + exToAudio.size());
    return dialogToSlick;
  }

  private void waitForTrie(Project project) {
    while (!project.isTrieBuilt()) {
      logger.info("wait for trie...");
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void addExToAudioEntry(Map<ClientExercise, String> exToAudio, List<ClientExercise> exercises, List<String> audio, ClientExercise exercise) {
    int exindex = exercises.size();
    String audioFile = audio.size() > exindex ? audio.get(exindex) : null;
    if (audioFile != null) {
      exToAudio.put(exercise, DIALOG + File.separator + audioFile);
    }
  }

  private void getFilesInDirectory(String projectLanguage,
                                   String dir,
                                   String absolutePath,

                                   List<String> sentences,
                                   List<String> audio,
                                   Map<String, Path> sentenceToFile) throws IOException {
    try (Stream<Path> paths = Files.walk(Paths.get(absolutePath))) {
      paths
          .filter(Files::isRegularFile)
          .forEach(file -> {
            if (file.getFileName().toString().endsWith("~")) {
              logger.info("skip tilde - " + file);
            } else {
              logger.info(dir + " found " + file);
              String fileName = file.getFileName().toString();
              String[] parts = fileName.split("_");
              if (fileName.endsWith("jpg")) {
//                  logger.info("skip dialog image " + fileName);
              } else if (fileName.endsWith(".wav")) {
                //              logger.info("audio " + fileName);
                String relPath = projectLanguage + File.separator + dir + File.separator + fileName;
                logger.info("audio rel path " + relPath);

                audio.add(relPath);
              } else if (fileName.endsWith(".txt") && parts.length == 3) { // slickDialog.g. 010_C01_00.txt
                logger.info(dir + " text " + fileName);
                sentences.add(fileName);
                sentenceToFile.put(fileName, file);
              }
            }
          });
    }
  }

  /**
   * @param coreVocabs
   * @param project
   * @return
   * @see #getDialogsByProp
   */
  private List<CVMatch> getCVs(List<String> coreVocabs, Project project) {
    List<CVMatch> cvs = new ArrayList<>(coreVocabs.size());

    coreVocabs.forEach(raw -> {
      Set<CommonExercise> coreExercises = new HashSet<>();
      CVMatch cvMatch = new CVMatch(coreExercises);
      cvs.add(cvMatch);
      String[] split = raw.split("\n");
      List<String> rawEntries = Arrays.asList(split);
      rawEntries.forEach(rawEntry -> {
        String entry = rawEntry.trim();

        CommonExercise exerciseBySearch = project.getExerciseByExactMatch(entry);

        if (exerciseBySearch != null) {
          remember(coreExercises, entry, exerciseBySearch);
        } else {
          logger.warn("getCVs can't find token '" + entry + "'");

          // remove things in parens
          entry = entry.replaceAll("\\([^()]*\\)", "");
          exerciseBySearch = project.getExerciseByExactMatch(entry);

          if (exerciseBySearch == null) {
            logger.warn("getCVs 1 can't find token '" + entry + "'");

            String[] split1 = entry.split("\\/");
            if (split1.length > 1) {
              entry = split1[0].trim();
              exerciseBySearch = project.getExerciseByExactMatch(entry);
              if (exerciseBySearch == null) {
                logger.warn("getCVs 2 can't find token '" + entry + "'");

                entry = entry.replaceAll("~", "").trim();
                exerciseBySearch = project.getExerciseByExactMatch(entry);
                if (exerciseBySearch == null) {
                  logger.warn("getCVs 3 can't find token '" + entry + "'");
                } else {
                  remember(coreExercises, entry, exerciseBySearch);
                }
              } else {
                remember(coreExercises, entry, exerciseBySearch);
              }
            }

          } else {
            remember(coreExercises, entry, exerciseBySearch);
          }

        }
      });
    });
    return cvs;
  }

  private void remember(Set<CommonExercise> coreExercises, String entry, CommonExercise exerciseBySearch) {
    logger.info("getCVs : core vocab '" + entry + "' = '" + exerciseBySearch.getForeignLanguage() +
        "'");
    coreExercises.add(exerciseBySearch);
  }

  static class CVMatch {
    private final Set<CommonExercise> netprofEntries;

    CVMatch(Set<CommonExercise> netprofEntries) {
      this.netprofEntries = netprofEntries;
    }

    Set<CommonExercise> getNetprofEntries() {
      return netprofEntries;
    }
  }

  static class DialogProps {
    final String docIDS;
    final String title;
    final String fltitle;
    final String dir;
    final String unit;
    final String chapter;
    final String page;
    final String pres;

    DialogProps(String docIDS,
                String title, String fltitle, String dir, String unit, String chapter, String page, String pres) {
      this.docIDS = docIDS;
      this.title = title;
      this.fltitle = fltitle;
      this.dir = dir;
      this.unit = unit;
      this.chapter = chapter;
      this.page = page;
      this.pres = pres;
    }
  }

  /**
   * @param project
   * @param coreExercises set - only unique exercises...
   * @param exercise
   */
  private void addCoreWords(Project project, Collection<ClientExercise> coreExercises, ClientExercise exercise) {
    String[] tokens = exercise.getForeignLanguage().split(" ");
    Set<String> uniq = new HashSet<>(Arrays.asList(tokens));
    uniq.forEach(token -> {
      CommonExercise exerciseBySearch = project.getExerciseByExactMatch(token);
      if (exerciseBySearch != null) {
        coreExercises.add(exerciseBySearch);
      }
    });
  }

  /**
   * Just page and presentation.
   *
   * @param page
   * @param topic
   * @return
   */
  @NotNull
  private List<ExerciseAttribute> getExerciseAttributes(String page, String topic) {
    List<ExerciseAttribute> attributes = new ArrayList<>(2);
    attributes.add(new ExerciseAttribute(DialogMetadata.PAGE.getLC(), page));
    attributes.add(new ExerciseAttribute(DialogMetadata.PRESENTATION.getLC(), topic));
    return attributes;
  }

  /**
   * @param attributes
   * @param speakers
   * @param path
   * @param fileText
   * @param unit
   * @param chapter
   * @param typeOrder
   * @return
   * @see #getDialogsByProp
   */
  @NotNull
  private Exercise getExercise(List<ExerciseAttribute> attributes,
                               Set<String> speakers,
                               Path path,
                               String fileText,
                               String unit, String chapter, List<String> typeOrder) {
    Exercise exercise = new Exercise();
    {
      Map<String, String> unitToValue = new HashMap<>();
      unitToValue.put(typeOrder.get(0), unit);
      unitToValue.put(typeOrder.get(1), chapter);
      exercise.setUnitToValue(unitToValue);
    }

    attributes.forEach(exercise::addAttribute);

    {
      int i1 = fileText.indexOf(":");
      String speaker = fileText.substring(0, i1).trim();
      speakers.add(speaker);
      //  logger.info("file name " + path.getFileName());
      {
        String[] split = path.getFileName().toString().split("\\.");
        String oldID = split[0];
        //  logger.info("ex " + oldID);
        exercise.getMutable().setOldID(oldID);
      }
      exercise.addAttribute(new ExerciseAttribute(DialogMetadata.SPEAKER.getCap(), speaker, false));
      // logger.info("speaker " + speaker);
      exercise.getMutable().setForeignLanguage(fileText.substring(i1 + 1).trim());
    }

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
