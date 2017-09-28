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

package mitll.langtest.server.audio.imagewriter;

import mitll.langtest.server.audio.image.*;
import mitll.langtest.server.audio.tools.AudioFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Factory for making an AudioImage class based on the type.  Treats waveform/spectrogram differently from
 * transcript images.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
class ImageFactory {
  private static final Logger logger = LogManager.getLogger(mitll.langtest.server.audio.imagewriter.ImageFactory.class);

  public static void loadFont() {
    try {
      GraphicsEnvironment ge =  GraphicsEnvironment.getLocalGraphicsEnvironment();
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("NotoSans-Regular.ttf")));
    } catch (IOException |FontFormatException e) {
      //Handle exception
    }
  }
  /**
   * @param type
   * @param audioFile
   * @param x
   * @param y
   * @param timeGridLineFraction
   * @return
   * @see mitll.langtest.server.audio.imagewriter.SimpleImageWriter#getFileForImageType(int, int, mitll.langtest.server.audio.tools.AudioFile, File, float, int, mitll.langtest.server.audio.image.ImageType, String, String)
   */
  static AudioImage makeImage(ImageType type, AudioFile audioFile, int x, int y, float timeGridLineFraction) {
    short[] buffer = audioFile.getBuffer();
    int displayEnd = buffer.length;
    return makeImage(type, buffer, audioFile.getFPS(), x, y, 0, displayEnd, timeGridLineFraction);
  }

  /**
   * @param type
   * @param audio
   * @param framesPerSecond
   * @param x
   * @param y
   * @param displayStart
   * @param displayEnd
   * @param timeGridLineFraction
   * @return
   */
  private static AudioImage makeImage(ImageType type, short[] audio, float framesPerSecond, int x, int y,
                                      int displayStart, int displayEnd,
                                      float timeGridLineFraction) {
    if (type == ImageType.WAVEFORM) {
      return new WaveformImage(audio, framesPerSecond, x, y, displayStart, displayEnd, timeGridLineFraction);
    } else if (type == ImageType.SPECTROGRAM) {
      return new SpectrumImage(audio, framesPerSecond, x, y, displayStart, displayEnd);
    } else {
      logger.error("asking for image for unknown type " + type);
      return null;
    }
  }

  /**
   * @param type
   * @param audioFile
   * @param x
   * @param y
   * @param events
   * @param scoreScalar
   * @param useScoreToColorBkg
   * @return
   * @see mitll.langtest.server.audio.imagewriter.TranscriptWriter#getFileForTranscriptImageType
   */
  public static AudioImage makeTranscriptImage(ImageType type, AudioFile audioFile, int x, int y,
                                               Map<Float, TranscriptEvent> events, float scoreScalar,
                                               boolean useScoreToColorBkg) {
    int displayEnd = audioFile.getBuffer().length;
    return makeTranscriptImage(type, audioFile.getFPS(), x, y, 0, displayEnd, events, scoreScalar, useScoreToColorBkg);
  }

  /**
   * @param type
   * @param framesPerSecond
   * @param x
   * @param y
   * @param displayStart
   * @param displayEnd
   * @param events
   * @param scoreScalar
   * @param useScoreToColorBkg
   * @return
   */
  private static AudioImage makeTranscriptImage(ImageType type, float framesPerSecond, int x, int y,
                                                int displayStart, int displayEnd,
                                                Map<Float, TranscriptEvent> events, float scoreScalar,
                                                boolean useScoreToColorBkg) {
    if (type == ImageType.WORD_TRANSCRIPT) {
      return new TranscriptImage(framesPerSecond, x, y, displayStart, displayEnd, events, scoreScalar, useScoreToColorBkg);
    } else if (type == ImageType.PHONE_TRANSCRIPT) {
      return new PhoneTranscriptImage(framesPerSecond, x, y, displayStart, displayEnd, events, scoreScalar, useScoreToColorBkg);
    } else {
      logger.error("asking for image for unknown type " + type);
      return null;
    }
  }
}
