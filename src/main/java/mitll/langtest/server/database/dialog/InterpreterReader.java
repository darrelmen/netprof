package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.ExcelUtil;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.project.Language;
import mitll.npdata.dao.SlickDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Read the excel spreadsheet that we're using (for now) to define the interpreter turns.
 */
public class InterpreterReader extends BaseDialogReader implements IDialogReader {
  private static final Logger logger = LogManager.getLogger(InterpreterReader.class);
  private static final String AUDIO_FILENAMES = "Audio Filenames";
  private static final String TEXT = "Text";
  private static final String ENG = "ENG";
  private static final String CHN = "CHN";
  private static final String M_2_M = "M2M";
  private static final String DEFAULT_UNIT = "1";
  private static final String DEFAULT_CHAPTER = "1";
  private static final boolean DEBUG = true;
  private static final String DIALOGS = "Dialogs";
  private static final String INTERPRETER = "I";
  //  private final boolean DEBUG = true;
  private final ExcelUtil excelUtil = new ExcelUtil();

  /**
   * @param defaultUser
   * @param exToAudio
   * @param project
   * @param englishProject
   * @return
   */
  @Override
  public Map<Dialog, SlickDialog> getDialogs(int defaultUser,
                                             Map<ClientExercise, String> exToAudio,
                                             Project project, Project englishProject) {
    Map<Dialog, SlickDialog> dialogToSlick = new HashMap<>();
    String dialogDataDir = getDialogDataDir(project);
    String projectLanguage = project.getLanguage().toLowerCase();

    File loc = new File(dialogDataDir);
    boolean directory = loc.isDirectory();
    if (!directory) logger.warn("huh? not a dir");

    File excelFile = new File(loc, "interpreter.xlsx");

    try {
      FileInputStream inp = new FileInputStream(excelFile);

      XSSFWorkbook wb = new XSSFWorkbook(inp);


      for (int i = 0; i < wb.getNumberOfSheets(); i++) {
        Sheet sheet = wb.getSheetAt(i);
        int physicalNumberOfRows = sheet.getPhysicalNumberOfRows();

        if (DEBUG)
          logger.info("getDialogs sheet " + sheet.getSheetName() + " had " + physicalNumberOfRows + " rows.");

        if (physicalNumberOfRows > 0) {
          if (sheet.getSheetName().equalsIgnoreCase(DIALOGS)) {
            dialogToSlick = readFromSheet(defaultUser, sheet, project, englishProject);
          }
        }
      }

      inp.close();
    } catch (IOException e) {
      logger.error(projectLanguage + " : looking for " + excelFile.getAbsolutePath() + " got " + e, e);
    }

    return dialogToSlick;
  }

  private final Set<Character> splitChar = new HashSet<>(Arrays.asList('。', '.', '?', '？'));

