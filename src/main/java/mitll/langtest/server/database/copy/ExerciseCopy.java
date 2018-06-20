package mitll.langtest.server.database.copy;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.server.database.userexercise.UserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickExerciseAttributeJoin;
import mitll.npdata.dao.SlickRelatedExercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by go22670 on 2/22/17.
 */
public class ExerciseCopy {
  private static final Logger logger = LogManager.getLogger(ExerciseCopy.class);

  private static final boolean DEBUG = false;
  public static final String INTEGRATED_CHINESE_2 = "Integrated Chinese 2";
  public static final String UNIT = "Unit";
  public static final String LESSON = "Lesson";
  public static final String CUSTOM = "Custom";

  /**
   * TODO :  How to make sure we don't add duplicates?
   *
   * @param db
   * @param oldToNewUser
   * @param projectid
   * @param typeOrder
   * @param checkConvert
   * @see CopyToPostgres#copyUserAndPredefExercisesAndLists
   */
  Map<String, Integer> copyUserAndPredefExercises(DatabaseImpl db,
                                                  Map<Integer, Integer> oldToNewUser,
                                                  int projectid,
                                                  Map<Integer, String> idToFL,
                                                  Collection<String> typeOrder,
                                                  Map<String, Integer> parentToChild, boolean checkConvert) {

    logger.info("copyUserAndPredefExercises for " + projectid + " typeOrder is " + typeOrder);
    if (typeOrder.isEmpty()) {
      logger.error("copyUserAndPredefExercises huh? type order is empty?\n\n\n");
    }
    Map<String, List<Exercise>> idToCandidateOverride = new HashMap<>();
    List<Exercise> customExercises = addUserExercises(db, oldToNewUser, DatabaseImpl.IMPORT_PROJECT_ID, typeOrder, idToCandidateOverride);

    List<Exercise> converted = new ArrayList<>();

    if (checkConvert) {
      String eg = specialIDs.iterator().next();
      logger.info("e.g. '" + eg +
          "'");
      customExercises.forEach(exercise -> {
        String oldID = exercise.getOldID();
        if (isConvertable(exercise)) {
          convertChinese(converted, exercise, oldID);
        } else if (oldID.startsWith(CUSTOM)) {
          logger.warn("hmm not a convertable " + oldID);
        }
      });
    }

    customExercises.removeAll(converted);
    List<CommonExercise> toImport = db.getExercises(DatabaseImpl.IMPORT_PROJECT_ID);
    toImport.addAll(converted);
    logger.info("importing " + toImport.size() + " customExercises with " + converted.size());

    Map<Integer, Integer> dominoToExID = new HashMap<>();
    SlickUserExerciseDAO slickUEDAO = (SlickUserExerciseDAO) db.getUserExerciseDAO();
    parentToChild.putAll(addExercises(
        db.getUserDAO().getImportUser(),
        projectid,
        idToFL,
        slickUEDAO,
        toImport,
        typeOrder,
        idToCandidateOverride,
        dominoToExID));

    Map<String, Integer> exToInt = getOldToNewExIDs(db, projectid);
    reallyAddingUserExercises(projectid, typeOrder, slickUEDAO, exToInt, customExercises);

    logger.info("copyUserAndPredefExercises : finished copying customExercises - found " + exToInt.size());
    return exToInt;
  }

  private void convertChinese(List<Exercise> converted, Exercise exercise, String oldID) {
    logger.info("converting " + exercise);
    exercise.getMutable().setPredef(true);

// SWAP! alt is always traditional
    String simplifiedChinese  = getAltFL(oldID);
    String traditionalChinese = exercise.getForeignLanguage();

    exercise.setForeignLanguage(simplifiedChinese);
    exercise.setAltFL(traditionalChinese);

    Map<String, String> uv = new HashMap<>();
    uv.put(UNIT, INTEGRATED_CHINESE_2);
    uv.put(LESSON, getLesson(oldID));

    logger.info("converting " + oldID + " : " + uv);
    logger.info("converting " + oldID + " trad " + traditionalChinese + " to " + simplifiedChinese);

    exercise.setUnitToValue(uv);
    converted.add(exercise);
  }

  private String getAltFL(String oldID) {
    int i = specialIDs.indexOf(oldID);
    if (i == -1) {
      return "";
    } else {
      return altMandarin.get(i);
    }
  }

  private String getLesson(String oldID) {
    int i = specialIDs.indexOf(oldID);
    if (i == -1) {
      return "";
    } else {
      return altLessonList.get(i);
    }
  }

