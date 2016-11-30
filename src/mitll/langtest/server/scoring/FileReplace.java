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

package mitll.langtest.server.scoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/1/13
 * Time: 4:38 PM
 * To change this template use File | Settings | File Templates.
 */
class FileReplace {
  private static final Logger logger = LogManager.getLogger(FileReplace.class);

  void doTemplateReplace(String infile, String outfile, Map<String,String> replaceMap) {
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
