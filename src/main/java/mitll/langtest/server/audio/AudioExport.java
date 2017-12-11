/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.audio;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.excel.ExcelExport;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.scoring.LTSFactory;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.MiniUser;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AudioExport {
  private static final Logger logger = LogManager.getLogger(AudioExport.class);

  private static final String MALE = "Male";
  private static final String FEMALE = "Female";

  private Collection<String> typeOrder;
  private final ServerProperties props;

  /**
   * @param props
   * @see DatabaseImpl#writeZip(OutputStream, Map, int, AudioExportOptions)
   */
  public AudioExport(ServerProperties props) {
    this.props = props;
  }

  /**
   * @param out
   * @param typeToSection
   * @param sectionHelper
   * @param exercisesForSelectionState
   * @param language
   * @param audioDAO
   * @param isDefectList
   * @throws Exception
   * @see DatabaseImpl#writeZip
   */
  public void writeZip(OutputStream out,
                       Map<String, Collection<String>> typeToSection,
                       ISection<?> sectionHelper,
                       Collection<CommonExercise> exercisesForSelectionState,
                       String language,
                       IAudioDAO audioDAO,
                       boolean isDefectList,
                       AudioExportOptions options,
                       boolean isEnglish) throws Exception {
    List<CommonExercise> copy = getSortedExercises(sectionHelper, exercisesForSelectionState, isEnglish);
    boolean skipAudio = typeToSection.isEmpty() && !options.isAllContext();

    logger.info("writeZip skip audio = " + skipAudio);

    writeToStream(copy, audioDAO,
        getPrefix(typeToSection, typeOrder),
        typeOrder,
        language,
        out,
        skipAudio, isDefectList, options);
  }

  /**
   * @param out
   * @param prefix
   * @param sectionHelper
   * @param exercisesForSelectionState
   * @param language1
   * @param audioDAO
   * @param isDefectList
   * @throws Exception
   * @see mitll.langtest.server.database.DatabaseImpl#writeUserListAudio
   */
  public void writeUserListAudio(OutputStream out,
                                 String prefix,
                                 ISection<?> sectionHelper,
                                 Collection<? extends CommonExercise> exercisesForSelectionState,
                                 String language1,
                                 IAudioDAO audioDAO,
                                 boolean isDefectList,
                                 AudioExportOptions options) throws Exception {
    List<CommonExercise> copy = getSortableExercises(sectionHelper, exercisesForSelectionState);
    new ExerciseSorter().sortByEnglish(copy, "");
    writeToStream(copy, audioDAO, prefix, typeOrder, language1, out, false,
        isDefectList, options);
  }

  /**
   * @param out
   * @param exercisesForSelectionState
   * @param audioDirectory
   * @param language
   * @throws Exception
   * @see mitll.langtest.server.database.DatabaseImpl#writeUserListAudio
   */
/*
  public void writeZipJustOneAudio(OutputStream out,
                                   Collection<? extends CommonExercise> exercisesForSelectionState,
                                   String audioDirectory,
                                   String language) throws Exception {
    List<CommonExercise> copy = new ArrayList<>(exercisesForSelectionState);
    new ExerciseSorter().sortByEnglish(copy, "");
    writeToStreamJustOneAudio(copy, audioDirectory, out, language);
  }
*/

  /**
   * @param sectionHelper
   * @param exercisesForSelectionState
   * @return
   * @see #writeZip
   */
  private List<CommonExercise> getSortedExercises(ISection<?> sectionHelper,
                                                  Collection<CommonExercise> exercisesForSelectionState,
                                                  boolean isEnglish) {
    List<CommonExercise> copy = getSortableExercises(sectionHelper, exercisesForSelectionState);
    new ExerciseSorter().getSorted(copy, false, isEnglish, "");
    return copy;
  }

  private List<CommonExercise> getSortableExercises(ISection<?> sectionHelper,
                                                    Collection<? extends CommonExercise> exercisesForSelectionState) {
    this.typeOrder = sectionHelper.getTypeOrder();
    return new ArrayList<>(exercisesForSelectionState);
  }

  /**
   * @param sectionHelper
   * @param typeToSection
   * @return
   * @see DatabaseImpl#getPrefix(Map, int)
   */
  public String getPrefix(ISection<?> sectionHelper, Map<String, Collection<String>> typeToSection) {
    return getPrefix(typeToSection, sectionHelper.getTypeOrder());
  }

  /**
   * @param typeToSection
   * @param typeOrder
   * @return
   * @seex #writeZip(OutputStream, Map, SectionHelper, Collection, String, AudioDAO, String, String, boolean, AudioExportOptions)
   * @see #getPrefix(ISection, Map)
   */
  private String getPrefix(Map<String, Collection<String>> typeToSection, Collection<String> typeOrder) {
    String prefix = "";
    for (String type : typeOrder) {
      Collection<String> selections = typeToSection.get(type);
      if (selections != null) {
        prefix += type + "_";
        for (String sel : selections) {
          prefix += sel + ",";
        }
        if (!selections.isEmpty()) prefix = prefix.substring(0, prefix.length() - 1);
        prefix += "_";
      }
    }
    if (prefix.isEmpty()) {
      prefix = "All";
    } else {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    return prefix;
  }

  /**
   * @param toWrite
   * @param audioDAO
   * @param name
   * @param typeOrder
   * @param language1
   * @param out
   * @param skipAudio
   * @param isDefectList
   * @throws Exception
   * @see #writeUserListAudio
   */
  private void writeToStream(Collection<CommonExercise> toWrite,
                             IAudioDAO audioDAO,
                             String name,
                             Collection<String> typeOrder,
                             String language1,
                             OutputStream out,
                             boolean skipAudio,
                             boolean isDefectList,
                             AudioExportOptions options) throws Exception {
    ZipOutputStream zOut = new ZipOutputStream(out);

    String baseName = baseName(name, language1).replaceAll(",", "_");
    String overallName = baseName + options.getInfo();
    //overallName = overallName.replaceAll("\\,", "_");

    logger.info("writeToStream overall name " + overallName);
    if (!skipAudio) {
      writeFolderContents(zOut,
          toWrite,
          audioDAO,
          overallName,
          isEnglish(language1),
          getCountryCode(language1),
          options,
          language1, options.isHasProjectSpecificAudio());
    } else {
      logger.info("writeToStream skip audio export.");
    }

    addSpreadsheetToZip(toWrite, typeOrder, language1, zOut, baseName, isDefectList);
  }

  private String baseName(String name, String language1) {
    return language1 + "_" + name;
  }

  /**
   * @param language1
   * @return
   * @see #writeToStream
   */
  private String getCountryCode(String language1) {
    return LTSFactory.getLocale(language1);
  }

  private void addSpreadsheetToZip(Collection<CommonExercise> toWrite, Collection<String> typeOrder,
                                   String language1, ZipOutputStream zOut, String overallName,
                                   boolean isDefectList) throws IOException {
    zOut.putNextEntry(new ZipEntry(overallName + File.separator + overallName + ".xlsx"));
    new ExcelExport(props).writeExcelToStream(toWrite, zOut, typeOrder, language1, isDefectList);
  }

  /**
   * @throws Exception
   * @paramx toWrite
   * @paramx audioDirectory
   * @paramx out
   * @paramx language
   * @seex #writeZipJustOneAudio
   */
/*  private void writeToStreamJustOneAudio(Collection<CommonExercise> toWrite,
                                         String audioDirectory,
                                         OutputStream out,
                                         String language) throws Exception {
    writeFolderContentsSimple(new ZipOutputStream(out), toWrite, audioDirectory, language);
  }*/
  private boolean isEnglish(String language1) {
    return language1.equalsIgnoreCase("English");
  }

  /**
   * @param zOut
   * @param toWrite
   * @param audioDAO
   * @param overallName
   * @param isEnglish
   * @param options
   * @param language
   * @param hasProjectSpecificAudio
   * @throws Exception
   * @see #writeToStream
   */
  private void writeFolderContents(ZipOutputStream zOut,
                                   Collection<CommonExercise> toWrite,
                                   IAudioDAO audioDAO,
                                   String overallName,
                                   boolean isEnglish,
                                   String countryCode,
                                   AudioExportOptions options,
                                   String language, boolean hasProjectSpecificAudio) throws Exception {
    //int c = 0;
    long then = System.currentTimeMillis();

    //logger.info("writeFolderContents overall name " + overallName);
    if (options.isAllContext()) {
      overallName += "_allContextAudio";
    }
    //logger.debug("found audio for " + exToAudio.size() + " items and writing " + toWrite.size() + " items ");
    // logger.debug("realContextPath " + realContextPath + " installPath " + installPath + " relativeConfigDir1 " +relativeConfigDir1);

    // get male and female majority users - map of user->childCount of recordings for this exercise
    Map<MiniUser, Integer> maleToCount = new HashMap<>();
    Map<MiniUser, Integer> femaleToCount = new HashMap<>();

    // attach audio
    int numAttach = attachAudio(toWrite, audioDAO, language, hasProjectSpecificAudio);

    boolean justContext = options.isJustContext() || options.isAllContext();
    if (!justContext) {
      populateGenderToCount(toWrite, maleToCount, femaleToCount);
    }
    // find the male and female with most recordings for this exercise
    MiniUser male = justContext ? null : getMaxUser(maleToCount);
    MiniUser female = justContext ? null : getMaxUser(femaleToCount);

    AudioConversion audioConversion = new AudioConversion(props.shouldTrimAudio(), props.getMinDynamicRange());

    int numMissing = 0;
    Set<String> names = new HashSet<>();

    logger.info("writeFolderContents " + toWrite.size() + " exercises, justContext = " + justContext);
    for (CommonExercise ex : toWrite) {
      boolean someAudio = false;

      // write male/female fast/slow
      if (justContext) {
        someAudio = someAudio ||
            copyContextAudioBothGenders(zOut, overallName, isEnglish, countryCode,
                audioConversion, names, ex, options.isJustMale(), language);
        if (options.isAllContext()) {
          someAudio = someAudio || copyContextAudioBothGenders(zOut, overallName, isEnglish, countryCode,
              audioConversion, names, ex, !options.isJustMale(), language);
        }
        if (!someAudio && numMissing < 20) {
          logger.warn("writeFolder : no context audio for exercise " + ex.getID() + " in " + ex.getAudioAttributes().size() + " attributes");
        }
        // if (someAudio) logger.info("found context for " + ex.getID());

      } else {
        MiniUser majorityUser = options.isJustMale() ? male : female;
        String speed = options.isJustRegularSpeed() ? AudioAttribute.REGULAR : AudioAttribute.SLOW;

        if (options.isUserList()) {
          AudioAttribute audioAttribute = getLatest(ex, true);
          if (audioAttribute != null) {
            copyAudioForExercise(zOut, overallName, isEnglish, audioConversion, names, ex, speed, audioAttribute, language);
            someAudio = true;
          } else {
            logger.info("writeFolder : no   male audio for " + ex.getID());
          }
          audioAttribute = getLatest(ex, false);
          if (audioAttribute != null) {
            copyAudioForExercise(zOut, overallName, isEnglish, audioConversion, names, ex, speed, audioAttribute, language);
            someAudio = true;
          } else {
            logger.info("writeFolder : no female audio for " + ex.getID());
          }
        } else {
          AudioAttribute recording = getAudioAttribute(majorityUser, ex, options.isJustMale(), speed);
          if (recording != null) {
            // logger.debug("found " + recording + " by " + recording.getUser());
            copyAudioForExercise(zOut, overallName, isEnglish, audioConversion, names, ex, speed, recording, language);
            someAudio = true;
          }
        }
      }

      if (!someAudio) {
        if (numMissing < 10) {
          logger.debug("writeFolderContents : no audio for exercise " + ex.getID());
        }
        numMissing++;
      }
    }
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > 1000) {
      logger.debug("writeFolderContents : " +
          "took " + diff + " millis " +
          "to export " + toWrite.size() + " items, " +
          "num with audio attached " + numAttach +
          " missing audio " + numMissing);
    }
    if (numMissing == numAttach) {
      logger.error("\nwriteFolderContents huh? no audio attached : " + numMissing);
    }
  }

  private AudioAttribute getLatest(CommonExercise ex, boolean isMale) {
    long latestTime = 0;
    AudioAttribute latest = null;
    for (AudioAttribute audioAttribute : ex.getAudioAttributes()) {
      if (audioAttribute.isValid() &&
          (isMale && audioAttribute.isMale()) ||
          (!isMale && !audioAttribute.isMale())
          ) {

        if (audioAttribute.getTimestamp() >= latestTime) {
          latest = audioAttribute;
          latestTime = audioAttribute.getTimestamp();
/*          logger.info("getLatest for " + ex.getID() + " male = " + isMale +
              " found new latest " + audioAttribute.getUniqueID() + " : " + audioAttribute);*/
        }
      }
    }

    return latest;
  }

  /**
   * @param zOut
   * @param overallName
   * @param isEnglish
   * @param audioConversion
   * @param names
   * @param ex
   * @param speed
   * @param audioAttribute
   * @param language
   * @throws IOException
   * @see #writeFolderContents(ZipOutputStream, Collection, IAudioDAO, String, boolean, String, AudioExportOptions, String, boolean)
   */
  private void copyAudioForExercise(ZipOutputStream zOut,
                                    String overallName,
                                    boolean isEnglish,
                                    AudioConversion audioConversion,
                                    Set<String> names,
                                    CommonExercise ex,
                                    String speed,
                                    AudioAttribute audioAttribute,
                                    String language) throws IOException {
    String name = overallName + File.separator + getUniqueName(ex, !isEnglish);
    copyAudio(zOut, names, name, speed, audioConversion, audioAttribute, ex.getID(), getTrackInfo(ex, audioAttribute,
        language));
  }

  private boolean copyContextAudioBothGenders(ZipOutputStream zOut,
                                              String overallName,
                                              boolean isEnglish,
                                              String countryCode,
                                              AudioConversion audioConversion,
                                              Set<String> names,
                                              CommonExercise ex,
                                              boolean justMale, String language) throws IOException {
    boolean someAudio = false;

    AudioAttribute latestContext = ex.getLatestContext(justMale);
    if (latestContext != null) {
      copyContextAudio(zOut, overallName, isEnglish, audioConversion, names, ex, latestContext, countryCode,
          language);
      someAudio = true;
    }
    return someAudio;
  }

  /**
   * @param toWrite
   * @param audioDAO
   * @param language
   * @return
   * @see #writeFolderContents
   */
  private int attachAudio(Collection<CommonExercise> toWrite, IAudioDAO audioDAO, String language, boolean hasProjectSpecificAudio) {
    int numAttach = 0;
    if (!toWrite.isEmpty()) {
      int projectID = toWrite.iterator().next().getProjectID();

      Map<Integer, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio(projectID, hasProjectSpecificAudio);
      for (CommonExercise ex : toWrite) {
        Collection<AudioAttribute> audioAttributes = exToAudio.get(ex.getID());
        if (audioAttributes != null) {
          audioDAO.attachAudio(ex, audioAttributes, language);
          numAttach++;
        }
      }
    }
    return numAttach;
  }

  private void populateGenderToCount(Collection<CommonExercise> toWrite,
                                     Map<MiniUser, Integer> maleToCount,
                                     Map<MiniUser, Integer> femaleToCount) {
    for (CommonExercise ex : toWrite) {
      for (AudioAttribute attr : ex.getAudioAttributes()) {
        MiniUser user = attr.getUser();

        Map<MiniUser, Integer> userToCount = (user.isMale()) ? maleToCount : femaleToCount;
        Integer count = userToCount.get(user);
        userToCount.put(user, (count == null) ? 1 : count + 1);
      }
    }
  }

  /**
   * ae_cntxt_c01_u01_e0003. Mp3
   * <p>
   * ae = Egyptian Arabic
   * contxt = context
   * c01 = Chapter 01
   * U01 = Unit 01
   * E0001 = entry one
   *
   * @param zOut
   * @param overallName
   * @param isEnglish
   * @param audioConversion
   * @param namesSoFar
   * @param ex
   * @param latestContext
   * @param language
   * @throws IOException
   */
  private void copyContextAudio(ZipOutputStream zOut,
                                String overallName,
                                boolean isEnglish,
                                AudioConversion audioConversion,
                                Set<String> namesSoFar,
                                CommonExercise ex,
                                AudioAttribute latestContext,
                                String countryCode,
                                String language) throws IOException {
    String speed = latestContext.getSpeed();
    String folder = overallName + File.separator;
    Map<String, String> unitToValue = ex.getUnitToValue();
    String name;

    int id = ex.getID();
    if (countryCode.isEmpty()) {
      name = folder + getUniqueName(ex, !isEnglish) + "_context";
    } else {
      // make a name that looks like: ae_cntxt_c01_u01_e0003. Mp3
      name = buildFileName(folder, countryCode, unitToValue, id);
    }
    copyAudio(zOut,
        namesSoFar,
        name,
        speed == null ? "" : speed,
        audioConversion,
        latestContext, id,
        getTrackInfo(ex, latestContext, language));
  }

  @NotNull
  private String buildFileName(String folder, String countryCode, Map<String, String> unitToValue, int id) {
    String name;
    StringBuilder builder = new StringBuilder();
    builder.append(countryCode);
    builder.append("_cntxt_");
    for (String type : typeOrder) {
      builder.append(type.toLowerCase().substring(0, 1));
      builder.append(unitToValue.get(type));
      builder.append("_");
    }
    builder.append("e");
    builder.append(id);
    name = folder + builder.toString();
    return name;
  }

  private TrackInfo getTrackInfo(CommonExercise ex, AudioAttribute latestContext, String language) {
    return new TrackInfo(ex.getForeignLanguage(), latestContext.getUser().getUserID(), ex.getEnglish(), language);
  }

  private MiniUser getMaxUser(Map<MiniUser, Integer> maleToCount) {
    MiniUser male = null;
    int mCount = 0;
    for (Map.Entry<MiniUser, Integer> pair : maleToCount.entrySet()) {
      if (male == null || pair.getValue() > mCount) {
        male = pair.getKey();
        mCount = pair.getValue();
      }
    }
    return male;
  }

  /**
   * @paramx zOut
   * @paramx toWrite
   * @paramx audioDirectory
   * @paramx language
   * @throws Exception
   * @seex #writeToStreamJustOneAudio(Collection, String, OutputStream, String)
   */
/*
  private void writeFolderContentsSimple(ZipOutputStream zOut,
                                         Collection<CommonExercise> toWrite,
                                         String audioDirectory,
                                         String language) throws Exception {
    logger.debug("writeFolderContentsSimple (" + language +
        ") writing " + toWrite.size() + " looking in " + audioDirectory);

    long then = System.currentTimeMillis();
    AudioConversion audioConversion = new AudioConversion(props.shouldTrimAudio(), props.getMinDynamicRange());

    int c = 0;
    int d = 0;
    for (CommonExercise ex : toWrite) {
      if (ex.getRefAudio() != null) {
        try {
          AudioAttribute audio = ex.getRegularSpeed();
          String artist = audio.getUser().getUserID();
          copyAudioSimple(zOut, audioConversion, audioDirectory, ex.getRefAudio(),
              new TrackInfo(ex.getForeignLanguage(), artist, ex.getEnglish(), language));
        } catch (IOException e) {
          //logger.debug("skipping duplicate " +e);
          d++;
        }
        c++;
      }
    }
    zOut.close();
    long now = System.currentTimeMillis();
    long diff = now - then;
    logger.debug("took " + diff + " millis to export " + toWrite.size() + " items " + c + " duplicates " + d);

    // if (diff > 1000) {
    //    logger.debug("took " + diff + " millis to export " + toWrite.size() + " items");
    // }
  }
*/

/*  private AudioAttribute getAudioAttribute(MiniUser majorityUser, CommonExercise ex, boolean isMale, boolean regularSpeed) {
    AudioAttribute recordingAtSpeed = majorityUser != null ? ex.getRecordingsBy(majorityUser.getID(), regularSpeed) : null;

    if (recordingAtSpeed == null) {  // can't find majority user or nothing by this person at that speed
      Collection<AudioAttribute> byGender = ex.getByGender(isMale);
      // choose the first one if not the majority majorityUser
      for (AudioAttribute attribute : byGender) {
        if ((attribute.isRegularSpeed() && regularSpeed) || (!attribute.isRegularSpeed() && !regularSpeed)) {
          recordingAtSpeed = attribute;
          break;
        }
      }
    }
    return recordingAtSpeed;
  }*/

  /**
   * Prefer recordings by the speaker with the most recordings.
   * If that person didn't do this cut, find someone with the matching gender and speed.
   *
   * @param majorityUser
   * @param ex
   * @param isMale
   * @param speed
   * @return
   * @see #writeFolderContents
   */
  private AudioAttribute getAudioAttribute(MiniUser majorityUser, CommonExercise ex, boolean isMale, String speed) {
    AudioAttribute recordingAtSpeed = majorityUser != null ? ex.getRecordingsBy(majorityUser.getID(), speed) : null;

    if (recordingAtSpeed == null) {  // can't find majority user or nothing by this person at that speed
      Collection<AudioAttribute> byGender = ex.getByGender(isMale);
      // choose the first one if not the majority majorityUser
      for (AudioAttribute attribute : byGender) {
        String speed1 = attribute.getSpeed();
        if (speed1 != null &&
            speed1.equalsIgnoreCase(speed)) {
          recordingAtSpeed = attribute;
          break;
        }
      }
    }
    return recordingAtSpeed;
  }

  /**
   * Worry about name collisions for entries composed of english-fl pairs
   *
   * @param includeFL true if not english
   * @param ex
   * @return
   * @see #writeFolderContents
   */
  private String getUniqueName(CommonExercise ex, boolean includeFL) {
    String trim = ex.getEnglish().trim();
    String foreignLanguage = ex.getForeignLanguage();

    String name = trim.equals("N/A") ? foreignLanguage : trim + (includeFL ? "_" + foreignLanguage : "");
    name = name.trim();
    name = name.replaceAll("\"", "\\'").replaceAll("\\?", "").replaceAll(":", "").replaceAll("/", " or ").replaceAll("\\\\", " or ");
    name = name.replaceAll(",", "_");
    return name;
  }

  /**
   * @param zOut
   * @param names           guarantees entries have unique names (sometimes duplicates in a chapter)
   * @param parent
   * @param speed
   * @param audioConversion
   * @param attribute
   * @param exid
   * @param trackInfo
   * @throws IOException
   * @see #copyAudio(ZipOutputStream, Set, String, String, AudioConversion, AudioAttribute, int, TrackInfo)
   * @see #copyContextAudio
   */
  private void copyAudio(ZipOutputStream zOut,
                         Set<String> names,
                         String parent,
                         String speed,
                         AudioConversion audioConversion,
                         AudioAttribute attribute,
                         int exid,
                         TrackInfo trackInfo) throws IOException {
    String s = ensureCompressedAudio(audioConversion, attribute, trackInfo);

    File mp3 = new File(s);
    if (mp3.exists()) {
      //  logger.debug("---> Did write " + mp3.getAbsolutePath());
      String name = getName(parent, speed, attribute);
      if (names.contains(name)) {
        name += "_" + exid;
      }

      boolean add = names.add(name);
      name = name.replaceAll(" ", "_");
      name += ".mp3";

      //logger.debug("copyContextAudio : mp3 name is " + name);
      if (add) {
        addZipEntry(zOut, mp3, name);
      } else {
        logger.info("skip duplicate " + name);
      }
    } else {
      String absolutePath = mp3.getAbsolutePath();
      if (!absolutePath.endsWith(AudioConversion.FILE_MISSING)) {
        logger.error("\tcopyAudio Didn't write " + absolutePath + " for " + exid);
      }
    }
  }

  private String ensureCompressedAudio(AudioConversion audioConversion, AudioAttribute attribute, TrackInfo trackInfo) {
    String audioRef = attribute.getActualPath();
    String baseAudioDir = getAudioDir(audioRef);
//    logger.debug("\tcopyAudio for ex id " +exid + " writing audio under context path " + baseAudioDir + " at " + audioRef);
    //String author = attribute.getUser().getUserID();
    return audioConversion.ensureWriteMP3(baseAudioDir, audioRef, false, trackInfo);
  }

  private String getAudioDir(String audioRef) {
    return audioRef.startsWith(ServerProperties.BEST_AUDIO) ? props.getAudioBaseDir() : props.getMediaDir();
  }

  /**
   * @return
   * @throws IOException
   * @paramx zOut
   * @paramx audioConversion
   * @paramx audioDirectory
   * @paramx audioRef
   * @paramx trackInfo
   * @seex #writeFolderContentsSimple
   */
/*  private void copyAudioSimple(ZipOutputStream zOut,
                               AudioConversion audioConversion,
                               String audioDirectory,
                               String audioRef,
                               TrackInfo trackInfo) {
    String filePath = audioConversion.ensureWriteMP3(audioDirectory, audioRef, false, trackInfo);
    File mp3 = new File(filePath);
    if (mp3.exists()) {
      String name = audioRef.replaceAll(".wav", ".mp3");
      //logger.debug("copyAudioSimple : mp3 name is " + name);
      try {
        addZipEntry(zOut, mp3, name);
      } catch (Exception e) {
        logger.debug("skip entry for audio " + name);
      }
    } else {
      logger.warn("\tDidn't write " + mp3.getAbsolutePath());
    }
  }*/
  private void addZipEntry(ZipOutputStream zOut, File mp3, String name) throws IOException {
    zOut.putNextEntry(new ZipEntry(name));
    FileUtils.copyFile(mp3, zOut);
    zOut.flush();
    zOut.closeEntry();
  }

  /**
   * @param parent
   * @param speed
   * @param attribute
   * @return
   * @see #copyContextAudio
   */
  private String getName(String parent, String speed, AudioAttribute attribute) {
    MiniUser user = attribute.getUser();
    String userInfo = user.isDefault() ?
        "" :
        (attribute.isMale() ? MALE : FEMALE);
    String speedSuffix = speed.equals(AudioAttribute.REGULAR) || speed.isEmpty() ? "" : "_" + speed;
    return parent + (user.isDefault() ? "" : "_" + userInfo) + speedSuffix;
  }
}
