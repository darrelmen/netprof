package mitll.langtest.server.database;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseFormatter;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

  private static final boolean SHOW_SKIPS = false;
  private final boolean isFlashcard;

  private List<Exercise> exercises = null;
  private Map<String, Exercise> idToExercise = new HashMap<String, Exercise>();
  private List<String> errors = new ArrayList<String>();
  private final String file;
  private SectionHelper sectionHelper = new SectionHelper();
  private boolean debug = false;
  private String mediaDir;
  private Set<String> missingSlowSet = new HashSet<String>();
  private Set<String> missingFastSet = new HashSet<String>();
  private boolean shouldHaveRefAudio = false;
  private boolean usePredefinedTypeOrder;
  private final String language;
  private boolean skipSemicolons;
  private int audioOffset = 0;
  private final int maxExercises;
  private ServerProperties serverProps;

  /**
   * @see mitll.langtest.server.SiteDeployer#readExercisesPopulateSite(mitll.langtest.shared.Site, String, java.io.InputStream)
   */
  public ExcelImport() {
    this.file = null;
    this.isFlashcard = false;
    this.language = "";
    maxExercises = Integer.MAX_VALUE;
  }

  /**
   * @param file
   * @param relativeConfigDir
   * @see DatabaseImpl#makeDAO
   */
  public ExcelImport(String file, String mediaDir, String relativeConfigDir, ServerProperties serverProps) {
    this.file = file;
    this.serverProps = serverProps;
    this.isFlashcard = serverProps.isFlashcard();
    maxExercises = serverProps.getMaxNumExercises();
    this.mediaDir = mediaDir;
    boolean missingExists = getMissing(relativeConfigDir, "missingSlow.txt", missingSlowSet);
    missingExists &= getMissing(relativeConfigDir, "missingFast.txt", missingFastSet);
    shouldHaveRefAudio = missingExists;
    this.usePredefinedTypeOrder = serverProps.usePredefinedTypeOrder();
    this.language = serverProps.getLanguage();
    this.skipSemicolons = serverProps.shouldSkipSemicolonEntries();
    this.audioOffset = serverProps.getAudioOffset();

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
      if (debug) sectionHelper.report();
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
    try {
      Iterator<Row> iter = sheet.rowIterator();
      Map<Integer, CellRangeAddress> rowToRange = getRowToRange(sheet);
      for (; iter.hasNext(); ) {
        Row next = iter.next();
        if (id > maxExercises) break;     // TODO make this an adult option
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
              //String segmentedChinese = getCell(next, segmentedIndex);

              if (inMergedRow && !lastRowValues.isEmpty()) {
                if (translit.length() == 0) {
                  translit = lastRowValues.get(2);
                  //logger.info("for row " + next.getRowNum() + " for translit using " + translit);
                }
              }

              boolean expectFastAndSlow = idIndex == -1;
              String idToUse = expectFastAndSlow ? "" + id++ : givenIndex;
              Exercise imported = getExercise(idToUse, weightIndex, next, english, foreignLanguagePhrase, translit,
                meaning, context, false, (audioIndex != -1) ? getCell(next, audioIndex) : "");
              if (imported.hasRefAudio() || !shouldHaveRefAudio) {  // skip items without ref audio, for now.
                boolean valid = true;
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
      if(this.serverProps.getCollectSynonyms())
         addSynonyms(englishToExercises);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

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
   * @param weightIndex
   * @param next
   * @param english
   * @param foreignLanguagePhrase
   * @param translit
   * @param meaning
   * @param context
   * @return
   * @see #readFromSheet(org.apache.poi.ss.usermodel.Sheet)
   */
  private Exercise getExercise(String id, int weightIndex, Row next,
                               String english, String foreignLanguagePhrase, String translit, String meaning,
                               String context, boolean promptInEnglish, String audioIndex) {
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
      imported = getExercise(id, english, foreignLanguagePhrase, translit, meaning, context, promptInEnglish, audioIndex);
    }
    imported.setEnglishSentence(english);
    if (translit.length() > 0) {
      imported.setTranslitSentence(translit);
    }
    List<String> inOrderTranslations = new ArrayList<String>(translations);
    imported.setRefSentences(inOrderTranslations);
    //if (!segmentedChinese.isEmpty()) imported.setSegmented(segmentedChinese);

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

    if (debug && false)
      logger.debug("unit(" +unitName+
        ")" + unitIndex + "/" + unit + " chapter " + chapterIndex + "/(" +chapterName+
        ")" + chapter + " week (" + weekName+ ") : " + week);

    if (unit.length() > 0) {
      pairs.add(sectionHelper.addExerciseToLesson(imported, unitName, unit));
    }
    if (chapter.length() > 0) {
      if (language.equalsIgnoreCase("English")) {
        chapter = (unitIndex == -1 ? "" : unit + "-") + chapter; // hack for now to get unique chapters...
      }
      pairs.add(sectionHelper.addExerciseToLesson(imported, chapterName, chapter));
    }
    if (week.length() > 0) {
      pairs.add(sectionHelper.addExerciseToLesson(imported, weekName, week));
    }
    sectionHelper.addAssociations(pairs);

    return false;
  }

  /**
   * @param id
   * @param english
   * @param foreignLanguagePhrase
   * @param translit
   * @param meaning
   * @param context
   * @param refAudioIndex
   * @return
   */
  private Exercise getExercise(String id,
                               String english, String foreignLanguagePhrase, String translit, String meaning,
                               String context, boolean promptInEnglish, String refAudioIndex) {
    String content = ExerciseFormatter.getContent(foreignLanguagePhrase, translit, english, meaning, context);
    Exercise imported = new Exercise("import", id, content, promptInEnglish, true, english);
    imported.addQuestion();   // TODO : needed?

    String audioDir = refAudioIndex.length() > 0 ? findBest(refAudioIndex) : id;
    if (audioOffset != 0) {
      audioDir = "" + (Integer.parseInt(audioDir.trim()) + audioOffset);
    }
    String fastAudioRef = mediaDir + File.separator + audioDir + File.separator + "Fast" + ".wav";
    String slowAudioRef = mediaDir + File.separator + audioDir + File.separator + "Slow" + ".wav";

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
      if((new Double(numericCellValue).intValue()) < numericCellValue)
         return "" + numericCellValue;
      else
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
}