  private static final String mandarinSpecial = "Custom_12\n" +
      "Custom_3\n" +
      "Custom_4\n" +
      "Custom_5\n" +
      "Custom_6\n" +
      "Custom_7\n" +
      "Custom_8\n" +
      "Custom_9\n" +
      "Custom_10\n" +
      "Custom_11\n" +
      "Custom_13\n" +
      "Custom_14\n" +
      "Custom_15\n" +
      "Custom_16\n" +
      "Custom_17\n" +
      "Custom_18\n" +
      "Custom_19\n" +
      "Custom_20\n" +
      "Custom_21\n" +
      "Custom_22\n" +
      "Custom_2\n" +
      "Custom_23\n" +
      "Custom_24\n" +
      "Custom_25\n" +
      "Custom_27\n" +
      "Custom_28\n" +
      "Custom_29\n" +
      "Custom_30\n" +
      "Custom_31\n" +
      "Custom_32\n" +
      "Custom_33\n" +
      "Custom_34\n" +
      "Custom_35\n" +
      "Custom_36\n" +
      "Custom_37\n" +
      "Custom_38\n" +
      "Custom_39\n" +
      "Custom_40\n" +
      "Custom_41\n" +
      "Custom_42\n" +
      "Custom_43\n" +
      "Custom_44\n" +
      "Custom_45\n" +
      "Custom_46\n" +
      "Custom_47\n" +
      "Custom_48\n" +
      "Custom_49\n" +
      "Custom_50\n" +
      "Custom_51\n" +
      "Custom_52\n" +
      "Custom_53\n" +
      "Custom_54\n" +
      "Custom_55\n" +
      "Custom_56\n" +
      "Custom_57\n" +
      "Custom_58\n" +
      "Custom_59\n" +
      "Custom_60\n" +
      "Custom_61\n" +
      "Custom_62\n" +
      "Custom_63\n" +
      "Custom_64\n" +
      "Custom_65\n" +
      "Custom_66\n" +
      "Custom_67\n" +
      "Custom_68\n" +
      "Custom_69\n" +
      "Custom_70\n" +
      "Custom_71\n" +
      "Custom_72\n" +
      "Custom_73\n" +
      "Custom_74\n" +
      "Custom_75\n" +
      "Custom_76\n" +
      "Custom_77\n" +
      "Custom_78\n" +
      "Custom_79\n" +
      "Custom_80\n" +
      "Custom_81\n" +
      "Custom_82\n" +
      "Custom_83\n" +
      "Custom_84\n" +
      "Custom_85\n" +
      "Custom_86\n" +
      "Custom_87\n" +
      "Custom_88\n" +
      "Custom_89\n" +
      "Custom_90\n" +
      "Custom_91\n" +
      "Custom_92\n" +
      "Custom_93\n" +
      "Custom_94\n" +
      "Custom_95\n" +
      "Custom_96\n" +
      "Custom_97\n" +
      "Custom_98\n" +
      "Custom_99\n" +
      "Custom_100\n" +
      "Custom_243\n" +
      "Custom_185\n" +
      "Custom_186\n" +
      "Custom_187\n" +
      "Custom_188\n" +
      "Custom_189\n" +
      "Custom_190\n" +
      "Custom_191\n" +
      "Custom_192\n" +
      "Custom_193\n" +
      "Custom_194\n" +
      "Custom_195\n" +
      "Custom_196\n" +
      "Custom_197\n" +
      "Custom_198\n" +
      "Custom_199\n" +
      "Custom_200\n" +
      "Custom_201\n" +
      "Custom_202\n" +
      "Custom_203\n" +
      "Custom_204\n" +
      "Custom_205\n" +
      "Custom_206\n" +
      "Custom_207\n" +
      "Custom_208\n" +
      "Custom_209\n" +
      "Custom_210\n" +
      "Custom_211\n" +
      "Custom_212\n" +
      "Custom_213\n" +
      "Custom_214\n" +
      "Custom_216\n" +
      "Custom_217\n" +
      "Custom_218\n" +
      "Custom_219\n" +
      "Custom_220\n" +
      "Custom_221\n" +
      "Custom_222\n" +
      "Custom_223\n" +
      "Custom_224\n" +
      "Custom_225\n" +
      "Custom_226\n" +
      "Custom_227\n" +
      "Custom_228\n" +
      "Custom_230\n" +
      "Custom_231\n" +
      "Custom_232\n" +
      "Custom_234\n" +
      "Custom_235\n" +
      "Custom_236\n" +
      "Custom_237\n" +
      "Custom_238\n" +
      "Custom_239\n" +
      "Custom_240\n" +
      "Custom_241\n" +
      "Custom_242\n" +
      "Custom_244\n" +
      "Custom_245\n" +
      "Custom_246\n" +
      "Custom_247\n" +
      "Custom_250\n" +
      "Custom_251\n" +
      "Custom_252\n" +
      "Custom_157\n" +
      "Custom_158\n" +
      "Custom_159\n" +
      "Custom_161\n" +
      "Custom_162\n" +
      "Custom_163\n" +
      "Custom_164\n" +
      "Custom_165\n" +
      "Custom_166\n" +
      "Custom_167\n" +
      "Custom_168\n" +
      "Custom_169\n" +
      "Custom_170\n" +
      "Custom_171\n" +
      "Custom_172\n" +
      "Custom_173\n" +
      "Custom_174\n" +
      "Custom_175\n" +
      "Custom_176\n" +
      "Custom_177\n" +
      "Custom_178\n" +
      "Custom_179\n" +
      "Custom_180\n" +
      "Custom_181\n" +
      "Custom_253\n" +
      "Custom_254\n" +
      "Custom_255\n" +
      "Custom_256\n" +
      "Custom_257\n" +
      "Custom_258\n" +
      "Custom_259\n" +
      "Custom_260\n" +
      "Custom_261\n" +
      "Custom_262\n" +
      "Custom_263\n" +
      "Custom_264\n" +
      "Custom_265\n" +
      "Custom_266\n" +
      "Custom_267\n" +
      "Custom_268\n" +
      "Custom_271\n" +
      "Custom_270\n" +
      "Custom_272\n" +
      "Custom_273\n" +
      "Custom_274\n" +
      "Custom_275\n" +
      "Custom_276\n" +
      "Custom_277\n" +
      "Custom_278\n" +
      "Custom_279\n" +
      "Custom_280\n" +
      "Custom_281\n" +
      "Custom_282\n" +
      "Custom_283\n" +
      "Custom_284\n" +
      "Custom_285\n" +
      "Custom_286\n" +
      "Custom_287\n" +
      "Custom_288\n" +
      "Custom_289\n" +
      "Custom_290\n" +
      "Custom_291\n" +
      "Custom_292\n" +
      "Custom_293\n" +
      "Custom_296\n" +
      "Custom_297\n" +
      "Custom_298\n" +
      "Custom_299\n" +
      "Custom_300\n" +
      "Custom_301\n" +
      "Custom_302\n" +
      "Custom_303\n" +
      "Custom_304\n" +
      "Custom_305\n" +
      "Custom_306\n" +
      "Custom_307\n" +
      "Custom_308\n" +
      "Custom_309\n" +
      "Custom_310\n" +
      "Custom_311\n" +
      "Custom_312\n" +
      "Custom_313\n" +
      "Custom_314\n" +
      "Custom_315\n" +
      "Custom_316\n" +
      "Custom_317\n" +
      "Custom_318\n" +
      "Custom_319\n" +
      "Custom_320\n" +
      "Custom_321\n" +
      "Custom_322\n" +
      "Custom_323\n" +
      "Custom_324\n" +
      "Custom_325\n" +
      "Custom_326\n" +
      "Custom_327\n" +
      "Custom_328\n" +
      "Custom_329\n" +
      "Custom_330\n" +
      "Custom_331\n" +
      "Custom_332\n" +
      "Custom_333\n" +
      "Custom_334\n" +
      "Custom_335\n" +
      "Custom_336\n" +
      "Custom_337\n" +
      "Custom_338\n" +
      "Custom_339\n" +
      "Custom_340\n" +
      "Custom_341\n" +
      "Custom_342\n" +
      "Custom_343\n" +
      "Custom_344\n" +
      "Custom_345\n" +
      "Custom_346\n" +
      "Custom_347\n" +
      "Custom_348\n" +
      "Custom_349\n" +
      "Custom_357\n" +
      "Custom_358\n" +
      "Custom_359\n" +
      "Custom_360\n" +
      "Custom_361\n" +
      "Custom_362\n" +
      "Custom_363\n" +
      "Custom_364\n" +
      "Custom_365\n" +
      "Custom_366\n" +
      "Custom_367\n" +
      "Custom_368\n" +
      "Custom_369\n" +
      "Custom_370\n" +
      "Custom_371\n" +
      "Custom_372\n" +
      "Custom_374\n" +
      "Custom_375\n" +
      "Custom_376\n" +
      "Custom_377\n" +
      "Custom_378\n" +
      "Custom_379\n" +
      "Custom_380\n" +
      "Custom_381\n" +
      "Custom_382\n" +
      "Custom_383\n" +
      "Custom_384\n" +
      "Custom_385\n" +
      "Custom_386\n" +
      "Custom_387\n" +
      "Custom_388\n" +
      "Custom_389\n" +
      "Custom_390\n" +
      "Custom_391\n" +
      "Custom_392\n" +
      "Custom_393\n" +
      "Custom_394\n" +
      "Custom_395\n" +
      "Custom_396\n" +
      "Custom_397\n" +
      "Custom_398\n" +
      "Custom_399\n" +
      "Custom_400\n" +
      "Custom_401\n" +
      "Custom_402\n" +
      "Custom_403\n" +
      "Custom_404\n" +
      "Custom_405\n" +
      "Custom_406\n" +
      "Custom_407\n" +
      "Custom_408\n" +
      "Custom_409\n" +
      "Custom_410\n" +
      "Custom_411\n" +
      "Custom_412\n" +
      "Custom_413\n" +
      "Custom_414\n" +
      "Custom_415\n" +
      "Custom_416\n" +
      "Custom_417\n" +
      "Custom_418\n" +
      "Custom_419\n" +
      "Custom_420\n" +
      "Custom_421\n" +
      "Custom_422\n" +
      "Custom_423\n" +
      "Custom_424\n" +
      "Custom_425\n" +
      "Custom_426\n" +
      "Custom_427\n" +
      "Custom_428\n" +
      "Custom_429\n" +
      "Custom_430\n" +
      "Custom_431\n" +
      "Custom_432\n" +
      "Custom_433\n" +
      "Custom_434\n" +
      "Custom_435\n" +
      "Custom_436\n" +
      "Custom_437\n" +
      "Custom_438\n" +
      "Custom_439\n" +
      "Custom_440\n" +
      "Custom_441\n" +
      "Custom_442\n" +
      "Custom_443\n" +
      "Custom_444\n" +
      "Custom_445\n" +
      "Custom_446\n" +
      "Custom_447\n" +
      "Custom_448\n" +
      "Custom_449\n" +
      "Custom_450\n" +
      "Custom_451\n" +
      "Custom_452\n" +
      "Custom_453\n" +
      "Custom_454\n" +
      "Custom_455\n" +
      "Custom_456\n" +
      "Custom_457\n" +
      "Custom_458\n" +
      "Custom_459\n" +
      "Custom_460\n" +
      "Custom_461\n" +
      "Custom_462\n" +
      "Custom_463\n" +
      "Custom_464";

