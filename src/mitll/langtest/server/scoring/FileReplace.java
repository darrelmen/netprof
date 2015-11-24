/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.scoring;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/1/13
 * Time: 4:38 PM
 * To change this template use File | Settings | File Templates.
 */
class FileReplace {
  private static final Logger logger = Logger.getLogger(FileReplace.class);

  public void doTemplateReplace(String infile, String outfile, Map<String,String> replaceMap) {
    FileReader file;
    String line;
    try {
      file = new FileReader(infile);
      BufferedReader reader = new BufferedReader(file);

      FileWriter output = new FileWriter(outfile);
      BufferedWriter writer = new BufferedWriter(output);

      while ((line = reader.readLine()) != null) {
        String replaced = line;
        for (Map.Entry<String, String> kv : replaceMap.entrySet()) {
          try {
            replaced = replaced.replaceAll(kv.getKey(),kv.getValue());
          } catch (Exception e) {
            logger.error("got " +e + " replacing '" + kv.getKey() + "' with '" + kv.getValue() + "'",e);
          }
        }
        writer.write(replaced +"\n");
      }
      reader.close();
      writer.close();
    } catch (Exception e) {
      logger.error("got " +e,e);
    }
  }
}
