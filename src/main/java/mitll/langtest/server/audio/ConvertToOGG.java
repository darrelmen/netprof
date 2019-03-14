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

package mitll.langtest.server.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by go22670 on 2/23/17.
 */
class ConvertToOGG extends AudioBase {
  private static final Logger logger = LogManager.getLogger(ConvertToOGG.class);

  private static final boolean DEBUG = false;
  private static final boolean SPEW = false;
  private static final String OGG = ".ogg";

  boolean writeOGG(File absolutePathToWav, boolean overwrite, TrackInfo trackInfo) {
    String oggFile = absolutePathToWav.getAbsolutePath().replace(WAV, OGG);
    File ogg = new File(oggFile);
    if (!ogg.exists() || overwrite) {
      if (DEBUG)
        logger.debug("writeOGG : doing ogg conversion for " + absolutePathToWav);

      if (DEBUG) logger.debug("writeOGG run ogg on " + absolutePathToWav + " making " + oggFile);

      if (!convertToOGGFileAndCheck(getOggenc(), absolutePathToWav.getAbsolutePath(), oggFile, trackInfo)) {
        logger.error("writeOGG ogg File missing for " + absolutePathToWav);
        return false;
      }
    }
    return true;
  }

  private String getOggenc() {
    String property = System.getProperty("os.name").toLowerCase();
    boolean isMac = property.contains("mac");
    boolean isWin = property.contains("win");

    String oggEncPath = isMac ? "/usr/local/bin/oggenc" : isWin ? "bin\\win32\\oggenc.exe" : "/usr/bin/oggenc";
    File file = new File(oggEncPath);
    if (!file.exists()) {
      logger.error("getOggenc : can't find oggenc at " + file.getAbsolutePath());
    }
    return oggEncPath;
  }

  /**
   * @param oggPath
   * @param pathToAudioFile
   * @param oggFile
   * @param trackInfo
   * @return
   * @see #writeCompressedVersions
   */
  private boolean convertToOGGFileAndCheck(String oggPath, String pathToAudioFile, String oggFile, TrackInfo trackInfo) {
    if (DEBUG) logger.debug("convertToOGGFileAndCheck convert " + pathToAudioFile + " to " + oggFile);
    String title = trackInfo.getTitle();
    String author = trackInfo.getArtist();
    if (title != null && title.length() > 30) {
      title = title.substring(0, 30);
    }
    if (title == null) title = "";
    ProcessBuilder oggProx = new ProcessBuilder(oggPath, pathToAudioFile,
        "-o", oggFile,
        "-t", title,
        "-a", author,
        "-c", trackInfo.getComment());
    try {
      //logger.debug("running lame" + oggProx.command());
      new ProcessRunner().runProcess(oggProx);
    } catch (IOException e) {
      //  logger.error("Couldn't run " + oggProx);
      logger.error("for " + oggProx + " got " + e, e);
    }

    File testFile = new File(oggFile);
    if (!testFile.exists()) {
      if (!new File(pathToAudioFile).exists()) {
        if (SPEW) logger.error("huh? source file " + pathToAudioFile + " doesn't exist?");
      } else {
        logger.error("didn't write OGG : " + testFile.getAbsolutePath() +
            " exe path " + oggPath +
            " command was " + oggProx.command());
        try {
          if (!new ProcessRunner().runProcess(oggProx, true)) {
            return false;
          }
        } catch (IOException e) {
          logger.error("for " + oggProx + " got " + e, e);
        }

      }
      return false;
    }
    return true;
  }
}
