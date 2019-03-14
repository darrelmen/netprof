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

package mitll.langtest.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Do something better here with figuring out which values to use in type->selection
 */
class ParamParser {
  private static final Logger logger = LogManager.getLogger(ParamParser.class);

  private final String[] split1;
  private Map<String, Collection<String>> selection;

  ParamParser(String... split1) {
    this.split1 = split1;
  }

  Map<String, Collection<String>> getSelection() {
    return selection;
  }

  ParamParser invoke(boolean ignoreUserAndProject) {
    selection = new TreeMap<>();
    for (String param : split1) {
    //  logger.info("UserAndSelection param '" + param + "'");
      String[] split = param.split("=");
      if (split.length == 2) {
        String key = split[0];
        String value = split[1];
        logger.info("\t" + key + " = " + value);
        if (ignoreUserAndProject &&
            (key.equals(ScoreServlet.HeaderValue.USER.toString()) || key.equalsIgnoreCase(ScoreServlet.HeaderValue.PROJID.name()))) {
          // skip it
          logger.info("UserAndSelection Skip " + key);
        } else {
          selection.put(key, Collections.singleton(value));
        }
      }
    }
    return this;
  }
}
