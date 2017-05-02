/**
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2016 - 2017. Other request for this document shall
 * be referred to DLIFLC.
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
 * © 2016 - 2017 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */
package mitll.langtest.server.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * JSONExporterTest
 * <br><br>
 * Copyright &copy; 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author Raymond Budd <a href=mailto:raymond.budd@ll.mit.edu>raymond.budd@ll.mit.edu</a>
 * @since May 01, 2017
 */
public class JSONExporterTest {//extends ExporterTestBase {
	private static final Logger log = LogManager.getLogger();
	
	private static final String EXPORT_OUT_HOME = "build" + File.separator + "test-working" + File.separator + 
			"export-results" + File.separator + "json" + File.separator;
	
	/*@BeforeClass public static void prepareDirectories() {
		ExporterTestBase.prepareOutputDirectories(EXPORT_OUT_HOME);
	}
	
	@Override protected String getExportOutHome() { return EXPORT_OUT_HOME; }
	
	@Test
	public void vocabExportNoExamSuccess() throws Exception {
		setupData("Simple Exam", SkillType.Vocabulary, TestItemType.Vocabulary, "SPANISH", 
				ExamType.Seeding, 10, 15, true, VocabularyItem.class);
		assertThat(docIds.size(), is(10));
		File f = new File(EXPORT_OUT_HOME, "test.json");

		JSONExporter exporter = new JSONExporter(getDominoContext(), expProj, f);
		List<HeadDocumentRevision> docs = getDSDelegate().getDocuments(expProj, expUser, true, true, new FindOptions<>());
		docs.remove(1);
		boolean result = exporter.exportDocuments(docs);
		assertThat(result, is(true));
		JsonObject readObj = readJSONObject("test.json");
		assertThat(readObj.containsKey("language"), is(true));
		assertThat(readObj.containsKey("exportTime"), is(true));
		assertThat(readObj.containsKey("project"), is(true));
		assertThat(readObj.containsKey("documents"), is(true));
	}

	@Test
	public void vocabExportDeserializationNoExam() throws Exception {
		setupData("Simple Exam", SkillType.Vocabulary, TestItemType.Vocabulary, "SPANISH", 
				ExamType.Seeding, 10, 15, true, VocabularyItem.class);
		assertThat(docIds.size(), is(10));
		JSONSerializer ser = getDocumentService().serializer();
		
		File f = new File(EXPORT_OUT_HOME, "test.json");
		JSONExporter exporter = new JSONExporter(getDominoContext(), expProj, f);
		List<HeadDocumentRevision> docs = getDSDelegate().getHeavyDocuments(expProj, expUser, true, true, new FindOptions<>());
		docs.remove(1);
		boolean result = exporter.exportDocuments(docs);
		assertThat(result, is(true));
		JsonObject readObj = readJSONObject("test.json");
		
		assertThat(readObj.containsKey("exportTime"), is(true));
		Date theTime = ser.dateFormat().parse(readObj.getString("exportTime")).get();
		assertThat(theTime, notNullValue());
		
		assertThat(readObj.containsKey("language"), is(true));
		JsonObject langObj = readObj.getJsonObject("language");
		Language l = ser.deserialize(Language.class, langObj.toString());
		assertThat(l, notNullValue());
		assertThat(l.getName(), is("SPANISH"));
		assertThat(l.getTrigraph(), is("SPA"));

		JsonObject projObj = readObj.getJsonObject("project");
		ProjectDescriptor pd = ser.deserialize(ProjectDescriptor.class, projObj.toString());
		assertThat(pd, notNullValue());
		assertThat(pd.getName(), is(expProj.getName()));
		assertThat(pd.getContent().getSkill(), is(SkillType.Vocabulary));

		JsonArray docArr = readObj.getJsonArray("documents");
		assertThat(docArr, hasSize(9));
		docArr.forEach(docObj -> {
			SimpleHeadDocumentRevision shDoc = ser.deserialize(SimpleHeadDocumentRevision.class, docObj.toString());
			assertThat(shDoc, notNullValue());
			assertThat(docIds.contains(shDoc.getId()), is(true));
			assertThat(shDoc.getDocument() instanceof VocabularyItem, is(true));
		});
	}
	
	@Test
	public void vocabExportWithExamSuccess() throws Exception {
		setupData("Simple Exam", SkillType.Vocabulary, TestItemType.Vocabulary, "SPANISH", 
				ExamType.Seeding, 10, 15, true, VocabularyItem.class);
		assertThat(docIds.size(), is(10));
		File f = new File(EXPORT_OUT_HOME, "test.json");

		Exam exam = getExamDelegate().getExam(expProjId, expExamId);
		Map<ValidationFormID, List<HeadDocumentRevision>> examDocs = getDominoContext().getExamExportDelegate()
				.getOrderedExportDocuments(expProj, expUser, expExamId, exam.getContent().getFormIds());

		JSONExporter exporter = new JSONExporter(getDominoContext(), expProj, f);
		boolean result = exporter.exportExamDocuments(exam, examDocs);
		assertThat(result, is(true));
		JsonObject readObj = readJSONObject("test.json");
		assertThat(readObj.containsKey("language"), is(true));
		assertThat(readObj.containsKey("exportTime"), is(true));
		assertThat(readObj.containsKey("project"), is(true));
		assertThat(readObj.containsKey("assembly"), is(true));
		assertThat(readObj.containsKey("doc_groups"), is(true));
	}
	
	@Test
	public void vocabExportDeserializationWithExam() throws Exception {
		setupData("Simple Exam", SkillType.Vocabulary, TestItemType.Vocabulary, "SPANISH", 
				ExamType.Seeding, 10, 15, true, VocabularyItem.class);
		assertThat(docIds.size(), is(10));
		JSONSerializer ser = getDocumentService().serializer();
		
		Exam exam = getExamDelegate().getExam(expProjId, expExamId);
		Map<ValidationFormID, List<HeadDocumentRevision>> examDocs = getDominoContext().getExamExportDelegate()
				.getOrderedExportDocuments(expProj, expUser, expExamId, exam.getContent().getFormIds());

		File f = new File(EXPORT_OUT_HOME, "test.json");
		JSONExporter exporter = new JSONExporter(getDominoContext(), expProj, f);

		boolean result = exporter.exportExamDocuments(exam, examDocs);
		assertThat(result, is(true));
		JsonObject readObj = readJSONObject("test.json");
		
		assertThat(readObj.containsKey("exportTime"), is(true));
		Date theTime = ser.dateFormat().parse(readObj.getString("exportTime")).get();
		assertThat(theTime, notNullValue());
		
		assertThat(readObj.containsKey("language"), is(true));
		JsonObject langObj = readObj.getJsonObject("language");
		Language l = ser.deserialize(Language.class, langObj.toString());
		assertThat(l, notNullValue());
		assertThat(l.getName(), is("SPANISH"));
		assertThat(l.getTrigraph(), is("SPA"));

		JsonObject projObj = readObj.getJsonObject("project");
		ProjectDescriptor pd = ser.deserialize(ProjectDescriptor.class, projObj.toString());
		assertThat(pd, notNullValue());
		assertThat(pd.getName(), is(expProj.getName()));
		assertThat(pd.getContent().getSkill(), is(SkillType.Vocabulary));

		JsonObject examObj = readObj.getJsonObject("assembly");
		Exam deserExam = ser.deserialize(Exam.class, examObj.toString());
		assertThat(deserExam, notNullValue());
		assertThat(deserExam.getName(), is("Simple Exam"));
		assertThat(deserExam.getContent().getExamType(), is(ExamType.Seeding));
		
		JsonArray groupArr = readObj.getJsonArray("doc_groups");
		assertThat(groupArr, hasSize(1));
		JsonObject groupObj = (JsonObject) groupArr.get(0);
		assertThat(groupObj.getString("group_id"), is("B"));
		JsonArray docArr = groupObj.getJsonArray("documents");
		docArr.forEach(docObj -> {
			SimpleHeadDocumentRevision shDoc = ser.deserialize(SimpleHeadDocumentRevision.class, docObj.toString());
			assertThat(shDoc, notNullValue());
			assertThat(docIds.contains(shDoc.getId()), is(true));
			assertThat(shDoc.getDocument() instanceof VocabularyItem, is(true));
		});
	}*/
	
//	@Test
//	public void vocabExportDeserialization() throws Exception {
//		setupData("Simple Exam", SkillType.Vocabulary, TestItemType.Vocabulary, "SPANISH", 
//				ExamType.Seeding, 10, 15, true, VocabularyItem.class);
//		assertThat(docIds.size(), is(10));
//		JSONSerializer ser = getDocumentService().serializer();
//		
//		File f = new File(EXPORT_OUT_HOME, "test.json");
//		JSONExporter exporter = new JSONExporter(getDominoContext(), expProj, f);
//		List<HeadDocumentRevision> docs = getDSDelegate().getHeavyDocuments(expProj, expUser, true, true, new FindOptions<>());
//		docs.remove(1);
//		boolean result = exporter.exportDocuments(docs);
//		assertThat(result, is(true));
//		JsonObject readObj = readJSONObject("test.json");
//		
//		assertThat(readObj.containsKey("exportTime"), is(true));
//		Date theTime = ser.dateFormat().parse(readObj.getString("exportTime")).get();
//		assertThat(theTime, notNullValue());
//		
//		assertThat(readObj.containsKey("language"), is(true));
//		JsonObject langObj = readObj.getJsonObject("language");
//		Language l = ser.deserialize(Language.class, langObj.toString());
//		assertThat(l, notNullValue());
//		assertThat(l.getName(), is("SPANISH"));
//		assertThat(l.getTrigraph(), is("SPA"));
//
//		JsonObject projObj = readObj.getJsonObject("project");
//		ProjectDescriptor pd = ser.deserialize(ProjectDescriptor.class, projObj.toString());
//		assertThat(pd, notNullValue());
//		assertThat(pd.getName(), is(expProj.getName()));
//		assertThat(pd.getContent().getSkill(), is(SkillType.Vocabulary));
//
//		JsonArray docArr = readObj.getJsonArray("documents");
//		assertThat(docArr, hasSize(9));
//		docArr.forEach(docObj -> {
//			SimpleHeadDocumentRevision shDoc = ser.deserialize(SimpleHeadDocumentRevision.class, docObj.toString());
//			assertThat(shDoc, notNullValue());
//			assertThat(docIds.contains(shDoc.getId()), is(true));
//			assertThat(shDoc.getDocument() instanceof VocabularyItem, is(true));
//		});
//	}
}
