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
@SuppressWarnings("ALL")
public class InterpreterReader extends BaseDialogReader implements IDialogReader {
  private static final Logger logger = LogManager.getLogger(InterpreterReader.class);
  private static final String AUDIO_FILENAMES = "Audio Filenames";
  private static final String TEXT = "Text";
  private static final String ENG = "ENG";
  private static final String CHN = "CHN";

  public static final String DEFAULT_UNIT = "1";
  public static final String DEFAULT_CHAPTER = "1";

  private static final boolean DEBUG = true;
  private static final String DIALOGS = "Dialogs";
  private static final String INTERPRETER = "I";
  private static final String INTERPRETERSPEAKER = "Interpreter";
  public static final String KEY_WORDS_TO_OMIT = "Key Words to Omit";
  public static final String CORE_WORDS_PHRASES = "Core words / phrases";
  public static final String TITLE = "title";
  public static final String INTERPRETER_XLSX = "interpreter.xlsx";
  public static final String ENGLISH_SPEAKER = "English Speaker";
  public static final String A_TALKER = "A";
  //  private final boolean DEBUG = true;
  private final ExcelUtil excelUtil = new ExcelUtil();

  private String unit;
  private String chapter;

  enum Columns {
    DIALOG,
    TURN,
    TALKER,
    LANGUAGE,
    ENGTEXT,
    L2TEXT,
    TEXTTRANSLITERATION,
    L2KEYWORDS,
    KEYWORDSTRANSLITERATION,
    KEYWORDSTRANSLATION,
    TITLE,
    ORIENTATION
  }


  public InterpreterReader(String unit, String chapter) {
    this.unit = unit;
    this.chapter = chapter;
  }

  @Override
  public Map<Dialog, SlickDialog> getInterpreterDialogs(int defaultUser, Project project, Project englishProject, String excel) {
    File excelFile = getExcelFile(project, excel);

    if (excelFile.exists()) {
      return getDialogsFromExcel(defaultUser, project, englishProject, excelFile);
    } else {
      logger.warn("getInterpreterDialogs no interpreter spreadsheet for " + project.getName() + " at " + excelFile.getAbsolutePath());
      return Collections.emptyMap();
    }
  }

  /**
   * @param defaultUser
   * @param exToAudio      NOT USED!
   * @param project
   * @param englishProject
   * @return
   */
  @Override
  public Map<Dialog, SlickDialog> getDialogs(int defaultUser,
                                             Map<ClientExercise, String> exToAudio,
                                             Project project,
                                             Project englishProject) {
//    File excelFile = getExcelFile(project);
//
//    if (excelFile.exists()) {
//      logger.info("no interpreter spreadsheet for " + project.getName());
//      return Collections.emptyMap();
//    } else {
//      return getDialogsFromExcel(defaultUser, project, englishProject, excelFile);
//    }

    return Collections.emptyMap();
  }

  private Map<Dialog, SlickDialog> getDialogsFromExcel(int defaultUser,
                                                       Project project,
                                                       Project englishProject,
                                                       File excelFile) {
    if (defaultUser == -1) {
      logger.error("default user is not set?");
    }

    Map<Dialog, SlickDialog> dialogToSlick = new HashMap<>();
    try {
      FileInputStream inp = new FileInputStream(excelFile);
      XSSFWorkbook wb = new XSSFWorkbook(inp);

      int numberOfSheets = wb.getNumberOfSheets();
      for (int i = 0; i < numberOfSheets; i++) {
        Sheet sheet = wb.getSheetAt(i);
        if (DEBUG) {
          logger.info("getDialogs sheet " + sheet.getSheetName() + " had " + sheet.getPhysicalNumberOfRows() + " rows.");
        }

        if (sheet.getPhysicalNumberOfRows() > 0) {
          if (sheet.getSheetName().equalsIgnoreCase(DIALOGS) || numberOfSheets == 1) {
            dialogToSlick = readFromSheet(defaultUser, sheet, project, englishProject);
          }
        }
      }

      inp.close();
    } catch (IOException e) {
      String projectLanguage = project.getLanguage().toLowerCase();
      logger.error(projectLanguage + " : looking for " + excelFile.getAbsolutePath() + " got " + e, e);
    }

    return dialogToSlick;
  }

