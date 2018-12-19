package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.ExcelUtil;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.exercise.ClientExercise;
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
  private static final boolean DEBUG = false;
  //  private final boolean DEBUG = true;
  private ExcelUtil excelUtil = new ExcelUtil();

  /**
   * @param defaultUser
   * @param exToAudio
   * @param project
   * @return
   */
  @Override
  public Map<Dialog, SlickDialog> getDialogs(int defaultUser,
                                             Map<ClientExercise, String> exToAudio,
                                             Project project) {
    Map<Dialog, SlickDialog> dialogToSlick = new HashMap<>();
    String dialogDataDir = getDialogDataDir(project);
    String projectLanguage = project.getLanguage().toLowerCase();

    String dirPath = dialogDataDir;
    File loc = new File(dirPath);
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
          dialogToSlick = readFromSheet(defaultUser, sheet, project);
        }
      }

      inp.close();
    } catch (IOException e) {
      logger.error(projectLanguage + " : looking for " + excelFile.getAbsolutePath() + " got " + e, e);
    }

    return dialogToSlick;
  }

  private Set<Character> splitChar = new HashSet<>(Arrays.asList('。', '.', '?', '？'));

  private Map<Dialog, SlickDialog> readFromSheet(int defaultUser,
                                                 Sheet sheet,
                                                 Project project) {
    Map<Dialog, SlickDialog> dialogToSlick = new LinkedHashMap<>();

    try {
      Iterator<Row> iter = sheet.rowIterator();
      // Map<Integer, CellRangeAddress> rowToRange = excelUtil.getRowToRange(sheet);
      Timestamp modified = new Timestamp(System.currentTimeMillis());

      List<String> columns;

      List<String> typeOrder = project.getTypeOrder();
      List<ClientExercise> exercises = new ArrayList<>();

      Set<String> speakers = new LinkedHashSet<>();

      int rows = 0;

      int audioFileIndex = -1;
      int textIndex = -1;
      boolean gotHeader = false;

      int lastDialogID = -1;
      int realID = -1;

      String imageBaseDir = getImageBaseDir(project);
      for (; iter.hasNext(); ) {
        Row next = iter.next();
        rows++;

        //  boolean inMergedRow = rowToRange.keySet().contains(next.getRowNum());

        if (!gotHeader) {
          columns = excelUtil.getHeader(next); // could be several junk rows at the top of the spreadsheet

          for (String col : columns) {
            String colNormalized = col.toLowerCase();
            int i = columns.indexOf(col);

            if (colNormalized.equalsIgnoreCase(AUDIO_FILENAMES)) {
              audioFileIndex = i;
            } else if (colNormalized.equalsIgnoreCase(TEXT)) {
              textIndex = i;
            }
          }
          gotHeader = true;
        } else {
          boolean isDelete = excelUtil.isDeletedRow(sheet, next, 1);
          if (isDelete) {
            logger.warn("skipping deleted row " + rows);
          } else {
            String audioFilenames = excelUtil.getCell(next, audioFileIndex);
            String text = excelUtil.getCell(next, textIndex);

            if (DEBUG) logger.info("row #" + rows + " : " + audioFilenames + " = " + text);

            List<String> parts = new ArrayList<>(Arrays.asList(audioFilenames.split("-")));
            int i = 0;
            String dialogID = parts.get(i++);
            String turnID = parts.get(i++);
            String speakerID = parts.get(i++);
            String language = parts.get(i++);
            String gender = parts.get(i++);

            String id = dialogID.substring(1);
            try {
              realID = Integer.parseInt(id);

              if (lastDialogID != realID && lastDialogID != -1) {
                String title = getTitle(lastDialogID);
                logger.info("adding dialog " + title);
                addDialogPair(defaultUser, project, dialogToSlick, modified, exercises, speakers, getOrientation(lastDialogID), title, getImageRef(imageBaseDir, ""+lastDialogID));

                // start new set of speakers...
                speakers = new LinkedHashSet<>();
                exercises = new ArrayList<>();
              }

              lastDialogID = realID;

              try {
                //int realTurnID = Integer.parseInt(turnID.substring(1));
                Language lang = Language.UNKNOWN;
                if (language.equalsIgnoreCase(ENG)) lang = Language.ENGLISH;
                else if (language.equalsIgnoreCase(CHN)) lang = Language.MANDARIN;
                else logger.warn("unknown language " + language);

                if (gender.equalsIgnoreCase(M_2_M)) {
                  List<String> sentences = Collections.singletonList(text);
                  if (speakerID.equals("I")) {
                    sentences = getSentences(text);
                  }

                  for (String s : sentences) {
                    exercises.add(getExercise(typeOrder, s, speakerID, lang, turnID, speakers));
                  }

                  logger.info("# exercises for " + id + " is " + exercises.size());
                }

              } catch (NumberFormatException e) {
                logger.warn("can't parse turn " + turnID);
              }
            } catch (NumberFormatException e) {
              logger.warn("can't parse dialog " + dialogID);
            }
          }
        }
      }


      String title = getTitle(realID);
      logger.info("finally, adding dialog " + title);
      addDialogPair(defaultUser, project, dialogToSlick, modified, exercises, speakers, getOrientation(realID), title, getImageRef(imageBaseDir, "" + realID));

    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return dialogToSlick;
  }

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
        Collections.emptySet(),
        title,
        orientation,
        orientation,
        dialogToSlick,
        DialogType.INTERPRETER);
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
    exercise.getMutable().setOldID(turnID);

    {
      Map<String, String> unitToValue = new HashMap<>();
      unitToValue.put(typeOrder.get(0), DEFAULT_UNIT);
      unitToValue.put(typeOrder.get(1), DEFAULT_CHAPTER);
      exercise.setUnitToValue(unitToValue);
    }
    speakers.add(speakerID);
    exercise.addAttribute(new ExerciseAttribute(DialogMetadata.SPEAKER.getCap(), speakerID, false));
    exercise.addAttribute(new ExerciseAttribute(DialogMetadata.LANGUAGE.getCap(), lang.name(), false));
    exercise.getMutable().setForeignLanguage(text);

    return exercise;
  }


}
