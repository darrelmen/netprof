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

package mitll.langtest.server.audio.image;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

import java.awt.*;
import java.awt.image.WritableRaster;

public class SpectrumImage extends AudioImage {
  // Window Length
  private static final int WINDOW_DEFAULT = 512;
  // Contrast
  private static final float ALPHA_DEFAULT = 0.38f;
  // Preemphasis Coefficient
  private static final float PE_ALPHA_DEFAULT = 0.97f;
  // Use Color
  private static final boolean USE_COLOR_DEFAULT = true;

  /**
   * @param audio
   * @param framesPerSecond
   * @param x
   * @param y
   * @param displayStart
   * @param displayEnd
   */
  public SpectrumImage(short[] audio, float framesPerSecond, int x, int y,
                       int displayStart, int displayEnd) {
    this(audio, framesPerSecond, x, y, displayStart, displayEnd,
        WINDOW_DEFAULT, ALPHA_DEFAULT, PE_ALPHA_DEFAULT, USE_COLOR_DEFAULT, JET_COLOR_MAP
    );
  }

  /**
   * Create a new spectrogram view with full zoom (default) from the supplied audio
   *
   * @param audio           buffer of samples as 16-bit linear
   * @param framesPerSecond sample rate of the audio buffer (for translation to seconds)
   * @param x               image width
   * @param y               image height
   * @param displayStart    zoom window start
   * @param displayEnd      zoom window end
   * @param window          Window Length
   * @param alpha           Contrast
   * @param peCoef          Preemphasis Coefficient
   * @param colorP          whether to use color map or not
   * @param colorMap        map power to color
   */
  private SpectrumImage(short[] audio, float framesPerSecond, int x, int y,
                        int displayStart, int displayEnd,
                        int window,
                        float alpha,
                        float peCoef,
                        boolean colorP,
                        float[][] colorMap) {
    super(framesPerSecond, x, y);

    SpectrumResults spectrumResults = computeSpectrum(audio, displayStart, displayEnd, x, y, window, peCoef);
    drawSpectrum(x, y, displayStart, displayEnd, colorMap, alpha, spectrumResults, colorP);
  }

  /**
   * Setup hamming window and FFT buffers
   */
  private float[] makeWindow(int window) {
    float[] win = new float[window];

    // Setup window
    for (int i = 0; i < window; i++)
      win[i] = 0.54f - 0.46f * (float) Math.cos(2.0f * 3.14159265f * (float) i / (float) window);

    return win;
  }

  private void drawSpectrum(int xm, int ym, int displayStart, int displayEnd, float[][] colorMap, float alpha, SpectrumResults spectrumResults, boolean colorP) {
    float[][] spectrum = spectrumResults.spectrum;
    float maxPower = spectrumResults.maxPower;
    int[] color = new int[3];
    WritableRaster wr = this.getRaster();

    if (colorP) {
      for (int x = 0; x < xm; x++)
        for (int f = 0; f < ym; f++) {
          getColorMapColor(spectrum[x][f], colorMap, maxPower, alpha, color);
          wr.setPixel(x, ym - f - 1, color);
        }
    } else {
      for (int x = 0; x < xm; x++)
        for (int f = 0; f < ym; f++) {
          getProportionalColor(spectrum[x][f], maxPower, alpha, color);
          wr.setPixel(x, ym - f - 1, color);
        }
    }

    Graphics2D g = this.createGraphics();

    // draw over status line
    g.setFont(new Font("Dialog", 0, 9));
    g.setColor(new Color(255, 255, 255));
    boolean drawBeginTime = displayStart > 0;
    if (drawBeginTime) {
      String str = String.format("Begin time = %.2f Seconds", toSeconds(displayStart));
      g.drawString(str, 5, 12);
    }
    float len = getLen(displayStart, displayEnd);

    String str = "Time span = " + String.format("%.2f", len) + " Seconds";
    g.drawString(str, drawBeginTime ? 180 : 5, 12);
    g.setColor(new Color(255, 0, 0));

    /*    if (false) {  // TODO : fix this later
    // time bar with hacks
    g.setColor(new Color(0, 0, 0));
    g.fillRect(0, ym, xm, ym + 15);
    for (float a = timeGridLineFraction; a < (1 - timeGridLineFraction); a += timeGridLineFraction) {
      int x = (int) (a * (float) xm);
      g.setColor(new Color(255, 255, 255));
      g.drawLine(x, ym, x, ym - 3);
      str = String.format("%.3f", a * len + toSeconds(displayStart));
      g.drawString(str, x - 15, ym - bottomMargin);
    }
    }*/
  }

