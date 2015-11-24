/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/6/13
 * Time: 6:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioExercise extends ExerciseShell {
  private static final String SPEED = "speed";
  private static final String REGULAR = "regular";
  private static final String SLOW = "slow";
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final String CONTEXT = "context";

  private Map<String, AudioAttribute> audioAttributes = new HashMap<String, AudioAttribute>();
  private Map<String, String> unitToValue = new HashMap<String, String>();
  private Map<String, ExerciseAnnotation> fieldToAnnotation = new HashMap<String, ExerciseAnnotation>();

  public AudioExercise() {}
  public AudioExercise(String id) {
    super(id);
  }

  public String getRefAudio() {
    AudioAttribute audio = getRegularSpeed();
    return audio != null ? audio.getAudioRef() : null;
  }

  /**
   * @see mitll.langtest.server.DatabaseServlet#getJsonForExercise
   * @param prefs
   * @return
   */
  public String getRefAudioWithPrefs(Set<Long> prefs) {
    AudioAttribute audio = getRegularSpeedWithPrefs(prefs);
    return audio != null ? audio.getAudioRef() : null;
  }

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#fixAudioPaths
   * @return
   */
  public AudioAttribute getRegularSpeed() {  return getAudio(SPEED, REGULAR); }

  public AudioAttribute getRegularSpeedWithPrefs(Set<Long> prefs) {
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
   * @see mitll.langtest.server.database.exercise.ExcelImport#getExercise
   * @param s
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
   * @see mitll.langtest.server.database.exercise.ExcelImport#addOldSchoolAudio(String, Exercise)
   * @param ref
   * @param user
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

  AudioAttribute getAudioPreferUsers(String name, String value, Set<Long> prefs) {
    AudioAttribute candidate = null;
   // long latest = 0;
    for (AudioAttribute audio : getAudioAttributes()) {
      if (audio.matches(name, value)) {
        if (prefs.contains(audio.getUser().getId())) {
          return audio;
        }
        else {

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

  /**
   * @return
   * @see mitll.langtest.server.DatabaseServlet#getJsonForExercise(CommonExercise)
   */
  public AudioAttribute getLatestContext(boolean isMale) {
    long latestTime = 0;
    AudioAttribute latest = null;
    for (AudioAttribute audioAttribute : getAudioAttributes()) {
      if (audioAttribute.getAudioType().startsWith(CONTEXT) &&
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

  public Map<String, AudioAttribute> getAudioRefToAttr() {
    Map<String, AudioAttribute> audioToAttr = new HashMap<String, AudioAttribute>();
    for (AudioAttribute attr : getAudioAttributes()) audioToAttr.put(attr.getAudioRef(), attr);
    return audioToAttr;
  }

  /**
   * @see #getUserMap(boolean)
   * @param isMale
   * @return
   */
  public Collection<AudioAttribute> getByGender(boolean isMale) {
    List<AudioAttribute> males = new ArrayList<AudioAttribute>();
    for (AudioAttribute audioAttribute : audioAttributes.values()) {
      MiniUser user = audioAttribute.getUser();
      if (user == null) {
        System.err.println ("getByGender : huh? there's no user attached to " + audioAttribute);
      }
      else if (isMale && user.isMale() || (!isMale && !user.isMale())) {
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
   * @see mitll.langtest.client.exercise.RecordAudioPanel#RecordAudioPanel
   * @param userID
   * @param regularSpeed map fast and slow to regular
   * @return
   * @see mitll.langtest.server.database.AudioExport#getAudioAttribute
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
   * @see mitll.langtest.server.database.AudioExport#getAudioAttribute(MiniUser, CommonExercise, boolean, String)
   * @param userID
   * @param speed
   * @return
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
        if (attr.getUser().getId() == userID) mine.add(attr);
      }
      else {
        System.err.println("getRecordingsBy : Can't find user for " + attr);
      }
    }
    return mine;
  }

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#makeAudioRow()
   * List of audio is sorted to show regular before slow.
   *
   * @param isMale
   * @return
   */
  public Map<MiniUser, List<AudioAttribute>> getUserMap(boolean isMale) {
    Map<MiniUser, List<AudioAttribute>> userToAudio = getUserToAudio(isMale);

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
   * @param isMale
   * @return
   * @see #getMostRecentAudio(boolean, java.util.Set)
   * @see #getUserMap(boolean)
   */
  private Map<MiniUser, List<AudioAttribute>> getUserToAudio(boolean isMale) {
    Map<MiniUser, List<AudioAttribute>> userToAudio = new HashMap<MiniUser, List<AudioAttribute>>();
    Collection<AudioAttribute> byGender = getByGender(isMale);

    for (AudioAttribute attribute : byGender) {
      if (!attribute.getAttributeKeys().contains(CONTEXT)) {
        List<AudioAttribute> audioAttributes1 = userToAudio.get(attribute.getUser());
        if (audioAttributes1 == null)
          userToAudio.put(attribute.getUser(), audioAttributes1 = new ArrayList<AudioAttribute>());
        audioAttributes1.add(attribute);
      }
    //  else {
    //    System.out.println("getUserToAudio : skipping context " + attribute);
    //  }
    }
  //  System.out.println("getUserToAudio : ret " +isMale+ " for " + userToAudio);

    return userToAudio;
  }

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.FastAndSlowASRScoringAudioPanel#getAfterPlayWidget()
   * @return
   */
  public List<AudioAttribute> getDefaultUserAudio() {
    List<AudioAttribute> males = new ArrayList<AudioAttribute>();
    for (AudioAttribute audioAttribute : audioAttributes.values()) {
      MiniUser user = audioAttribute.getUser();
      if (user == null) {
        System.err.println("getByGender : huh? there's no user attached to " + audioAttribute);
      } else if (user.isUnknownDefault()) {
        males.add(audioAttribute);
      }
    }
    return males;
  }

  /**
   * So we probably want the most recent recordings but bias first towards ones that have both fast and slow.
   *
   * @param isMale
   * @param preferredUsers
   * @return singleton map not containing default user -
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.FastAndSlowASRScoringAudioPanel#getAfterPlayWidget()
   */
  public Map<MiniUser, List<AudioAttribute>> getMostRecentAudio(boolean isMale, Set<Long> preferredUsers) {
    Map<MiniUser, List<AudioAttribute>> userToAudio = getUserToAudio(isMale);
    //System.out.println("\tgetMostRecentAudio userToAudio " + userToAudio + " " + preferredUsers);

    long bothTimestamp = 0;
    long timestamp = 0;
    MiniUser bothLatest = null;
    MiniUser latest = null;
    MiniUser defaultUser = null;
    for (Map.Entry<MiniUser, List<AudioAttribute>> pair : userToAudio.entrySet()) {
      boolean reg = false, slow = false;
      for (AudioAttribute audioAttribute : pair.getValue()) {
        MiniUser user = pair.getKey();
        //System.out.println("\t\tgetMostRecentAudio user " + user + "" + (user.isDefault() ? " DEFAULT " : ""));

        if (user.getId() != -1) {
          if (audioAttribute.isRegularSpeed()) reg = true;
          if (audioAttribute.isSlow()) slow = true;

          long timestamp1 = audioAttribute.getTimestamp();
          if (reg && slow && bothTimestamp < timestamp1) {
          //  System.out.println("\t\tlatest is " + new Date(timestamp1));
            if (bothLatest == null || !preferredUsers.contains(bothLatest.getId())) {
              bothTimestamp = timestamp1;
            //  System.out.println("\t\t\tlatest is " + new Date(bothTimestamp));
              bothLatest = user;
            }
          }
          if (timestamp <= timestamp1) {
            if (latest == null || !preferredUsers.contains(latest.getId())) {
              timestamp = timestamp1;
              latest = user;
            }
          }
        } else {
          //System.out.println("\t\tgetMostRecentAudio found default user " + user);
          defaultUser = user;
        }
      }
    }

    MiniUser toUse = bothLatest != null ? bothLatest : latest;

  //  System.out.println("\tgetMostRecentAudio toUse " + toUse);

    Map<MiniUser, List<AudioAttribute>> userToAudioSingle = new HashMap<MiniUser, List<AudioAttribute>>();
    if (toUse == null && !userToAudio.isEmpty()) {
      if (userToAudio.size() > 1 || defaultUser == null) {
        System.err.println("AudioExercise.getMostRecentAudio : huh? user->audio map size=" + userToAudio.size() +
            " was " + userToAudio + " but couldn't find latest user?");
      }
    }
    else {
      List<AudioAttribute> value = userToAudio.get(toUse);
      if (value != null) {
        userToAudioSingle.put(toUse, value);
      }
    }

    sortRegBeforeSlow(userToAudioSingle);
//    System.out.println("\tgetMostRecentAudio userToAudioSingle " + userToAudioSingle);
    return userToAudioSingle;
  }

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#makeAudioRow()
   * @param malesMap
   * @return
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
   * @see mitll.langtest.server.database.custom.UserListManager#addAnnotations
   * @param field
   * @param status
   * @param comment
   */
  public void addAnnotation(String field, String status, String comment) {
    fieldToAnnotation.put(field, new ExerciseAnnotation(status,comment));
  }

  public Map<String, ExerciseAnnotation> getFieldToAnnotation() {  return fieldToAnnotation;  }
  public void setFieldToAnnotation(Map<String, ExerciseAnnotation> fieldToAnnotation) { this.fieldToAnnotation = fieldToAnnotation; }

  public ExerciseAnnotation getAnnotation(String field) {
    if (!fieldToAnnotation.containsKey(field)) {
      if (field.endsWith(WAV)) {
        String key = field.replaceAll(WAV, MP3);
        ExerciseAnnotation exerciseAnnotation = fieldToAnnotation.get(key);
        //if (exerciseAnnotation == null && !fieldToAnnotation.isEmpty()) {
        //  System.out.println("getAnnotation : Can't find " + field + " in "+ fieldToAnnotation.keySet());
        //}

        return exerciseAnnotation;
      }
      else if (field.endsWith(MP3)) {
        String key = field.replaceAll(MP3, WAV);
        ExerciseAnnotation exerciseAnnotation = fieldToAnnotation.get(key);
   //     if (exerciseAnnotation == null && !fieldToAnnotation.isEmpty()) {
//          System.out.println("getAnnotation : Can't find " + field + " in "+ fieldToAnnotation.keySet());
     //   }

        return exerciseAnnotation;
      }
    }
    return fieldToAnnotation.get(field);
  }

  public Collection<String> getFields() { return fieldToAnnotation.keySet(); }

  public Map<String, String> getUnitToValue() { return unitToValue; }

  /**
   * @see mitll.langtest.server.database.exercise.SectionHelper#addExerciseToLesson
   * @param unit
   * @param value
   */
  public void addUnitToValue(String unit, String value) {
    if (value == null) return;
    if (value.isEmpty()) {
      System.out.println("addUnitToValue " + unit + " value " + value);
    }
    this.getUnitToValue().put(unit, value);
  }
  public void setUnitToValue(Map<String, String> unitToValue) {
    this.unitToValue = unitToValue;
  }

  public int getNumAudio() {
    return getAudioAttributes().size();
  }

  public boolean removeAudio(AudioAttribute audioAttribute) {
    return audioAttributes.remove(audioAttribute.getKey()) != null;
  }

  public String toString() {
    return super.toString() +" audio attr (" +getAudioAttributes().size()+
      ") :" + getAudioAttributes() + " and " +fieldToAnnotation + " annotations, unit/lesson " + unitToValue;
  }
}
