package mitll.langtest.server.audio;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.Exercise;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 5:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class SkipWords extends English {
  private static Logger logger = Logger.getLogger(SkipWords.class);

  private void dumpDir2(String audioDir, String language, String dbName, String spreadsheet) {
    logger.warn("audio dir " + audioDir + " lang " + language + " db " + dbName + " spreadsheet " + spreadsheet);

    Set<String> files = getFilesInBestDir(audioDir);

    final String configDir = getConfigDir(language);

    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      dbName,
      configDir +
        spreadsheet);

    writeMissingFiles(files, configDir, unitAndChapter);
  }

}
