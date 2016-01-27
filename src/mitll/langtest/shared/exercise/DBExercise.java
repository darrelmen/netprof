/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import java.util.Map;

/**
 * Created by go22670 on 12/16/15.
 */
public class DBExercise extends BaseObject implements CommonShell {
  public enum FIELDS {ENGLISH, MEANING, FL, EX_STATE, SECOND_STATE, UNIT_TO_VALUE}

  public DBExercise() {
  }

  public DBExercise(long id, String exid) {
    super(id, exid);
  }

  @Override
  public STATE getState() {
    Field state1 = nameToField.get(FIELDS.EX_STATE.name());
    if (state1 == null) return STATE.UNSET;
    String state = state1.getValue();
    return STATE.valueOf(state);
  }

  @Override
  public void setState(STATE state) {
    addField(FIELDS.EX_STATE.name(), state.name());
  }

  @Override
  public STATE getSecondState() {
    Field secondState = nameToField.get(FIELDS.SECOND_STATE.name());
    if (secondState == null) return STATE.UNSET;
    String state = secondState.getValue();
    return STATE.valueOf(state);
  }

  @Override
  public void setSecondState(STATE state) {
    addField(FIELDS.SECOND_STATE.name(), state.name());
  }

  @Override
  public Map<String, String> getUnitToValue() {
    return null;
  }

  @Override
  public String getEnglish() {
    return nameToField.get(FIELDS.ENGLISH.name()).getValue();
  }

  public void setEnglish(CommonShell shell) {
    addField(DBExercise.FIELDS.ENGLISH.name(), shell.getEnglish());
  }

  @Override
  public String getMeaning() {
    return nameToField.get(FIELDS.MEANING.name()).getValue();
  }

  public void setMeaning(CommonShell shell) {
    if (!shell.getMeaning().isEmpty())
      addField(DBExercise.FIELDS.MEANING.name(), shell.getMeaning());
  }

  public void addUnitToValue(String key, String value) {
    String name = FIELDS.UNIT_TO_VALUE.name();
    Field unitToValue = nameToField.get(name);
    if (unitToValue == null) {
      unitToValue = new Field(name, Field.FIELD_TYPE.MAP);
      addField(name, unitToValue);
    }
    unitToValue.put(key, value);
  }

  @Override
  public String getForeignLanguage() {
    return nameToField.get(FIELDS.FL.name()).getValue();
  }

  @Override
  public String getTransliteration() {
    return null;
  }

  @Override
  public String getContext() {
    return null;
  }

  @Override
  public String getContextTranslation() {
    return null;
  }

  public void setForeignLanguage(CommonShell shell) {
    addField(FIELDS.FL.name(), shell.getForeignLanguage());
  }
}