  String mandarinAlt = "今天天气比昨天好，不下雪了。\n" +
      "我约了朋友明天去滑冰，不知道天气会怎么样，冷不冷？\n" +
      "我刚才看了网上的天气预报，明天天气比今天更好。\n" +
      "不但不会下雪，而且会暖和一点。\n" +
      "是吗？太好了！\n" +
      "你约了谁去滑冰？\n" +
      "白英爱。\n" +
      "你约了白英爱？可是他今天早上坐飞机去纽约了。\n" +
      "真的啊？那我明天怎么办？\n" +
      "你还是在家看电视吧！\n" +
      "英爱，纽约那么好玩儿，你怎么在网上，没出去？\n" +
      "这儿的天气非常糟糕。\n" +
      "怎么了？\n" +
      "昨天下大雨，今天又下雨了。\n" +
      "这个周末这儿天气很好，你快一点回来吧。\n" +
      "这个周末纽约也会暖和一点儿。\n" +
      "我下个星期有一个面试，还不能回去。\n" +
      "我在加州找了一个工作，你也去吧。\n" +
      "加州冬天不冷，夏天不热，春天和秋天更舒服。\n" +
      "加州好是好，可是我更喜欢纽约。\n" +
      "今天天气比昨天好，不下雪了。\n" +
      "请进，请进。\n" +
      "人怎么这么多？好像一个位子都没有了。\n" +
      "服务员，请问，还有没有位子？\n" +
      "有，有，有，那张桌子没有人。\n" +
      "两位想吃点什么？\n" +
      "王朋，你点菜吧。\n" +
      "好。先给我们两盘饺子，要素的。\n" +
      "除了饺子以外，还要什么？\n" +
      "李友，你说呢？\n" +
      "还要一盘家常豆腐，不要放肉，我吃素。\n" +
      "我们的家常豆腐没有肉。\n" +
      "还要两碗酸辣汤，请别放味精，少放点儿盐。\n" +
      "有小白菜吗？\n" +
      "对不起，小白菜刚卖完。\n" +
      "那就不要青菜了。\n" +
      "那喝点儿什么呢？\n" +
      "我要一杯冰茶。\n" +
      "李友，你喝什么？\n" +
      "我很渴，请给我一杯可乐，多放点儿冰。\n" +
      "好，两盘饺子，一盘家常豆腐，\n" +
      "两碗酸辣汤，一杯冰茶，一杯可乐，多放冰。\n" +
      "还要别的吗？\n" +
      "不要别的了，这些够了。\n" +
      "服务员，我们都饿了，请上菜快一点。\n" +
      "没问题，菜很快就能做好。\n" +
      "师傅，请问今天晚饭有什么好吃的？\n" +
      "我们今天有糖醋鱼，\n" +
      "甜甜的、酸酸的，好吃极了，\n" +
      "你买一个吧。\n" +
      "好。今天有没有红烧牛肉？\n" +
      "没有。你已经要鱼了，别吃肉了。\n" +
      "来个凉拌黄瓜吧？\n" +
      "好。再来一碗米饭。\n" +
      "一共多少钱？\n" +
      "糖醋鱼，四块五，\n" +
      "凉拌黄瓜，一块七；\n" +
      "一碗米饭，五毛钱。一共六块七。\n" +
      "师傅，糟糕，我忘了带饭卡了。\n" +
      "这是十块钱。\n" +
      "找你三块三。\n" +
      "师傅，钱你找错了，\n" +
      "多找了我一块钱。\n" +
      "对不起，我没有看清楚。\n" +
      "没关系。\n" +
      "下个星期四再来。\n" +
      "好，再见。\n" +
      "小白，下课了，上哪儿去？\n" +
      "您好，常老师。\n" +
      "我想去学校的电脑中心，不知道怎么走\n" +
      "听说就在运动场旁边。\n" +
      "电脑中心没有运动场那么远。\n" +
      "你知道学校图书馆在哪里吗？\n" +
      "知道，离王朋的宿舍不远。\n" +
      "电脑中心离图书馆很近，\n" +
      "就在图书馆和学生活动中心中间。\n" +
      "常老师，您去哪儿呢？\n" +
      "我想到学校书店去买书。\n" +
      "书店在什么地方？\n" +
      "就在学生活动中心里边。\n" +
      "我们一起走吧。\n" +
      "好。\n" +
      "我们去中国城吃中国饭吧！\n" +
      "我没去过中国城，不知道中国城在哪儿。\n" +
      "没问题，你开车，我告诉你怎么走。\n" +
      "你有谷歌地图吗？拿给我看看。\n" +
      "手机在宿舍里，我忘了带了。\n" +
      "没有地图，走错了怎么办？\n" +
      "没有地图没关系，中国城我去过很多次，不用地图也能找到。\n" +
      "你从这儿一直往南开，到第三个路口，往西一拐就到了。\n" +
      "哎，我不知道东南西北。\n" +
      "那你一直往前开，到第三个红绿灯，往右一拐就到了。\n" +
      "不对，不对。\n" +
      "你看这个路口只能往左拐，不能往右拐。\n" +
      "那就是下一个路口。往右拐，再往前开。\n" +
      "到了，到了，你看见了吗？前面有很多中国字。\n" +
      "那不是中文，那是日文，我们到了小东京了。\n" +
      "是吗？那我们不吃中国饭了，吃日本饭吧！\n" +
      "王朋，你做什么呢？\n" +
      "我看书呢。\n" +
      "今天高小音过生日\n" +
      "晚上我们在她家开舞会，你能去吗？\n" +
      "能去。几点？\n" +
      "七点。\n" +
      "我们先吃饭，\n" +
      "吃完饭再唱歌跳舞。\n" +
      "有哪些人？\n" +
      "小音和她的男朋友\n" +
      "小音的表姊\n" +
      "白英爱、你妹妹王红，\n" +
      "听说还有小音的中学同学。\n" +
      "你要送给小音什么生日礼物？\n" +
      "我买了一本书送给她。\n" +
      "那我带什么东西？\n" +
      "饮料或者水果都可以。\n" +
      "那我带一些饮料，\n" +
      "再买一把花儿。\n" +
      "小音爱吃水果\n" +
      "我再买一些苹果、梨和西瓜吧！\n" +
      "你住的地方离小音家很远，\n" +
      "水果很重，我开车去接你\n" +
      "我们一起去吧。\n" +
      "好，我六点半在楼下等你。\n" +
      "王朋，李友，快进来。\n" +
      "小音，祝你生日快乐！\n" +
      "这是送给你的生日礼物。\n" +
      "谢谢！\n" +
      "太好了！我一直想买这本书。\n" +
      "带这么多东西，\n" +
      "你们太客气了。\n" +
      "哥哥，李友，你们来了。\n" +
      "啊。小红，你怎么样？\n" +
      "我很好。每天都在学英文。\n" +
      "小红，你每天练习英文练习多长时间？\n" +
      "三个半钟头。\n" +
      "还看两个钟头的英文电视。\n" +
      "哎，你们是什么时候到的？\n" +
      "刚到。\n" +
      "白英爱没跟你们一起来吗？\n" +
      "她还没来？\n" +
      "我以为她已经来了。\n" +
      "王朋，李友，来，我给你们介绍一下，\n" +
      "这是我表姊海伦，这是她的儿子汤姆。\n" +
      "你好，海伦。\n" +
      "你好，王朋。\n" +
      "文中和小音都说你又聪明又用功。\n" +
      "哪里，哪里。\n" +
      "你的中文说得真好，是在哪儿学的？\n" +
      "在暑期班学的。\n" +
      "哎，汤姆长得真可爱！\n" +
      "你们看，他笑了，他几岁了？\n" +
      "刚一岁，是去年生的，属狗。\n" +
      "你们看，他的脸圆圆的，眼睛大大的\n" +
      "鼻子高高的\n" +
      "嘴不大也不小，\n" +
      "长得很像海伦\n" +
      "妈妈这么漂亮\n" +
      "儿子长大一定也很帅。\n" +
      "来，来，来，我们吃蛋糕吧。\n" +
      "等等白英爱吧，\n" +
      "她最爱吃蛋糕\n" +
      "医生，我肚子疼死了。\n" +
      "你昨天吃什么东西了？\n" +
      "我姐姐上个星期过生日，蛋糕没吃完。\n" +
      "昨天晚上我吃了几口，夜里肚子就疼起来了，\n" +
      "今天早上上了好几次厕所。\n" +
      "你把蛋糕放在哪儿了？\n" +
      "放在冰箱里了。\n" +
      "放了几天了？\n" +
      "五、六天了。\n" +
      "发烧吗？\n" +
      "不发烧。\n" +
      "你躺下。先检查一下。\n" +
      "你吃蛋糕把肚子吃坏了。\n" +
      "得打针吗？\n" +
      "不用打针，吃这种药就可以。\n" +
      "一天三次，一次两片。\n" +
      "医生，一天吃几次？\n" +
      "请您再说一遍。\n" +
      "一天三次，一次两片。\n" +
      "好！饭前吃还是饭后吃？\n" +
      "饭前饭后都可以。\n" +
      "不过，你最好二十四小时不要吃饭。\n" +
      "那我要饿死了。\n" +
      "不行，这个办法不好。\n" +
      "王朋，你怎么了？\n" +
      "眼睛怎么红红的，感冒了吗？\n" +
      "没感冒\n" +
      "我也不知道眼睛怎么了，\n" +
      "最近这几天身体很不舒服。\n" +
      "眼睛又红又痒。\n" +
      "你一定是对什么过敏了。\n" +
      "我想也是，所以去药店买了一些药。\n" +
      "已经吃了四五种了，花了不少钱，都没有用。\n" +
      "把你的药拿出来给我看看。\n" +
      "这些就是。\n" +
      "这些药没有用。\n" +
      "为什么不去看病？\n" +
      "你没有健康保险吗？\n" +
      "我有保险。\n" +
      "可是我这个学期功课很多，看病太花时间。\n" +
      "那你也得赶快去看病\n" +
      "要不然病会越来越重。\n" +
      "我想再吃点儿别的药试试。\n" +
      "我上次生病，没去看医生，\n" +
      "休息了两天，最后也好了。\n" +
      "不行，不行，你太懒了。\n" +
      "再说，你不能自己乱吃药。\n" +
      "走，我跟你看病去。\n" +
      "王朋跟李友在同一个学校学习，他们认识已经快半年了。\n" +
      "王朋常常帮李友练习说中文。\n" +
      "他们也常常一起出去玩儿，\n" +
      "每次都玩儿得很高兴。\n" +
      "李友对王朋的印象很好\n" +
      "王朋也很喜欢李友，他们成了好朋友。\n" +
      "这个周末学校演一个中国电影，\n" +
      "我们一起去看，好吗？\n" +
      "好啊！不过听说看电影的人很多，\n" +
      "买得到票吗？\n" +
      "票已经买好了，\n" +
      "我费了很大的力气才买到。\n" +
      "好极了！我早就想看中国电影了。\n" +
      "还有别人跟我们一起去吗？\n" +
      "没有，就我们俩。\n" +
      "好。什么时候？\n" +
      "后天晚上八点\n" +
      "看电影以前，我请你吃饭。\n" +
      "太好了！一言为定。\n" +
      "喂，请问李友小姐在吗？\n" +
      "我就是。请问你是哪一位？\n" +
      "我姓费，你还记得我吗？\n" +
      "姓费？\n" +
      "你还记得上个月高小音的生日舞会吗？\n" +
      "我就是最后请你跳舞的那个人。\n" +
      "你再想想。想起来了吗？\n" +
      "对不起，我想不起来。\n" +
      "我是高小音的中学同学。\n" +
      "是吗？你是怎么知道我的电话号码的？\n" +
      "是小音告诉我的。\n" +
      "费先生，你有事吗？\n" +
      "这个周末你有空儿吗？我想请你去跳舞。\n" +
      "这个周末不行，下个星期我有三个考试。\n" +
      "没关系，下个周末怎么样？\n" +
      "你考完试，我们好好儿玩儿玩儿。\n" +
      "下个周末也不行，\n" +
      "我要从宿舍搬出去，得打扫，整理房间。\n" +
      "你看下下个周末，好不好？\n" +
      "对不起，下下个周末更不行了，\n" +
      "我要跟我的男朋友去纽约旅行。\n" +
      "……那……\n" +
      "费先生，对不起，我的手机没电了。再见！\n" +
      "喂……喂……\n" +
      "王朋在学校的宿舍住了两个学期了。\n" +
      "他觉得宿舍太吵，睡不好觉\n" +
      "房间太小，连电脑都放不下\n" +
      "再说也没有地方可以做饭，很不方便\n" +
      "所以准备下个学期搬出去住。\n" +
      "他找房子找了一个多月了，可是还没有找到合适的。\n" +
      "刚才他在报纸上看到了一个广告，说学校附近有一套公寓出租\n" +
      "离学校很近，走路只要五分钟，很方便。\n" +
      "公寓有一个卧室，一个厨房，一个卫生间，一个客厅，还带家具。\n" +
      "王朋觉得这套公寓可能对他很合适。\n" +
      "喂，请问你们是不是有公寓出租？\n" +
      "有啊，一房一厅，非常干净，还带家具\n" +
      "有什么家具？\n" +
      "客厅里有一套沙发、一张饭桌跟四把椅子。\n" +
      "卧室里有一张床、一张书桌和一个书架。\n" +
      "你们那里安静不安静？\n" +
      "非常安静。\n" +
      "每个月房租多少钱？\n" +
      "八百五十元。\n" +
      "八百五十美元？人民币差不多是…… 有一点儿贵，能不能便宜点儿？\n" +
      "那你不用付水电费。\n" +
      "要不要付押金？\n" +
      "要多付一个月的房租当押金，搬出去的时候还给你。\n" +
      "另外，我们公寓不准养宠物。\n" +
      "没关系，我对养宠物没有兴趣，什么宠物都不养。\n" +
      "那太好了。你今天下午来看看吧。\n" +
      "好\n" +
      "你看，我的肚子越来越大了。\n" +
      "你平常吃得那么多，又不运动，当然越来越胖了。\n" +
      "那怎么办呢？\n" +
      "如果怕胖，你一个星期运动两、三次，一次半个小时。\n" +
      "肚子就会小了。\n" +
      "我两年没运动了，做什么运动呢？\n" +
      "最简单的运动是跑步。\n" +
      "冬天那么冷，夏天那么热，跑步太难受了。\n" +
      "你打网球吧。\n" +
      "打网球得买网球拍、网球鞋\n" +
      "你知道，网球拍、网球鞋贵极了！\n" +
      "找几个人打篮球吧。买个篮球很便宜。\n" +
      "那每次都得打电话约人，麻烦死了。\n" +
      "你去游泳吧。\n" +
      "不用找人，也不用花很多钱，什么时候去都可以。\n" +
      "游泳？我怕水，太危险了，淹死了怎么办？\n" +
      "我也没办法了\n" +
      "你不愿意运动，那就继续胖下去吧。\n" +
      "王朋的妹妹王红刚从北京来，要在美国上大学\n" +
      "现在住在高小音家里学英文。\n" +
      "王朋的妹妹王红刚从北京来，要在美国上大学\n" +
      "现在住在高小音家里学英文。\n" +
      "为了提高英文水平，她每天都看两个小时的电视。\n" +
      "快把电视打开，足球比赛开始了。\n" +
      "是吗？我也喜欢看足球赛。\n" +
      "这是什么足球啊？怎么不是圆的？\n" +
      "这不是国际足球，这是美式足球。\n" +
      "足球应该用脚踢，为什么那个人用手抱著跑呢？\n" +
      "美式足球可以用手。\n" +
      "你看，你看，那么多人都压在一起\n" +
      "下面的人不是要被压坏了吗？\n" +
      "别担心，他们的身体都很棒，而且还穿特别的运动服，没问题。\n" +
      "看了半天也看不懂。还是看别的吧。\n" +
      "你在美国住半年就会喜欢了。\n" +
      "我男朋友看美式足球的时候，常常连饭都忘了吃。\n" +
      "李友，时间过得真快，马上就要放假了\n" +
      "我们的同学，有的去暑期班学习，有的去公司实习，有的回家打工，你有什么计划？\n" +
      "我还没有想好。你呢，王朋。\n" +
      "我暑假打算回北京去看父母。\n" +
      "是吗？我听说北京这个城市很有意思。\n" +
      "当然。北京是中国的首都，也是中国的政治、文化中心，有很多名胜古迹。\n" +
      "对啊，长城很有名。\n" +
      "还有，北京的好饭馆多得不得了。\n" +
      "真的？我去过香港、台北，还没去过北京，要是能去北京就好了。\n" +
      "那你跟我一起回去吧，我当你的导游。\n" +
      "真的吗？那太好了！护照我已经有了，我得赶快办签证。\n" +
      "那我马上给旅行社打电话订飞机票。\n" +
      "天一旅行社，你好\n" +
      "你好。请问六月初到北京的机票多少钱？\n" +
      "您要买单程票还是往返票？\n" +
      "我要买两张往返票。\n" +
      "你想买哪家航空公司的？\n" +
      "哪家的便宜，就买哪家的。\n" +
      "请等等，我查一下……\n" +
      "好几家航空公司都有航班。中国国际航空公司，一千五，直飞。\n" +
      "西北航空公司正在打折，差不多一千四百六十，可是要转机。\n" +
      "西北只比国航便宜四十几块钱，我还是买国航吧。\n" +
      "哪一天走？哪一天回来？\n" +
      "六月十号走，七月十五号回来。现在可以订位子吗？\n" +
      "可以。你们喜欢靠窗户的还是靠走道的？\n" +
      "靠走道的。\n" +
      "对了，我朋友吃素，麻烦帮他订一份素餐。\n" +
      "没问题……您在北京要订旅馆、租车吗？\n" +
      "不用，谢谢。\n" +
      "小姐，这是我们的机票。\n" +
      "请把护照给我看看。你们有几件行李要托运？\n" +
      "两件。这个包不托运，我们带上飞机。\n" +
      "麻烦您把箱子拿上来。\n" +
      "小姐，没超重吧？\n" +
      "没有，这是你们的护照、机票，这是登机牌。\n" +
      "请到五号登机口上飞机。\n" +
      "谢谢\n" +
      "哥哥，你们去北京了，就我一个人在这儿。\n" +
      "小红，别哭，我们几个星期就回来\n" +
      "你好好儿地学英文，别乱跑。\n" +
      "不是几个星期就回来，是几个星期以后才回来。\n" +
      "别担心，我姐姐小音会照顾你。\n" +
      "对，别担心。\n" +
      "飞机几点起飞？\n" +
      "中午十二点，还有两个多小时。\n" +
      "白英爱，你什么时候去纽约实习？\n" +
      "我不去纽约了。文中帮我在加州找了一份实习工作。\n" +
      "对，我们下个星期开车去加州。\n" +
      "是吗？一边儿开车，一边儿玩儿，太好了。\n" +
      "开车小心，祝你们玩儿得高兴。\n" +
      "祝你们一路平安。\n" +
      "到了北京以后，别忘了给我们发个电子邮件。\n" +
      "好，那我们秋天见。\n" +
      "下个学期见。\n" +
      "再见\n" +
      "小朋！\n" +
      "爸！妈！\n" +
      "累坏了吧？\n" +
      "还好。爸，妈，我给你们介绍一下，这是我的同学李友。\n" +
      "叔叔、阿姨，你们好。\n" +
      "欢迎你来北京。\n" +
      "李友，你的中文说得真好。\n" +
      "谢谢，是因为王朋教得好\n" +
      "哪里，是因为你聪明。\n" +
      "哎，你们俩都聪明。\n" +
      "小朋，你好像瘦了点儿。是不是打工太忙，没有时间吃饭？\n" +
      "我没瘦。我常常运动，身体比以前棒多了。\n" +
      "小红怎么样？\n" +
      "她很好，英文水平提高了很多。\n" +
      "走吧，我们上车以后，再慢慢儿地聊吧。\n" +
      "爷爷、奶奶在烤鸭店等我们呢！\n" +
      "烤鸭店";

