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

package mitll.langtest.shared.exercise;

import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.server.audio.AudioExport;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.SimpleUser;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/6/13
 * Time: 6:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioExercise extends ExerciseShell {
  private final transient Logger logger = Logger.getLogger("AudioExercise");

  private static final String SPEED = "speed";
  private static final String REGULAR = "regular";
  private static final String SLOW = "slow";
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final String CONTEXT = "context";

  /**
   * NOTE : can't use concurrent hash map.
   */
  private Map<String, AudioAttribute> audioAttributes = new HashMap<>();
  private Map<String, ExerciseAnnotation> fieldToAnnotation = new HashMap<>();
  protected int projectid;

  public AudioExercise() {
  }

  /**
   * @param id
   * @param projectid
   * @param isContext
   * @paramx shouldSwap
   */
  AudioExercise(int id, int projectid, boolean isContext) {
    super(id, isContext);
    this.projectid = projectid;
  }

  public synchronized String getRefAudio() {
    AudioAttribute audio = getRegularSpeed();
    return audio != null ? audio.getAudioRef() : null;
  }

  /**
   * @param prefs
   * @return
   * @see mitll.langtest.server.json.JsonExport#addContextAudioRefs
   */
  public synchronized String getRefAudioWithPrefs(Collection<Integer> prefs) {
    AudioAttribute audio = getRegularSpeedWithPrefs(prefs);
    return audio != null ? audio.getAudioRef() : null;
  }

  /**
   * @return
   * @see mitll.langtest.client.scoring.DialogExercisePanel#getRegularSpeedIfAvailable
   */
  public synchronized AudioAttribute getRegularSpeed() {
    AudioAttribute audio = getAudio(SPEED, REGULAR);
    return audio == null ? getAudio(CONTEXT, REGULAR) : audio;
  }

  private AudioAttribute getRegularSpeedWithPrefs(Collection<Integer> prefs) {
    return getAudioPreferUsers(SPEED, REGULAR, prefs);
  }

  public synchronized String getSlowAudioRef() {
    AudioAttribute audio = getSlowSpeed();
    return audio != null ? audio.getAudioRef() : null;
  }

  /**
   * Latest recording by a user wins.
   *
   * @param audioAttribute
   * @see mitll.langtest.server.database.audio.BaseAudioDAO#attachAudioAndFixPath
   */
  public synchronized boolean addAudio(AudioAttribute audioAttribute) {
    if (audioAttribute == null) throw new IllegalArgumentException("adding null audio?");
    else {
      String key = audioAttribute.getKey();
      AudioAttribute currentByKey = audioAttributes.get(key);
      if (currentByKey == null || currentByKey.getTimestamp() < audioAttribute.getTimestamp()) {
        audioAttributes.put(key, audioAttribute);
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * CLIENT ONLY
   */
  public synchronized boolean clearRefAudio() {
    AudioAttribute audio = getRegularSpeed();
    if (audio != null) {
      return audioAttributes.remove(audio.getKey()) != null;
    } else {
      return false;
    }
  }

  /**
   * CLIENT ONLY
   */
  public synchronized void clearSlowRefAudio() {
    AudioAttribute audio = getSlowSpeed();
    if (audio != null) audioAttributes.remove(audio.getKey());
  }

  private AudioAttribute getSlowSpeed() {
    return getAudio(SPEED, SLOW);
  }

  /**
   * Get the first matching audio cut.
   * return the latest recording, or respecting gender.
   *
   * @param name
   * @param value
   * @return
   */
  private AudioAttribute getAudio(String name, String value) {
    AudioAttribute latest = null;
    for (AudioAttribute audio : getAudioAttributesLocal()) {
      if (audio.matches(name, value)) {
        if (latest == null || audio.getTimestamp() > latest.getTimestamp()) {
          latest = audio;
        }
      }
    }
    return latest;
  }

  private AudioAttribute getAudioPreferUsers(String name, String value, Collection<Integer> prefs) {
    AudioAttribute candidate = null;
    for (AudioAttribute audio : getAudioAttributesLocal()) {
      if (audio.matches(name, value)) {
        if (prefs.contains(audio.getUser().getID())) {
          return audio;
        } else {

          candidate = audio;
        }
      }
    }
    return candidate;
  }

  public synchronized boolean hasRefAudio() {
    return !audioAttributes.isEmpty();
  }

  public synchronized boolean hasAudioNonContext(boolean vocab) {
    return getAudioAttributesLocal()
        .stream()
        .anyMatch(audioAttribute -> (vocab == !audioAttribute.isContextAudio()));
  }

  public synchronized boolean hasContextAudio() {
    return getAudioAttributesLocal()
        .stream()
        .anyMatch(AudioAttribute::isContextAudio);
  }

  /**
   * @return
   */
  private Collection<AudioAttribute> getAudioAttributesLocal() {
    return audioAttributes.values();
  }

  public synchronized Collection<AudioAttribute> getAudioAttributes() {
    return new ArrayList<>(audioAttributes.values());
  }

  public synchronized Collection<AudioAttribute> getContextAudio() {
    return getAudioAttributesLocal()
        .stream()
        .filter(AudioAttribute::isContextAudio)
        .collect(Collectors.toList());
  }

  public synchronized AudioAttribute getFirst() {
    return audioAttributes.isEmpty() ? null : audioAttributes.values().iterator().next();
  }

  public synchronized Collection<String> getAudioPaths() {
    Collection<AudioAttribute> audioAttributes1 = getAudioAttributesLocal();
    Set<String> paths = new HashSet<>(audioAttributes1.size());
    for (AudioAttribute attr : audioAttributes1) {
      paths.add(attr.getAudioRef());
    }
    return paths;
    //return audioAttributes1.stream().map(AudioAttribute::getAudioRef).collect(Collectors.toSet());
  }

  /**
   * Get most recently recorded audio - maybe want highest score instead?
   * Prefer matching gender and matching speed.
   *
   * @param isMale    prefer gender match
   * @param isRegular prefer speed match
   * @return null only if no audio at all
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#hasAudio
   * @see IAudioDAO#getNativeAudio
   */
  public synchronized AudioAttribute getAudioAttributePrefGender(boolean isMale, boolean isRegular) {
    Collection<AudioAttribute> collect = getAudioPrefGender(isMale);

    Optional<AudioAttribute> max = collect
        .stream()
        .filter(p -> p.isRegularSpeed() && isRegular || p.isSlow() && !isRegular)
        .max((o1, o2) -> -1 * Long.compare(o1.getTimestamp(), o2.getTimestamp()));

    AudioAttribute audioAttribute = max.orElse(null);

    if (audioAttribute == null) {
      max = collect
          .stream()
          .max((o1, o2) -> -1 * Long.compare(o1.getTimestamp(), o2.getTimestamp()));
      audioAttribute = max.orElse(null);
    }

    return audioAttribute;
  }

  /**
   * @param isMale
   * @return
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#getContextPlay
   */
  public synchronized AudioAttribute getAudioAttrPrefGender(boolean isMale) {
    Collection<AudioAttribute> audioPrefGender = getAudioPrefGender(isMale);
    return audioPrefGender.isEmpty() ? null : audioPrefGender.iterator().next();
  }

  /**
   * Try to get matching gender but fall back to all audio if no match.
   *
   * @param isMale
   * @return
   */
  @NotNull
  private Collection<AudioAttribute> getAudioPrefGender(boolean isMale) {
    Collection<AudioAttribute> audioAttributes = getAudioAttributesLocal();

    // logger.info("getAudioPrefGender " + isMale + " " + audioAttributes.size());
    Collection<AudioAttribute> collect = audioAttributes
        .stream()
        .filter(p -> p.isMale() == isMale)
        .collect(Collectors.toList());

    // logger.info("getAudioPrefGender after " + isMale + " " + collect.size());
    if (collect.isEmpty()) {
      collect = audioAttributes;
    }
    //   logger.info("getAudioPrefGender return " + isMale + " " + collect.size());

    return collect;
  }

  /**
   * @param isMale true if by male speaker
   * @return
   * @see mitll.langtest.server.json.JsonExport#addContextAudioRefs
   */
  public synchronized AudioAttribute getLatestContext(boolean isMale) {
    long latestTime = 0;
    AudioAttribute latest = null;
    for (AudioAttribute audioAttribute : getAudioAttributesLocal()) {
      if (audioAttribute.getAudioType() == AudioType.CONTEXT_REGULAR &&
          ((isMale && audioAttribute.isMale()) || (!isMale && !audioAttribute.isMale()))
      ) {
        if (audioAttribute.getTimestamp() >= latestTime) {
          latest = audioAttribute;
          latestTime = audioAttribute.getTimestamp();
        }
      }
    }

    return latest;
  }

  public synchronized Map<String, AudioAttribute> getAudioRefToAttr() {
    Collection<AudioAttribute> audioAttributes = getAudioAttributesLocal();
    Map<String, AudioAttribute> audioToAttr = new HashMap<String, AudioAttribute>(audioAttributes.size());
    audioAttributes.forEach(attr -> audioToAttr.put(attr.getAudioRef(), attr));
    return audioToAttr;
  }


  private void sortByAge(List<AudioAttribute> males) {
    males.sort(Comparator.comparingInt(o -> {
      MiniUser user = o.getUser();
      return user == null ? 100 : user.getAge();
    }));
  }

  /**
   * @param userID
   * @param regularSpeed map fast and slow to regular
   * @return
   * @see RecordAudioPanel#getAudioAttribute
   * @see AudioExport#getAudioAttribute
   */
  public synchronized AudioAttribute getRecordingsBy(long userID, boolean regularSpeed) {
    List<AudioAttribute> recordingsBy = getRecordingsBy(userID);

    for (AudioAttribute attr : recordingsBy) {
      if (attr.isRegularSpeed() && regularSpeed || (attr.isSlow() && !regularSpeed)) {
        return attr;
      }
    }

    return null;
  }

  /**
   * @param userID
   * @param speed
   * @return
   * @see AudioExport#getAudioAttribute(MiniUser, CommonExercise, boolean, String)
   */
  public synchronized AudioAttribute getRecordingsBy(long userID, String speed) {
    List<AudioAttribute> recordingsBy = getRecordingsBy(userID);

    for (AudioAttribute attr : recordingsBy) {
      if (attr.getSpeed() != null && attr.getSpeed().equalsIgnoreCase(speed)) {
        return attr;
      }
    }

    return null;
  }

  private List<AudioAttribute> getRecordingsBy(long userID) {
    List<AudioAttribute> mine = new ArrayList<AudioAttribute>();
    for (AudioAttribute attr : getAudioAttributesLocal()) {
      if (attr.getUser() != null) {
        if (attr.getUser().getID() == userID) mine.add(attr);
      }
    }
    return mine;
  }

  /**
   * @param isMale
   * @param includeContext
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#addAudioByGender
   * List of audio is sorted to show regular before slow.
   */
  public synchronized Map<MiniUser, List<AudioAttribute>> getUserMap(boolean isMale, boolean includeContext) {
    Map<MiniUser, List<AudioAttribute>> userToAudio = getUserToAudio(isMale, includeContext);
    sortRegBeforeSlow(userToAudio);
    return userToAudio;
  }

  private void sortRegBeforeSlow(Map<MiniUser, List<AudioAttribute>> userToAudio) {
    for (List<AudioAttribute> lists : userToAudio.values()) {
      lists.sort((o1, o2) -> o1.isRegularSpeed() && o2.isSlow() ? -1 : o1.isSlow() && o2.isRegularSpeed() ? +1 : 0);
    }
  }

  /**
   * Skip context audio
   *
   * @param isMale
   * @param includeContext
   * @return
   * @see #getMostRecentAudio
   * @see #getUserMap(boolean, boolean)
   */
  private Map<MiniUser, List<AudioAttribute>> getUserToAudio(boolean isMale, boolean includeContext) {
    Map<MiniUser, List<AudioAttribute>> userToAudio = new HashMap<>();

    for (AudioAttribute attribute : getByGender(isMale)) {
      if (!attribute.getAttributeKeys().contains(CONTEXT) || includeContext) {
        List<AudioAttribute> audioAttributes1 = userToAudio.computeIfAbsent(attribute.getUser(), k -> new ArrayList<>());
        audioAttributes1.add(attribute);
      }
      //  else {
      //    logger.info("getUserToAudio : skipping context " + attribute);
      //  }
    }
    //  logger.info("getUserToAudio : ret " +isMale+ " for " + userToAudio);

    return userToAudio;
  }

  /**
   * @param isMale
   * @return
   * @see #getUserMap(boolean, boolean)
   * @see AudioExport#getAudioAttribute(MiniUser, CommonExercise, boolean, String)
   */
  public synchronized Collection<AudioAttribute> getByGender(boolean isMale) {
    List<AudioAttribute> males = simpleByGender(isMale);
    sortByAge(males);
    return males;
  }

  /**
   * @return
   * @see mitll.langtest.server.decoder.RefResultDecoder#ensure
   */
  public synchronized List<AudioAttribute> getDefaultUserAudio() {
    List<AudioAttribute> males = new ArrayList<AudioAttribute>();
    for (AudioAttribute audioAttribute : audioAttributes.values()) {
      if (audioAttribute.isValid()) {
        MiniUser user = audioAttribute.getUser();
        if (user != null && user.isUnknownDefault()) {
          males.add(audioAttribute);
        }
      }
    }
    return males;
  }

  /**
   * So if we can find one speaker of the gender who has done both regular and slow, take that person.
   * Otherwise take latest of regular and latest of slow.
   *
   * @param isMale
   * @param includeContext
   * @return
   * @see import mitll.langtest.client.scoring.ChoicePlayAudioPanel#addChoices
   */
  public synchronized List<AudioAttribute> getMostRecentAudioEasy(boolean isMale, boolean includeContext) {
    Map<Integer, List<AudioAttribute>> userToAudio = simpleByGenderByContext(isMale, includeContext);
    //logger.info("\tgetMostRecentAudio userToAudio " + userToAudio + "\n\tpref" + preferredVoices + "\n\tinclude "+includeContext);

    long bothTimestamp = 0;
    int bothLatestUser = -1;

    AudioAttribute latestReg = null;
    AudioAttribute latestSlow = null;

    for (Map.Entry<Integer, List<AudioAttribute>> userToAudioForUser : userToAudio.entrySet()) {
      boolean reg = false, slow = false;

      for (AudioAttribute audioAttribute : userToAudioForUser.getValue()) {
        if (!audioAttribute.isValid()) {
          logger.warning("getMostRecentAudioEasy skip invalid audio " + audioAttribute);
          continue;
        }

//        if (audioAttribute.getExid() == 9444) {
//          logger.info("got " + audioAttribute);
//        }

        if (audioAttribute.isRegularSpeed()) reg = true;
        if (audioAttribute.isSlow()) slow = true;

        long candidateTimestamp = audioAttribute.getTimestamp();

        if (reg && slow && bothTimestamp < candidateTimestamp) {
          bothTimestamp = candidateTimestamp;
          bothLatestUser = audioAttribute.getUserid();
        }

        if (reg) {
          if (latestReg == null || latestReg.getTimestamp() < candidateTimestamp) {
            latestReg = audioAttribute;

//            if (audioAttribute.getExid() == 9444) {
//              logger.info("\tnew latest  " + latestReg);
//            }
          }
        }
        if (slow) {
          if (latestSlow == null || latestSlow.getTimestamp() < candidateTimestamp) {
            latestSlow = audioAttribute;
          }
        }
      }
    }

    List<AudioAttribute> ret;
    if (bothLatestUser == -1) {
      ret = new ArrayList<>();
      if (latestReg != null) {
        ret.add(latestReg);
//        if (latestReg.getExid() == 9444) {
//          logger.info("\t latest reg  " + latestReg);
//        }
      }
      // else logger.warning("getMostRecentAudioEasy no reg  speed audio for " + getID());
      if (latestSlow != null) {
        ret.add(latestSlow);
      }
      // else logger.warning("getMostRecentAudioEasy no slow speed audio for " + getID());

    } else {
      ret = userToAudio.get(bothLatestUser);
    }
    return ret;
  }

  private Map<Integer, List<AudioAttribute>> simpleByGenderByContext(boolean isMale, boolean isContext) {
    Map<Integer, List<AudioAttribute>> userToAudio = new HashMap<>();
    for (AudioAttribute attribute : simpleByGender(isMale)) {
      boolean contextAudio = attribute.isContextAudio();
      if ((contextAudio && isContext) ||
          (!contextAudio && !isContext)) {
        List<AudioAttribute> audioAttributes1 = userToAudio.computeIfAbsent(attribute.getUserid(), k -> new ArrayList<>());
        audioAttributes1.add(attribute);
      }
      //  else {
      //    logger.info("getUserToAudio : skipping context " + attribute);
      //  }
    }
    return userToAudio;
  }

  @NotNull
  private List<AudioAttribute> simpleByGender(boolean isMale) {
    List<AudioAttribute> males = new ArrayList<>();
    for (AudioAttribute audioAttribute : audioAttributes.values()) {
      MiniUser user = audioAttribute.getUser();
      if (user == null) {
        //logger.error ("getByGender : huh? there's no user attached to " + audioAttribute);
      } else if (isMale && user.isMale() || (!isMale && !user.isMale())) {
        males.add(audioAttribute);
      }
    }
    return males;
  }

  /**
   * So we probably want the most recent recordings but bias first towards ones that have both fast and slow.
   * <p>
   * preferredVoices matches are more important than more recent recordings.
   * <p>
   * TODO : somehow associated preferred voices with a project?
   *
   * @param isMale
   * @param preferredVoices if we find audio from preferred voices, we will use it, no matter how old it is
   * @param includeContext
   * @return singleton map not containing default user -
   * @seex mitll.langtest.client.scoring.FastAndSlowASRScoringAudioPanel#getAfterPlayWidget
   */
  public synchronized Map<MiniUser, List<AudioAttribute>> getMostRecentAudio(boolean isMale,
                                                                             Collection<Integer> preferredVoices,
                                                                             boolean includeContext) {
    Map<MiniUser, List<AudioAttribute>> userToAudio = getUserToAudio(isMale, includeContext);

    //logger.info("\tgetMostRecentAudio userToAudio " + userToAudio + "\n\tpref" + preferredVoices + "\n\tinclude "+includeContext);

    long bothTimestamp = 0;
    long timestamp = 0;
    MiniUser bothLatest = null;
    MiniUser latest = null;
    for (Map.Entry<MiniUser, List<AudioAttribute>> pair : userToAudio.entrySet()) {
      boolean reg = false, slow = false;
      for (AudioAttribute audioAttribute : pair.getValue()) {

        if (!audioAttribute.isValid()) {
          logger.warning("skip invalid audio " + audioAttribute);
          continue;
        }

        MiniUser user = pair.getKey();
        //System.out.println("\t\tgetMostRecentAudio user " + user + "" + (user.isDefault() ? " DEFAULT " : ""));

        if (user.getID() != -1) { // these shouldn't get attached anyway
          if (audioAttribute.isRegularSpeed()) reg = true;
          if (audioAttribute.isSlow()) slow = true;

          long timestamp1 = audioAttribute.getTimestamp();
          if (reg && slow && bothTimestamp < timestamp1) {
            //  System.out.println("\t\tlatest is " + new Date(timestamp1));
            if (bothLatest == null || !preferredVoices.contains(bothLatest.getID())) {
              bothTimestamp = timestamp1;
              //           logger.info("\t\t\tlatest is " + new Date(bothTimestamp));
              bothLatest = user;
            }
          }
          if (timestamp <= timestamp1) {
            if (latest == null || !preferredVoices.contains(latest.getID())) {
              timestamp = timestamp1;
              latest = user;
            }
          }
        } else {
          logger.info("\t\tgetMostRecentAudio found default user " + user);
        }
      }
    }

    MiniUser toUse = bothLatest != null ? bothLatest : latest;

    Map<MiniUser, List<AudioAttribute>> userToAudioSingle = new HashMap<MiniUser, List<AudioAttribute>>();
    if (toUse == null && !userToAudio.isEmpty()) {
//      if (userToAudio.size() > 1 || defaultUser == null) {
//        System.err.println("AudioExercise.getMostRecentAudio : huh? user->audio map size=" + userToAudio.size() +
//            " was " + userToAudio + " but couldn't find latest user?");
//      }

    } else {
      List<AudioAttribute> value = userToAudio.get(toUse);
      if (value != null) {
        userToAudioSingle.put(toUse, value);
      }
    }

    sortRegBeforeSlow(userToAudioSingle);
    return userToAudioSingle;
  }

  /**
   * TODO : Sort by age - why would that be a good idea?
   *
   * @param malesMap
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#makeAudioRow()
   */
  public List<MiniUser> getSortedUsers(Map<MiniUser, List<AudioAttribute>> malesMap) {
    List<MiniUser> maleUsers = new ArrayList<MiniUser>(malesMap.keySet());
    maleUsers.sort(Comparator.comparing(SimpleUser::getUserID));
    return maleUsers;
  }

  /**
   * @param field
   * @param status
   * @param comment
   * @see IUserListManager#addAnnotations
   */
  public void addAnnotation(String field, String status, String comment) {
    fieldToAnnotation.put(field, new ExerciseAnnotation(status, comment));
  }

  public Map<String, ExerciseAnnotation> getFieldToAnnotation() {
    return fieldToAnnotation;
  }

  /**
   * @param fieldToAnnotation
   * @see Exercise#Exercise
   */
  void setFieldToAnnotation(Map<String, ExerciseAnnotation> fieldToAnnotation) {
    this.fieldToAnnotation = fieldToAnnotation;
  }

  public ExerciseAnnotation getAnnotation(String field) {
    if (!fieldToAnnotation.containsKey(field)) {
      if (field.endsWith(WAV)) {
        String key = field.replaceAll(WAV, MP3);
        return fieldToAnnotation.get(key);
      } else if (field.endsWith(MP3)) {
        String key = field.replaceAll(MP3, WAV);
        return fieldToAnnotation.get(key);
      }
    }
    return fieldToAnnotation.get(field);
  }

  public Collection<String> getFields() {
    return fieldToAnnotation.keySet();
  }

  public synchronized boolean removeAudio(AudioAttribute audioAttribute) {
    return audioAttributes.remove(audioAttribute.getKey()) != null;
  }

  public int getProjectID() {
    return projectid;
  }

  public void setProjectID(int projectid) {
    this.projectid = projectid;
  }

  /**
   * For instance for languages like serbo-croatian, where the same foreign item may have two different
   * forms.
   *
   * @param foreignLanguage
   */
  public void setAltFL(String foreignLanguage) {
    this.altfl = foreignLanguage;
  }

  public String toString() {
    return super.toString() +
        " project " + projectid +
        " audio attr (" + getAudioAttributesLocal().size() +
        ") :" + getAudioAttributesLocal() + " and " +
        fieldToAnnotation + " annotations";
  }
}