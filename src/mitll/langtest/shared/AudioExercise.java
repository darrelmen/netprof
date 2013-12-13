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
      audioAttributes.put(audioAttribute.getAttributes().toString(),audioAttribute);
    }
  }

  /**
   * @see mitll.langtest.server.database.ExcelImport#getExercise(String, String, String, String, String, String, boolean, String)
   * @param s
   */
  public void setSlowRefAudio(String s) {
    if (s != null && s.length() > 0 && !s.equals("null")) {
      AudioAttribute audioAttribute = new AudioAttribute(s).markSlow();
      audioAttributes.put(audioAttribute.getAttributes().toString(), audioAttribute);
    }
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

  public void addAnnotation(String field, String status, String comment) {
    fieldToAnnotation.put(field,new ExerciseAnnotation(status,comment));
  }


  public Map<String, ExerciseAnnotation> getFieldToAnnotation() {
    return fieldToAnnotation;
  }

  public String toString() {
    return super.toString() +" audio attr (" +getAudioAttributes().size()+
      ") :" + getAudioAttributes() + " and " +fieldToAnnotation + " annotations";
  }
}