  String altLesson = "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L11\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L12\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L13\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L14\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L15\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L16\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L17\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L18\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L19\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20\n" +
      "L20";

  private List<String> specialIDs = Arrays.asList(mandarinSpecial.split("\n"));
  private List<String> altMandarin = Arrays.asList(mandarinAlt.split("\n"));
  private List<String> altLessonList = Arrays.asList(altLesson.split("\n"));


  private boolean isConvertable(Exercise exercise) {
    Set<String> convert = new HashSet<>(specialIDs);
    return convert.contains(exercise.getOldID());
  }

  Map<String, Integer> getOldToNewExIDs(DatabaseImpl db, int projectid) {
    return ((SlickUserExerciseDAO) db.getUserExerciseDAO()).getOldToNew(projectid).getOldToNew();
  }

  /**
   * @param projectid
   * @param typeOrder
   * @param slickUEDAO
   * @param exToInt
   * @param exercises
   * @see #copyUserAndPredefExercises
   */
  private void reallyAddingUserExercises(int projectid,
                                         Collection<String> typeOrder,
                                         SlickUserExerciseDAO slickUEDAO,
                                         Map<String, Integer> exToInt,
                                         List<Exercise> exercises) {
    List<SlickExercise> bulk = new ArrayList<>();

    for (Exercise userCandidate : exercises) {
      String oldID = userCandidate.getOldID();
      Integer existingPredefID = exToInt.get(oldID);

      if (existingPredefID != null) {
        CommonExercise byExID = slickUEDAO.getByExID(existingPredefID, false);
        if (byExID.getEnglish().equals(userCandidate.getEnglish()) &&
            byExID.getForeignLanguage().equals(userCandidate.getForeignLanguage())) {
          logger.debug("reallyAddingUserExercises - user exercise with same old id " + oldID + " as predef " + byExID);
        } else {
          logger.warn("reallyAddingUserExercises Collision - user exercise with same old id " + oldID + " as predef " + byExID);
        }
      } else {
        bulk.add(slickUEDAO.toSlick(userCandidate, projectid, typeOrder));
      }
    }
    logger.info("reallyAddingUserExercises Adding " + bulk.size() + " user exercises");
    slickUEDAO.addBulk(bulk);
  }

