/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

/**
 * Created by go22670 on 12/16/15.
 */
public class ExObject extends BaseObject implements CommonShell {
  public enum FIELDS {ENGLISH, MEANING, FL, EX_STATE, SECOND_STATE}

  public ExObject() {}

  public ExObject(long id, String exid) {
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
  public String getEnglish() {
    return nameToField.get(FIELDS.ENGLISH.name()).getValue();
  }

  public void setEnglish(CommonShell shell) {
    addField(ExObject.FIELDS.ENGLISH.name(), shell.getEnglish());
  }

  @Override
  public String getMeaning() {
    return nameToField.get(FIELDS.MEANING.name()).getValue();
  }

  public void setMeaning(CommonShell shell) {
    if (!shell.getMeaning().isEmpty())
      addField(ExObject.FIELDS.MEANING.name(), shell.getMeaning());
  }

  @Override
  public String getForeignLanguage() {
    return nameToField.get(FIELDS.FL.name()).getValue();
  }

  public void setForeignLanguage(CommonShell shell) {
    addField(FIELDS.FL.name(), shell.getForeignLanguage());
  }
}
