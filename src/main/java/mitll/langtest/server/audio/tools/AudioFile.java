package mitll.langtest.server.audio.tools;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Handles reading from an audio file.
 *
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/22/11
 * Time: 5:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioFile {
  private static final Logger logger = Logger.getLogger("AudioFile");

  // Audio data/metadata
  private static final AudioFormat DEFAULT_FORMAT = new AudioFormat(16000.0f, 16, 1, true, false); /// Default Audio format

  private short[] buffer;                      /// buffer of audio samples
  private float end;                    /// absolute start and end times (in seconds)
  private String name;

  public AudioFile(String audio) throws IOException {
    open(audio);
  }

  private void open(String audio) throws IOException {
    File f = new File(audio);
    if (!f.exists()) {
      logger.warning("ERROR: can't find " + audio + " at " + f.getAbsolutePath());
      return;
    }
    open(f);
  }

  /**
   * Open an audio file and read its contents for display and zoom
   *
   * @param f a filename for an 16-bit linear wave file
   * @throws Exception
   */
  private void open(File f) throws IOException {
    AudioInputStream fwav;
    int len;
    this.name = f.getName();
    //System.out.println("opening " + f.getAbsolutePath());
    AudioFileFormat format;
    try {
      fwav = AudioSystem.getAudioInputStream(f);
      //System.out.println("opening " + f.getName() + " got " + fwav);

      format = AudioSystem.getAudioFileFormat(f);
      //System.out.println("opening " + f.getName() + " format " + format);

      len = format.getFrameLength();
      //System.out.println("opening " + f.getName() + " len " + len);

    } catch (UnsupportedAudioFileException e) {
      if (!f.getName().endsWith("raw")) {
        logger.warning("WARNING: Opening file using default RAW parameters '" + e.getMessage() + "'\n");
      }
      len = (int) f.length() / DEFAULT_FORMAT.getFrameSize();
      fwav = new AudioInputStream(new FileInputStream(f), DEFAULT_FORMAT, len);
      //format = new AudioFileFormat(AudioFileFormat.Type.SND, DEFAULT_FORMAT, len);
    }
    //logger.warning("Format: " + format.toString());
    AudioInputStream wav = AudioSystem.getAudioInputStream(new AudioFormat.Encoding("PCM_SIGNED"), fwav);    // /32767.0
    AudioFormat audioFormat = wav.getFormat();

    buffer = readAudioFile(len, wav);

    end = (float) getBuffer().length / audioFormat.getFrameRate(); //format.getFormat().getFrameRate();
  }

  private short[] readAudioFile(int len, AudioInputStream wav) throws IOException {
    byte[] tmp = new byte[2];
    short[] shorts = new short[len];

    for (int i = 0; i < len; i++) {
      /*int read =*/
      wav.read(tmp);
      shorts[i] = (short) ((tmp[0] & 0xFF) | (tmp[1] << 8));
    }
    wav.close();
    return shorts;
  }

  public short[] getBuffer() {
    return buffer;
  }
  public float getFPS() {
    return (float) buffer.length / end;
  }
  public String getName() {
    return name;
  }
}