  /**
   * @param importUser
   * @param projectid
   * @param idToFL
   * @param slickUEDAO
   * @param exercises
   * @param typeOrder
   * @param idToCandidateOverride
   * @param dominoToExID
   * @return parent->child map - NOT USED IN SYNC
   * @see #copyUserAndPredefExercises
   * @see mitll.langtest.server.domino.ProjectSync#getDominoUpdateResponse
   */
  public Map<String, Integer> addExercises(int importUser,
                                           int projectid,
                                           Map<Integer, String> idToFL,
                                           SlickUserExerciseDAO slickUEDAO,
                                           Collection<CommonExercise> exercises,
                                           Collection<String> typeOrder,
                                           Map<String, List<Exercise>> idToCandidateOverride, Map<Integer, Integer> dominoToExID) {

    logger.info("copyUserAndPredefExercises for project " + projectid +
        "\n\tfound " + exercises.size() + " old exercises" +
        "\n\tand   " + idToCandidateOverride.size() + " overrides");

    // TODO : why not add it to interface?
    Map<CommonExercise, Integer> exToInt = addExercisesAndAttributes(importUser, projectid, slickUEDAO, exercises, typeOrder,
        idToCandidateOverride, dominoToExID);

    idToFL.putAll(slickUEDAO.getIDToFL(projectid));

    logger.info("copyUserAndPredefExercises old->new for project #" + projectid +
        " : " + exercises.size() + " exercises, " + exToInt.size());

    return addContextExercises(projectid, slickUEDAO, exToInt, importUser, exercises, typeOrder);
  }

