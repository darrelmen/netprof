package mitll.langtest.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  protected Map<String,AudioAttribute> audioAttributes = new HashMap<String, AudioAttribute>();
  protected Map<String,String> unitToValue = new HashMap<String, String>();
  private Map<String,ExerciseAnnotation> fieldToAnnotation = new HashMap<String, ExerciseAnnotation>();

  public AudioExercise() {}
  public AudioExercise(String id, String tooltip) {  super(id,tooltip); }

  public String getRefAudio() {
    AudioAttribute audio = getAudio(SPEED, REGULAR);
    return audio != null ? audio.getAudioRef() : null;
  }

  public String getSlowAudioRef() {
    AudioAttribute audio = getAudio(SPEED, SLOW);
    return audio != null ? audio.getAudioRef() : null;
  }

  /**
   * @see mitll.langtest.server.database.ExcelImport#getExercise(String, String, String, String, String, String, boolean, String)
   * @param s
   */
  public void setRefAudio(String s) {
    if (s != null && s.length() > 0 && !s.equals("null")) {
      AudioAttribute audioAttribute = new AudioAttribute(s).markRegular();
      addAudio(audioAttribute);
    }
  }

  public void addAudio(AudioAttribute audioAttribute) {
    audioAttributes.put(audioAttribute.getKey(),audioAttribute);
  }

  /**
   * @see mitll.langtest.server.database.ExcelImport#getExercise(String, String, String, String, String, String, boolean, String)
   * @param s
   */
  public void setSlowRefAudio(String s) {
    if (s != null && s.length() > 0 && !s.equals("null")) {
      AudioAttribute audioAttribute = new AudioAttribute(s).markSlow();
      addAudio(audioAttribute);
    }
  }

  public void clearRefAudio() {
    AudioAttribute audio = getAudio(SPEED, REGULAR);
    if (audio != null) audioAttributes.remove(audio.getKey());
  }

  public void clearSlowRefAudio() {
    AudioAttribute audio = getAudio(SPEED, SLOW);
    if (audio != null) audioAttributes.remove(audio.getKey());
  }

  public AudioAttribute getAudio(String name, String value) {
    for (AudioAttribute audio : getAudioAttributes()) {
      if (audio.matches(name,value)) return audio;
    }
    return null;
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
  private Collection<AudioAttribute> getByGender(boolean isMale) {
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

  public AudioAttribute getRecordingsBy(long userID, boolean regularSpeed) {
    //List<AudioAttribute> mine = new ArrayList<AudioAttribute>();
    for (AudioAttribute attr : getRecordingsBy(userID)) {
      if (attr.isRegularSpeed() && regularSpeed || (attr.isSlow() && !regularSpeed)) return attr;
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

  public String toString() {
    return super.toString() +" audio attr (" +getAudioAttributes().size()+
      ") :" + getAudioAttributes() + " and " +fieldToAnnotation + " annotations, unit/lesson " + unitToValue;
  }
}
