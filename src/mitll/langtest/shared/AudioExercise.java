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
  public static final String REGULAR = "regular";
  public static final String SLOW = "slow";
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";

  protected Map<String,AudioAttribute> audioAttributes = new HashMap<String, AudioAttribute>();
  protected Map<String,String> unitToValue = new HashMap<String, String>();
  private Map<String,ExerciseAnnotation> fieldToAnnotation = new HashMap<String, ExerciseAnnotation>();

  public AudioExercise() {}

  public AudioExercise(String id) {
    super(id);
  }

  public AudioExercise(String id, String tooltip) {
    super(id, tooltip);
  }

  public String getRefAudio() {
    AudioAttribute audio = getRegularSpeed();
    return audio != null ? audio.getAudioRef() : null;
  }

  public AudioAttribute getRegularSpeed() {
    return getAudio(SPEED, REGULAR);
  }

  public String getSlowAudioRef() {
    AudioAttribute audio = getSlowSpeed();
    return audio != null ? audio.getAudioRef() : null;
  }

  public AudioAttribute getSlowSpeed() {
    return getAudio(SPEED, SLOW);
  }

  /**
   * @see mitll.langtest.server.database.ExcelImport#getExercise
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
    audioAttributes.put(audioAttribute.getKey(),audioAttribute);
  }

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

  public AudioAttribute getAudio(String name, String value) {
    for (AudioAttribute audio : getAudioAttributes()) {
      if (audio.matches(name,value)) return audio;
    }
    return null;
  }

  public Collection<AudioAttribute> getAudioAtSpeed(boolean isRegular) {
    return getAudioAtSpeed(isRegular ? REGULAR : SLOW);
  }

  /**
   * @see mitll.langtest.server.database.AudioExport#writeFolderContents
   * @param value
   * @return
   */
  private Collection<AudioAttribute> getAudioAtSpeed(String value) {
    List<AudioAttribute> ret = new ArrayList<AudioAttribute>();
    for (AudioAttribute audio : getAudioAttributes()) {
      if (audio.matches(SPEED, value)) ret.add(audio);
    }
    return ret;
  }

  public boolean hasRefAudio() {
    return !audioAttributes.isEmpty();
  }

  public Collection<AudioAttribute> getAudioAttributes() {
    return audioAttributes.values();
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

    Collections.sort(males, new Comparator<AudioAttribute>() {
      @Override
      public int compare(AudioAttribute o1, AudioAttribute o2) {
        return o1.getUser().getAge() < o2.getUser().getAge() ? -1 : o1.getUser().getAge() > o2.getUser().getAge() ? +1 : 0;
      }
    });
    return males;
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
   // boolean hasRecordings = !recordingsBy.isEmpty();

    for (AudioAttribute attr : recordingsBy) {
      if (attr.isRegularSpeed() && regularSpeed || (attr.isSlow() && !regularSpeed)) {
        return attr;
      }
    }

    //return hasRecordings && regularSpeed ? recordingsBy.iterator().next() : null;
    return null;
  }

  public AudioAttribute getRecordingsBy(long userID, String speed) {
    List<AudioAttribute> recordingsBy = getRecordingsBy(userID);

    for (AudioAttribute attr : recordingsBy) {
      if (attr.getSpeed().equalsIgnoreCase(speed)) {
        return attr;
      }
    }

    return null;
  }

  public List<AudioAttribute> getRecordingsBy(long userID) {
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
   * @see mitll.langtest.client.custom.ReviewEditableExercise#makeAudioRow()
   * List of audio is sorted to show regular before slow.
   *
   * @param isMale
   * @return
   */
  public Map<MiniUser, List<AudioAttribute>> getUserMap(boolean isMale) {
    Map<MiniUser, List<AudioAttribute>> userToAudio = new HashMap<MiniUser, List<AudioAttribute>>();
    Collection<AudioAttribute> byGender = getByGender(isMale);
    for (AudioAttribute attribute : byGender) {
      List<AudioAttribute> audioAttributes1 = userToAudio.get(attribute.getUser());
      if (audioAttributes1 == null)
        userToAudio.put(attribute.getUser(), audioAttributes1 = new ArrayList<AudioAttribute>());
      audioAttributes1.add(attribute);
    }

    for (List<AudioAttribute> lists : userToAudio.values()) {
      Collections.sort(lists,new Comparator<AudioAttribute>() {
        @Override
        public int compare(AudioAttribute o1, AudioAttribute o2) {
          return o1.isRegularSpeed() && o2.isSlow() ? -1 : o1.isSlow() && o2.isRegularSpeed() ? +1 : 0;
        }
      });
    }
    return userToAudio;
  }

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
        if (exerciseAnnotation == null && !fieldToAnnotation.isEmpty()) {
          System.out.println("getAnnotation : Can't find " + field + " in "+ fieldToAnnotation.keySet());
        }

        return exerciseAnnotation;
      }
    }
    return fieldToAnnotation.get(field);
  }

  public Collection<String> getFields() { return fieldToAnnotation.keySet(); }

  public Map<String, String> getUnitToValue() { return unitToValue; }

  /**
   * @see mitll.langtest.server.database.SectionHelper#addExerciseToLesson
   * @param unit
   * @param value
   */
  public void addUnitToValue(String unit, String value) {
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
