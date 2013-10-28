package mitll.langtest.server.database;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.Exercise;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads an excel spreadsheet from DLI.
 * <p/>
 * User: GO22670
 * Date: 2/6/13
 * Time: 8:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExcelImport implements ExerciseDAO {
  private static Logger logger = Logger.getLogger(ExcelImport.class);

  private static final boolean TESTING = false;
  private static final boolean SHOW_SKIPS = false;
  private static final int MIN_TABOO_ITEMS = 1;

  private final boolean isFlashcard;
  private boolean tabooEnglish;

  private List<Exercise> exercises = null;
  private Map<String, Exercise> idToExercise = new HashMap<String, Exercise>();
  private List<String> errors = new ArrayList<String>();
  private final String file;
  private SectionHelper sectionHelper = new SectionHelper();
  private boolean debug = false;
  private String mediaDir;
  private Set<String> missingSlowSet = new HashSet<String>();
  private Set<String> missingFastSet = new HashSet<String>();
  private StimulusInfo stimulusInfo = null;
  private boolean shouldHaveRefAudio = false;
  private boolean usePredefinedTypeOrder;
  private final String language;
  private boolean skipSemicolons;
  private File exampleSentenceFile;
  int audioOffset = 0;

  /**
   * @see mitll.langtest.server.SiteDeployer#readExercisesPopulateSite(mitll.langtest.shared.Site, String, java.io.InputStream)
   */
  public ExcelImport() {
    this.file = null;
    this.isFlashcard = false;
    this.language = "";
    exampleSentenceFile = null;
  }

  /**
   * @param file
   * @param relativeConfigDir
   * @see DatabaseImpl#makeDAO
   */
  public ExcelImport(String file, String mediaDir, String relativeConfigDir, ServerProperties serverProps) {
    this.file = file;
    this.isFlashcard = serverProps.isFlashcard();
    this.mediaDir = mediaDir;
    boolean missingExists = getMissing(relativeConfigDir, "missingSlow.txt", missingSlowSet);
    missingExists &= getMissing(relativeConfigDir, "missingFast.txt", missingFastSet);
    shouldHaveRefAudio = missingExists;
    this.usePredefinedTypeOrder = serverProps.usePredefinedTypeOrder();
    this.language = serverProps.getLanguage();
    this.skipSemicolons = serverProps.shouldSkipSemicolonEntries();
    //this.tabooEnglish = serverProps.doTabooEnglish();
    this.audioOffset = serverProps.getAudioOffset();
    String exampleSentenceFile1 = serverProps.getExampleSentenceFile();
    if (exampleSentenceFile1 != null && exampleSentenceFile1.length() > 0) {
      this.exampleSentenceFile = new File(relativeConfigDir, exampleSentenceFile1);
      File badClues = new File(relativeConfigDir, "badClues.txt");
      stimulusInfo = readSampleSentenceFile2(exampleSentenceFile, badClues);
      logger.debug("ExcelImport : found " + exampleSentenceFile.getAbsolutePath());
    } else {
      //logger.debug("ExcelImport : didn't find example sentence file " + exampleSentenceFile1);
    }

/*    logger.debug("\n\n\n\n ---> ExcelImport : config " + relativeConfigDir +
      " media dir " +mediaDir + " slow missing " +missingSlowSet.size() + " fast " + missingFastSet.size());*/
  }

  public boolean getMissing(String relativeConfigDir, String file, Set<String> missing) {
    File missingSlow = new File(relativeConfigDir, file);
    if (missingSlow.exists()) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(missingSlow));
        String line;
        while ((line = reader.readLine()) != null) {
          String trim = line.trim();
          if (trim.length() > 0) {
            missing.add(trim);
          }
        }
        reader.close();

        logger.debug("Read from " + missingSlow.getAbsolutePath() + " and found " + missing.size());

      } catch (Exception e) {
        logger.error("Reading " + missingSlow.getAbsolutePath() + " Got  " + e, e);
      }

    } else {
      logger.debug("Can't find " + file + " under " + relativeConfigDir + " abs path " + missingSlow.getAbsolutePath());
    }
    return missingSlow.exists();
  }

  @Override
  public SectionHelper getSectionHelper() {
    return sectionHelper;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getExercises()
   */
  public List<Exercise> getRawExercises() {
    synchronized (this) {
      if (exercises == null) {
        exercises = readExercises(new File(file));
        for (Exercise e : exercises) idToExercise.put(e.getID(), e);
      }
    }
    return exercises;
  }

  public Exercise getExercise(String id) {
    if (idToExercise.isEmpty()) logger.warn("huh? couldn't find any exercises..?");

    return idToExercise.get(id);
  }

  /**
   * @param file
   * @return
   * @see #getRawExercises()
   */
  private List<Exercise> readExercises(File file) {
    try {
      return readExercises(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      logger.error("looking for " + file.getAbsolutePath() + " got " + e, e);
    }
    return new ArrayList<Exercise>();
  }

  /**
   * @param inp
   * @return
   * @see mitll.langtest.server.SiteDeployer#readExercises(mitll.langtest.shared.Site, org.apache.commons.fileupload.FileItem)
   */
  public List<Exercise> readExercises(InputStream inp) {
    List<Exercise> exercises = new ArrayList<Exercise>();
    try {
      Workbook wb = WorkbookFactory.create(inp);

      for (int i = 0; i < wb.getNumberOfSheets(); i++) {
        Sheet sheet = wb.getSheetAt(i);
        if (sheet.getPhysicalNumberOfRows() > 0) {
          logger.info("------------ reading sheet " + sheet.getSheetName() + " ------------------");
          Collection<Exercise> exercises1 = readFromSheet(sheet);
          exercises.addAll(exercises1);
          logger.info("sheet " + sheet.getSheetName() + " had " + exercises1.size() + " items.");
          if (!exercises1.isEmpty()) {
            Exercise first = exercises1.iterator().next();
            logger.debug("e.g. " + first + " content  " + first.getContent() + " weight " + first.getWeight());
          }
        }
      }

      if (!errors.isEmpty()) {
        logger.warn("there were " + errors.size() + " errors");
        int count = 0;
        for (String error : errors) {
          if (count++ < 10) logger.warn(error);
        }
      }
      sectionHelper.report();
      inp.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InvalidFormatException e) {
      e.printStackTrace();
    }
    return exercises;
  }

  /**
   * @param sheet
   * @return
   * @see #readExercises(java.io.InputStream)
   */
  private Collection<Exercise> readFromSheet(Sheet sheet) {
    List<Exercise> exercises = new ArrayList<Exercise>();
    // logger.debug("for " + sheet.getSheetName() + " regions " +sheet.getNumMergedRegions());
    int id = 0;
    boolean gotHeader = false;
    FileExerciseDAO dao = new FileExerciseDAO("", language, isFlashcard);

    int colIndexOffset = -1;

    int transliterationIndex = -1;
    int unitIndex = -1;
    int chapterIndex = -1;
    int weekIndex = -1;
    int weightIndex = -1;
    int meaningIndex = -1;
    int idIndex = -1;
    int contextIndex = -1;
    int segmentedIndex = -1;
    int audioIndex = -1;
    List<String> lastRowValues = new ArrayList<String>();
    Map<String, List<Exercise>> englishToExercises = new HashMap<String, List<Exercise>>();
    int semis = 0;
    int logging = 0;
    int skipped = 0;
    int englishSkipped = 0;
    String unitName = null, chapterName = null, weekName = null;
    Set<String> withExamples = new HashSet<String>();
    try {
      Iterator<Row> iter = sheet.rowIterator();
      Map<Integer, CellRangeAddress> rowToRange = getRowToRange(sheet);
      for (; iter.hasNext(); ) {
        Row next = iter.next();
        if (TESTING && id > 300) break;     // TODO make this an adult option
        //    logger.warn("------------ Row # " + next.getRowNum() + " --------------- ");
        boolean inMergedRow = rowToRange.keySet().contains(next.getRowNum());

        List<String> columns = new ArrayList<String>();
        if (!gotHeader) {
          Iterator<Cell> cellIterator = next.cellIterator();
          while (cellIterator.hasNext()) {
            Cell next1 = cellIterator.next();
            columns.add(next1.toString().trim());
          }
        }

        if (!gotHeader) {
          List<String> predefinedTypeOrder = new ArrayList<String>();
          for (String col : columns) {
            String colNormalized = col.toLowerCase();
            //  colNormalized = colNormalized.toLowerCase()
            if (colNormalized.startsWith("Word".toLowerCase())) {
              gotHeader = true;
              colIndexOffset = columns.indexOf(col);
            } else if (colNormalized.contains("transliteration")) {
              transliterationIndex = columns.indexOf(col);
            } else if (colNormalized.contains("unit") || colNormalized.contains("book")) {
              unitIndex = columns.indexOf(col);
              predefinedTypeOrder.add(col);
              unitName = col;
            } else if (colNormalized.contains("chapter") || colNormalized.contains("lesson")) {
              chapterIndex = columns.indexOf(col);
              predefinedTypeOrder.add(col);
              chapterName = col;
            } else if (colNormalized.contains("week")) {
              weekIndex = columns.indexOf(col);
              predefinedTypeOrder.add(col);
              weekName = col;
            } else if (colNormalized.contains("weight")) {
              weightIndex = columns.indexOf(col);
            } else if (colNormalized.contains("meaning")) {
              meaningIndex = columns.indexOf(col);
            } else if (colNormalized.contains("id")) {
              idIndex = columns.indexOf(col);
            } else if (colNormalized.contains("context")) {
              contextIndex = columns.indexOf(col);
            } else if (colNormalized.contains("segmented")) {
              segmentedIndex = columns.indexOf(col);
            } else if (colNormalized.contains("audio_index")) {
              audioIndex = columns.indexOf(col);
            }
          }
          if (usePredefinedTypeOrder) {
            sectionHelper.setPredefinedTypeOrder(predefinedTypeOrder);
          }

          logger.info("columns word index " + colIndexOffset +
            " week " + weekIndex + " unit " + unitIndex + " chapter " + chapterIndex +
            " meaning " + meaningIndex +
            " transliterationIndex " + transliterationIndex +
            " contextIndex " + contextIndex +
            " id " + idIndex + " audio " + audioIndex
          );
        } else {
          int colIndex = colIndexOffset;
          String english = getCell(next, colIndex++).trim();
          String foreignLanguagePhrase = getCell(next, colIndex).trim();
          String translit = getCell(next, transliterationIndex);

          // remove starting or ending tics
          foreignLanguagePhrase = cleanTics(foreignLanguagePhrase);

          //logger.info("for row " + next.getRowNum() + " english = " + english + " in merged " + inMergedRow + " last row " + lastRowValues.size());

          if (inMergedRow && !lastRowValues.isEmpty()) {
            if (english.length() == 0) {
              english = lastRowValues.get(0);
              //logger.info("-------- > for row " + next.getRowNum() + " english using " + english);
            }
          }
          if (english.length() == 0) {
            //logger.info("-------- > for row " + next.getRowNum() + " english is blank ");
            englishSkipped++;
          }
          if (gotHeader && english.length() > 0) {
            if (inMergedRow) logger.info("got merged row ------------ ");

            if (inMergedRow && !lastRowValues.isEmpty()) {
              if (foreignLanguagePhrase.length() == 0) {
                foreignLanguagePhrase = lastRowValues.get(1);
                //logger.info("for row " + next.getRowNum() + " for foreign lang using " + foreignLanguagePhrase);
              }
            }
            if (foreignLanguagePhrase.length() == 0) {
              logger.info("Got empty foreign language phrase row #" + next.getRowNum() + " for " + english);
              errors.add(sheet.getSheetName() + "/" + "row #" + (next.getRowNum() + 1) + " phrase was blank.");
              id++;    // TODO is this the right thing for Dari and Farsi???
            } else if (skipSemicolons && (foreignLanguagePhrase.contains(";") || translit.contains(";"))) {
              semis++;
              id++;     // TODO is this the right thing for Dari and Farsi???
            } else {
              String meaning = getCell(next, meaningIndex);
              String givenIndex = getCell(next, idIndex);
              String context = getCell(next, contextIndex);
              String segmentedChinese = getCell(next, segmentedIndex);

              if (inMergedRow && !lastRowValues.isEmpty()) {
                if (translit.length() == 0) {
                  translit = lastRowValues.get(2);
                  //logger.info("for row " + next.getRowNum() + " for translit using " + translit);
                }
              }

              boolean expectFastAndSlow = idIndex == -1;
              String idToUse = expectFastAndSlow ? "" + id++ : givenIndex;
              boolean promptInEnglish = stimulusInfo != null && tabooEnglish;
              Exercise imported = getExercise(idToUse, dao, weightIndex, next, english, foreignLanguagePhrase, translit,
                meaning, context, segmentedChinese, promptInEnglish, (audioIndex != -1) ? getCell(next, audioIndex) : "");
              if (imported.hasRefAudio() || !shouldHaveRefAudio) {  // skip items without ref audio, for now.
                boolean valid = true;
                //  boolean enoughItems = true;
                if (stimulusInfo != null) {
                  valid = addTabooQuestions(withExamples, imported);
                }
                if (valid) {
                  recordUnitChapterWeek(unitIndex, chapterIndex, weekIndex, next, imported, unitName, chapterName, weekName);

                  // keep track of synonyms (or better term)
                  rememberExercise(exercises, englishToExercises, imported);
                } else {
                  if (SHOW_SKIPS)
                    logger.debug("skipping exercise " + imported.getID() + " : '" + imported.getEnglishSentence() + "' since no sample sentences");
                }
              } else {
                if (logging++ < 3) {
                  logger.info("skipping exercise " + imported.getID() + " : '" + imported.getEnglishSentence() + "' since no audio.");
                }
                skipped++;
              }
              if (inMergedRow) {
                lastRowValues.add(english);
                lastRowValues.add(foreignLanguagePhrase);
                lastRowValues.add(translit);
              } else if (!lastRowValues.isEmpty()) {
                lastRowValues.clear();
              }
            }
          } else if (gotHeader && foreignLanguagePhrase.length() > 0) {
            errors.add(sheet.getSheetName() + "/" + "row #" + (next.getRowNum() + 1) + " Word/Expression was blank");
          }
        }
      }

      addSynonyms(englishToExercises);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    if (stimulusInfo != null)
      logger.info("got  " + withExamples.size() + " examples out of " + exercises.size() + " exercises.");
    logger.info("max exercise id = " + id);
    if (skipped > 0) {
      logger.info("Skipped " + skipped + " entries with missing audio. " + (100f * ((float) skipped) / (float) id) + "%");
    }
    if (englishSkipped > 0) {
      logger.info("Skipped " + englishSkipped + " entries with missing english. " + (100f * ((float) englishSkipped) / (float) id) + "%");
    }
    if (semis > 0) {
      logger.info("Skipped " + semis + " entries with semicolons or " + (100f * ((float) semis) / (float) id) + "%");
    }
    return exercises;
  }

  private Map<Integer, CellRangeAddress> getRowToRange(Sheet sheet) {
    Map<Integer, CellRangeAddress> rowToRange = new HashMap<Integer, CellRangeAddress>();
    for (int r = 0; r < sheet.getNumMergedRegions(); r++) {
      CellRangeAddress mergedRegion = sheet.getMergedRegion(r);
      for (int rr = mergedRegion.getFirstRow(); rr <= mergedRegion.getLastRow(); rr++) {
        rowToRange.put(rr, mergedRegion);
      }
/*      logger.debug("for " + sheet.getSheetName() + " region  " + mergedRegion + " " +
          mergedRegion.getFirstRow() + " " + mergedRegion.getFirstColumn());*/
    }
    return rowToRange;
  }

  private boolean addTabooQuestions(Set<String> withExamples, Exercise imported) {
    boolean valid;
    String wordToGuess = tabooEnglish ? imported.getEnglishSentence().trim() : imported.getRefSentence().trim();
    List<String> clues = stimulusInfo.wordToClues.get(wordToGuess);
    valid = (clues != null);
    if (valid) {
      withExamples.add(wordToGuess);
      boolean enoughItems = clues.size() > MIN_TABOO_ITEMS;
      if (enoughItems) {
        imported.getQuestions().clear();
        for (int i = 0; i < clues.size(); i++) {
          String clue = clues.get(i);
          List<List<String>> answersForEachClue = stimulusInfo.wordToAnswers.get(wordToGuess);
          List<String> answers = answersForEachClue == null ? Collections.singletonList(wordToGuess) : answersForEachClue.get(i);

          imported.addQuestion(tabooEnglish ? Exercise.EN : Exercise.FL, clue, answers.get(0), answers);
          //logger.debug("exercise id " + imported.getID() + " num clues " + clues.size() + " clue " + clue + " answer " + answers.get(0) + " : " + imported.getQuestions());
        }
        //logger.debug("exercise id " + imported.getID() + " has questions " + imported.getQuestions());
      } else {
        logger.warn("not enough items for " + wordToGuess);
        valid = enoughItems;
      }
    }
    return valid;
  }

  private void rememberExercise(List<Exercise> exercises, Map<String, List<Exercise>> englishToExercises, Exercise imported) {
    String englishSentence = imported.getEnglishSentence();
    List<Exercise> exercisesForSentence = englishToExercises.get(englishSentence);
    if (exercisesForSentence == null)
      englishToExercises.put(englishSentence, exercisesForSentence = new ArrayList<Exercise>());
    exercisesForSentence.add(imported);

    exercises.add(imported);
  }

  private void addSynonyms(Map<String, List<Exercise>> englishToExercises) {
    for (List<Exercise> exercises2 : englishToExercises.values()) {
      if (exercises2.size() > 1) {
        Set<String> translationSet = new HashSet<String>();

        List<String> translations = new ArrayList<String>();
        List<String> transliterations = new ArrayList<String>();
        List<String> audioRefs = new ArrayList<String>();
        for (Exercise e : exercises2) {
          for (int i = 0; i < e.getRefSentences().size(); i++) {
            try {
              String ref = e.getRefSentences().get(i);
              String transLower = ref.toLowerCase().trim();

              if (!e.getTranslitSentences().isEmpty()) {
                String translit = e.getTranslitSentences().get(i);
                if (!translationSet.contains(transLower)) {
                  translations.add(ref);
                  transliterations.add(translit);
                  translationSet.add(transLower);
                  audioRefs.add(e.getRefAudio());
                }
              }
            } catch (Exception e1) {
              logger.error("got " + e1 + " on " + e, e1);
            }
          }
        }

        if (translations.size() > 1) {
          for (Exercise e : exercises2) {
            e.setSynonymSentences(translations);
            e.setSynonymTransliterations(transliterations);
            e.setSynonymAudioRefs(audioRefs);
            //logger.debug("e " + e.getID() + " '" + e.getEnglishSentence() + "' has " + e.getSynonymAudioRefs().size() + " audio refs or " + translations);
          }
        }
      } else if (false) {   // hard to test
        for (Exercise e : exercises2) {
          List<String> doubles = new ArrayList<String>();
          doubles.add(e.getRefAudio());
          doubles.add(e.getRefAudio());
          e.setSynonymAudioRefs(doubles);
        }
      }
    }
  }

  private String cleanTics(String foreignLanguagePhrase) {
    if (foreignLanguagePhrase.startsWith("\'")) {
      foreignLanguagePhrase = foreignLanguagePhrase.substring(1);
    }
    if (foreignLanguagePhrase.endsWith("\'"))
      foreignLanguagePhrase = foreignLanguagePhrase.substring(0, foreignLanguagePhrase.length() - 1);
    return foreignLanguagePhrase;
  }

/*  private void checkLTS(int id, BufferedWriter writer, SmallVocabDecoder svd, ModernStandardArabicLTS lts, String english, String foreignLanguagePhrase) {
    List<String> tokens = svd.getTokens(foreignLanguagePhrase);
    try {

      for (String token : tokens) {
        String[][] process = lts.process(token);
        if (process == null) {
          String message = "couldn't do lts on exercise #" + (id - 1) + " token '" + token +
            "' length " + token.length() + " trim '" + token.trim() +
            "' " +
            " '" + foreignLanguagePhrase + "' english = '" + english + "'";
          logger.error(message);
          //logger.error("\t tokens " + tokens + " num =  " + tokens.size());

          writer.write(message);
          writer.write("\n");
        }
      }
    } catch (Exception e) {
      logger.error("couldn't do lts on " + (id - 1) + " " + foreignLanguagePhrase + " " + english);
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }*/

  /**
   * @param id
   * @param dao
   * @param weightIndex
   * @param next
   * @param english
   * @param foreignLanguagePhrase
   * @param translit
   * @param meaning
   * @param context
   * @param segmentedChinese
   * @param refAudioIndex
   * @return
   * @see #readFromSheet(org.apache.poi.ss.usermodel.Sheet)
   */
  private Exercise getExercise(String id, FileExerciseDAO dao, int weightIndex, Row next,
                               String english, String foreignLanguagePhrase, String translit, String meaning,
                               String context, String segmentedChinese, boolean promptInEnglish, String audioIndex) {
    Exercise imported;
    List<String> translations = new ArrayList<String>();
    if (foreignLanguagePhrase.length() > 0) {
      translations.addAll(Arrays.asList(foreignLanguagePhrase.split(";")));
    }
    if (isFlashcard) {
      imported = new Exercise("flashcardStimulus", id, english, translations, english);
      if (imported.getEnglishSentence() == null && imported.getContent() == null) {
        logger.warn("both english sentence and content null for exercise " + id);
      }
    } else {
      imported = getExercise(id, dao, english, foreignLanguagePhrase, translit, meaning, context, promptInEnglish, audioIndex);
    }
    imported.setEnglishSentence(english);
    if (translit.length() > 0) {
      imported.setTranslitSentence(translit);
    }
    List<String> inOrderTranslations = new ArrayList<String>(translations);
    imported.setRefSentences(inOrderTranslations);
    if (!segmentedChinese.isEmpty()) imported.setSegmented(segmentedChinese);

    if (weightIndex != -1) {
      imported.setWeight(getNumericCell(next, weightIndex));
    }
    return imported;
  }

  private boolean recordUnitChapterWeek(int unitIndex, int chapterIndex, int weekIndex,
                                        Row next,
                                        Exercise imported, String unitName, String chapterName, String weekName) {
    String unit = getCell(next, unitIndex);
    String chapter = getCell(next, chapterIndex);
    String week = getCell(next, weekIndex);
    List<SectionHelper.Pair> pairs = new ArrayList<SectionHelper.Pair>();

    if (unit.length() == 0 &&
      chapter.length() == 0 &&
      week.length() == 0
      ) {
      unit = "Blank";
    }

    // hack to trim off leading tics
    if (unit.startsWith("'")) unit = unit.substring(1);
    if (unit.equals("intro")) unit = "Intro"; // hack
    if (chapter.startsWith("'")) chapter = chapter.substring(1);
    if (week.startsWith("'")) week = week.substring(1);

    if (debug)
      logger.debug("unit " + unitIndex + "/" + unit + " chapter " + chapterIndex + "/" + chapter + " week " + week);

    if (unit.length() > 0) {
      if (usePredefinedTypeOrder) {
        pairs.add(sectionHelper.addExerciseToLesson(imported, unitName, unit));
      } else {
        pairs.add(sectionHelper.addUnitToLesson(imported, unit));
      }
    }
    if (chapter.length() > 0) {
      if (language.equalsIgnoreCase("English")) {
        chapter = (unitIndex == -1 ? "" : unit + "-") + chapter; // hack for now to get unique chapters...
      }
      if (usePredefinedTypeOrder) {
        pairs.add(sectionHelper.addExerciseToLesson(imported, chapterName, chapter));
      } else {
        pairs.add(sectionHelper.addChapterToLesson(imported, chapter));
      }
    }
    if (week.length() > 0) {
      if (usePredefinedTypeOrder) {
        pairs.add(sectionHelper.addExerciseToLesson(imported, weekName, chapter));
      } else {
        pairs.add(sectionHelper.addWeekToLesson(imported, week));
      }
    }
    sectionHelper.addAssociations(pairs);

    return false;
  }

  /**
   * @param id
   * @param dao
   * @param english
   * @param foreignLanguagePhrase
   * @param translit
   * @param meaning
   * @param context
   * @param refAudioIndex
   * @return
   * @see #getExercise(String, FileExerciseDAO, int, org.apache.poi.ss.usermodel.Row, String, String, String, String, String, String, boolean, String)
   */
  private Exercise getExercise(String id, FileExerciseDAO dao,
                               String english, String foreignLanguagePhrase, String translit, String meaning,
                               String context, boolean promptInEnglish, String refAudioIndex) {
    String content = dao.getContent(foreignLanguagePhrase, translit, english, meaning, context);
    Exercise imported = new Exercise("import", id, content, promptInEnglish, true, english);
    imported.addQuestion();   // TODO : needed?

    String prefix = language.equalsIgnoreCase("msa") ? id + "_" : "";
    String audioDir = refAudioIndex.length() > 0 ? findBest(refAudioIndex) : id;
    if (audioOffset != 0) {
      audioDir = "" +(Integer.parseInt(audioDir.trim())+audioOffset);
    }
    String fastAudioRef = mediaDir + File.separator + audioDir + File.separator + prefix + "Fast" + ".wav";
    String slowAudioRef = mediaDir + File.separator + audioDir + File.separator + prefix + "Slow" + ".wav";

    imported.setType(Exercise.EXERCISE_TYPE.REPEAT_FAST_SLOW);

    if (!missingFastSet.contains(audioDir)) {
      imported.setRefAudio(ensureForwardSlashes(fastAudioRef));
    }
    if (!missingSlowSet.contains(audioDir)) {
      imported.setSlowRefAudio(ensureForwardSlashes(slowAudioRef));
    }

    return imported;
  }

  /**
   * Assumes audio index field looks like : 11109 8723 8722 8721
   *
   * @param refAudioIndex
   * @return
   */
  private String findBest(String refAudioIndex) {
    String best = "";
    for (String recording : refAudioIndex.split("\\s+")) {
      if (!missingFastSet.contains(recording) && !missingSlowSet.contains(recording)) {
        best = recording;
        break;
      } else {
        best = recording;
      }
    }
    return best;
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  private String getCell(Row next, int col) {
    if (col == -1) return "";
    Cell cell = next.getCell(col);
    if (cell == null) return "";
    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
      double numericCellValue = cell.getNumericCellValue();
      return "" + new Double(numericCellValue).intValue();
    } else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
      return cell.getStringCellValue().trim();
    } else {
      return cell.toString().trim();
    }
  }

  private double getNumericCell(Row next, int col) {
    Cell cell = next.getCell(col);
    if (cell == null) return -1;
    return (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) ? cell.getNumericCellValue() : -1;
  }

  public List<Exercise> getExercises() {
    return exercises;
  }

  public Set<String> getSections() {
    return sectionHelper.getSections();
  }

  public Map<String, Lesson> getSection(String type) {
    return sectionHelper.getSection(type);
  }

  public List<String> getErrors() {
    return errors;
  }

/*  private void populateSentenceExamples(List<Exercise> rawExercises, String relativeConfigDir, String exampleFile) {
    File examples = new File(relativeConfigDir, exampleFile);

    populateExampleSentences(rawExercises, examples);
  }*/

/*  private void populateExampleSentences(List<Exercise> rawExercises, File examples) {
    Map<String, List<Exercise>> refToEx = new HashMap<String, List<Exercise>>();
    for (Exercise e : rawExercises) {
      List<Exercise> exForRef = refToEx.get(e.getRefSentence().trim());
      if (exForRef == null) refToEx.put(e.getRefSentence().trim(), exForRef = new ArrayList<Exercise>());
      exForRef.add(e);
    }

    readSampleSentenceFile(examples, refToEx);
  }

  private void readSampleSentenceFile(File examples, Map<String, List<Exercise>> refToEx) {
    try {
   *//*   String name = "C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\war\\config\\taboo\\examples.txt";
      File fname = new File(name);*//*
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(examples), FileExerciseDAO.ENCODING));
      String line2;
      int c = 0;
      while ((line2 = reader.readLine()) != null) {
        c++;
        String[] split = line2.split("\\t");
        List<Exercise> exercises1 = refToEx.get(split[0].trim());
        if (exercises1 != null) {
          for (Exercise e : exercises1) {
            //if (e.getSynonymSentences().isEmpty()) logger.debug("adding to " +e);
            e.getSynonymSentences().add(split[1]);
          }
        }
      }
      logger.debug("populateExampleSentences : read " + c + " examples");

      reader.close();
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }
  }*/

  /**
   * @param examples
   * @return
   * @see mitll.langtest.server.database.ExcelImport#ExcelImport()
   */
  private StimulusInfo readSampleSentenceFile2(File examples, File badClueFile) {
    Map<String, List<String>> wordToSamples = new HashMap<String, List<String>>();
    Map<String, List<List<String>>> wordToAnswers = new HashMap<String, List<List<String>>>();
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(examples), FileExerciseDAO.ENCODING));
      String line2;
      int c = 0;
      if (tabooEnglish) {
        while ((line2 = reader.readLine()) != null) {
          c++;
          if (line2.trim().length() > 0) {
            String[] split = line2.split("\\t");
            String word = split[0].trim();
            if (split.length == 1) {
              logger.warn("bad line " + line2 + " len " + split.length);
            } else if (split.length == 2) {
              String sentence = split[1];

              addAnswer(wordToSamples, word, sentence);
            } else if (split.length == 3) {
              String answers = split[2];

              List<List<String>> samples = wordToAnswers.get(word);
              if (samples == null) wordToAnswers.put(word, samples = new ArrayList<List<String>>());
              samples.add(Arrays.asList(answers.split(",")));
            }
          }
        }
        logger.debug("readSampleSentenceFile2 : read " + c + " examples");
      } else {
        BufferedReader reader2 = new BufferedReader(new InputStreamReader(new FileInputStream(badClueFile), FileExerciseDAO.ENCODING));
        Map<String, Set<String>> wordToBadClues = readBadSpanishClues(reader2);
        readSpanishClues(wordToSamples, wordToAnswers, reader, wordToBadClues);
      }

      reader.close();
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }
    return new StimulusInfo(wordToSamples, wordToAnswers);
  }

  void readSpanishClues(Map<String, List<String>> wordToSamples, Map<String, List<List<String>>> wordToAnswers,
                        File examples
  ) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(examples), FileExerciseDAO.ENCODING));
    Map<String, Set<String>> wordToBadClues = new HashMap<String, Set<String>>();
    readSpanishClues(wordToSamples, wordToAnswers, reader, wordToBadClues);
  }

  private void readSpanishClues(Map<String, List<String>> wordToSamples, Map<String, List<List<String>>> wordToAnswers,
                                BufferedReader reader,
                                Map<String, Set<String>> wordToBadClues) throws IOException {
    String line2;
    line2 = reader.readLine(); // skip header
    int c = 0;
    int badClueCount = 0;
    while ((line2 = reader.readLine()) != null) {
      c++;
      if (line2.trim().length() > 0) {
        String[] split = line2.split("\\t");
        if (split.length < 3) {
          logger.warn("bad line " + line2 + " len " + split.length);
        } else {
          String word = split[2].trim().toLowerCase();

          String sentence = split[0];
          String answer = split[1];

          Set<String> badClues = wordToBadClues.get(word);
          if (badClues != null && badClues.contains(getCleanedClue(sentence))) {
            // logger.warn("skipping word " + word + " clue " + sentence);
            badClueCount++;
          } else {
            List<String> samples = wordToSamples.get(word);
            if (samples == null) wordToSamples.put(word, samples = new ArrayList<String>());
            //if (!sentence.endsWith("?") || true) { // replace ? with underscore

            sentence = getBlankSequence(sentence, answer);
            // }
            samples.add(sentence);

            List<List<String>> answers = wordToAnswers.get(word);
            if (answers == null) wordToAnswers.put(word, answers = new ArrayList<List<String>>());
            answers.add(Arrays.asList(answer.split(",")));
          }
        }
      }
    }
    logger.debug("readSampleSentenceFile2 : read " + c + " examples threw out " + badClueCount + " bad clues.");
  }

  String getCleanedClue(String answer) {
    return answer.replaceAll("_", "").replaceAll("\\?", "").replaceAll("\\s+", "");
  }

  private Map<String, Set<String>> readBadSpanishClues(BufferedReader reader) throws IOException {
    String line2;
    line2 = reader.readLine(); // skip header
    int c = 0;
    Map<String, Set<String>> wordToClues = new HashMap<String, Set<String>>();
    while ((line2 = reader.readLine()) != null) {
      c++;
      if (line2.trim().length() > 0) {
        String[] split = line2.split("\\t");
        if (split.length < 2) {
          logger.warn("readBadSpanishClues bad line " + line2 + " len " + split.length);
        } else {
          String word = split[0].trim().toLowerCase();
          String clue = split[1];
          Set<String> samples = wordToClues.get(word);
          if (samples == null) wordToClues.put(word, samples = new HashSet<String>());
          //if (!sentence.endsWith("?") || true) { // replace ? with underscore

          //  sentence = getBlankSequence(sentence, answer);
          // }
          samples.add(clue);
        }
      }
    }
    logger.debug("readBadSpanishClues : read " + c + " examples");
    return wordToClues;
  }

  private String getBlankSequence(String sentence, String answer) {
    boolean isQuestion = (sentence.endsWith("?"));
    String[] words = answer.split("\\p{Z}+"); // fix for unicode spaces! Thanks Jessica!
    StringBuilder builder = new StringBuilder();
    for (String token : words) {
      builder.append(token.replaceAll(".", "_"));
      builder.append(" ");
    }
    String sequenceOfBlanks = builder.toString().trim();

    if (isQuestion) {
      sentence = sentence.substring(0, sentence.length() - 1);
    }
    String previousSentence = sentence;
    sentence = sentence.replace("?", sequenceOfBlanks);
    if (isQuestion) {
      if (sentence.equals(previousSentence)) {  // no blank yet added
        sentence += sequenceOfBlanks;
      }
      sentence += "?";
    }
    return sentence;
  }

  private static class StimulusInfo {
    Map<String, List<String>> wordToClues;
    Map<String, List<List<String>>> wordToAnswers;

    public StimulusInfo(Map<String, List<String>> wordToClues, Map<String, List<List<String>>> wordToAnswers) {
      this.wordToClues = wordToClues;
      this.wordToAnswers = wordToAnswers;
    }
  }

  public static void main(String[] arg) {
//                   String guess = "los can't suburbios.";

/*
    guess = guess.replaceAll("\\p{Punct}$",""); // remove trailing punctuation
    System.out.println("guess " + guess);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    try {
      System.out.println(sdf.parse("2013-09-25").getTime());      // chapter 11
      System.out.println(sdf.parse("2013-10-08").getTime());
      System.out.println(sdf.parse("2013-10-29").getTime());
      System.out.println(sdf.parse("2013-11-13").getTime());     // chapter 14
    } catch (ParseException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
*/
    Map<String, List<String>> clueToAnswers = new HashMap<String, List<String>>();

    try {
      String name = "C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\Batch_1260169_batch_results.csv";
      // String name2 = "C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\war\\config\\taboo\\nextExamples.txt";

      //BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(name2), FileExerciseDAO.ENCODING));

      File fname = new File(name);
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fname), FileExerciseDAO.ENCODING));
      String line2;

      // "20KD21O3W5XHH1RBPGTK5EPLB5ZGA5","29B2KYEPLSP7M2N83UA3VAHQEXPVP1","Spanish Guessing Game","Guess Spanish words from Spanish clues.",
      // "Spanish, language, game, clues, guess, español, lengua, juego, conjetura","$0.05","Fri Sep 06 20:54:21 GMT 2013","5","BatchId:1260169;","3600","172800","Fri Sep 13 20:54:21 GMT 2013","","","2H5ZKPFE4EGDYVOO2JZIGNZWY0D7HM","A3C2DVYUP88DE7","Approved","Sat Sep 07 10:42:30 GMT 2013","Sat Sep 07 10:43:07 GMT 2013","Mon Sep 09 03:43:07 PDT 2013","Sat Sep 07 17:50:38 PDT 2013","","","37","100% (641/641)","100% (641/641)","100% (488/488)","____ ____ ____ para limpiar el restaurante al final de la noche hay que ____ ____ ____","____ ____ Telas para cubrir las ventanas.","Tengo que hacer del numero uno, tengo que usar ____ ____","barrer el suelo","las cortinas","el primero","","","",""


      Set<String> refs = new HashSet<String>();
      line2 = reader.readLine();
      int c = 0;
      while ((line2 = reader.readLine()) != null) {

        String[] split = line2.split("\",\"");

        int i = 27;


        try {
          String c1 = split[i++].replaceAll("_", "");
          String c2 = split[i++].replaceAll("_", "");
          //  System.out.println("clue " + c2);
          String c3 = split[i++].replaceAll("_", "");
          //  System.out.println("clue " + c3);

          String a1 = split[i++];
          if (c < 10)
            System.out.println("" + c1 + "\t= " + a1);
          String a2 = split[i++];
          if (c < 10) System.out.println("" + c2 + "\t= " + a2);
          String a3 = split[i++];
          if (c++ < 10) System.out.println("" + c3 + "\t= " + a3);

          addAnswer(clueToAnswers, c1, a1);
          addAnswer(clueToAnswers, c2, a2);
          addAnswer(clueToAnswers, c3, a3);
        } catch (Exception e) {
          System.err.println("couldn't parse " + line2);
          //  e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        //  if (c++ > 10) break;

      }

      float num = 0;
      float count = 0;


      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("verifiedClues.txt"), FileExerciseDAO.ENCODING));

      for (Map.Entry<String, List<String>> clueToAnswer : clueToAnswers.entrySet()) {
        num += clueToAnswer.getValue().size();
        writer.write(clueToAnswer.getKey() + "\n");

        count++;
      }
      writer.close();

      System.out.println("average answers " + (num / count) + " num " + num);