  /**
   * @param importUser
   * @param projectid
   * @param slickUEDAO
   * @param exercises
   * @param typeOrder
   * @see mitll.langtest.server.domino.ProjectSync#getDominoUpdateResponse
   */
  public void addContextExercises(int importUser,
                                  int projectid,
                                  SlickUserExerciseDAO slickUEDAO,
                                  Collection<CommonExercise> exercises,
                                  Collection<String> typeOrder) {
    List<SlickRelatedExercise> pairs = new ArrayList<>();

    Timestamp now = new Timestamp(System.currentTimeMillis());

    logger.info("addContextExercises adding " + exercises.size() + " context exercises ");
    for (CommonExercise context : exercises) {
      logger.info("addContextExercises adding context " + context);
      logger.info("addContextExercises context id     " + context.getID() + " with parent " + context.getParentExerciseID());
      if (context.getParentExerciseID() > 0) {
        SlickRelatedExercise e = insertContextExercise(projectid, slickUEDAO, importUser, typeOrder,
            now, context.getParentExerciseID(), context);
        context.getMutable().setID(e.contextexid());
        pairs.add(e);
      } else {
        logger.warn("addContextExercises ex " + context.getID() + " " + context.getEnglish() + " has no parent id set?");
      }
    }
    logger.info("addContextExercises adding " + pairs.size() + " pairs exercises ");

    slickUEDAO.addBulkRelated(pairs);
  }

