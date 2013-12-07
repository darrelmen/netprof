package mitll.langtest.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/6/13
 * Time: 6:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioExercise extends ExerciseShell {
  protected List<AudioAttribute> audioAttributes = new ArrayList<AudioAttribute>();

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
    if (s != null) audioAttributes.add(new AudioAttribute(s).markFast());
  }

  /**
   * @see mitll.langtest.server.database.ExcelImport#getExercise(String, mitll.langtest.server.database.FileExerciseDAO, String, String, String, String, String, boolean, String)
   * @param s
   */
  public void setSlowRefAudio(String s) {
    if (s != null) audioAttributes.add(new AudioAttribute(s).markSlow());
  }

  public AudioAttribute getAudio(String name, String value) {
    for (AudioAttribute audio : audioAttributes) {
      if (audio.matches(name,value)) return audio;
    }
    return null;
  }

  public boolean hasRefAudio() { return !audioAttributes.isEmpty(); }

  public List<AudioAttribute> getAudioAttributes() {
    return audioAttributes;
  }
}
