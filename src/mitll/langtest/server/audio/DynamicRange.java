package mitll.langtest.server.audio;

import org.apache.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * Created by go22670 on 10/30/15.
 * <p>
 * function minmax(audio_name)
 * [audio_data, fs] = audioread(audio_name);
 * % determine how many samples are in a 50ms window
 * seconds_per_sample = 1/fs;
 * window = floor(.05/seconds_per_sample);
 * slide = floor(window/10);   % number of samples to slide the window
 * a = 1;
 * o = a + window;
 * minrms = 1;
 * maxrms = 0;
 * datasize = size(audio_data);
 * while o < datasize(1)
 * res = rms(audio_data(a:a+slide));
 * if res < .000032    % less than -89.9 dB
 * a = a + slide;
 * o = o + slide;
 * continue;
 * end
 * res = rms(audio_data(o-slide:o));
 * if res < .000032    % less than -89.9 dB
 * a = a + slide;
 * o = o + slide;
 * continue;
 * end
 * res = rms(audio_data(a:o));
 * if res > maxrms
 * maxrms = res;
 * elseif res < minrms
 * minrms = res;
 * end
 * a = a + slide;
 * o = o + slide;
 * end
 * name_size = size(audio_name);
 * stat_name = audio_name(1:name_size(2)-4);
 * stat_name = strcat(stat_name, '-stats.txt');
 * STATID = fopen(stat_name, 'w');
 * <p>
 * fprintf(STATID,'Max-Min Range:\t%.2fdB\n', 20*log10(maxrms)-20*log10(minrms));
 * fprintf(STATID,'Maximum Sample Value:\t%d\n', max(audio_data)*32768);
 * fprintf(STATID,'Minimum Sample Value:\t%d\n', min(audio_data)*32768);
 * fprintf(STATID,'Total RMS Value:\t%.2fdBFS\n', 20*log10(rms(audio_data)));
 * fprintf(STATID,'Minimum RMS Value:\t%.2fdBFS\n', 20*log10(minrms));
 * fprintf(STATID,'Maximum RMS Value:\t%.2fdBFS\n', 20*log10(maxrms));
 */
public class DynamicRange {
  private static final Logger logger = Logger.getLogger(DynamicRange.class);
  private static final int MinRecordLength = (10000 / 2); // 10000 = 0.7 second
  private static final int WinSize = 10;
  private static final double PowerThreshold = -79.50f;//-55.0f;
  private static final double VarianceThreshold = 20.0f;
  private static final double CLIPPED_RATIO = 0.005; // 1/2 %
  private static final double LOG_OF_TEN = Math.log(10.0);

  private static final short clippedThreshold = 32704; // 32768-64
  private static final short clippedThresholdMinus = -32704; // 32768-64
  private static final short ct = 32112;
  // private static final short clippedThreshold2 = 32752; // 32768-16
  // private static final short clippedThreshold2Minus = -32752; // 32768-16
  private static final double MAX_VALUE = 32768.0f;

  private double dB(double power) {
    return 20.0 * Math.log(power < 0.0001f ? 0.0001f : power) / LOG_OF_TEN;
  }

  /**
   * @param file
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getImageForAudioFile
   */
  public double getDurationInSeconds(String file) {
    return getDurationInSeconds(new File(file));
  }

  /**
   * @param file
   * @return
   * @see mitll.langtest.server.scoring.ASRScoring#scoreRepeatExercise
   */
  public double getDurationInSeconds(File file) {
    AudioInputStream audioInputStream = null;
    try {
      audioInputStream = AudioSystem.getAudioInputStream(file);
      double dur = getDurationInSeconds(audioInputStream);
      audioInputStream.close();
      return dur;
    } catch (Exception e) {
      try {
        if (audioInputStream != null) audioInputStream.close();
      } catch (IOException e1) {
        logger.error("got " + e, e);
      }
      logger.error("got " + e, e);
    }
    return 0d;
  }

  private double getDurationInSeconds(AudioInputStream audioInputStream) {
    long frames = audioInputStream.getFrameLength();
    AudioFormat format = audioInputStream.getFormat();
    return ((double) frames) / format.getFrameRate();
  }