  /**
   * @param importUser
   * @param projectid
   * @param slickUEDAO
   * @param exercises
   * @param typeOrder
   * @param idToCandidateOverride
   * @param dominoToExID
   * @return
   * @see #addExercises(int, int, Map, SlickUserExerciseDAO, Collection, Collection, Map, Map)
   * @see #addPredefExercises
   */
  private Map<CommonExercise, Integer> addExercisesAndAttributes(int importUser,
                                                                 int projectid,
                                                                 SlickUserExerciseDAO slickUEDAO,
                                                                 Collection<CommonExercise> exercises,
                                                                 Collection<String> typeOrder,
                                                                 Map<String, List<Exercise>> idToCandidateOverride,
                                                                 Map<Integer, Integer> dominoToExID) {
    Map<CommonExercise, Integer> exToInt = new HashMap<>();
    Map<Integer, List<Integer>> exToJoins =
        addPredefExercises(projectid, slickUEDAO, importUser, exercises, typeOrder, idToCandidateOverride, exToInt);
    exToInt.forEach((commonExercise, exid) -> dominoToExID.put(commonExercise.getDominoID(), exid));

    List<SlickExerciseAttributeJoin> joins = getSlickExerciseAttributeJoins( importUser, exToJoins);

    logger.info("copyUserAndPredefExercises adding " + joins.size() + " attribute joins");
    slickUEDAO.addBulkAttributeJoins(joins);
    return exToInt;
  }

  /**
   * @param importUser
   * @param exToJoins
   * @return
   * @paramx exToInt
   * @see #addExercisesAndAttributes(int, int, SlickUserExerciseDAO, Collection, Collection, Map, Map)
   */
  @NotNull
  private List<SlickExerciseAttributeJoin> getSlickExerciseAttributeJoins(//Map<Integer, Integer> exToInt,
                                                                          int importUser,
                                                                          Map<Integer, List<Integer>> exToJoins) {
    Timestamp nowT = new Timestamp(System.currentTimeMillis());
    List<SlickExerciseAttributeJoin> joins = new ArrayList<>();

    for (Map.Entry<Integer, List<Integer>> pair : exToJoins.entrySet()) {
      logger.info("getSlickExerciseAttributeJoins : ex->id  " + pair);
      // String key = pair.getKey();
      Integer dbID = pair.getKey();
      //     Integer dbID = exToInt.get(key);
      //logger.info("getSlickExerciseAttributeJoins : ex " + key + " = " + dbID);
//
//      if (dbID == null) {
//        logger.warn("getSlickExerciseAttributeJoins : huh? no db id for exercise " + key);
//      } else {
      pair.getValue().forEach(attrid -> joins.add(new SlickExerciseAttributeJoin(-1, importUser, nowT, dbID, attrid)));
//      }
    }

    return joins;
  }

  /**
   * Assumes we don't have exercise ids on the exercises yet.
   *
   * NOTE : only can handle one exercise and one matching context exercise.
   *
   * @param projectid
   * @param slickUEDAO
   * @param exToInt
   * @param importUser
   * @param exercises
   * @param typeOrder
   * @return parentToChild
   * @see #copyUserAndPredefExercises
   */
  private Map<String, Integer> addContextExercises(int projectid,
                                                   SlickUserExerciseDAO slickUEDAO,
                                                   Map<CommonExercise, Integer> exToInt,
                                                   int importUser,
                                                   Collection<CommonExercise> exercises,
                                                   Collection<String> typeOrder) {
    int n = 0;
    int ct = 0;
    List<SlickRelatedExercise> pairs = new ArrayList<>();

    Timestamp now = new Timestamp(System.currentTimeMillis());

    if (typeOrder.isEmpty()) {
      logger.error("addContextExercises : huh? type order is empty...?");
    }

    Set<String> missing = new HashSet<>();

    Map<String, Integer> parentToChild = new HashMap<>();

    for (CommonExercise ex : exercises) {
      String oldID = ex.getOldID();

      if (DEBUG) {
        logger.info("addContextExercises adding ex " + ex.getID() + " old " + oldID + " : " + ex.getEnglish() + " : " + ex.getForeignLanguage() + " with " + ex.getDirectlyRelated().size() + " sentences");
      }

      if (oldID == null) logger.error("addContextExercises : huh? old parentID is null for " + ex);
      Integer parentID = exToInt.get(ex);

//      logger.info("exToInt '" +oldID + "' => " +parentID + " vs ex parentID " + ex.getID());
      if (parentID == null) {
        logger.error("addContextExercises can't find " + oldID + " in map of " + exToInt.size());
        missing.add(oldID);
      } else {
        int contextCount = 1;
        for (CommonExercise context : ex.getDirectlyRelated()) {
          context.getMutable().setOldID(parentID + "_" + (contextCount++));

          SlickRelatedExercise relation = insertContextExercise(projectid, slickUEDAO, importUser, typeOrder, now, parentID, context);
          pairs.add(relation);
          int newContextExID = relation.contextexid();
          //  logger.info("\taddContextExercises context id is "+ context.getID());
          if (context.getID() == -1) {
//            logger.info("---> addContextExercises set context id to " + newContextExID);
            context.getMutable().setID(newContextExID);
          }
          parentToChild.put(oldID, newContextExID);

          if (DEBUG) {
            logger.info("addContextExercises map parent ex " + parentID + " -> child ex " + newContextExID +
                " ( " + ex.getDirectlyRelated().size());
          }

          ct++;
          if (ct % 400 == 0) logger.debug("addContextExercises inserted " + ct + " context exercises");
        }
        n++;
      }
    }

    if (!missing.isEmpty()) logger.error("huh? couldn't find " + missing.size() + " exercises : " + missing);

    slickUEDAO.addBulkRelated(pairs);
    logger.info("addContextExercises imported " + n + " predef exercises and " + ct + " context exercises, parent->child size " + parentToChild.size());

    return parentToChild;
  }

  private SlickRelatedExercise insertContextExercise(int projectid,
                                                     SlickUserExerciseDAO slickUEDAO,
                                                     int importUser,
                                                     Collection<String> typeOrder,
                                                     Timestamp now,
                                                     Integer parentExerciseID,
                                                     CommonExercise context) {
    int contextid =
        slickUEDAO.insert(slickUEDAO.toSlick(context, false, projectid, importUser, true, typeOrder));

    return new SlickRelatedExercise(-1, parentExerciseID, contextid, projectid, now);
  }