  @NotNull
  private File getExcelFile(Project project, String excelFile) {
    String dialogDataDir = getDialogDataDir(project);

    File loc = new File(dialogDataDir);
    boolean directory = loc.isDirectory();
    if (!directory) logger.warn("huh? not a dir");

    return new File(loc, excelFile);
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

      Map<Columns, Integer> colToIndex = new HashMap<>();

      String title = "";
      String orientation = "";

      boolean gotHeader = false;

      int lastDialogID = -1;
      String lastTitle = "";
      String lastOrientation = "";
      int realID = -1;

      String imageBaseDir = getImageBaseDir(project);
      for (; iter.hasNext(); ) {
        Row theRow = iter.next();
        rows++;

        if (!gotHeader) {
          columns = excelUtil.getHeader(theRow); // could be several junk rows at the top of the spreadsheet

          for (String col : columns) {
            String colNormalized = col.toUpperCase().replaceAll("\\s++", "");
            try {
              Columns columns1 = Columns.valueOf(colNormalized);
              colToIndex.put(columns1, columns.indexOf(col));
            } catch (IllegalArgumentException e) {
              logger.warn("\n\ncouldn't parse col header '" + col + "' = " + colNormalized);
            }
          }
          gotHeader = true;
        } else {
          boolean isDelete = excelUtil.isDeletedRow(sheet, theRow, 1);
          if (isDelete) {
            logger.warn("readFromSheet skipping deleted row " + rows);
          } else {
            Map<Columns, String> colToValue = new HashMap<>();
            colToIndex.forEach((k, v) -> colToValue.put(k, excelUtil.getCell(theRow, v)));
            String l2Text = colToValue.get(Columns.L2TEXT);
            if (l2Text.isEmpty()) continue;

            title = colToValue.get(Columns.TITLE).trim();
            orientation = colToValue.get(Columns.ORIENTATION).trim();

            if (!title.isEmpty()) {
              logger.info("row #" + rows + " title " + title);
            } else {
              logger.info("row EMPTY #" + rows + " title " + title);
            }

            if (DEBUG) {
              String s = colToValue.get(Columns.ENGTEXT);

              if (!s.isEmpty())
                logger.info("row #" + rows + " = " + s + " : " + l2Text);
            }

            Language colLang = getLanguage(colToValue.get(Columns.LANGUAGE).trim(), project);
            if (colLang == Language.UNKNOWN) {
              logger.info("SKIP row #" + rows);
              continue;
            }
            try {
              realID = Integer.parseInt(colToValue.get(Columns.DIALOG));

              if (lastDialogID != realID && lastDialogID != -1) {
                logger.info("readFromSheet adding dialog title " + lastTitle);

                addDialog(defaultUser, project, engProject, dialogToSlick, modified, typeOrder, exercises,
                    coreEng, coreFL, speakers,
                    lastDialogID,
                    imageBaseDir, lastTitle, lastOrientation);

                // start new set of speakers...
                speakers = new LinkedHashSet<>();
                exercises = new ArrayList<>();
                coreEng = new HashSet<>();
                coreFL = new HashSet<>();
              }

              lastDialogID = realID;

              if (!title.isEmpty()) {
                lastTitle = title;
                logger.info("\n\n\ncurrent title       '" + title + "'");
              }

              if (!orientation.isEmpty()) {
                lastOrientation = orientation;
                logger.info("\n\n\ncurrent orientation " + orientation);
              }

              try {
                String engText = colToValue.get(Columns.ENGTEXT);
                String turnID = colToValue.get(Columns.TURN);
                String talker = colToValue.get(Columns.TALKER);

                Language projectLang = project.getLanguageEnum();
                String nativeSpeaker = project.getLanguageEnum().toDisplay() + " Speaker";

                String interpreterTurn = turnID + "-I";
                if (colLang == Language.ENGLISH) {
                  if (speakers.isEmpty()) {
                    if (talker.equalsIgnoreCase(A_TALKER)) {
                      speakers.add(ENGLISH_SPEAKER);
                      speakers.add(INTERPRETERSPEAKER);
                      speakers.add(nativeSpeaker);
                    } else {
                      speakers.add(nativeSpeaker);
                      speakers.add(INTERPRETERSPEAKER);
                      speakers.add(ENGLISH_SPEAKER);
                    }
                  }

                  logger.info("readFromSheet " + colLang + " add " + realID + " : " + talker + " @ " + turnID + " " + colToValue.get(Columns.LANGUAGE) + " " + engText);

                  {
                    Exercise exercise = getExercise(typeOrder, engText, "", "", ENGLISH_SPEAKER, Language.ENGLISH, turnID);
                    //   logger.info("readFromSheet add " + turnID + " " + exercise.getFLToShow() + " " + exercise.getEnglish());
                    exercises.add(exercise);
                  }
                  {
                    Exercise exercise1 = getExercise(typeOrder, l2Text, engText, colToValue.get(Columns.TEXTTRANSLITERATION), INTERPRETERSPEAKER, projectLang, interpreterTurn);
                    //    logger.info("readFromSheet add " + interpreterTurn + " " + exercise1.getForeignLanguage() + " " + exercise1.getEnglish());
                    exercises.add(exercise1);
                  }
                  addCoreVocab(coreFL, colToValue, interpreterTurn, INTERPRETERSPEAKER);
                } else {
                  if (speakers.isEmpty()) {
                    if (talker.equalsIgnoreCase(A_TALKER)) {
                      speakers.add(nativeSpeaker);
                      speakers.add(INTERPRETERSPEAKER);
                      speakers.add(ENGLISH_SPEAKER);
                    } else {
                      speakers.add(ENGLISH_SPEAKER);
                      speakers.add(INTERPRETERSPEAKER);
                      speakers.add(nativeSpeaker);
                    }
                  }

                  logger.info("readFromSheet 2 add " + realID + " : " + talker + " @ " + turnID + " " + colToValue.get(Columns.LANGUAGE) + " " + engText);

                  exercises.add(getExercise(typeOrder, l2Text, engText, colToValue.get(Columns.TEXTTRANSLITERATION), nativeSpeaker, projectLang, turnID));
                  exercises.add(getExercise(typeOrder, engText, "", "", INTERPRETERSPEAKER, Language.ENGLISH, interpreterTurn));
                  addCoreVocab(coreFL, (Map<Columns, String>) colToValue, turnID, nativeSpeaker);
                }
              } catch (NumberFormatException e) {
                logger.warn("readFromSheet can't parse turn ");
              }
            } catch (NumberFormatException e) {
              logger.warn("readFromSheet can't parse dialog ");
            }
          }
        }
      }

      logger.info("title " + lastTitle + " : " + lastOrientation);
      addDialog(defaultUser, project, engProject, dialogToSlick, modified, typeOrder,
          exercises, coreEng, coreFL, speakers, lastDialogID, imageBaseDir,
          lastTitle, lastOrientation);

    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return dialogToSlick;
  }

