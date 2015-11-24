/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.scoring;

import corpus.LTS;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/1/13
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigFileCreator {
  private static Logger logger = Logger.getLogger(ConfigFileCreator.class);

  private final String platform = Utils.package$.MODULE$.platform();
  private final Map<String, String> properties;
  private final LTS letterToSoundClass;
  private final String scoringDir;

  private static final String DICT_WO_SP = "dict-wo-sp";
  private static final String TEMP_DIR = "TEMP_DIR";
  private static final String MODELS_DIR_VARIABLE = "MODELS_DIR";
  private static final String N_OUTPUT = "N_OUTPUT";
  private static final String LEVANTINE_N_OUTPUT = "" + 38;

  private static final String N_HIDDEN = "N_HIDDEN";
  private static final String N_HIDDEN_DEFAULT = "" + 2500;
  private static final String OPT_SIL = "OPT_SIL";
  private static final String OPT_SIL_DEFAULT = "true";   // rsi-sctm-hlda
  private static final String HLDA_DIR = "HLDA_DIR";
  private static final String LM_TO_USE = "LM_TO_USE";
  private static final String LTS_CLASS = "LTS_CLASS";

  private static final String HLDA_DIR_DEFAULT = "rsi-sctm-hlda";
  private static final String SMALL_LM_SLF = "smallLM.slf";

  private static final String CFG_TEMPLATE_PROP = "configTemplate";
  private static final String CFG_TEMPLATE_DEFAULT = "generic-nn-model.cfg.template";

  private static final String DECODE_CFG_TEMPLATE_PROP = "decodeConfigTemplate";
  private static final String DECODE_CFG_TEMPLATE_DEFAULT = "arabic-nn-model-decode.cfg.template";

  private static final String DEFAULT_MODELS_DIR = "models.dli-levantine";

  public ConfigFileCreator(Map<String, String> properties, LTS letterToSoundClass, String scoringDir) {
    this.properties = properties;
    this.letterToSoundClass = letterToSoundClass;
    this.scoringDir = scoringDir;
  }

  /**
   * Creates a hydec config file from a template file by doing variable substitution.<br></br>
   * Also use the properties map to look for variables.
   *
   * @see ASRScoring#computeRepeatExerciseScores
   * @param tmpDir where hydec will run and where the config file will be
   * @param modelsDir to point to, for config to use
   * @param decode if using the decoder cfg
   * @return path to config file
   */
  public String getHydecConfigFile(String tmpDir, String modelsDir, boolean decode) {
    boolean onWindows = platform.startsWith("win");
    Map<String,String> kv = new HashMap<String, String>();

    String levantineNOutput = getProp(N_OUTPUT, LEVANTINE_N_OUTPUT);
    String nHidden = getProp(N_HIDDEN, N_HIDDEN_DEFAULT);
    String cfgTemplate = getProp(CFG_TEMPLATE_PROP, CFG_TEMPLATE_DEFAULT);
    if (onWindows) {
      tmpDir = doWindowsSlashReplace(tmpDir);
    }

    if (decode) {
      cfgTemplate = getProp(DECODE_CFG_TEMPLATE_PROP, DECODE_CFG_TEMPLATE_DEFAULT);
      kv.put(LM_TO_USE, tmpDir + File.separator +File.separator + SMALL_LM_SLF); // hack! TODO hack replace
      if (letterToSoundClass != null) {
        String value = letterToSoundClass.getClass().toString();
        //if (value.endsWith("EmptyLTS")) {
        //  value = EnglishLTS.class.toString();
        //  logger.info("mapping empty lts to " + value);
       // }
      //  logger.info("setting lts to " + value);
        kv.put(LTS_CLASS, value);
      }

      // new FileCopier().copy(modelsDir+File.separator+"phones.dict",tmpDir+File.separator +"dict");   // Audio.hscore in pron sets dictionary=this value
    }
    //logger.info("using config from template " + cfgTemplate);

    kv.put(TEMP_DIR,tmpDir);
    kv.put(MODELS_DIR_VARIABLE, modelsDir);
    kv.put(N_OUTPUT, levantineNOutput);
    kv.put(N_HIDDEN, nHidden);
    kv.put(OPT_SIL, getProp(OPT_SIL, OPT_SIL_DEFAULT));
    kv.put(HLDA_DIR, getProp(HLDA_DIR, HLDA_DIR_DEFAULT));
    if (onWindows) kv.put("/","\\\\");

    // we need to create a custom config file for each run, complicating the caching of the ASRParameters...
    String modelCfg = cfgTemplate.substring(0, cfgTemplate.length() - ".template".length());

    String configFile = tmpDir+ File.separator+ modelCfg;
    //logger.debug("getHydecConfigFile : tmpDir is " + tmpDir);

    String pathToConfigTemplate = scoringDir + File.separator + "configurations" + File.separator + cfgTemplate;
    //logger.debug("template config is at " + pathToConfigTemplate + " map is " + kv);
    new FileReplace().doTemplateReplace(pathToConfigTemplate, configFile, kv);
    return configFile;
  }

  /**
   * @see mitll.langtest.server.scoring.ASRScoring#makeDict()
   * @return
   */
  public String getDictFile() {
    return getDictFile(getModelsDir());
  }

  private String getDictFile(String modelsDir) {
    String hldaDir = getProp(HLDA_DIR, HLDA_DIR_DEFAULT);
    String dictFile = modelsDir + File.separator + hldaDir + File.separator + DICT_WO_SP;
    boolean dictExists = new File(dictFile).exists();
    if (!dictExists) {
      dictFile = modelsDir + File.separator + DICT_WO_SP;
    }
    return dictFile;
  }

  public String getModelsDir() {
    String modelsDir = scoringDir + File.separator + getProp(MODELS_DIR_VARIABLE, DEFAULT_MODELS_DIR);
    if (platform.startsWith("win")) {
      modelsDir = doWindowsSlashReplace(modelsDir);
    }
    return modelsDir;
  }

  private String doWindowsSlashReplace(String tmpDir) {
    return tmpDir.replaceAll("\\\\","\\\\\\\\");
  }

  private String getProp(String var, String defaultValue) {
    return properties.containsKey(var) ? properties.get(var) : defaultValue;
  }
}