  /**
   * Actually add the exercises to postgres exercise table.
   *
   * @param projectid
   * @param slickUEDAO
   * @param importUser
   * @param exercises
   * @param typeOrder
   * @param idToCandidateOverride
   * @param exToInt
   * @see #addExercisesAndAttributes(int, int, SlickUserExerciseDAO, Collection, Collection, Map, Map)
   */
  private Map<Integer, List<Integer>> addPredefExercises(int projectid,
                                                         SlickUserExerciseDAO slickUEDAO,

                                                         int importUser,
                                                         Collection<CommonExercise> exercises,
                                                         Collection<String> typeOrder,
                                                         Map<String, List<Exercise>> idToCandidateOverride,
                                                         Map<CommonExercise, Integer> exToInt) {
    List<SlickExercise> bulk = new ArrayList<>();
    logger.info("addPredefExercises for " + projectid + " copying " + exercises.size() + " exercises");
    if (typeOrder == null || typeOrder.isEmpty()) {
      logger.error("addPredefExercises huh? no type order?");
    }
    long now = System.currentTimeMillis();

    Map<ExerciseAttribute, Integer> attrToID = new HashMap<>();
    Map<Integer, List<Integer>> exToJoins = new HashMap<>();

    int replacements = 0;
    int converted = 0;
//    logger.info("addPredefExercises adding " + exercises.size());

    for (CommonExercise ex : exercises) {
      String oldID = ex.getOldID();
      if (oldID.isEmpty()) {
        logger.warn("old id is empty for " + ex);
      }
      if (ex.isContext()) {
        logger.warn("addPredefExercises huh? ex " + ex.getOldID() + "/" + ex.getID() + " is a context exercise???\n\n\n");
      }
//      logger.info("addPredefExercises adding ex old #" + oldID + " " + ex.getEnglish() + " " + ex.getForeignLanguage());

      List<Exercise> exercises1 = idToCandidateOverride.get(oldID);

      CommonExercise exToUse = ex;
      if (exercises1 != null && !exercises1.isEmpty()) {
        for (CommonExercise candidate : exercises1) {
          if (candidate.getUpdateTime() > ex.getUpdateTime() &&
              !candidate.getEnglish().equals(ex.getEnglish()) &&
              !candidate.getForeignLanguage().equals(ex.getForeignLanguage())
              ) {
            logger.info("addPredefExercises" +
                "\n\tfor old id " + oldID +
                " replacing" +
                "\n\toriginal " + ex +
                "\n\twith     " + candidate);
            if (candidate.getDirectlyRelated().isEmpty()) {
              candidate.getDirectlyRelated().addAll(exToUse.getDirectlyRelated());
            }
            exToUse = candidate;
            replacements++;

            if (!exToUse.isPredefined()) {
              exToUse.getMutable().setPredef(true);
              converted++;
              logger.info("addPredefExercises converting " + exToUse.getID() + " " + exToUse.getForeignLanguage() + " " + ex.getEnglish());
            }
          }
        }
      }

      SlickExercise e = slickUEDAO.toSlick(exToUse,
          false,
          projectid,
          importUser,
          false, typeOrder);

      int exerciseID = slickUEDAO.insert(e);
      exToInt.put(exToUse, exerciseID);
      //bulk.add(e);

      addAttributesAndRememberIDs(slickUEDAO,
          projectid, importUser,
          now, attrToID, exToJoins,

          exerciseID, ex.getAttributes());
    }

//    logger.info("addPredefExercises add   bulk  " + bulk.size() + " exercises");
    // slickUEDAO.addBulk(bulk);
    logger.info("addPredefExercises added bulk  " + bulk.size() + " exercises, " + replacements + " replaced, " + converted + " converted");
    logger.info("addPredefExercises will add    " + exToJoins.size() + " attributes");
    return exToJoins;
  }

  /**
   * @param slickUEDAO
   * @param projectid
   * @param importUser
   * @param now
   * @param attrToID   map of attribute to db id
   * @param exToJoins  map of old ex id to new attribute db id
   * @param newID      - at this point we don't have exercise db ids - could be done differently...
   * @see #addPredefExercises
   */
  private void addAttributesAndRememberIDs(SlickUserExerciseDAO slickUEDAO,
                                           int projectid,
                                           int importUser,
                                           long now,
                                           Map<ExerciseAttribute, Integer> attrToID,
                                           Map<Integer, List<Integer>> exToJoins,

                                           //String oldID,
                                           int newID,
                                           List<ExerciseAttribute> attributes) {
    if (attributes != null && !attributes.isEmpty()) {
      List<Integer> joins = new ArrayList<>();
      exToJoins.put(newID, joins);
      addAttributes(slickUEDAO,
          projectid,
          importUser,
          attributes,
          now,
          attrToID,
          joins);
    }
  }

  /**
   * @param slickUEDAO
   * @param projectid
   * @param importUser
   * @param attributes to translate into slick attributes
   * @param now
   * @param attrToID   map of attribute to db id, so we can only store unique attributes (generally)
   * @param joins      attribute ids to associate with this exercise
   */
  private void addAttributes(SlickUserExerciseDAO slickUEDAO,
                             int projectid,
                             int importUser,
                             List<ExerciseAttribute> attributes,

                             long now,
                             Map<ExerciseAttribute, Integer> attrToID,
                             List<Integer> joins) {
    for (ExerciseAttribute attribute : attributes) {
      int id;
      if (attrToID.containsKey(attribute)) {
        id = attrToID.get(attribute);
      } else {
        id = slickUEDAO.addAttribute(projectid, now, importUser, attribute);
        attrToID.put(attribute, id);

        logger.info("addPredef " + attribute + " = " + id);
      }
      joins.add(id);
    }
  }

  /**
   * @param db
   * @param oldToNewUser
   * @param projectid
   * @param typeOrder
   * @see #copyUserAndPredefExercises
   */
  private List<Exercise> addUserExercises(DatabaseImpl db,
                                          Map<Integer, Integer> oldToNewUser,
                                          int projectid,
                                          Collection<String> typeOrder,
                                          Map<String, List<Exercise>> idToCandidateOverride) {
    List<Exercise> userExercises = new ArrayList<>();
    try {
      int c = 0;
      UserExerciseDAO ueDAO = new UserExerciseDAO(db);
      ueDAO.setExerciseDAO(db.getExerciseDAO(projectid)); // for the type order
      Collection<Exercise> allUserExercises = ueDAO.getAllUserExercises();
      if (allUserExercises.isEmpty()) {
        logger.error("addUserExercises : no user exercises for " + projectid + " and " + ueDAO);
      }
      logger.info("addUserExercises copying " + allUserExercises.size() + " user exercises for project " + projectid);

      if (typeOrder.isEmpty()) {
        logger.error("addUserExercises huh? for " + projectid + " type order is empty?\n\n\n");
      }

      int overrides = 0;
      int userEx = 0;
      for (Exercise userExercise : allUserExercises) {
        Integer userID = oldToNewUser.get(userExercise.getCreator());
        if (userID == null) {
          if (c++ < 50)
            logger.error("user exercise : no user " + userExercise.getCreator() + " for exercise " + userExercise);
        } else {
          userExercise.setCreator(userID);
          String oldID = userExercise.getOldID();
          if (userExercise.isOverride()) {
            List<Exercise> exercises = idToCandidateOverride.computeIfAbsent(oldID, k -> new ArrayList<>());
            exercises.add(userExercise);
            overrides++;
          } else {
            userExercises.add(userExercise);
            userEx++;
          }
        }
      }

      logger.info("addUserExercises overrides " + overrides + " user ex " + userEx);

    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return userExercises;
  }
}