  private void addCoreVocab(Set<CoreEntry> coreFL, Map<Columns, String> colToValue, String turnToUse, String speaker) {
    if (!colToValue.get(Columns.L2KEYWORDS).isEmpty()) {
      String[] split = colToValue.get(Columns.L2KEYWORDS).split("\\|");
      String s = colToValue.get(Columns.KEYWORDSTRANSLITERATION);
      String[] split2 = s == null ? new String[split.length] : s.split("\\|");
      String keyTrans = colToValue.get(Columns.KEYWORDSTRANSLATION);
      String[] split3 = keyTrans == null ? new String[split.length] : keyTrans.split("\\|");
      for (int i = 0; i < split.length; i++) {
        String s1 = split3[i];
        if (s1 == null) s1 = "";
        String s2 = split2[i];
        if (s2 == null) s2 = "";

        coreFL.add(new CoreEntry(split[i].trim(), s1.trim(), s2.trim(), speaker, turnToUse));
      }
    }
  }

  @NotNull
  private String getConvertedSpeakerID(Project project, String speakerID, Language lang) {
    if (speakerID.equalsIgnoreCase(A_TALKER) || speakerID.equalsIgnoreCase("B")) {
      speakerID = getSpeaker(lang, project.getLanguageEnum().toDisplay());
    } else if (speakerID.equalsIgnoreCase("I")) {
      speakerID = "Interpreter";
    }
    return speakerID;
  }