//      logger.debug("read " + c + " examples, found " + refs.size() + " refs");
      //    residual.removeAll(refs);
      //  logger.debug("not found " + residual);
      reader.close();
      //writer.close();
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }
    String name =
      "C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\war\\config\\taboo\\ch10_allturk_cleaned.tsv";

    Map<String, List<String>> wordToClues = new HashMap<String, List<String>>();
    Map<String, List<List<String>>> wordToAnswers = new HashMap<String, List<List<String>>>();
    int totalBad = 0;
    int totalGood = 0;
    try {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("badClues.txt"), FileExerciseDAO.ENCODING));

      ExcelImport excelImport = new ExcelImport();
      excelImport.readSpanishClues(wordToClues, wordToAnswers, new File(name));
      for (Map.Entry<String, List<String>> wordToClue : wordToClues.entrySet()) {
        for (String clue : wordToClue.getValue()) {
          String cclue = clue.replaceAll("_", "");
          if (cclue.endsWith("?")) cclue = cclue.substring(0, cclue.length() - 1);
          List<String> answers = clueToAnswers.get(cclue);
          if (answers != null) {
            String vocabPhrase = wordToClue.getKey();
            List<List<String>> expectedAnswers = wordToAnswers.get(vocabPhrase);
            Set<String> possible = new HashSet<String>();
            for (List<String> ans : expectedAnswers) {
              possible.addAll(ans);
            }

            float bad = 0;
            float all = answers.size();
            int i = 0;
            for (String guess : answers) {
              if (!possible.contains(guess) && !possible.contains(guess.toLowerCase())) {
                logger.info("#" + i++ +
                  " for " + vocabPhrase +
                  " bad guess '" + guess + "' expecting " + possible);
                bad++;
              } else {
                logger.debug("#" + i++ + " for " + vocabPhrase +
                  " good guess '" + guess + "' expecting " + possible);
              }
            }
            if (bad > 1) {// || ((bad / all) > 0.5 && all > 2)) {
              System.out.println("got " + bad + " out of " + all + " for " + cclue);
              writer.write(vocabPhrase + "\t" + excelImport.getCleanedClue(clue) + "\n");
              totalBad++;
            } else if (all > 0) totalGood++;
          }
          //else logger.debug("no answer for " + clue + " / " +cclue);
        }
      }
      writer.close();
      logger.warn("bad " + totalBad + " good " + totalGood);
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  private static void addAnswer(Map<String, List<String>> clueToAnswers, String c1, String a1) {
    List<String> answers = clueToAnswers.get(c1);
    if (answers == null) clueToAnswers.put(c1, answers = new ArrayList<String>());
    answers.add(a1);
  }

  public static void main2(String[] arg) {
/*
    ExcelImport config = new ExcelImport(
      "C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\war\\config\\english\\ESL_ELC_5071-30books_chapters.xlsx", false, "config\\bestAudio", "war\\config\\english", true, "English");
*/
    testTaboo();

    //populateSentenceExamples(rawExercises);
    //writer.close();
/*
    List<String> typeOrder = config.sectionHelper.getTypeOrder();
    System.out.println(" type order " +typeOrder);

    System.out.println(" section nodes " + config.sectionHelper.getSectionNodes());*/
  }

  private static void testTaboo() {
    ServerProperties serverProps = new ServerProperties();
    serverProps.getProperties().put("language", "English");
    serverProps.getProperties().put("collectAudio", "false");
    ExcelImport config = new ExcelImport(
      "C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\war\\config\\taboo\\wordlist3.xlsx", "config\\bestAudio", "war\\config\\taboo", serverProps);
    List<Exercise> rawExercises = config.getRawExercises();

    System.out.println("first " + rawExercises.get(0));
    Map<String, List<Exercise>> refToEx = new HashMap<String, List<Exercise>>();
    for (Exercise e : rawExercises) {
      List<Exercise> exForRef = refToEx.get(e.getRefSentence().trim());
      if (exForRef == null) refToEx.put(e.getRefSentence().trim(), exForRef = new ArrayList<Exercise>());
      exForRef.add(e);
    }

    logger.debug("ref has " + refToEx.size());

    try {
      String name = "C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\spaFromTatoeba.csv";
      String name2 = "C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\war\\config\\taboo\\nextExamples.txt";

      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(name2), FileExerciseDAO.ENCODING));

      File fname = new File(name);
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fname), FileExerciseDAO.ENCODING));
      String line2;
      int c = 0;
      int por = 0;
      int para = 0;
      int cuando = 0;
      int este = 0;
      Set<String> refs = new HashSet<String>();
      while ((line2 = reader.readLine()) != null) {
        c++;
        String[] split = line2.split("\\t");
        String sentence = split[2].trim();

        for (String ref : refToEx.keySet()) {
          if (sentence.contains(ref)) {
            int i = sentence.indexOf(ref);
            boolean spaceBefore = false, spaceAfter = false;
            char before = ' ';
            char after = ' ';
            if (i > 0) {
              before = sentence.charAt(i - 1);
              spaceBefore = !Character.isAlphabetic(before);
            }
            if (i + ref.length() < sentence.length()) {
              after = sentence.charAt(i + ref.length());
              spaceAfter = !Character.isAlphabetic(after);
            }
            if (spaceBefore && spaceAfter) {
              boolean valid = true;
              if (ref.equals("por")) {
                por++;
                if (por > 50) valid = false;
              }
              if (ref.equals("para")) {
                para++;
                if (para > 50) valid = false;
              }
              if (ref.equals("cuando")) {
                cuando++;
                if (cuando > 50) valid = false;
              }
              if (ref.equals("este")) {
                este++;
                if (este > 50) valid = false;
              }
              if (valid) {
                //logger.debug("found '" + sentence + "' for '" + ref + "'");
                writer.write(ref);
                writer.write("\t");
                writer.write(sentence);
                writer.write("\n");
                refs.add(ref);
              }
              //  break;
            } else {
              logger.debug("for '" + sentence + "' and '" + ref + "' before '" + before + "' after '" + after + "'");
            }
          }
        }
      }
      logger.debug("read " + c + " examples, found " + refs.size() + " refs");
      HashSet<String> residual = new HashSet<String>(refToEx.keySet());
      residual.removeAll(refs);
      logger.debug("not found " + residual);
      reader.close();
      writer.close();
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }
  }
}
