package mitll.langtest.shared;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/6/13
 * Time: 6:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioExercise extends ExerciseShell {
  protected Map<String,AudioAttribute> audioAttributes = new HashMap<String, AudioAttribute>();
  private Map<String,ExerciseAnnotation> fieldToAnnotation = new HashMap<String, ExerciseAnnotation>();

  public AudioExercise() {}
  public AudioExercise(String id, String tooltip) {  super(id,tooltip); }

  public String getRefAudio() {
    AudioAttribute audio = getAudio("speed", "regular");
    return audio != null ? audio.getAudioRef() : null;
  }

  public String getSlowAudioRef() {
    AudioAttribute audio = getAudio("speed", "slow");
    return audio != null ? audio.getAudioRef() : null;
  }

  public void setRefAudio(String s) {
    if (s != null && s.length() > 0 && !s.equals("null")) {
      AudioAttribute audioAttribute = new AudioAttribute(s).markFast();
      audioAttributes.put(audioAttribute.getKey(),audioAttribute);
    }
  }

  /**
   * @see mitll.langtest.server.database.ExcelImport#getExercise(String, String, String, String, String, String, boolean, String)
   * @param s
   */
  public void setSlowRefAudio(String s) {
    if (s != null && s.length() > 0 && !s.equals("null")) {
      AudioAttribute audioAttribute = new AudioAttribute(s).markSlow();
      audioAttributes.put(audioAttribute.getKey(), audioAttribute);
    }
  }

 // public void clearAudio() { clearRefAudio(); clearSlowRefAudio(); }
  public void clearRefAudio() {
    AudioAttribute audio = getAudio("speed", "regular");
    if (audio != null) audioAttributes.remove(audio.getKey());
  }

  public void clearSlowRefAudio() {
    AudioAttribute audio = getAudio("speed", "slow");
    if (audio != null) audioAttributes.remove(audio.getKey());
  }

  public AudioAttribute getAudio(String name, String value) {
    for (AudioAttribute audio : getAudioAttributes()) {
      if (audio.matches(name,value)) return audio;
    }
    return null;
  }

  public boolean hasRefAudio() { return !audioAttributes.isEmpty(); }

  public Collection<AudioAttribute> getAudioAttributes() { return audioAttributes.values();  }
  public void forgetAllAudio() { audioAttributes.clear(); }

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
      if (field.endsWith(".wav")) {
        String key = field.replaceAll(".wav", ".mp3");
        ExerciseAnnotation exerciseAnnotation = fieldToAnnotation.get(key);
        if (exerciseAnnotation == null && !fieldToAnnotation.isEmpty()) {
          System.out.println("Can't find " + field + " in "+ fieldToAnnotation.keySet());
        }

        return exerciseAnnotation;
      }
      else if (field.endsWith(".mp3")) {
        String key = field.replaceAll(".mp3", ".wav");
        ExerciseAnnotation exerciseAnnotation = fieldToAnnotation.get(key);
        if (exerciseAnnotation == null && !fieldToAnnotation.isEmpty()) {
          System.out.println("Can't find " + field + " in "+ fieldToAnnotation.keySet());
        }

        return exerciseAnnotation;
      }
      else {
        if (!fieldToAnnotation.isEmpty()) {
          System.out.println("getAnnotation : Can't find " + field + " in "+ fieldToAnnotation.keySet());
        }
      }
    }
    return fieldToAnnotation.get(field);
  }

  public Collection<String> getFields() { return fieldToAnnotation.keySet(); }

  public String toString() {
    return super.toString() +" audio attr (" +getAudioAttributes().size()+
      ") :" + getAudioAttributes() + " and " +fieldToAnnotation + " annotations";
  }
}