  /**
   * Verify audio messages
   *
   * @param file audio byte array with header
   * @return true if well formed
   * @see AudioConversion#isValid(File, boolean)
   */
  public RMSInfo getDynamicRange(File file) {
    AudioInputStream ais = null;
    try {
      logger.info("opening " + file.getName());

      ais = AudioSystem.getAudioInputStream(file);
      AudioFormat format = ais.getFormat();
      logger.info("file " + file.getName() + " sample rate " + format.getSampleRate());

      boolean bigEndian = format.isBigEndian();
      if (bigEndian) {
        logger.warn("huh? file " + file.getAbsoluteFile() + " is in big endian format?");
      }
      int fsize = format.getFrameSize();
      assert (fsize == 2);
      assert (format.getChannels() == 1);

      // frames per second
      double frameRate = format.getFrameRate();
      double secPerFrame = 1d / frameRate;

      logger.info(" frameRate" + frameRate);
      logger.info(" secPerFrame" + secPerFrame);
      logger.info(" fsize" + fsize);

      short window = Double.valueOf(Math.floor(.05f / secPerFrame)).shortValue();
      short slide  = Double.valueOf(Math.floor((double) window / 10f)).shortValue();

      //     int a = 1;
//      int o = a + window;

      double minrms = 1;
      double maxrms = 0;

//      long frameLength = ais.getFrameLength();

      // Verify audio power
      //  double pm = 0.0f, p2 = 0.0f, n = 0.0f;
      int bufSize = WinSize * fsize;
      //   int bufSize = window * fsize;
      byte[] buf = new byte[bufSize];
      //   int countClipped = 0;
      //    int cc = 0;
//
      //short max = 0;
      //short nmax = 0;

      short minSample = Short.MAX_VALUE;
      short maxSample = Short.MIN_VALUE;

      //double[] samples = new double[bufSize / 2];

      int first = 0;
      int last = 0;
      int windowCount = 0;

      double firstTotal = 0;
      //double firstTotalCopy = 0;
      double lastTotal = 0;
      double windowTotal = 0;
      int lastStart = window - slide;
      double allTotal = 0;
      int allCount = 0;
      int sIndex = 0;
      boolean valid = true;

      logger.info("bufSize " + bufSize);
      logger.info("fsize " + fsize);
      logger.info("slide " + slide);
      logger.info("window " + window);
      logger.info("lastStart " + lastStart);
      int c = 0;
      while (ais.read(buf) == bufSize) {
        c++;
        for (int i = 0; i < bufSize; i += fsize)
          for (int s = 0; s < fsize; s += 2) {
            // short tmp = (short) ((buf[i + s] << 8) | buf[i + s + 1]); // BIG ENDIAN
            short tmp = (short) ((buf[i + s] & 0xFF) | (buf[i + s + 1] << 8)); // LITTLE ENDIAN

            if (tmp < minSample) minSample = tmp;
            if (tmp > maxSample) maxSample = tmp;

            double r = ((double) tmp) / MAX_VALUE;
            sIndex++;
            //        samples[sIndex++] = tmp;
            double squared = r * r;

            allTotal += squared;
            allCount++;

            // TODO : DUDE - don't do this - how can you be adding two both front and back at the same time????
            if (first < slide) {
              firstTotal += squared;
              first++;
            }
            if (windowCount > lastStart) {
              lastTotal += squared;
              last++;
            }

            windowTotal += squared;
            windowCount++;

            // GAH - check last separate of first...
            if (first == slide// && last == slide
                ) {
              double fres = srms(firstTotal, slide);
              if (fres < 0.000032) { // start over
                logger.info("start over first");
              } else {
                //logger.info(c + " first res " + fres + " total " + firstTotal);
                double lres = srms(lastTotal, slide);
                if (lres < 0.000032) { // start over
                  logger.info("start over last");
                } else {
//                  logger.info("last res " + lres);
                  if (windowCount != window) {
                    if (c < 100)
                    logger.warn("huh? windowCount " + windowCount + " vs " + window);
                  }

                  double res = srms(windowTotal, (double) windowCount);
                  //logger.info("c " + c + " " + sIndex + " res " + res + " total " + windowTotal + " " + windowCount);

                  if (res > maxrms) maxrms = res;
                  else if (res < minrms) minrms = res;
                }
              }

              windowTotal -= firstTotal;
              windowCount -= slide;

              firstTotal = 0;
              first = 0;

              lastTotal = 0;
              last = 0;
            }
          }
      }

      logger.info("did " + c);
      DecimalFormat decimalFormat = new DecimalFormat("##.##");

      logger.info("maxrms:\t" + maxrms);

      logger.info("minrms:\t" + minrms);
      logger.info("allTotal:\t" + allTotal);

      logger.info("allCount:\t" + allCount);

      double range = 20 * Math.log10(maxrms) - 20 * Math.log10(minrms);

      double totalRMS = 20 * Math.log10(srms(allTotal, allCount));
      double minRMS = 20 * Math.log10(minrms);
      double maxRMS = 20 * Math.log10(maxrms);

      logger.info("Max-Min Range:\t" + decimalFormat.format(range) + "dB");

      logger.info("Maximum Sample Value:\t" +
              maxSample
      );
      logger.info("Minimum Sample Value:\t" + minSample);

      logger.info("Total RMS Value:\t" + decimalFormat.format(totalRMS) + "dBFS");
      logger.info("Minimum RMS Value:\t" + decimalFormat.format(minRMS) + "dBFS");
      logger.info("Maximum RMS Value:\t" + decimalFormat.format(maxRMS) + "dBFS");


      return new RMSInfo();
    } catch (Exception e) {
      logger.error("Got " + e, e);
    } finally {
      try {
        if (ais != null) ais.close();
      } catch (IOException e) {
        logger.error("Got " + e, e);
      }
    }
    return null;
  }

  private double rms(short[] samples) {
    double rms = 0;
    double len = samples.length;
    for (short sample : samples) {
      double fsample = (double) sample;
      rms += (fsample * fsample);
    }

    return Math.sqrt(rms / len);
  }

  private double srms(double rms, double len) {
    return Math.sqrt(rms / len);
  }

  private short max(short[] samples) {
    short max = Short.MIN_VALUE;

    for (short sample : samples) {
      if (sample > max) max = sample;
    }

    return max;
  }


  private short min(short[] samples) {
    short min = Short.MAX_VALUE;

    for (short sample : samples) {
      if (sample < min) min = sample;
    }

    return min;
  }

  public static class RMSInfo {
    double maxMin;
    int max, min;
    double totalRMS, minRMS, maxRMS;

    public RMSInfo() {
    }

    public String toString() {
      return "Max-Min Range:\t" +
          "55.00dB" +
          "\n" +
          "Maximum Sample Value:\t14060\n" +
          "Minimum Sample Value:\t-15087\n" +
          "Total RMS Value:\t-24.03dBFS\n" +
          "Minimum RMS Value:\t-69.26dBFS\n" +
          "Maximum RMS Value:\t-14.26dBFS";
    }
  }
}
