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
 *
 *
 */

package mitll.langtest.server.database.audio;

import com.google.common.base.CharMatcher;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioExport;
import mitll.langtest.server.audio.AudioExportOptions;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.user.MiniUser;
import mitll.npdata.dao.DBConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.ar.ArabicNormalizer;
import org.moxieapps.gwt.highcharts.client.Lang;

import java.io.File;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class BaseAudioDAO extends DAO {
  private static final Logger logger = LogManager.getLogger(BaseAudioDAO.class);

  private static final String UNKNOWN = "unknown";
  public static final String TOTAL = "total";
  public static final String TOTAL_CONTEXT = "totalContext";
  public static final String MALE = "male";
  public static final String FEMALE = "female";

  private static final String CMALE = "cmale";
  private static final String CFEMALE = "cfemale";

  public static final String MALE_FAST = "maleFast";
  public static final String MALE_SLOW = "maleSlow";
  public static final String FEMALE_FAST = "femaleFast";
  public static final String FEMALE_SLOW = "femaleSlow";

  public static final String CMALE_FAST = "cmaleFast";
  private static final String CMALE_SLOW = "cmaleSlow";
  public static final String CFEMALE_FAST = "cfemaleFast";
  private static final String CFEMALE_SLOW = "cfemaleSlow";

  private static final String TRANSLITERATION = "transliteration";
  private static final int WARN_DURATION = 25;

  protected final IUserDAO userDAO;
  private final int netProfDurLength;

  private static final boolean DEBUG_AUDIO_REPORT = false;
  private static final boolean DEBUG_ATTACH = false;
  private static final boolean DEBUG_ATTACH_PATH = false;

  /**
   * @param database
   * @param userDAO
   * @see SlickAudioDAO#SlickAudioDAO(Database, DBConnection, IUserDAO)
   */
  BaseAudioDAO(Database database, IUserDAO userDAO) {
    super(database);
    this.userDAO = userDAO;
    netProfDurLength = database.getServerProps().getAudioBaseDir().length();
  }

  /**
   * TODO : Seems really expensive - avoid doing this if we can.
   *
   * @param projid
   * @param hasProjectSpecificAudio
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#recalcAlignments(int, Project)
   * @see mitll.langtest.server.services.ScoringServiceImpl#getAllAudioIDs
   */
  public Map<Integer, List<AudioAttribute>> getExToAudio(int projid, boolean hasProjectSpecificAudio) {
    long then = System.currentTimeMillis();
    Map<Integer, List<AudioAttribute>> exToAudio = new HashMap<>();
    Map<Integer, Set<String>> idToPaths = new HashMap<>();
    //logger.info("getExToAudio - for project #" + projid);
    Collection<AudioAttribute> attributesByProject = getAudioAttributesByProjectThatHaveBeenChecked(projid, hasProjectSpecificAudio);
    logger.info("getExToAudio - for " + projid + " got " + attributesByProject.size());
    for (AudioAttribute audio : attributesByProject) {
      Integer exid = audio.getExid();
      List<AudioAttribute> audioAttributes = exToAudio.get(exid);
      Set<String> paths = idToPaths.get(exid);
      if (audioAttributes == null) {
        exToAudio.put(exid, audioAttributes = new ArrayList<>());
        idToPaths.put(exid, paths = new HashSet<>());
      }
      {
        String audioRef = audio.getAudioRef();
        if (!paths.contains(audioRef)) {
          audioAttributes.add(audio);
          paths.add(audioRef);
        }
      }
      //    else {
      //logger.warn("skipping " +audioRef + " on " + exid);
      //  }
    }

    long now = System.currentTimeMillis();
    logger.info("getExToAudio " +
        " project " + +projid +
        "(" + database.getProject(projid).getName() +
        ") took " + (now - then) + " millis to get  " + attributesByProject.size() + " audio entries " + this);
//    logger.debug("map size is " + exToAudio.size());
    return exToAudio;
  }

  public abstract Collection<AudioAttribute> getAudioAttributesByProjectThatHaveBeenChecked(int projid, boolean hasProjectSpecificAudio);

  /**
   * So we remember domino users for the lifetime of the app - maybe we should clear it periodically?
   * Or will we restart the server every day?
   * Don't want to keep hitting domino for user info...
   * <p>
   * Concurrent since could be multiple threads coming through.
   */
  final Map<Integer, MiniUser> idToMini = new ConcurrentHashMap<>();

  /**
   * TODO : consider why doing this all the time
   *
   * @param exercises
   * @param language
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getFullExercises
   */
  public <T extends ClientExercise> void attachAudioToExercises(Collection<T> exercises, Language language) {
    Set<Integer> exerciseIDs = exercises.stream().map(HasID::getID).collect(Collectors.toSet());
    logger.info("attachAudioToExercises to " + exercises.size() + " exercises for " +
        language + " : " + exerciseIDs.size());

    exercises.forEach(exercise -> exercise.getDirectlyRelated()
        .forEach(exercise1 -> exerciseIDs.add(exercise1.getID())));

    if (DEBUG_ATTACH) logger.info("attachAudioToExercises getting audio for " + new TreeSet<>(exerciseIDs));

    //  logger.info("attachAudioToExercises getting audio for " + exerciseIDs.size() + " id->mini " + idToMini.size());

    long then = System.currentTimeMillis();
    Map<Integer, List<AudioAttribute>> audioAttributesForExercises = getAudioAttributesForExercises(exerciseIDs, idToMini);
    long now = System.currentTimeMillis();
    if (now - then > 10) {
      logger.info("attachAudioToExercises took " + (now - then) + " millis to get audio attributes for " + exerciseIDs.size());
    }
    boolean doDEBUG = DEBUG_ATTACH;// || exercises.size() < 6;// || (id == 125524) || (id == 126304);

    for (ClientExercise exercise : exercises) {
      int id = exercise.getID();

      List<AudioAttribute> audioAttributes = audioAttributesForExercises.get(id);

      if (audioAttributes == null) {
        if (doDEBUG) logger.info("attachAudioToExercises no audio for " + id);
      } else {
        boolean attachedAll = attachAudio(exercise, audioAttributes, language, doDEBUG);

        if (doDEBUG) {
          logger.info("attachAudioToExercises for" +
              "\n\tex        # " + id +
              "\n\tattachedAll " + attachedAll +
              "\n\tattr        " + audioAttributes.size());
          for (AudioAttribute audioAttribute : audioAttributes) {
            logger.info("\tattachAudioToExercises for ex " + id + " attach " + audioAttribute);
          }
        }
      }

      addContextAudio(language, audioAttributesForExercises, exercise);
    }
    if (DEBUG_ATTACH) {
      logger.info("attachAudioToExercises finished attach to " + exercises.size() + " exercises for " +
          language + " : " + exerciseIDs.size());
    }
  }

  /**
   * @param language
   * @param audioAttributesForExercises
   * @param exercise
   * @see #attachAudioToExercises(Collection, Language)
   */
  private void addContextAudio(Language language,
                               Map<Integer, List<AudioAttribute>> audioAttributesForExercises,
                               ClientExercise exercise) {
    int id = exercise.getID();
    boolean doDEBUG = DEBUG_ATTACH;

    List<AudioAttribute> onlyContextFromParent = exercise.getAudioAttributes()
        .stream()
        .filter(AudioAttribute::isContextAudio)
        .collect(Collectors.toList());

    for (ClientExercise contextSentence : exercise.getDirectlyRelated()) {
      int contextID = contextSentence.getID();
      List<AudioAttribute> audioAttributes = audioAttributesForExercises.get(contextID);

      if (onlyContextFromParent.isEmpty()) {
        if (doDEBUG)
          logger.warn("addContextAudio for " + id + " and " + contextID + " found " + onlyContextFromParent.size() + " to attach ");
      } else {
        if (doDEBUG)
          logger.info("addContextAudio for " + id + " and " + contextID + " found " + onlyContextFromParent.size() + " to attach e.g. " + onlyContextFromParent.iterator().next().getTranscript());
        attachAudio(contextSentence, onlyContextFromParent, language, false);
      }

      if (audioAttributes != null) { // not sure when this would be true...
//        logger.info("addContextAudio found context audio for context exercise " + contextID + " " + audioAttributes.size());
        /*boolean attachedAll =*/
        attachAudio(contextSentence, audioAttributes, language, false);
      } else {
        if (doDEBUG)
          logger.info("addContextAudio no audio found for context parent exercise " + id + " and context " + contextID);
      }
    }
  }

  /**
   * Get the audio references from the database
   *
   * @param exids
   * @param idToMini
   * @return
   */
  abstract Map<Integer, List<AudioAttribute>> getAudioAttributesForExercises(Set<Integer> exids, Map<Integer, MiniUser> idToMini);

  /**
   * @param firstExercise
   * @param language
   * @see mitll.langtest.server.services.ExerciseServiceImpl#attachAudio
   * @see DatabaseImpl#writeUserListAudio(OutputStream, int, int, AudioExportOptions)
   */
  public int attachAudioToExercise(ClientExercise firstExercise, Language language, Map<Integer, MiniUser> idToMini) {
    long then = System.currentTimeMillis();
    int id = firstExercise.getID();
    Collection<AudioAttribute> audioAttributes = getAudioAttributesForExercise(id, idToMini);
    long now = System.currentTimeMillis();

    if (now - then > WARN_DURATION)
      logger.warn("attachAudioToExercise took " + (now - then) +
          " to get " + audioAttributes.size() + " attributes for ex #" + id);
/*    if (DEBUG) {
      logger.debug("\attachAudio : found " + audioAttributes.size() + " for " + firstExercise.getOldID());
      for (AudioAttribute attribute : audioAttributes) {
        logger.debug("\t\attachAudio : exid " + firstExercise.getOldID() + " audio " + attribute);
      }

      for (AudioAttribute attribute : firstExercise.getAudioAttributes()) {
        logger.debug("\t\tbefore attachAudio on ex : exid " + firstExercise.getOldID() + " audio " + attribute);
      }
    }*/

    then = now;
    boolean attachedAll = attachAudio(firstExercise, audioAttributes, language, false);
    now = System.currentTimeMillis();

    if (now - then > WARN_DURATION)
      logger.warn("attachAudioToExercise took " + (now - then) + " to attach audio to " + id);

    if (!attachedAll) {
      if (DEBUG_ATTACH) {
        logger.info("attachAudioToExercise didn't attach all audio to " + id + " " + firstExercise.getForeignLanguage());
      }

/*    if (DEBUG) {
      for (AudioAttribute attribute : firstExercise.getAudioAttributes()) {
        logger.debug("\t\tafter attachAudio : after on ex exid " + firstExercise.getOldID() + " audio " + attribute);
      }
      */
    }

    return audioAttributes.size();
  }

  /**
   * TODO : deal with possibility of audio being in either bestAudio or in answers...
   * <p>
   * TODOx : rewrite this so it's not insane -adding and removing attributes??
   * <p>
   * Complicated, but separates old school "Default Speaker" audio into a second pile.
   * If we've already added an audio attribute with the path for a default speaker, then we remove it.
   * <p>
   * TODO : why would we want to skip adding audio from the initial path set?
   *
   * @param firstExercise
   * @param audioAttributes
   * @param language
   * @param debug
   * @see AudioExport#writeFolderContents
   * @see #attachAudioToExercise
   * @see mitll.langtest.server.json.JsonExport#getJsonArray
   * @see
   */
  private boolean attachAudio(ClientExercise firstExercise,
                              Collection<AudioAttribute> audioAttributes,
                              Language language,
                              boolean debug) {
    boolean allSucceeded = true;

    Collection<Integer> currentIDs = getAudioIDs(firstExercise);

    String mediaDir = database.getServerProps().getMediaDir();
    boolean doDebug = debug || DEBUG_ATTACH;

    for (AudioAttribute attr : audioAttributes) {
      if (!currentIDs.contains(attr.getUniqueID())) {
        boolean didIt = attachAudioAndFixPath(firstExercise, mediaDir, attr, language, debug);
        if (didIt) {
//          logger.debug("\tadding path '" + attr.getAudioRef() + "' " + attr + " to " + firstExercise.getOldID());
          if (doDebug)
            logger.info("attachAudio attach audio " + attr.getUniqueID() +
                "\n\t" + (attr.isMale() ? "male" : "female") +
                "\n\tuserid " + attr.getUser().getUserID() +
                "\n\ttype " + attr.getAudioType() +
                "\n\tto exercise " + firstExercise.getID() +
                "\n\ttranscript  '" + attr.getTranscript() + "'");

        } else {
          if (doDebug && allSucceeded) {
            String foreignLanguage = attr.isContextAudio() && firstExercise.hasContext() ? firstExercise.getContext() : firstExercise.getForeignLanguage();
            logger.info("attachAudio not attaching audio " +
                "\n\tid          " + attr.getUniqueID() +
                "\n\tto exercise " + firstExercise.getID() +
                "\tsince transcript has changed : " +
                "\n\told     '" + attr.getTranscript() + "'" +
                "\n\t vs new '" + foreignLanguage + "'");
          }
          allSucceeded = false;
        }
      }
    }

    return allSucceeded;
  }

  private Collection<Integer> getAudioIDs(ClientExercise firstExercise) {
    Collection<AudioAttribute> audioAttributes1 = firstExercise.getAudioAttributes();
    synchronized (audioAttributes1) {
      return audioAttributes1.stream().map(AudioAttribute::getUniqueID).collect(Collectors.toSet());
    }
  }

  /**
   * TODO : Why does this have to be so complicated???
   * <p>
   * So this happens on the znetProf side where we don't have access to the actual file
   * We have confidence the file is there b/c we check it's existence every 24 hours, or on every startup of the
   * pnetProf instance.
   * <p>
   * TODO : not sure what to do if we have multiple context sentences...
   *
   * @param firstExercise
   * @param mediaDir
   * @param attr
   * @param language
   * @param debug
   * @return false if the text of the exercise and the transcript on the audio don't match
   * @see #attachAudio
   */
  private boolean attachAudioAndFixPath(ClientExercise firstExercise,
                                        String mediaDir,
                                        AudioAttribute attr,
                                        Language language, boolean debug) {
    Collection<ClientExercise> directlyRelated = firstExercise.getDirectlyRelated();
    boolean isContext = attr.isContextAudio();
    String exerciseText = isContext && !directlyRelated.isEmpty() ?
        directlyRelated.iterator().next().getForeignLanguage() :
        firstExercise.getForeignLanguage();
    String transcript = attr.getTranscript();

    boolean forgiving = language != Language.LEVANTINE;

    boolean isMatch = forgiving ? isMatchExToAudioArabic(attr, exerciseText) : matchTranscriptAttr(exerciseText, transcript);
    if (isMatch) {
      // add to both if context???
      if (DEBUG_ATTACH) {
        logger.info("attachAudioAndFixPath \n\tforgiving " + forgiving +
            "\n\texercise text '" + exerciseText + "'" +
            "\n\ttranscript    '" + transcript + "'" +
            "\n\tpath           " + attr.getAudioRef() +
            "\n\tuserid         " + attr.getUserid()+
            "\n\tuser           " + attr.getUser().getUserID()+
            "\n\taudio id       " + attr.getUniqueID()
        );
      }

      firstExercise.getMutableAudio().addAudio(attr);

      if (isContext) {

        for (ClientExercise dir : directlyRelated) {
          if (isMatchExToAudio(attr, dir, true)) {
            firstExercise.getMutableAudio().addAudio(attr);
            break;
          }
        }
      }

      String audioRef = attr.getAudioRef();
      if (audioRef == null)
        logger.error("attachAudioAndFixPath huh? no audio ref for " + attr + " under " + firstExercise);
      else {
        // so a path to the file on disk will now look like /opt/netProf/bestAudio/spanish/bestAudio/123/regular_XXX.wav
        // in the database we store just bestAudio/123/regular_XXX.wav

        // or if we store bestAudio/spanish/123/regular_YYY.wav ...? e.g. for newly recorded audio

        String lang = language.getLanguage();
        String prefix = mediaDir + File.separator + lang;
        String relPrefix = prefix.substring(netProfDurLength);
        if (!audioRef.contains(lang)) {
          if (DEBUG_ATTACH_PATH) {
            logger.info("attachAudioAndFixPath audioref " + audioRef + " does not contain '" + prefix +
                "' before " + attr.getAudioRef());
          }
          attr.setAudioRef(relPrefix + File.separator + audioRef);
          if (DEBUG_ATTACH_PATH) {
            logger.info("attachAudioAndFixPath : after " + attr.getAudioRef());
          }
        }
        if (DEBUG_ATTACH_PATH) {
          logger.debug("\tattachAudioAndFixPath now '" + attr.getAudioRef() + "'");
        }
      }
      return true;
    } else {
      boolean doDebug = debug || DEBUG_ATTACH;

      if (doDebug) {
        logger.info("attachAudioAndFixPath : not attaching audio " + attr.getUniqueID() + " to " + firstExercise.getID() + "/" + firstExercise.getOldID() + " since transcript has changed. " +
            "\n\tAudio          '" + attr.getTranscript() + "'" +
            "\n\tvs exercise    '" + exerciseText + "'" +
            "\n\tnorm Audio     '" + getNorm(attr.getTranscript()) + "'" +
            "\n\tnorm exercise  '" + getNorm(exerciseText) + "'" +
            "\n\tnorm2 Audio    '" + removePunct(getNorm(attr.getTranscript())) + "'" +
            "\n\tnorm2 exercise '" + removePunct(getNorm(exerciseText)) + "'" +
            "\n\tnorm3 Audio    '" + getNorm(removePunct(attr.getTranscript())) + "'" +
            "\n\tnorm3 exercise '" + getNorm(removePunct(exerciseText)) + "'"
        );
      }

      return false;
    }
  }

  private boolean isMatchExToAudio(AudioAttribute attr, CommonShell dir, boolean forgivingMatch) {
    String foreignLanguage = dir.getForeignLanguage();
    String transcript = attr.getTranscript();
    return forgivingMatch ? isMatchExToAudioArabic(attr, foreignLanguage) : isNoAccentMatch(foreignLanguage, transcript);
//    return isMatchExToAudio(attr, foreignLanguage);
  }

  /**
   * Todo : check if language is arabic before doing normArabic.
   *
   * @param foreignLanguage
   * @return
   * @paramx attr
   */
  private boolean isMatchExToAudioArabic(AudioAttribute attr, String foreignLanguage) {
    String normFL = getNorm(removePunct(foreignLanguage));
    String normT = getNorm(removePunct(attr.getTranscript()));
    return matchTranscriptAttr(normFL, normT);
  }

  private String getNorm(String foreignLanguage) {
    return StringUtils.stripAccents(normArabic(foreignLanguage, normalizer));
  }

  private final ArabicNormalizer normalizer = new ArabicNormalizer();

  private String normArabic(String f, ArabicNormalizer normalizer) {
    char[] s2 = f.toCharArray();
    normalizer.normalize(s2, f.length());
    return new String(s2);
  }

  abstract Collection<AudioAttribute> getAudioAttributesForExercise(int exid, Map<Integer, MiniUser> idToMini);

  /**
   * Get back the ids of exercises recorded by people who are the same gender as the userid.
   * <p>
   * TODO : don't do this - won't scale with users
   * TODO : use the transcript map
   *
   * @param userid         only used to determine the gender we should show
   * @param projid
   * @param exToTranscript
   * @return ids with both regular and slow speed recordings
   * @see mitll.langtest.server.services.ExerciseServiceImpl#filterByUnrecorded
   */
  public Collection<Integer> getRecordedBySameGender(int userid, int projid, Map<Integer, String> exToTranscript) {
  /*  return getAudioExercisesForGenderBothSpeeds(
        projid, userDAO.isMale(userid),
        AudioType.REGULAR.toString(),
        AudioType.SLOW.toString()
    );   */
    return getAudioExercisesForGenderBothSpeeds(
        projid,
        userDAO.isMale(userid),
        exToTranscript
    );
  }

  /**
   * @param projectid
   * @param exercises
   * @return
   * @see DatabaseImpl#getMaleFemaleProgress(int)
   */
  public Map<String, Float> getMaleFemaleProgress(int projectid, Collection<CommonExercise> exercises) {
    float total = exercises.size();
    Set<Integer> uniqueIDs = new HashSet<>();

    // int context = 0;
    Map<Integer, String> exToTranscript = new HashMap<>();
    Map<Integer, String> exToContextTranscript = new HashMap<>();

    for (CommonExercise shell : exercises) {
      {
        int exid = shell.getID();
        boolean add = uniqueIDs.add(exid);
        if (!add) {
          logger.warn("getMaleFemaleProgress found duplicate id " + exid + " : " + shell);
        }
        exToTranscript.put(exid, shell.getForeignLanguage());
      }
      {
        //  if (shell.hasContext()) context++;
        shell.getDirectlyRelated().forEach(commonExercise ->
            exToContextTranscript.put(commonExercise.getID(), commonExercise.getForeignLanguage()));
//        if (context == 20) logger.info("getMaleFemaleProgress " + exToContextTranscript);
      }
    }

//    logger.info("getMaleFemaleProgress found " + total + " total exercises, " +        uniqueIDs.size());// +
    // " unique" +
    // " males " + userMapMales.size() + " females " + userMapFemales.size());

    long then = System.currentTimeMillis();
    Map<String, Float> recordedReport = getRecordedReport(projectid, total, exToContextTranscript.size(), uniqueIDs,
        exToTranscript, exToContextTranscript);
    long now = System.currentTimeMillis();

    if (now - then > 200) logger.info("getRecordedReport for" +
        "\n\t project " + projectid +
        "\n\twith     " + exercises.size() + " exercises " +
        "\n\ttook     " + (now - then));

    return recordedReport;
  }

  /**
   * @param projid
   * @param total
   * @param exerciseIDs
   * @return
   * @see #getMaleFemaleProgress
   */
  private Map<String, Float> getRecordedReport(int projid,
                                               float total,
                                               float totalContext,
                                               Set<Integer> exerciseIDs,
                                               Map<Integer, String> exToTranscript,
                                               Map<Integer, String> exToContextTranscript) {
    Set<Integer> maleReg = new HashSet<>();
    Set<Integer> femaleReg = new HashSet<>();
    getCountForGender(projid, AudioType.REGULAR, exerciseIDs, exToTranscript, maleReg, femaleReg);

    float maleFast = (float) maleReg.size();
    if (DEBUG_AUDIO_REPORT) logger.info("getRecordedReport male fast " + maleFast);
    float femaleFast = (float) femaleReg.size();

    Set<Integer> maleSlowSpeed = new HashSet<>();
    Set<Integer> femaleSlowSpeed = new HashSet<>();
    getCountForGender(projid, AudioType.SLOW, exerciseIDs, exToTranscript, maleSlowSpeed, femaleSlowSpeed);
    float maleSlow = (float) maleSlowSpeed.size();
    float femaleSlow = (float) femaleSlowSpeed.size();

    if (DEBUG_AUDIO_REPORT) logger.info("getRecordedReport male slow " + maleSlow);

    maleReg.retainAll(maleSlowSpeed);
    float male = maleReg.size();
    if (DEBUG_AUDIO_REPORT) logger.info("male total " + male);

//    Set<Integer> femaleIDs = userMapFemales.keySet();
    //   femaleIDs = new HashSet<>(femaleIDs);
    //  femaleIDs.add(BaseUserDAO.DEFAULT_FEMALE_ID);
//    float femaleFast = getCountForGender(projid, AudioType.REGULAR, uniqueIDs, exToTranscript, femaleReg, , false);
//    float femaleSlow = getCountForGender(projid, AudioType.SLOW, uniqueIDs, exToTranscript, femaleSlowSpeed, , false);

    if (DEBUG_AUDIO_REPORT) logger.info("female fast " + femaleFast + " slow " + femaleSlow);

    // overlap
    femaleReg.retainAll(femaleSlowSpeed);
    float female = femaleReg.size();

    if (DEBUG_AUDIO_REPORT) logger.info("female total " + female);

    Set<Integer> conMaleReg = new HashSet<>();
    Set<Integer> conFemaleReg = new HashSet<>();


    // TODO : add male/female fast/slow for context
    getCountForGender(projid, AudioType.CONTEXT_REGULAR, exToContextTranscript.keySet(), exToContextTranscript, conMaleReg, conFemaleReg);
    // float cfemale = getCountForGender(projid, AudioType.CONTEXT_REGULAR, uniqueIDs, exToContextTranscript, conSlow, , false);

    float cmaleReg = conMaleReg.size();
    float cfemaleReg = conFemaleReg.size();

    Set<Integer> conMaleSlow = new HashSet<>();
    Set<Integer> conFemaleSlow = new HashSet<>();

    // TODO : add male/female fast/slow for context
    getCountForGender(projid, AudioType.CONTEXT_SLOW, exToContextTranscript.keySet(), exToContextTranscript, conMaleSlow, conFemaleSlow);


    conMaleReg.retainAll(conMaleSlow);
    float cmale = conMaleReg.size();

    conFemaleReg.retainAll(conFemaleSlow);
    float cfemale = conFemaleReg.size();


    float cmaleSlow = conMaleSlow.size();
    float cfemaleSlow = conFemaleSlow.size();

    if (DEBUG_AUDIO_REPORT) logger.info("cmale fast " + cmaleReg + " cfemale fast " + cfemaleReg);
    Map<String, Float> report = new HashMap<>();
    report.put(BaseAudioDAO.TOTAL, total);
    report.put(BaseAudioDAO.TOTAL_CONTEXT, totalContext);
    report.put(BaseAudioDAO.MALE, male);
    report.put(BaseAudioDAO.FEMALE, female);

    report.put(BaseAudioDAO.MALE_FAST, maleFast);
    report.put(BaseAudioDAO.MALE_SLOW, maleSlow);
    report.put(BaseAudioDAO.FEMALE_FAST, femaleFast);
    report.put(BaseAudioDAO.FEMALE_SLOW, femaleSlow);

    report.put(BaseAudioDAO.CMALE, cmale);
    report.put(BaseAudioDAO.CFEMALE, cfemale);

    report.put(BaseAudioDAO.CMALE_FAST, cmaleReg);
    report.put(BaseAudioDAO.CFEMALE_FAST, cfemaleReg);

    report.put(BaseAudioDAO.CMALE_SLOW, cmaleSlow);
    report.put(BaseAudioDAO.CFEMALE_SLOW, cfemaleSlow);
    return report;
  }

  /**
   * @param projid
   * @param audioSpeed
   * @param uniqueIDs
   * @param exToTranscript
   * @param idsOfRecordedExercisesForFemales
   * @return
   * @see #getRecordedReport
   */
  abstract void getCountForGender(int projid,
                                  AudioType audioSpeed,
                                  Set<Integer> uniqueIDs,
                                  Map<Integer, String> exToTranscript,
                                  Set<Integer> idsOfRecordedExercisesForMales,
                                  Set<Integer> idsOfRecordedExercisesForFemales);

  /**
   * Items that are recorded must have both regular and slow speed audio.
   *
   * @param userid
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#markRecordedState
   */
  public Collection<Integer> getRecordedExForUser(int userid) {
    try {
      Set<Integer> validAudioAtReg = getValidAudioOfType(userid, AudioType.REGULAR);
      Set<Integer> validAudioAtSlow = getValidAudioOfType(userid, AudioType.SLOW);
      validAudioAtReg.retainAll(validAudioAtSlow);
      return validAudioAtReg;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new HashSet<>();
  }

  /**
   * TODO make this a like "context=%"
   *
   * @param userid
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#markRecordedState
   */
  public Collection<Integer> getRecordedExampleForUser(int userid) {
    try {
      return getValidAudioOfType(userid, AudioType.CONTEXT_REGULAR);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new HashSet<>();
  }

  abstract Set<Integer> getValidAudioOfType(int userid, AudioType audioType);

  /**
   * @param userid
   * @param exerciseID
   * @param audioType
   * @return
   * @see IAudioDAO#addOrUpdate
   */
  protected AudioAttribute getAudioAttribute(int userid, int exerciseID, AudioType audioType, Map<Integer, MiniUser> idToMini) {
    AudioAttribute audioAttr = null;
    Collection<AudioAttribute> audioAttributes = getAudioAttributesForExercise(exerciseID, idToMini);
    //logger.debug("for  " +exerciseID + " found " + audioAttributes);

    for (AudioAttribute audioAttribute : audioAttributes) {
      AudioType audioType1 = audioAttribute.getAudioType();
      //logger.debug("\tfor  " +audioAttribute + " against " + userid + "/" + audioType  + " audio type " + audioType1);
      if (audioAttribute.getUserid() == userid && audioType1 == audioType) {
        //logger.debug("\tfound  " +audioAttribute + " for " + userid + "/" + audioType );
        audioAttr = audioAttribute;
        break;
      }
    }
    return audioAttr;
  }

  /**
   * @param i
   * @param userid
   * @param audioRef
   * @param exerciseID
   * @param timestamp
   * @param audioType
   * @param durationInMillis
   * @param transcript
   * @param dnr
   * @return
   * @see IAudioDAO#addOrUpdate
   * @deprecated
   */
/*  protected AudioAttribute getAudioAttribute(int i,
                                             int userid, String audioRef, int exerciseID, long timestamp,
                                             AudioType audioType, long durationInMillis, String transcript, float dnr) {
    MiniUser miniUser = userDAO.getMiniUser(userid);
    MiniUser.Gender realGender = miniUser.getRealGender();

    return new AudioAttribute(i, userid,
        exerciseID, // id
        audioRef, // answer
        timestamp,
        durationInMillis, audioType,
        miniUser, transcript,
        audioRef,
        dnr,
        -1, realGender);
  }*/

  /**
   * @param userExercise
   * @param fieldToAnnotation
   * @return
   * @see DatabaseImpl#editItem
   */
  public Set<AudioAttribute> getAndMarkDefects(AudioAttributeExercise userExercise,
                                               Map<String, ExerciseAnnotation> fieldToAnnotation) {
    Set<AudioAttribute> defects = new HashSet<>();

    for (Map.Entry<String, ExerciseAnnotation> fieldAnno : fieldToAnnotation.entrySet()) {
      ExerciseAnnotation value = fieldAnno.getValue();
      if (!value.isCorrect()) {  // i.e. defect
        String key = fieldAnno.getKey();
        AudioAttribute audioAttribute = userExercise.getAudioRefToAttr().get(key);
        if (audioAttribute != null) {
          logger.debug("getAndMarkDefects : found defect " + audioAttribute +
              " anno : " + value +
              " field  " + key);
          // logger.debug("\tmarking defect on audio");
          defects.add(audioAttribute);
          markDefect(audioAttribute);
        } else if (!key.equals(TRANSLITERATION)) {
          logger.warn("\tgetAndMarkDefects can't mark defect on audio : looking for field '" + key +
              "' in " + userExercise.getAudioRefToAttr().keySet());
        }
      }
    }

    return defects;
  }

  /**
   * @param attribute
   * @return
   * @see DatabaseImpl#markAudioDefect(AudioAttribute)
   */
  public int markDefect(AudioAttribute attribute) {
    return markDefect(attribute.getUserid(), attribute.getExid(), attribute.getAudioType());
  }

  /**
   * TODO : confirm this works...
   * <p>
   * Go back and mark gender on really old audio that had no user info on it.
   *
   * @param userid
   * @param projid
   * @param attr
   * @return
   * @see mitll.langtest.server.services.QCServiceImpl#markGender
   */
  public void addOrUpdateUser(int userid, int projid, AudioAttribute attr) {
    long timestamp = attr.getTimestamp();
    if (timestamp == 0) timestamp = System.currentTimeMillis();
    ServerProperties serverProps = database.getServerProps();
    boolean hasProjectSpecificAudio = database.getProject(projid).hasProjectSpecificAudio();
    float dnr = new AudioCheck(serverProps.shouldTrimAudio(), serverProps.getMinDynamicRange()).getDNR(new File(attr.getActualPath()));
    addOrUpdateUser(new AudioInfo(userid, attr.getExid(), projid, attr.getAudioType(), attr.getAudioRef(), timestamp,
        (int) attr.getDurationInMillis(), BaseAudioDAO.UNKNOWN, dnr, attr.getResultid(), attr.getRealGender(), hasProjectSpecificAudio)
    );
  }

  abstract void addOrUpdateUser(AudioInfo info);

  abstract int markDefect(int userid, int exerciseID, AudioType audioType);

  boolean isBadUser(int userid) {
    return userid < BaseUserDAO.DEFAULT_FEMALE_ID;
  }

  /**
   * @param userid
   * @param projid
   * @param exToTranscript
   * @return
   * @see mitll.langtest.server.database.exercise.FilterResponseHelper#getRecordedByMatchingGender
   * @see SlickAudioDAO#getContextAudioExercises
   */
  public Set<Integer> getRecordedBySameGenderContext(int userid, int projid, Map<Integer, String> exToTranscript) {
    return getContextAudioExercises(projid, userDAO.isMale(userid), exToTranscript);
  }


  abstract Set<Integer> getAudioExercisesForGender(boolean male,
                                                   String audioSpeed,
                                                   int projid);


  abstract Set<Integer> getAudioExercisesForGenderBothSpeeds(int projid,
                                                             boolean isMale,
                                                             Map<Integer, String> exToTranscript);

  abstract Set<Integer> getContextAudioExercises(int projid,
                                                 boolean isMale,
                                                 Map<Integer, String> exToContextTranscript);

  /**
   * @param transcript
   * @param exerciseFL
   * @return
   * @see SlickAudioDAO#getCountForGender
   */
  boolean isNoAccentMatch(String transcript, String exerciseFL) {
    if (exerciseFL == null) return false;

    String before = trimWhitespace(exerciseFL);
    String noAccents = StringUtils.stripAccents(before);

    String trimmed = trimWhitespace(transcript);
    String noAccentsTranscript = trimmed == null ? null : StringUtils.stripAccents(trimmed);

    return matchTranscript(before, trimmed) ||
        matchTranscript(noAccents, noAccentsTranscript);
  }

  private String trimWhitespace(String against) {
    return CharMatcher.WHITESPACE.trimFrom(against);
  }

  private boolean matchTranscript(String foreignLanguage, String transcript) {
    return transcript == null ||
        foreignLanguage.isEmpty() ||
        transcript.isEmpty() ||
        removePunct(transcript).toLowerCase().equals(removePunct(foreignLanguage).toLowerCase());
  }

  /**
   * TODO : don't duplicate code
   * remove punct before we get here.
   *
   * @return
   * @paramx foreignLanguage
   * @paramx transcript
   * @see mitll.langtest.server.database.audio.BaseAudioDAO#isMatchExToAudio
   */
  private boolean matchTranscriptAttr(String foreignLanguage, String transcript) {
    foreignLanguage = foreignLanguage.trim();
    if (transcript != null) {
      transcript = transcript.trim();
    }

    return transcript == null ||
        foreignLanguage.isEmpty() ||
        transcript.isEmpty() ||
        transcript.toLowerCase()
            .equals(
                foreignLanguage
                    .toLowerCase());
  }

  /**
   * Same as in audio attribute - ?
   *
   * @param t
   * @return
   * @see #matchTranscript
   */
  private String removePunct(String t) {
    return t
        .replaceAll("\\p{P}", "")
        .replaceAll("\\u2005", "")
        .replaceAll("\\s++", "");
  }
}
