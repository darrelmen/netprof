package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.ExcelUtil;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.exercise.MutableExercise;
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
  private static final String INTERPRETER1 = "Interpreter";
  public static final String KEY_WORDS_TO_OMIT = "Key Words to Omit";
  public static final String CORE_WORDS_PHRASES = "Core words / phrases";
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

  private final Set<Character> SPLIT_CHARS = new HashSet<>(Arrays.asList('。', '.', '?', '？'));

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

      int titleIndex = -1;
      int orientationIndex = -1;
      int pinyinIndex = -1;
      int corePinyinIndex = -1;

      String title = "";
      String orientation = "";

      boolean gotHeader = false;

      int lastDialogID = -1;
      String lastRawDialogID = "";
      String lastTitle = "";
      String lastOrientation = "";
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
            } else if (colNormalized.equalsIgnoreCase(KEY_WORDS_TO_OMIT)) {
              coreWordsIndex = i;
            } else if (colNormalized.equalsIgnoreCase(CORE_WORDS_PHRASES)) {
              coreWordsIndex = i;
            } else if (colNormalized.equalsIgnoreCase("title")) {
              titleIndex = i;
            } else if (colNormalized.equalsIgnoreCase("orientation")) {
              orientationIndex = i;
            } else if (colNormalized.equalsIgnoreCase("pinyin")) {
              pinyinIndex = i;
            } else if (colNormalized.equalsIgnoreCase("Core words in Pinyin")) {
              corePinyinIndex = i;
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

            title = getCellValue(titleIndex, theRow);
            orientation = getCellValue(orientationIndex, theRow);
            String pinyin = getCellValue(pinyinIndex, theRow);
            String corePinyin = getCellValue(corePinyinIndex, theRow);

            if (!title.isEmpty()) lastTitle = title;
            if (!orientation.isEmpty()) lastOrientation = orientation;
            if (DEBUG) {
              if (!text.isEmpty())
                logger.info("row #" + rows + " : " + audioFilenames + " = " + text);
            }

            List<String> parts = new ArrayList<>(Arrays.asList(audioFilenames.split("-")));
            int i = 0;

            int size = parts.size();
            if (size < 4) {
              if (!audioFilenames.isEmpty()) {
                logger.warn("readFromSheet Skip first col with " + audioFilenames);
              }
              continue;
            }

            String dialogID = parts.get(i++);
            String turnID = parts.get(i++);
            String speakerID = parts.get(i++);

            String triple = dialogID + turnID + speakerID;

            String language = parts.get(i++);
            Language lang = getLanguage(language);

            speakerID = getConvertedSpeakerID(project, speakerID, lang);

            String gender = size == 4 ? "" : parts.get(i++);

            String id = dialogID.substring(1);
            try {
              realID = Integer.parseInt(id);

              if (lastDialogID != realID && lastDialogID != -1) {
                //title = getTitleOrDefault(lastDialogID, title);
                logger.info("readFromSheet adding dialog " + title);
                addDialog(defaultUser, project, engProject, dialogToSlick, modified, typeOrder, exercises,
                    coreEng, coreFL, speakers,
                    lastRawDialogID,
                    imageBaseDir, title, orientation);

                // start new set of speakers...
                speakers = new LinkedHashSet<>();
                exercises = new ArrayList<>();
                coreEng = new HashSet<>();
                coreFL = new HashSet<>();
              }

              lastDialogID = realID;
              lastRawDialogID = dialogID;

              try {
                boolean genderMatch = /*gender.equalsIgnoreCase(M_2_M) ||*/ gender.isEmpty();
                boolean newTurn = !triple.equalsIgnoreCase(prevtriple);
                if (!text.isEmpty()) {
                  if (genderMatch || newTurn) {
                    List<String> sentences = getSentences(text, speakerID);
                    for (String phrase : sentences) {
                      logger.info("readFromSheet add " + realID + " : " + speakerID + " @ " + turnID + " " + language + " " + text);
                      String trans = sentences.size() == 1 ? pinyin : "";
                      exercises.add(getExercise(typeOrder, phrase, trans, speakerID, lang, turnID, speakers));
                    }

                    if (!coreWords.isEmpty()) {
                      Set<CoreEntry> core = lang == Language.ENGLISH ? coreEng : coreFL;
                      core.add(new CoreEntry(coreWords, corePinyin, speakerID, turnID));
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

      addDialog(defaultUser, project, engProject, dialogToSlick, modified, typeOrder, exercises, coreEng, coreFL, speakers, lastRawDialogID, imageBaseDir, lastTitle, lastOrientation);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return dialogToSlick;
  }

  @NotNull
  private String getConvertedSpeakerID(Project project, String speakerID, Language lang) {
    if (speakerID.equalsIgnoreCase("A")) {
      if (lang == Language.ENGLISH) {
        speakerID = "English Speaker";
      }
      else {
        speakerID = project.getLanguageEnum().toDisplay() + " Speaker";
      }
    }
    else if (speakerID.equalsIgnoreCase("B")) {
      if (lang == Language.ENGLISH) {
        speakerID = "English Speaker";
      }
      else {
        speakerID = project.getLanguageEnum().toDisplay() + " Speaker";
      }
    }
    else if (speakerID.equalsIgnoreCase("I")) {
      speakerID = "Interpreter";
    }
    return speakerID;
  }

  private void addDialog(int defaultUser, Project project, Project engProject,
                         Map<Dialog, SlickDialog> dialogToSlick,
                         Timestamp modified,
                         List<String> typeOrder,
                         List<ClientExercise> exercises,
                         Set<CoreEntry> coreEng, Set<CoreEntry> coreFL,
                         Set<String> speakers,
                         String lastRawDialogID,
                         String imageBaseDir, String title, String orientation) {
    final Set<ClientExercise> coreExercises = getCoreExercises(project, engProject, typeOrder, coreEng, coreFL);

    int lastDialogID = getLastDialogID(lastRawDialogID);

    String imageRef = getImageRef(imageBaseDir, "" + lastDialogID);

    if (project.getLanguageEnum() == Language.RUSSIAN) {
      imageRef = imageBaseDir + lastRawDialogID + ".png";
    }
    addDialogPair(defaultUser, project, dialogToSlick,
        modified, exercises, coreExercises, speakers,
        getOrientationOrDefault(lastDialogID, orientation),
        getTitleOrDefault(lastDialogID, title), imageRef);
  }

  private int getLastDialogID(String lastRawDialogID) {
    int lastDialogID = -1;
    String id = lastRawDialogID.substring(1);
    try {
      lastDialogID = Integer.parseInt(id);
    } catch (Exception e) {
    }
    return lastDialogID;
  }

  @NotNull
  private String getOrientationOrDefault(int lastDialogID, String orientation) {
    return orientation.isEmpty() ? getOrientation(lastDialogID) : orientation;
  }

  @NotNull
  private String getTitleOrDefault(int lastDialogID, String title) {
    return title.isEmpty() ? getTitle(lastDialogID) : title;
  }

  private String getCellValue(int titleIndex, Row theRow) {
    return titleIndex > 0 ? excelUtil.getCell(theRow, titleIndex) : "";
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
//    List<CoreEntry> toAdd = addCoreWords(project, coreExercises, coreFL);
    Language languageEnum = project.getLanguageEnum();
    coreFL.forEach(phrase -> coreExercises.add(getExercise(typeOrder, phrase.getText(), phrase.getTransliteration(), phrase.getSpeakerID(), languageEnum, phrase.getTurnID())));
  }

  static class CoreEntry {
    private final String text;
    private final String transliteration;
    private final String speakerID;
    private final String turnID;

    CoreEntry(String text, String transliteration, String speakerID, String turnID) {
      this.text = text;
      this.transliteration = transliteration;
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

    public String getTransliteration() {
      return transliteration;
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
//  private List<CoreEntry> addCoreWords(Project project, Collection<ClientExercise> coreExercises, Collection<CoreEntry> tokens) {
//    List<CoreEntry> toAdd = new ArrayList<>();
//    tokens.forEach(token -> {
//      CommonExercise exerciseBySearch = project.getExerciseByExactMatch(token.getText());
//      if (exerciseBySearch == null) {
//        toAdd.add(token);
//      } else {
//        coreExercises.add(exerciseBySearch);
//      }
//    });
//    return toAdd;
//  }

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
      if (SPLIT_CHARS.contains(character)) {
        String trim = s.trim();
        trim = trim.replaceAll(" ", "");
        if (!trim.isEmpty()) {
          sentences.add(trim);
        }
        s = "";
      }
    }
    String trim = s.trim();
    trim = trim.replaceAll(" ", "");
    if (!trim.isEmpty()) sentences.add(trim);

    if (DEBUG) {
      if (sentences.size() > 1) {
        logger.info("getSentences " + sentences.size() +
            "\n\tfrom " + text);
        sentences.forEach(logger::info);
      }
    }
    return sentences;
  }

  /**
   * @param realID
   * @return
   * @see #readFromSheet
   */
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
                             String orientation,
                             String title,
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
        orientation.startsWith("Interpreter") ? orientation : title,
        title,
        dialogToSlick,
        DialogType.INTERPRETER);
  }

  private Exercise getExercise(List<String> typeOrder, String text, String transliteration, String speakerID, Language lang, String turnID) {
    return getExercise(typeOrder, text, transliteration, speakerID, lang, turnID, new HashSet<>());
  }

  /**
   * @param typeOrder
   * @param text
   * @param speakerID
   * @param lang
   * @param speakers  - modified maybe!
   * @return
   */
  private Exercise getExercise(List<String> typeOrder, String text, String transliteration, String speakerID, Language lang,
                               String turnID,

                               Set<String> speakers) {
    Exercise exercise = new Exercise();

    MutableExercise mutable = exercise.getMutable();
    if (!turnID.isEmpty()) {
      mutable.setOldID(turnID);
    }

    exercise.setUnitToValue(getDefaultUnitAndChapter(typeOrder));

    if (!speakerID.isEmpty()) {
      speakers.add(speakerID);
      exercise.addAttribute(new ExerciseAttribute(DialogMetadata.SPEAKER.getCap(), speakerID, false));
    }
    exercise.addAttribute(new ExerciseAttribute(DialogMetadata.LANGUAGE.getCap(), lang.name(), false));

    mutable.setForeignLanguage(text);
    mutable.setTransliteration(transliteration);

    return exercise;
  }

  @NotNull
  private Map<String, String> getDefaultUnitAndChapter(List<String> typeOrder) {
    Map<String, String> unitToValue = new HashMap<>();
    unitToValue.put(typeOrder.get(0), DEFAULT_UNIT);
    unitToValue.put(typeOrder.get(1), INTERPRETER1);
    return unitToValue;
  }
}
