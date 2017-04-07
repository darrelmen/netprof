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

import mitll.langtest.server.audio.AudioExport;
import mitll.langtest.shared.user.MiniUser;
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
  final transient Logger logger = Logger.getLogger("AudioExercise");

  private static final String SPEED = "speed";
  private static final String REGULAR = "regular";
  private static final String SLOW = "slow";
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final String CONTEXT = "context";
  String altfl = "";

  private Map<String, AudioAttribute> audioAttributes = new HashMap<String, AudioAttribute>();
  private Map<String, ExerciseAnnotation> fieldToAnnotation = new HashMap<String, ExerciseAnnotation>();
  protected int projectid;

  public AudioExercise() {
  }

  public AudioExercise(int id, int projectid) {
    super(id);
    this.projectid = projectid;
  }

  public String getRefAudio() {
    AudioAttribute audio = getRegularSpeed();
    return audio != null ? audio.getAudioRef() : null;
  }

  /**
   * @param prefs
   * @return
   * @see mitll.langtest.server.json.JsonExport#addContextAudioRefs
   */
  public String getRefAudioWithPrefs(Collection<Long> prefs) {
    AudioAttribute audio = getRegularSpeedWithPrefs(prefs);
    return audio != null ? audio.getAudioRef() : null;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#fixAudioPaths
   */
  public AudioAttribute getRegularSpeed() {
    return getAudio(SPEED, REGULAR);
  }

  public AudioAttribute getRegularSpeedWithPrefs(Collection<Long> prefs) {
    return getAudioPreferUsers(SPEED, REGULAR, prefs);
  }

  public String getSlowAudioRef() {
    AudioAttribute audio = getSlowSpeed();
    return audio != null ? audio.getAudioRef() : null;
  }

  public AudioAttribute getSlowSpeed() {
    return getAudio(SPEED, SLOW);
  }

  /**
   * @param s
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
   * @deprecated - try to avoid this
   */
  public void setRefAudio(String s) {
    if (s != null && s.length() > 0 && !s.equals("null")) {
      AudioAttribute audioAttribute = new AudioAttribute(s);
      addAudio(audioAttribute);
    }
  }

  public void addAudio(AudioAttribute audioAttribute) {
    if (audioAttribute == null) throw new IllegalArgumentException("adding null audio?");
    else {
      audioAttributes.put(audioAttribute.getKey(), audioAttribute);
    }
  }

  /**
   * @param ref
   * @param user
   * @see mitll.langtest.server.database.exercise.AttachAudio#addOldSchoolAudio
   */
  public void addAudioForUser(String ref, MiniUser user) {
    addAudio(new AudioAttribute(ref, user));
  }

  public void clearRefAudio() {
    AudioAttribute audio = getRegularSpeed();
    if (audio != null) audioAttributes.remove(audio.getKey());
  }

  public void clearSlowRefAudio() {
    AudioAttribute audio = getSlowSpeed();
    if (audio != null) audioAttributes.remove(audio.getKey());
  }

  /**
   * Get the first matching audio cut.
   * Doesn't worry about getting the latest one, or respecting gender.
   *
   * @param name
   * @param value
   * @return
   */
  AudioAttribute getAudio(String name, String value) {
    for (AudioAttribute audio : getAudioAttributes()) {
      if (audio.matches(name, value)) return audio;
    }
    return null;
  }

  AudioAttribute getAudioPreferUsers(String name, String value, Collection<Long> prefs) {
    AudioAttribute candidate = null;
    // long latest = 0;
    for (AudioAttribute audio : getAudioAttributes()) {
      if (audio.matches(name, value)) {
        if (prefs.contains((long) audio.getUser().getID())) {
          return audio;
        } else {

          candidate = audio;
        }
      }
    }
    return candidate;
  }

  public boolean hasRefAudio() {
    return !audioAttributes.isEmpty();
  }

  public Collection<AudioAttribute> getAudioAttributes() {
    return audioAttributes.values();
  }

  public AudioAttribute getAudioAttributePrefGender(boolean isMale, boolean isRegular) {
    Collection<AudioAttribute> collect = getAudioPrefGender(isMale);
    Optional<AudioAttribute> max = collect.stream()
        .filter(p -> p.isRegularSpeed() && isRegular || p.isSlow() && !isRegular)
        .max((o1, o2) -> -1 * Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp()));
    return max.orElse(null);
  }

  public AudioAttribute getAudioAttrPrefGender(boolean isMale) {
    Collection<AudioAttribute> audioPrefGender = getAudioPrefGender(isMale);
    return audioPrefGender.isEmpty() ? null : audioPrefGender.iterator().next();
  }

  @NotNull
  private Collection<AudioAttribute> getAudioPrefGender(boolean isMale) {
    Collection<AudioAttribute> audioAttributes = getAudioAttributes();
    Collection<AudioAttribute> collect = audioAttributes.stream().filter(p -> p.isMale() == isMale).collect(Collectors.toList());
    if (collect.isEmpty()) {
      collect = audioAttributes;
    }
    return collect;
  }

  /**
   * @param isMale true if by male speaker
   * @return
   * @see mitll.langtest.server.json.JsonExport#addContextAudioRefs
   */
  public AudioAttribute getLatestContext(boolean isMale) {
    long latestTime = 0;
    AudioAttribute latest = null;
    for (AudioAttribute audioAttribute : getAudioAttributes()) {
      if (audioAttribute.getAudioType().isContext() &&
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

  /*public AudioAttribute getLatest(boolean isMale) {
    long latestTime = 0;
    AudioAttribute latest = null;
    for (AudioAttribute audioAttribute : getAudioAttributes()) {
      if (
          (isMale && audioAttribute.isMale()) ||
              (!isMale && !audioAttribute.isMale())
          ) {

        if (audioAttribute.getTimestamp() >= latestTime) {
          latest = audioAttribute;
          latestTime = audioAttribute.getTimestamp();
        }
      }
    }

    return latest;
  }*/

  public Map<String, AudioAttribute> getAudioRefToAttr() {
    Map<String, AudioAttribute> audioToAttr = new HashMap<String, AudioAttribute>();
    for (AudioAttribute attr : getAudioAttributes()) audioToAttr.put(attr.getAudioRef(), attr);
    return audioToAttr;
  }

  /**
   * @param isMale
   * @return
   * @see #getUserMap(boolean, boolean)
   */
  public Collection<AudioAttribute> getByGender(boolean isMale) {
    List<AudioAttribute> males = new ArrayList<AudioAttribute>();
    for (AudioAttribute audioAttribute : audioAttributes.values()) {
      MiniUser user = audioAttribute.getUser();
      if (user == null) {
        //logger.error ("getByGender : huh? there's no user attached to " + audioAttribute);
      } else if (isMale && user.isMale() || (!isMale && !user.isMale())) {
        males.add(audioAttribute);
      }
    }

    sortByAge(males);
    return males;
  }

  public void sortByAge(List<AudioAttribute> males) {
    Collections.sort(males, new Comparator<AudioAttribute>() {
      @Override
      public int compare(AudioAttribute o1, AudioAttribute o2) {
        return o1.getUser().getAge() < o2.getUser().getAge() ? -1 : o1.getUser().getAge() > o2.getUser().getAge() ? +1 : 0;
      }
    });
  }

  /**
   * @param userID
   * @param regularSpeed map fast and slow to regular
   * @return
   * @see mitll.langtest.client.exercise.RecordAudioPanel#RecordAudioPanel
   * @see AudioExport#getAudioAttribute
   */
  public AudioAttribute getRecordingsBy(long userID, boolean regularSpeed) {
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
  public AudioAttribute getRecordingsBy(long userID, String speed) {
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
    for (AudioAttribute attr : getAudioAttributes()) {
      if (attr.getUser() != null) {
        if (attr.getUser().getID() == userID) mine.add(attr);
      }
//      else {
//        System.err.println("getRecordingsBy : Can't find user for " + attr);
//      }
    }
    return mine;
  }

  /**
   * @param isMale
   * @param includeContext
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#makeAudioRow()
   * List of audio is sorted to show regular before slow.
   */
  public Map<MiniUser, List<AudioAttribute>> getUserMap(boolean isMale, boolean includeContext) {
    Map<MiniUser, List<AudioAttribute>> userToAudio = getUserToAudio(isMale, includeContext);
    sortRegBeforeSlow(userToAudio);
    return userToAudio;
  }

  private void sortRegBeforeSlow(Map<MiniUser, List<AudioAttribute>> userToAudio) {
    for (List<AudioAttribute> lists : userToAudio.values()) {
      Collections.sort(lists, new Comparator<AudioAttribute>() {
        @Override
        public int compare(AudioAttribute o1, AudioAttribute o2) {
          return o1.isRegularSpeed() && o2.isSlow() ? -1 : o1.isSlow() && o2.isRegularSpeed() ? +1 : 0;
        }
      });
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
  public Map<MiniUser, List<AudioAttribute>> getUserToAudio(boolean isMale, boolean includeContext) {
    Map<MiniUser, List<AudioAttribute>> userToAudio = new HashMap<MiniUser, List<AudioAttribute>>();
    Collection<AudioAttribute> byGender = getByGender(isMale);

    for (AudioAttribute attribute : byGender) {
      if (!attribute.getAttributeKeys().contains(CONTEXT) || includeContext) {
        List<AudioAttribute> audioAttributes1 = userToAudio.get(attribute.getUser());
        if (audioAttributes1 == null)
          userToAudio.put(attribute.getUser(), audioAttributes1 = new ArrayList<AudioAttribute>());
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
   * @return
   * @see mitll.langtest.client.scoring.FastAndSlowASRScoringAudioPanel#getAfterPlayWidget
   */
  public List<AudioAttribute> getDefaultUserAudio() {
    List<AudioAttribute> males = new ArrayList<AudioAttribute>();
    for (AudioAttribute audioAttribute : audioAttributes.values()) {
      if (audioAttribute.isValid()) {
        MiniUser user = audioAttribute.getUser();
        if (user == null) {
          //System.err.println("getByGender : huh? there's no user attached to " + audioAttribute);
        } else if (user.isUnknownDefault()) {
          males.add(audioAttribute);
        }
      }
    }
    return males;
  }

  /**
   * So we probably want the most recent recordings but bias first towards ones that have both fast and slow.
   * <p>
   * preferredVoices matches are more important than more recent recordings.
   * <p>
   * TODO : somehow associated preferred voices with a project!
   *
   * @param isMale
   * @param preferredVoices if we find audio from preferred voices, we will use it, no matter how old it is
   * @param includeContext
   * @return singleton map not containing default user -
   * @see mitll.langtest.client.scoring.FastAndSlowASRScoringAudioPanel#getAfterPlayWidget
   */
  public Map<MiniUser, List<AudioAttribute>> getMostRecentAudio(boolean isMale,
                                                                Collection<Long> preferredVoices,
                                                                boolean includeContext) {
    Map<MiniUser, List<AudioAttribute>> userToAudio = getUserToAudio(isMale, includeContext);
    //logger.info("\tgetMostRecentAudio userToAudio " + userToAudio + "\n\tpref" + preferredVoices + "\n\tinclude "+includeContext);

    long bothTimestamp = 0;
    long timestamp = 0;
    MiniUser bothLatest = null;
    MiniUser latest = null;
    //MiniUser defaultUser = null;
    for (Map.Entry<MiniUser, List<AudioAttribute>> pair : userToAudio.entrySet()) {
      boolean reg = false, slow = false;
      for (AudioAttribute audioAttribute : pair.getValue()) {
        if (!audioAttribute.isValid()) continue;

        MiniUser user = pair.getKey();
        //System.out.println("\t\tgetMostRecentAudio user " + user + "" + (user.isDefault() ? " DEFAULT " : ""));

        if (user.getID() != -1) { // these shouldn't get attached anyway
          if (audioAttribute.isRegularSpeed()) reg = true;
          if (audioAttribute.isSlow()) slow = true;

          long timestamp1 = audioAttribute.getTimestamp();
          if (reg && slow && bothTimestamp < timestamp1) {
            //  System.out.println("\t\tlatest is " + new Date(timestamp1));
            if (bothLatest == null || !preferredVoices.contains((long) bothLatest.getID())) {
              bothTimestamp = timestamp1;
              //           logger.info("\t\t\tlatest is " + new Date(bothTimestamp));
              bothLatest = user;
            }
          }
          if (timestamp <= timestamp1) {
            if (latest == null || !preferredVoices.contains((long) latest.getID())) {
              timestamp = timestamp1;
              latest = user;
            }
          }
        } else {
          //         logger.info("\t\tgetMostRecentAudio found default user " + user);
          //   defaultUser = user;
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
   * @param malesMap
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#makeAudioRow()
   */
  public List<MiniUser> getSortedUsers(Map<MiniUser, List<AudioAttribute>> malesMap) {
    List<MiniUser> maleUsers = new ArrayList<MiniUser>(malesMap.keySet());
    Collections.sort(maleUsers, new Comparator<MiniUser>() {
      public int compare(MiniUser o1, MiniUser o2) {
        return o1.getAge() < o2.getAge() ? -1 : o1.getAge() > o2.getAge() ? +1 : 0;
      }
    });
    return maleUsers;
  }

  /**
   * @param field
   * @param status
   * @param comment
   * @see mitll.langtest.server.database.custom.UserListManager#addAnnotations
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
  protected void setFieldToAnnotation(Map<String, ExerciseAnnotation> fieldToAnnotation) {
    this.fieldToAnnotation = fieldToAnnotation;
  }

  public ExerciseAnnotation getAnnotation(String field) {
    if (!fieldToAnnotation.containsKey(field)) {
      if (field.endsWith(WAV)) {
        String key = field.replaceAll(WAV, MP3);
        ExerciseAnnotation exerciseAnnotation = fieldToAnnotation.get(key);
        return exerciseAnnotation;
      } else if (field.endsWith(MP3)) {
        String key = field.replaceAll(MP3, WAV);
        ExerciseAnnotation exerciseAnnotation = fieldToAnnotation.get(key);
        return exerciseAnnotation;
      }
    }
    return fieldToAnnotation.get(field);
  }

  public Collection<String> getFields() {
    return fieldToAnnotation.keySet();
  }

  public boolean removeAudio(AudioAttribute audioAttribute) {
    return audioAttributes.remove(audioAttribute.getKey()) != null;
  }

  public int getProjectID() {
    return projectid;
  }

  public void setProjectID(int projectid) {
    this.projectid = projectid;
  }

  /**
   * @return
   * @see mitll.langtest.client.custom.exercise.CommentNPFExercise#addAltFL
   */
  public String getAltFL() {
    return altfl;
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
        " audio attr (" + getAudioAttributes().size() +
        ") :" + getAudioAttributes() + " and " +
        fieldToAnnotation + " annotations";//, unit/lesson " + getUnitToValue();
  }

}