  /**
   * @param defaultUser
   * @param sheet
   * @param project
   * @param engProject  so we can add english core words and phrases
   * @return
   */
  private Map<Dialog, SlickDialog> readFromSheet(int defaultUser,
                                                 Sheet sheet,
                                                 Project project,
                                                 Project engProject) {
    Map<Dialog, SlickDialog> dialogToSlick = new LinkedHashMap<>();

    try {
      Iterator<Row> iter = sheet.rowIterator();
      Timestamp modified = new Timestamp(System.currentTimeMillis());

      List<String> columns;

      List<String> typeOrder = project.getTypeOrder();
      List<ClientExercise> exercises = new ArrayList<>();
      Set<CoreEntry> coreEng = new HashSet<>();
      Set<CoreEntry> coreFL = new HashSet<>();

      Set<String> speakers = new LinkedHashSet<>();

      int rows = 0;

      int audioFileIndex = -1;
      int textIndex = -1;
      int coreWordsIndex = -1;
      boolean gotHeader = false;

      int lastDialogID = -1;
      int realID = -1;
      String prevtriple = "";

      String imageBaseDir = getImageBaseDir(project);
      for (; iter.hasNext(); ) {
        Row theRow = iter.next();
        rows++;

        if (!gotHeader) {
          columns = excelUtil.getHeader(theRow); // could be several junk rows at the top of the spreadsheet

          for (String col : columns) {
            String colNormalized = col.toLowerCase();
            int i = columns.indexOf(col);

            if (colNormalized.equalsIgnoreCase(AUDIO_FILENAMES)) {
              audioFileIndex = i;
            } else if (colNormalized.equalsIgnoreCase(TEXT)) {
              textIndex = i;
            } else if (colNormalized.equalsIgnoreCase("Key Words to Omit")) {
              coreWordsIndex = i;
            } else if (colNormalized.equalsIgnoreCase("Core words / phrases")) {
              coreWordsIndex = i;
            }
          }
          gotHeader = true;
        } else {
          boolean isDelete = excelUtil.isDeletedRow(sheet, theRow, 1);
          if (isDelete) {
            logger.warn("readFromSheet skipping deleted row " + rows);
          } else {
            String audioFilenames = excelUtil.getCell(theRow, audioFileIndex);
            String text = excelUtil.getCell(theRow, textIndex);
            String coreWords = excelUtil.getCell(theRow, coreWordsIndex);
            if (DEBUG) {
              if (!text.isEmpty())
                logger.info("row #" + rows + " : " + audioFilenames + " = " + text);
            }

            List<String> parts = new ArrayList<>(Arrays.asList(audioFilenames.split("-")));
            int i = 0;

            if (parts.size() < 4) {
              logger.warn("readFromSheet Skip first col with " + audioFilenames);
              continue;
            }

            String dialogID = parts.get(i++);
            String turnID = parts.get(i++);
            String speakerID = parts.get(i++);

            String triple = dialogID + turnID + speakerID;

            String language = parts.get(i++);
            String gender = parts.get(i++);

            String id = dialogID.substring(1);
            try {
              realID = Integer.parseInt(id);

              if (lastDialogID != realID && lastDialogID != -1) {
                String title = getTitle(lastDialogID);
                logger.info("readFromSheet adding dialog " + title);
                final Set<ClientExercise> coreExercises = getCoreExercises(project, engProject, typeOrder, coreEng, coreFL);

                addDialogPair(defaultUser, project, dialogToSlick,
                    modified, exercises, coreExercises, speakers, getOrientation(lastDialogID), title, getImageRef(imageBaseDir, "" + lastDialogID));

                // start new set of speakers...
                speakers = new LinkedHashSet<>();
                exercises = new ArrayList<>();
                coreEng = new HashSet<>();
                coreFL = new HashSet<>();
              }

              lastDialogID = realID;

              try {
                Language lang = getLanguage(language);
                //  addCoreWords(coreEng, coreFL, coreWords, lang);

                boolean genderMatch = gender.equalsIgnoreCase(M_2_M) || gender.isEmpty();
                boolean newTurn = !triple.equalsIgnoreCase(prevtriple);
                if (!text.isEmpty()) {
                  if (genderMatch || newTurn) {
                    for (String s : getSentences(text, speakerID)) {
                      logger.info("add " + realID + " : " + speakerID + " @ " + turnID + " " + language + " " + text);
                      exercises.add(getExercise(typeOrder, s, speakerID, lang, turnID, speakers));
                    }

                    if (!coreWords.isEmpty()) {
                      if (lang == Language.ENGLISH) {
                        coreEng.add(new CoreEntry(coreWords, speakerID, turnID));
                      } else {
                        coreFL.add(new CoreEntry(coreWords, speakerID, turnID));
                      }
                    }
                    prevtriple = triple;
                    logger.info("readFromSheet # exercises (" + triple + ") for " + id + " is " + exercises.size());
                  } else {
                    logger.warn("readFromSheet skipping " + gender);
                  }
                }

              } catch (NumberFormatException e) {
                logger.warn("readFromSheet can't parse turn " + turnID);
              }
            } catch (NumberFormatException e) {
              logger.warn("readFromSheet can't parse dialog " + dialogID);
            }
          }
        }
      }


      String title = getTitle(realID);
      logger.info("finally, adding dialog " + title);
      final Set<ClientExercise> coreExercises = getCoreExercises(project, engProject, typeOrder, coreEng, coreFL);
      addDialogPair(defaultUser, project, dialogToSlick, modified, exercises, coreExercises, speakers,
          getOrientation(realID), title, getImageRef(imageBaseDir, "" + realID));

    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return dialogToSlick;
  }

//  private void addCoreWords(Set<String> coreEng, Set<String> coreFL, String coreWords, Language lang) {
//    if (!coreWords.trim().isEmpty()) {
//      if (lang == Language.ENGLISH) {
//        coreEng.add(coreWords);
//      } else {
//        coreFL.add(coreWords);
//      }
//    }
//  }

  /**
   * TODO : what to do about english?
   *
   * TODO : what to do about fl?
   *
   * @param project
   * @param typeOrder
   * @param coreEng
   * @param coreFL
   * @return
   */
  @NotNull
  private Set<ClientExercise> getCoreExercises(Project project, Project engProject, List<String> typeOrder,
                                               Set<CoreEntry> coreEng, Set<CoreEntry> coreFL) {
    final Set<ClientExercise> coreExercises = new HashSet<>();

    addCoreExercises(project, typeOrder, coreFL, coreExercises);
    addCoreExercises(engProject, typeOrder, coreEng, coreExercises);

    return coreExercises;
  }

  private void addCoreExercises(Project project, List<String> typeOrder, Set<CoreEntry> coreFL, Set<ClientExercise> coreExercises) {
    List<CoreEntry> toAdd = addCoreWords(project, coreExercises, coreFL);
    Language languageEnum = project.getLanguageEnum();
    toAdd.forEach(phrase -> coreExercises.add(getExercise(typeOrder, phrase.getText(), phrase.getSpeakerID(), languageEnum, phrase.getTurnID())));
  }

  static class CoreEntry {
    private final String text;
    private final String speakerID;
    private final String turnID;

    CoreEntry(String text, String speakerID, String turnID) {
      this.text = text;
      this.speakerID = speakerID;
      this.turnID = turnID;
    }

    String getText() {
      return text;
    }

    String getSpeakerID() {
      return speakerID;
    }

    String getTurnID() {
      return turnID;
    }
  }

  private List<String> getSentences(String text, String speakerID) {
    List<String> sentences = Collections.singletonList(text);
    if (speakerID.equals(INTERPRETER)) {
      sentences = getSentences(text);
    }
    return sentences;
  }

  @NotNull
  private Language getLanguage(String language) {
    Language lang = Language.UNKNOWN;
    if (language.equalsIgnoreCase(ENG)) lang = Language.ENGLISH;
    else if (language.equalsIgnoreCase(CHN)) lang = Language.MANDARIN;
    else if (language.equalsIgnoreCase("FRE")) lang = Language.FRENCH;
    else if (language.equalsIgnoreCase("RUS")) lang = Language.RUSSIAN;
    else logger.warn("unknown language " + language);
    return lang;
  }

  /**
   * @param project
   * @param coreExercises set - only unique exercises...
   * @param tokens
   */
  private List<CoreEntry> addCoreWords(Project project, Collection<ClientExercise> coreExercises, Collection<CoreEntry> tokens) {
    List<CoreEntry> toAdd = new ArrayList<>();
    tokens.forEach(token -> {
      CommonExercise exerciseBySearch = project.getExerciseByExactMatch(token.getText());
      if (exerciseBySearch == null) {
        toAdd.add(token);
      } else {
        coreExercises.add(exerciseBySearch);
      }
    });
    return toAdd;
  }

  /**
   * Split them.
   *
   * @param text
   * @return
   */
  private List<String> getSentences(String text) {
    List<String> sentences = new ArrayList<>();
    String s = "";
    for (int j = 0; j < text.length(); j++) {
      Character character = text.charAt(j);
      s += character;
      if (splitChar.contains(character)) {
        sentences.add(s.trim());
        s = "";
      }
    }
    if (!s.isEmpty()) sentences.add(s.trim());

    if (DEBUG) {
      if (sentences.size() > 1) {
        logger.info("getSentences " + sentences.size() +
            "\n\tfrom " + text);
        sentences.forEach(logger::info);
      }
    }
    return sentences;
  }

  @NotNull
  private String getOrientation(int realID) {
    switch (realID) {
      case 1:
        return "Can I borrow your cellphone?";
      case 2:
        return "How do you use Wechat?";
      case 3:
        return "How do you become a US Citizen?";
      case 4:
        return "Traffic Stop";
      case 5:
        return "Acupuncture";
      case 6:
        return "Book train tickets";
      default:
        return getTitle(realID);
    }
  }

  @NotNull
  private String getTitle(int realID) {
    return "Interpreter Dialog #" + realID;
  }

  private void addDialogPair(int defaultUser, Project project, Map<Dialog, SlickDialog> dialogToSlick,
                             Timestamp modified,
                             List<ClientExercise> exercises,
                             Set<ClientExercise> coreExercises,
                             Set<String> speakers,
                             String orientation, String title,
                             String imageRef) {
    logger.info("new dialog " + orientation + " with " + exercises.size() + " exercises");
    List<ExerciseAttribute> attributes = new ArrayList<>();
    addSpeakerAttrbutes(attributes, speakers);


    addDialogPair(defaultUser,
        project.getID(),
        modified,
        imageRef,
        DEFAULT_UNIT, DEFAULT_CHAPTER,
        attributes,
        exercises,
        coreExercises,
        orientation,
        orientation,
        title,
        dialogToSlick,
        DialogType.INTERPRETER);
  }

  private Exercise getExercise(List<String> typeOrder, String text, String speakerID, Language lang, String turnID) {
    return getExercise(typeOrder, text, speakerID, lang, turnID, new HashSet<>());
  }

  /**
   * @param typeOrder
   * @param text
   * @param speakerID
   * @param lang
   * @param speakers  - modified maybe!
   * @return
   */
  private Exercise getExercise(List<String> typeOrder, String text, String speakerID, Language lang,
                               String turnID,

                               Set<String> speakers) {
    Exercise exercise = new Exercise();

    if (!turnID.isEmpty()) {
      exercise.getMutable().setOldID(turnID);
    }

    {
      Map<String, String> unitToValue = new HashMap<>();
      unitToValue.put(typeOrder.get(0), DEFAULT_UNIT);
      unitToValue.put(typeOrder.get(1), DEFAULT_CHAPTER);
      exercise.setUnitToValue(unitToValue);
    }
    if (!speakerID.isEmpty()) {
      speakers.add(speakerID);
      exercise.addAttribute(new ExerciseAttribute(DialogMetadata.SPEAKER.getCap(), speakerID, false));
    }
    exercise.addAttribute(new ExerciseAttribute(DialogMetadata.LANGUAGE.getCap(), lang.name(), false));
    exercise.getMutable().setForeignLanguage(text);

    return exercise;
  }


}