  private void getColorMapColor(float power, float[][] colors, float maxPower, float alpha, int[] color) {
    float nf = (float) Math.pow(dB(power) / dB(maxPower), 1.0f / alpha) * (float) (colors.length - 1);
    int norm = Math.min((int) nf, colors.length - 2);

    for (int b = 0; b < 3; b++)
      color[b] = Math.round((colors[norm + 1][b] - colors[norm][b]) * (nf - (float) norm) + colors[norm][b]);
  }

  private void getProportionalColor(float power, float maxPower, float alpha, int[] color) {
    for (int b = 0; b < 3; b++)
      color[b] = (int) (Math.pow(dB(power) / dB(maxPower), 1.0f / alpha) * 254.9999);
  }

  /**
   * Compute the clipped dB power value from x^2 (max = 200dB above minimum power)
   *
   * @param in sample (power value)
   */
  private float dB(float in) {
    return Math.min(10.0f * (flog10(in)), 200.0f);
  }

  /**
   * Compute floored log10() where floor = log10(1e-7)
   *
   * @param in value
   */
  private static float flog10(float in) {
    return in < 1e-7 ? (float) Math.log10(1e-7) : (float) Math.log10(in);
  }

  /**
   * Compute the spectrum for the supplied range of samples
   *
   * @param buffer   audio bytes
   * @param start    beginning of the range (as a sample index)
   * @param end      end of the range (as a sample index)
   * @param xm       number of frames to subsample the buffer range to
   * @param nFilters the number of evenly-spaced rectangular filters to downsample the spectrum into (usually the height of the spectrogram)
   * @param window   fft window
   * @param peCoef   Preemphasis Coefficient
   * @return pair of 2D spectrum and max power
   * @see #SpectrumImage(short[], float, int, int, int, int, int, float, float, boolean, float[][])
   */
  private SpectrumResults computeSpectrum(short[] buffer, int start, int end, int xm, int nFilters,
                                          int window, float peCoef) {
    float dF = ((float) window / 2.0f) / (float) nFilters; // ffts per filter
    float dX = (float) (end - start) / (float) xm;         // seconds per pixel

    float minPower = Float.MAX_VALUE;
    float maxPower = Float.MIN_VALUE;
    float[][] spectrum = new float[xm][nFilters];
    FloatFFT_1D fft = new FloatFFT_1D(window);
    float[] tmp = new float[window];
    float[] win = makeWindow(window);

    // compute the subsampled spectrum
    for (int f = 0; f < xm; f++) {
      float center = (float) start + (float) f * dX;
      int s = Math.round(center - (float) (window / 2));
      int e = Math.min(Math.round(center + (float) (window / 2)), s + window);
      int k;

      for (k = 0; s < 0; s++) tmp[k++] = 0.0f;
      for (int i = s; i < Math.min(e, end); i++)
        tmp[i - s + k] = ((float) buffer[i] - (float) buffer[Math.max(i - 1, 0)] * peCoef) * win[i - s];
      for (int j = Math.min(e, end); j < e; j++) tmp[j - s + k] = 0.0f; // Pad

      fft.realForward(tmp);

      // Compute the power spectrum : rectangular windows
      for (k = 0; k < nFilters; k++) spectrum[f][k] = 0.0f;

      //float totalE = 0.0f;
      spectrum[f][0] = tmp[1] * tmp[1];
      for (int y = 0; y < nFilters; y++) {
        int fftb = (int) Math.min(Math.round(((float) y) * dF), (float) (window - 1) / 2.0f) * 2;
        spectrum[f][y] += (tmp[fftb] * tmp[fftb] + tmp[fftb + 1] * tmp[fftb + 1]) / dF;

        if (spectrum[f][y] > maxPower) maxPower = spectrum[f][y];
        if (spectrum[f][y] < minPower) minPower = spectrum[f][y];
        //  totalE += spectrum[f][y];
      }
    }
    return new SpectrumResults(spectrum, maxPower);
  }

  private static class SpectrumResults {
    final float[][] spectrum;
    final float maxPower;

    public SpectrumResults(float[][] spectrum, float maxPower) {
      this.spectrum = spectrum;
      this.maxPower = maxPower;
    }
  }

  @Override
  public IMAGE_FORMAT getFormat() {
    return IMAGE_FORMAT.JPG;
  }

  @Override
  public ImageType getName() {
    return ImageType.SPECTROGRAM;
  }
}
