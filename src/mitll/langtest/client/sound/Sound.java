/**
 * 
 */
package mitll.langtest.client.sound;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * @author gregbramble
 *
 */
class Sound {
	private final AudioControl parent;
	public JavaScriptObject sound;
	
	public Sound(AudioControl parent){
		this.parent = parent;
	}

  public AudioControl getParent() {
    return parent;
  }
}
