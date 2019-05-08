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
package mitll.langtest.server.database.project;

import mitll.hlt.domino.server.data.IDominoContext;
import mitll.hlt.domino.server.extern.importers.DocumentImporterBase;
import mitll.hlt.domino.server.extern.importers.metadata.BaseExcelReader;
import mitll.hlt.domino.server.extern.importers.vocab.VocabularyItemFactory;
import mitll.hlt.domino.shared.model.document.CStringMetadata;
import mitll.hlt.domino.shared.model.document.VocabularyItem;
import mitll.hlt.domino.shared.model.metadata.MetadataTypes.VocabularyMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.InputStream;
import java.util.*;

/**
 * @see ProjectManagement.MyExcelVocabularyImporter#importDocument(DocumentImporterBase.Command)
 */
public class ExcelReader extends BaseExcelReader<VocabularyItem> {
  private static final Logger log = LogManager.getLogger();

  private static final String CONTEXT_TRANSLATION = "context translation";
  private static final String TRANSLATION_OF_CONTEXT = "Translation of Context";
  private static final String CONTEXT = "context";

  private static final String ID = "np_id";
  private static final String UNIT = "unit";
  private static final String CHAPTER = "chapter";
  private static final String SEMESTER = "semester";
  private static final String WORD = "word";
  private static final String TOPIC = "topic";
  private static final String SUBTOPIC = "sub";
  private static final String GRAMMAR = "grammar";
  private static final String TRANSLITERATION = "transliteration";
  private static final String ALT = "alt";
  private static final String ALT_CONTEXT = "alt context sentence";

  private static final Map<String, String> grammarMap;
  private static final Map<String, String> topicMap;
  private static final Map<String, Map<String, String>> subtopicMaps;

  private MyVocabularyImportCommand cmd;

  private IDominoContext ctx;
  private String unitColumnHeader = UNIT;
  private String chapter = CHAPTER;

  ExcelReader(String fileName, IDominoContext ctx, MyVocabularyImportCommand cmd) {
    super(fileName);
    this.ctx = ctx;
    this.cmd = cmd;
  }

