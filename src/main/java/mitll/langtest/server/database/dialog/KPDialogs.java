package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.npdata.dao.SlickDialog;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Dialog data from Paul - 6/20/18
 */
public class KPDialogs extends DialogReader implements IDialogReader {
 // private static final Logger logger = LogManager.getLogger(KPDialogs.class);

  private final String docIDS =
      "333815\n" +
          "333816\n" +
          "333817\n" +
          "333818\n" +
          "333819\n" +
          "333821\n" +
          "333822\n" +
          "333823\n" +
          "333824\n" +
          "333825";

  private final String title =
      "Meeting someone for the first time\n" +
          "What time is it?\n" +
          "You dialed the wrong number.\n" +
          "What will you do during the coming school break?\n" +
          "Where should I go to exchange currency?\n" +
          "What do Koreans do in their spare time?\n" +
          "Please give me two tickets for the 10:30 showing?\n" +
          "Please exchange this for a blue tie.\n" +
          "Common Ailments and Symptoms\n" +
          "Medical Emergencies";

  private final String fltitle =
      "처음 만났을 때\n" +
          "지금 몇 시예요?\n" +
          "전화 잘못 거셨어요.\n" +

          "이번 방학에 뭐 할 거야?\n" +
          "환전하려면 어디로 가야 해요?\n" +
          "한국 사람들은 시간이 날 때 뭐 해요?\n" +

          "10시 반 표 두 장 주세요.\n" +
          "파란색 넥타이로 바꿔 주세요.\n" +
          "독감에 걸려서 고생했어.\n" +

          "구급차 좀 빨리 보내주세요.";

  private final String dir =
      "010_C01\n" +
          "001_C05\n" +
          "003_C09\n" +

          "023_C09\n" +
          "036_C13\n" +
          "001_C17\n" +
          "019_C17\n" +
          "001_C18\n" +
          "005_C29\n" +
          "010_C30";

  private final String unit =
      "1\n" +
          "2\n" +
          "3\n" +
          "3\n" +
          "4\n" +
          "5\n" +
          "5\n" +
          "5\n" +
          "8\n" +
          "8";
  private final String chapter =
      "1\n" +
          "5\n" +
          "9\n" +
          "9\n" +
          "13\n" +
          "17\n" +
          "17\n" +
          "18\n" +
          "29\n" +
          "30";
  private final String page =
      "12\n" +
          "5\n" +
          "5\n" +
          "15\n" +
          "25\n" +
          "5\n" +
          "26\n" +
          "4\n" +
          "7\n" +
          "12";
  private final String pres =
      "Topic Presentation B\n" +
          "Topic Presentation A\n" +
          "Topic Presentation A\n" +
          "Topic Presentation A\n" +
          "Topic Presentation C\n" +
          "Topic Presentation A\n" +
          "Topic Presentation C\n" +
          "Topic Presentation A\n" +
          "Topic Presentation A\n" +
          "Topic Presentation B";

  private final String coreVocab1 = "고맙습니다 / 감사합니다\n" +
      "고향 \n" +
      "공군 \n" +
      "괜찮습니다 \n" +
      "네/예\n" +
      "만나서 반갑습니다 \n" +
      "미안합니다 / 죄송합니다\n" +
      "실례합니다 \n" +
      "~ 씨 \n" +
      "아니오 \n" +
      "어디\n" +
      "~이/가 어떻게 되십니까?\n" +
      "~은 / 는\n" +
      "~이/가\n" +
      "~이다 \n" +
      "이름 / 성함 (↑)\n" +
      "제 (↓) / 내\n" +
      "처음 뵙겠습니다\n" +
      "천만에요 \n" +
      "캘리포니아";

  private final String coreVocab2 = "가끔\n" +
      "~까지 \n" +
      "끝나다 \n" +
      "매일\n" +
      "반\n" +
      "~번\n" +
      "보내다 \n" +
      "~부터 \n" +
      "~ 분\n" +
      "~ 시 \n" +
      "시간 \n" +
      "시작(하다) \n" +
      "언제\n" +
      "얼마나 \n" +
      "~에\n" +
      "오전\n" +
      "오후\n" +
      "자주\n" +
      "저녁 \n" +
      "점심\n" +
      "~주/~주일\n" +
      "~쯤 \n" +
      "하루";

  private final String coreVocab3 = "국 \n" +
      "기다리다\n" +
      "남기다\n" +
      "다시\n" +
      "문자\n" +
      "메시지/메세지\n" +
      "바꾸다 \n" +
      "보내다\n" +
      "부탁(하다)/부탁드리다\n" +
      "여보세요\n" +
      "음성\n" +
      "~의\n" +
      "잠시만/잠깐만\n" +
      "전화(를) 걸다 \n" +
      "중학교\n" +
      "통화 중\n" +
      "핸드폰/휴대폰";

  private final String coreVocab4 = "나흘\n" +
      "~ 때 \n" +
      "~(으)로 \n" +
      "방학\n" +
      "~박~일 \n" +
      "비행기\n" +
      "사흘\n" +
      "아직\n" +
      "이틀\n" +
      "–(으)ㄹ 거다\n" +
      "–(으)ㄹ 수 있다 / 없다\n" +
      "특별하다\n" +
      "편하다\n" +
      "표\n" +
      "한 ~\n" +
      "힘들다";

  private final String coreVocab5 = "가방\n" +
      "네모나다\n" +
      "녹색\n" +
      "동그랗다\n" +
      "두고 내리다\n" +
      "모양\n" +
      "바꾸다\n" +
      "배낭\n" +
      "보라색\n" +
      "세모나다\n" +
      "센터\n" +
      "–(으)려면\n" +
      "잃어버리다\n" +
      "주황색\n" +
      "지하\n" +
      "확인(하다) \n" +
      "환전(하다)\n" +
      "환전소";

  private final String coreVocab6 = "게임\n" +
      "관람(하다) \n" +
      "낚시(하다) \n" +
      "다양(하다)\n" +
      "~대/세대\n" +
      "레저\n" +
      "뮤지컬\n" +
      "바이올린\n" +
      "스포츠\n" +
      "시간이 나다\n" +
      "악기\n" +
      "암벽등반\n" +
      "연극\n" +
      "연주(하다)\n" +
      "음악회\n" +
      "~ 이상\n" +
      "인터넷\n" +
      "조사(하다 / 되다)\n" +
      "주로\n" +
      "즐기다\n" +
      "초대권\n" +
      "피아노\n" +
      "활동\n" +
      "헬스";


  private final List<String> cv = Arrays.asList(coreVocab1, coreVocab2, coreVocab3, coreVocab4, coreVocab5, coreVocab6);

  private final DialogProps dialogProps = new DialogProps(docIDS, title, fltitle, dir, unit, chapter, page, pres);

  /**
   * @param defaultUser
   * @param exToAudio
   * @param englishProject
   * @return
   * @see mitll.langtest.server.database.project.DialogPopulate#populateDatabase
   */
  @Override
  public Map<Dialog, SlickDialog> getDialogs(int defaultUser,
                                             Map<ClientExercise, String> exToAudio,
                                             Project project, Project englishProject) {
    return getDialogsByProp(defaultUser, exToAudio, project, dialogProps, cv);
  }


}