  @NotNull
  private String getSpeaker(Language lang, String s) {
    String speakerID;
    if (lang == Language.ENGLISH) {
      speakerID = ENGLISH_SPEAKER;
    } else {
      speakerID = s + " Speaker";
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
                         int dialogID,
                         String imageBaseDir, String title, String orientation) {
    final Set<ClientExercise> coreExercises = getCoreExercises(project, engProject, typeOrder, coreEng, coreFL);

    String imageRef = getImageRef(imageBaseDir, "dialog" + dialogID);

    logger.info("#" + dialogID +
        " image ref " + imageRef);

    addDialogPair(defaultUser, project, dialogToSlick,
        modified, exercises, coreExercises, speakers,
        getOrientationOrDefault(dialogID, orientation),
        getTitleOrDefault(dialogID, title), imageRef);
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
    boolean empty = orientation.isEmpty();
    if (empty) {
      logger.warn("using default orientation!\n\n\n");
    }
    return empty ? getOrientation(lastDialogID) : orientation;
  }

  @NotNull
  private String getTitleOrDefault(int lastDialogID, String title) {
    return title.isEmpty() ? getTitle(lastDialogID) : title;
  }

  private String getCellValue(int titleIndex, Row theRow) {
    return titleIndex > 0 ? excelUtil.getCell(theRow, titleIndex) : "";
  }

  /**
   * TODO : what to do about english?
   * <p>
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
    Language languageEnum = project.getLanguageEnum();
    coreFL.forEach(phrase -> {
      coreExercises.add(getExercise(typeOrder, phrase.getText(), phrase.getEnglish(), phrase.getTransliteration(), phrase.getSpeakerID(), languageEnum, phrase.getTurnID()));
    });
  }

  private static class CoreEntry {
    private final String text;
    private final String english;
    private final String transliteration;
    private final String speakerID;
    private final String turnID;

    CoreEntry(String text, String english, String transliteration, String speakerID, String turnID) {
      this.text = text;
      this.transliteration = transliteration;
      this.speakerID = speakerID;
      this.turnID = turnID;
      this.english = english;
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

    public String getEnglish() {
      return english;
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
  private Language getLanguage(String language, Project project) {
    Language lang = Language.UNKNOWN;
    if (language.equalsIgnoreCase(ENG)) lang = Language.ENGLISH;
    else if (language.equalsIgnoreCase(CHN)) lang = Language.MANDARIN;
    else if (language.equalsIgnoreCase("FRE")) lang = Language.FRENCH;
    else if (language.equalsIgnoreCase("RUS")) lang = Language.RUSSIAN;
    else if (language.equalsIgnoreCase("L2")) lang = project.getLanguageEnum();
    else logger.error("unknown language '" + language + "'");
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

  /**
   * @param defaultUser
   * @param project
   * @param dialogToSlick
   * @param modified
   * @param exercises
   * @param coreExercises
   * @param speakers
   * @param orientation
   * @param title
   * @param imageRef
   * @see #addDialog(int, Project, Project, Map, Timestamp, List, List, Set, Set, Set, String, String, String, String)
   */
  private void addDialogPair(int defaultUser, Project project, Map<Dialog, SlickDialog> dialogToSlick,
                             Timestamp modified,
                             List<ClientExercise> exercises,
                             Set<ClientExercise> coreExercises,
                             Set<String> speakers,
                             String orientation,
                             String title,
                             String imageRef) {
    logger.info(" title " + title +
        " new dialog " + orientation + " with " + exercises.size() + " exercises");
    List<ExerciseAttribute> attributes = new ArrayList<>();
    addSpeakerAttrbutes(attributes, speakers);

    addDialogPair(defaultUser,
        project.getID(),
        modified,
        imageRef,
        unit, chapter,
        attributes,
        exercises,
        coreExercises,
        orientation,
        orientation.startsWith("Interpreter") ? orientation : title,
        title,
        dialogToSlick,
        DialogType.INTERPRETER,
        project.getProject().countrycode());
  }

  /**
   * @param typeOrder
   * @param text
   * @param speakerID
   * @param lang
   * @param speakers  - modified maybe!
   * @return
   */
  private Exercise getExercise(List<String> typeOrder,

                               String text,
                               String english,
                               String transliteration,

                               String speakerID, Language lang,
                               String turnID) {
    Exercise exercise = new Exercise();

    MutableExercise mutable = exercise.getMutable();
    if (!turnID.isEmpty()) {
      mutable.setOldID(turnID);
    }

    exercise.setUnitToValue(getDefaultUnitAndChapter(typeOrder));

    addAttributes(speakerID, lang, exercise);

    mutable.setForeignLanguage(text);
    mutable.setEnglish(english);
    mutable.setTransliteration(transliteration);

    return exercise;
  }

  private void addAttributes(String speakerID, Language lang, Exercise exercise) {
    if (!speakerID.isEmpty()) {
      exercise.addAttribute(new ExerciseAttribute(DialogMetadata.SPEAKER.getCap(), speakerID, false));
    }
    exercise.addAttribute(new ExerciseAttribute(DialogMetadata.LANGUAGE.getCap(), lang.name(), false));
  }

  @NotNull
  private Map<String, String> getDefaultUnitAndChapter(List<String> typeOrder) {
    Map<String, String> unitToValue = new HashMap<>();
    unitToValue.put(typeOrder.get(0), unit);
    unitToValue.put(typeOrder.get(1), chapter);
    return unitToValue;
  }
}