  /**
   * @param sheet
   * @return
   * @see #readRows(InputStream)
   */
  @Override
  protected Collection<VocabularyItem> readFromSheet(Sheet sheet, List<String> errors) {
    List<VocabularyItem> exercises = new ArrayList<>();
    boolean gotHeader = false;

    int transliterationIndex = cmd.getTransliterationIndex();
    int termIndex = cmd.getTermIndex();
    int meaningIndex = cmd.getMeaningIndex();
    int idIndex = cmd.getIdIndex();
    int contextIndex = cmd.getContextIndex();
    int contextTranslationIndex = cmd.getContextTranslationIndex();
    int unitIndex = cmd.getUnitIndex();
    int chapterIndex = cmd.getChapterIndex();
    int semesterIndex = cmd.getSemesterIndex();
    int topicIndex = cmd.getTopicIndex();
    int subtopicIndex = cmd.getSubtopicIndex();
    int grammarIndex = cmd.getGrammarIndex();
    int altIndex = cmd.getAltIndex();
    int altContextIndex = cmd.getAltContextIndex();

    List<String> lastRowValues = new ArrayList<>();
    Set<String> knownIds = new HashSet<>();

    try {
      Iterator<Row> iter = sheet.rowIterator();
      Map<Integer, Collection<CellRangeAddress>> rowToRanges = getRowToRanges(sheet);
      List<String> columns;

      for (; iter.hasNext(); ) {
        Row next = iter.next();
        //		if (id > maxExercises) break;
        boolean inMergedRow = rowToRanges.keySet().contains(next.getRowNum());

        if (!gotHeader) {
          columns = getHeader(next); // could be several junk rows at the top of the spreadsheet

          // When ignoring the header row, just assume we got it and move on.
          if (cmd.isIgnoreHeader()) {
            gotHeader = true;
          } else {
            // otherwise, look up the header details.
            log.info("Looking for columns in: {}", columns);
            for (String col : columns) {
              String colNormalized = col.toLowerCase();
              if (colNormalized.startsWith(WORD)) {
                gotHeader = true;
                meaningIndex = columns.indexOf(col);
                if (termIndex < 0) {
                  termIndex = meaningIndex + 1;
                }
              } else if (transliterationIndex < 0 && colNormalized.contains(TRANSLITERATION)) {
                transliterationIndex = columns.indexOf(col);
              } else if (idIndex < 0 && colNormalized.contains(ID)) {
                idIndex = columns.indexOf(col);
              } else if (contextTranslationIndex < 0 && contextTransMatch(colNormalized)) { //be careful of ordering wrt this and the next item
                contextTranslationIndex = columns.indexOf(col);
              } else if (contextIndex < 0 && colNormalized.contains(CONTEXT)) {
                contextIndex = columns.indexOf(col);
              } else {
                if (unitIndex < 0 && colNormalized.contains(unitColumnHeader)) {
                  unitIndex = columns.indexOf(col);
                } else {
                  if (chapterIndex < 0 && colNormalized.contains(chapter)) {
                    chapterIndex = columns.indexOf(col);
                  } else if (semesterIndex < 0 && colNormalized.contains(SEMESTER)) {
                    semesterIndex = columns.indexOf(col);
                  } else if (topicIndex < 0 && colNormalized.contains(TOPIC)) {
                    topicIndex = columns.indexOf(col);
                  } else if (subtopicIndex < 0 && colNormalized.contains(SUBTOPIC)) {
                    subtopicIndex = columns.indexOf(col);
                  } else if (grammarIndex < 0 && colNormalized.contains(GRAMMAR)) {
                    grammarIndex = columns.indexOf(col);
                  } else if (altIndex < 0 && colNormalized.contains(ALT)) {
                    altIndex = columns.indexOf(col);
                  } else if (altContextIndex < 0 && colNormalized.contains(ALT_CONTEXT)) {
                    altContextIndex = columns.indexOf(col);
                  }
                }
              }
            }
          }

          log.info("got header " + gotHeader + " term index=" + termIndex +
              "\n\tunit=" + unitIndex +
              "\n\tchapter=" + chapterIndex +
              "\n\tsemester=" + semesterIndex +
              "\n\tmeaning=" + meaningIndex +
              " transliterationIndex=" + transliterationIndex +
              " contextIndex=" + contextIndex +
              " topic=" + topicIndex + " subtopic=" + subtopicIndex +
              " alt=" + altIndex + " altContext=" + altContextIndex +
              " grammar=" + grammarIndex + " id=" + idIndex);
        } else {
          boolean isDelete = isDeletedRow(sheet, next, idIndex);
          String meaning = getCell(next, meaningIndex).trim();
          // remove starting or ending tics
          String foreignLanguagePhrase = cleanTics(getCell(next, termIndex).trim());
          String translit = getCell(next, transliterationIndex);
          //String firstVal = getCell(next, unitIndex);

          //log.info("for row " + next.getRowNum() + " english = " + english +
          // " in merged " + inMergedRow + " last row " + lastRowValues.size());

          if (inMergedRow && !lastRowValues.isEmpty()) {
            if (meaning.length() == 0) {
              meaning = lastRowValues.get(0);
            }
          }
          if (gotHeader && meaning.length() > 0) {
            if (inMergedRow) log.info("got merged row ------------ ");

            if (inMergedRow && !lastRowValues.isEmpty()) {
              if (foreignLanguagePhrase.length() == 0) {
                foreignLanguagePhrase = lastRowValues.get(1);
                //log.info("for row " + next.getRowNum() + " for foreign lang using " + foreignLanguagePhrase);
              }
            }
            if (foreignLanguagePhrase.length() == 0) {
              log.info("Got empty foreign language phrase row #" + next.getRowNum() + " for " + meaning);
              errors.add(sheet.getSheetName() + "/" + "row #" + (next.getRowNum() + 1) + " phrase was blank.");
//						} else if (skipSemicolons && (foreignLanguagePhrase.contains(";") || translit.contains(";"))) {
//							//semis++;
            } else {
              if (inMergedRow && !lastRowValues.isEmpty()) {
                if (translit.length() == 0) {
                  translit = lastRowValues.get(2);
                }
              }

              String idVal = idIndex != -1 ? getCell(next, idIndex) : "";
              String context = getCell(next, contextIndex);
              String contextTranslation = getCell(next, contextTranslationIndex);
              String unitVal = unitIndex != -1 ? getCell(next, unitIndex) : "";
              String chapterVal = chapterIndex != -1 ? getCell(next, chapterIndex) : "";
              String semesterVal = semesterIndex != -1 ? getCell(next, semesterIndex) : "";

              String topicVal = topicIndex != -1 ? getCell(next, topicIndex) : "";
              String subtopicVal = subtopicIndex != -1 ? getCell(next, subtopicIndex) : "";
              String grammarVal = grammarIndex != -1 ? getCell(next, grammarIndex) : "";
              String altVal = altIndex != -1 ? getCell(next, altIndex) : "";
              String altContextVal = altContextIndex != -1 ? getCell(next, altContextIndex) : "";

              topicVal = convertTopic(topicVal);
              subtopicVal = convertSubtopic(topicVal, subtopicVal);
              grammarVal = convertGrammar(grammarVal);

              VocabularyItem imported = isDelete ? null :
                  VocabularyItemFactory.create(ctx, idVal, unitVal, chapterVal, foreignLanguagePhrase,
                      meaning, translit, altVal, context, contextTranslation, altContextVal,
                      topicVal, subtopicVal, grammarVal);
              if (!isDelete) {

                imported.addMetadataField(new CStringMetadata("v-semester", semesterVal));

                String npId = getNPId(imported);
                if (knownIds.contains(npId)) {
                  log.warn("readFromSheet : found duplicate entry under " + npId + " " + imported);
                } else {
                  knownIds.add(npId);
                  exercises.add(imported);
                }
              }
              //else {
//                if (isDelete) {
//                  //deleted++;
//                } else {
//                  //skipped++;
//                }
              // }
              if (inMergedRow) {
                //log.debug("found merged row...");
                lastRowValues.add(meaning);
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
    } catch (Exception e) {
      log.error("got " + e, e);
    }

    return exercises;
  }

  private String convertGrammar(String grammar) {
    String grammarLC = grammar.toLowerCase();
    Optional<String> dominoGrammar = grammarMap.keySet().stream().filter(grammarLC::startsWith)
        .findFirst()
        .map(grammarMap::get);
    String s = dominoGrammar.orElse("");
    if (!grammar.isEmpty() && s.isEmpty()) {
      log.warn("skipping grammar '" + grammar + "'");
    }
    return s;
  }

  private String convertTopic(String topic) {
    String topicLC = topic.toLowerCase();
    Optional<String> dominoTopic = topicMap.keySet().stream().filter(topicLC::startsWith)
        .findFirst()
        .map(topicMap::get);
    String s = dominoTopic.orElse("");
    if (!topic.isEmpty() && s.isEmpty()) {
      log.warn("skipping topic " + topic);
    }
    return s;
  }

  private String convertSubtopic(String topic, String subtopic) {
    Map<String, String> subtopicM = subtopicMaps.get(topic);
    if (subtopicM != null) {
      String subtopicLC = subtopic.toLowerCase();
      Optional<String> dominoSubtopic = subtopicM.keySet().stream().filter(subtopicLC::startsWith)
          .findFirst()
          .map(subtopicM::get);
      String s = dominoSubtopic.orElse("");
      if (!topic.isEmpty() && s.isEmpty()) {
        log.warn("skipping subtopic " + subtopicLC);
      }
      return s;
    } else if (!topic.isEmpty()) {
      log.warn("skipping topic " + topic);
    }
    return "";
  }


  private boolean contextTransMatch(String colNormalized) {
    return colNormalized.contains(CONTEXT_TRANSLATION) || colNormalized.contains(TRANSLATION_OF_CONTEXT.toLowerCase());
  }

  private boolean isDeletedRow(Sheet sheet, Row next, int colIndex) {
    boolean isDelete = false;
    try {
      Cell cell = next.getCell(colIndex);
      if (cell != null) {
        CellStyle cellStyle = cell.getCellStyle();
        if (cellStyle != null) {
          isDelete = sheet.getWorkbook().getFontAt(cellStyle.getFontIndex()).getStrikeout();
        }
      }
    } catch (Exception e) {
      log.debug("got error reading delete strikeout at row " + next.getRowNum());
    }
    return isDelete;
  }

  private String getCell(Row next, int col) {
    if (col == -1) return "";
    Cell cell = next.getCell(col);
    if (cell == null) return "";
    if (cell.getCellTypeEnum() == CellType.NUMERIC) {
      double numericCellValue = cell.getNumericCellValue();
      if ((new Double(numericCellValue).intValue()) < numericCellValue)
        return "" + numericCellValue;
      else
        return "" + new Double(numericCellValue).intValue();
    } else if (cell.getCellTypeEnum() == CellType.STRING) {
      return cell.getStringCellValue().trim();
    } else {
      return cell.toString().trim();
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

  private String getNPId(VocabularyItem item) {
    return item.getMetadataField(VocabularyMetadata.V_NP_ID).getDisplayValue();
  }

  /**
   skipping subtopic agriculture
   skipping subtopic arts
   skipping subtopic banking
   skipping subtopic body parts
   skipping subtopic clothing
   skipping subtopic daily life
   skipping subtopic days, months, time
   skipping subtopic descriptions
   skipping subtopic directions
   skipping subtopic emergency terms
   skipping subtopic entertainment, media
   skipping subtopic family, relatives
   skipping subtopic general cultural
   skipping subtopic general economic
   skipping subtopic general geopgraphy
   skipping subtopic general military
   skipping subtopic general political
   skipping subtopic general security
   skipping subtopic greetings, introductions, farewells
   skipping subtopic health
   skipping subtopic helpful words, phrases
   skipping subtopic historical / famous
   skipping subtopic holidays, social events
   skipping subtopic housing, household goods
   skipping subtopic landmarks
   skipping subtopic law enforcement
   skipping subtopic leisure activities, sports
   skipping subtopic location
   skipping subtopic numbers
   skipping subtopic occupations
   skipping subtopic personal information
   skipping subtopic religion
   skipping subtopic shopping
   skipping subtopic signs
   skipping subtopic states, provinces
   skipping subtopic trade issues
   skipping subtopic traffic
   skipping subtopic violence
   skipping subtopic warfare
   */
  static {
    topicMap = new HashMap<>();
    topicMap.put("basics", "Basics");
    topicMap.put("cult", "Cultural & Social");
    topicMap.put("econ", "Economic & Political");
    topicMap.put("geog", "Geography & Environment");
    topicMap.put("mili", "Military & Security");
    topicMap.put("sci", "Scientific & Technological");

    subtopicMaps = new HashMap<>();

    Map<String, String> m = new HashMap<>();
    m.put("bio", "Biography & Anatomy");
    m.put("body", "Biography & Anatomy");
    m.put("clothing", "Biography & Anatomy");
    m.put("family", "Biography & Anatomy");
    m.put("personal", "Biography & Anatomy");

    m.put("everyday", "Everyday Vocabulary");
    m.put("descriptions", "Everyday Vocabulary");
    m.put("helpful", "Everyday Vocabulary");
    m.put("housing", "Everyday Vocabulary");
    m.put("expression", "Expressions, Cohesive Devices");
    m.put("greeting", "Expressions, Cohesive Devices");
    m.put("occupation", "Occupations");
    m.put("times", "Times, Colors, Numbers");
    m.put("daily", "Times, Colors, Numbers");
    m.put("days", "Times, Colors, Numbers");
    m.put("numbers", "Times, Colors, Numbers");
    subtopicMaps.put("Basics", m);

    m = new HashMap<>();
    m.put("customs", "Customs & Traditions");
    m.put("general cultural", "Customs & Traditions");
    m.put("daily", "Customs & Traditions");
    m.put("holidays", "Customs & Traditions");
    m.put("education", "Education & Training");
    m.put("food", "Food & Drink");
    m.put("humanities", "Humanities (Lang., Lit., Rel., Arts, etc.)");
    m.put("religion", "Humanities (Lang., Lit., Rel., Arts, etc.)");
    m.put("arts", "Humanities (Lang., Lit., Rel., Arts, etc.)");
    m.put("leisure", "Leisure & Entertainment");
    m.put("entertainment", "Leisure & Entertainment");
    m.put("shop", "Leisure & Entertainment");
    subtopicMaps.put("Cultural & Social", m);

    m = new HashMap<>();
    m.put("econ", "Economic & Business");
    m.put("general", "Economic & Business");
    m.put("agriculture", "Economic & Business");
    m.put("banking", "Economic & Business");
    m.put("shop", "Economic & Business");
    m.put("trade", "Economic & Business");

    m.put("ind", "Industry");
    m.put("legal", "Legal & Courts");
    m.put("polit", "Political & Government");
    m.put("general poli", "Political & Government");
    m.put("elections", "Political & Government");
    m.put("institutions", "Political & Government");
    m.put("international relation", "Political & Government");
    m.put("trans", "Transportation & Travel");
    subtopicMaps.put("Economic & Political", m);

    m = new HashMap<>();
    m.put("cities", "Cities, States, Countries");
    m.put("states", "Cities, States, Countries");
    m.put("countries", "Cities, States, Countries");
    m.put("climate", "Climate & Weather");
    m.put("agriculture", "Climate & Weather");
    m.put("direct", "Directions & Landmarks");
    m.put("location", "Directions & Landmarks");
    m.put("historical", "Directions & Landmarks");
    m.put("landmarks", "Directions & Landmarks");
    m.put("signs", "Directions & Landmarks");
    m.put("general env", "General Environment");
    m.put("general ge", "General Geography");
    subtopicMaps.put("Geography & Environment", m);

    m = new HashMap<>();
    m.put("crime", "Crime, Terrorism, Violence");
    m.put("terrorism", "Crime, Terrorism, Violence");
    m.put("viol", "Crime, Terrorism, Violence");
    m.put("mil", "Military & Warfare");
    m.put("general mil", "Military & Warfare");
    m.put("history", "Military & Warfare");
    m.put("warfare", "Military & Warfare");
    m.put("ranks", "Ranks");
    m.put("occupations", "Ranks");
    m.put("law", "Security & Law Enforcement");
    m.put("traffic", "Security & Law Enforcement");
    m.put("security", "Security & Law Enforcement");
    m.put("general sec", "Security & Law Enforcement");
    m.put("weaponry", "Weaponry & Equipment");
    subtopicMaps.put("Military & Security", m);

    m = new HashMap<>();
    m.put("sci", "Scientific & Technological");
    m.put("Research".toLowerCase(), "Scientific & Technological");

    m.put("comp", "Computer & Internet");
    m.put("general sci", "General Scientific");
    m.put("general tech", "General Technological");
    m.put("health", "Medical & Health");
    m.put("medical", "Medical & Health");
    m.put("emergency", "Medical & Health");
    m.put("natural", "Natural Sciences (Phy., Chem., Bio., etc.)");
    subtopicMaps.put("Scientific & Technological", m);

    // MUST BE LOWERCASE

    grammarMap = new HashMap<>();
    grammarMap.put("alpha", "Alphabet");
    grammarMap.put("interject", "Interjections");
    grammarMap.put("adj", "Adjectives");

    grammarMap.put("adv", "Adverbs");
    grammarMap.put("conj", "Conjunctions");
    grammarMap.put("caus", "Causative Verbs");
    grammarMap.put("trans", "Transitive Verbs");
    grammarMap.put("Verb_Transitive".toLowerCase(), "Transitive Verbs");
    grammarMap.put("intrans", "Intransitive Verbs");
    grammarMap.put("Verb_Intransitive".toLowerCase(), "Intransitive Verbs");
    grammarMap.put("nouns", "Nouns");
    grammarMap.put("noun", "Nouns");
    grammarMap.put("pro", "Pronouns");
    grammarMap.put("pre", "Prefixes/Suffixes");
    grammarMap.put("post", "Postpositions");

  }

  public void setUnitColumnHeader(String unitColumnHeader) {
    this.unitColumnHeader = unitColumnHeader.toLowerCase();
  }

  public void setChapter(String chapter) {
    this.chapter = chapter.toLowerCase();
  }
}
