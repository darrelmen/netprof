/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.audio.tools;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

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
