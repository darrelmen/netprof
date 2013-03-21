#!/bin/bash
# -*- mode: scala -*-
exec env JAVA_OPTS="-Dfile.encoding=UTF8 -Xmx5g -Xss2M -XX:MaxPermSize=512M -XX:+UseParallelOldGC -XX:ReservedCodeCacheSize=128m" scripts/hydec-runner "$0" "$@"
!#
import corpus._
import decode._
import fe._
import train._
import pron._ 

// -------------------------------------------------------------------------------------------------------------------------------------
// Job Settings
// -------------------------------------------------------------------------------------------------------------------------------------
mode  := "sge-mt.q@mt7"
jobs  := 40
//logfn = "test.log"

// -------------------------------------------------------------------------------------------------------------------------------------
// Input parameters
// -------------------------------------------------------------------------------------------------------------------------------------
val parts       = 40;
val sampleRate  = 16000.0f
val nhidden = "1500,2000"
val prefix          = "/data/archive/jray/dli/LevantineExp12_3/HSonly+PlanFile"

val initCorpus = new HTKCorpus(prefix = prefix / "audio.rawFiles", srate = sampleRate, spkfn = null, anafn = prefix / "analist.lev.all")
val initDict   = Dictionary(dict = prefix / "dict.hs", format = "bbn")
val initTrans  = Transcript(prefix / "trans.lev.all", "swb")

val trainCorpus = new HTKCorpus(prefix = prefix / "audio.rawFiles", srate = sampleRate, spkfn = null, 
                                anafn = prefix / "analist.lev.train")
val trainDict   = Dictionary(dict = prefix / "dict.hs", format = "bbn")
val trainTrans  = Transcript(prefix / "trans.lev.train", "swb")

val silWords = Set("sil", "sp", "<s>", "</s>", "<no-speech>", "<cough>", "<breath>", "<sta>", "<int>", "<lipsmack>")
// -------------------------------------------------------------------------------------------------------------------------------------
// Setup FE and other shared components
// -------------------------------------------------------------------------------------------------------------------------------------
def fExtractor = SPFE(sampleRate = 16000.0f, frameRate = 100.0f, lofreq = 120, hifreq = 8000, maxDelta = 3, numcep = 13, ctype = "PLP", lifter = 0, nmfb = 24, window = 25.0f, lpcorder = 12)
def splitter   = Splitter[SegmentedFile, FEInfo](parts = parts)
def decsplit   = Splitter[SegmentedFile, FEInfo](parts = 40)

// -------------------------------------------------------------------------------------------------------------------------------------
// Process Definitions
// -------------------------------------------------------------------------------------------------------------------------------------
val feProcess    = splitter ->: fExtractor
def trainProcess(tag : String = "", pattr : String = prefix / "pattr", qspec : String = prefix / "q-spec", likeThreshold : Float = 1000.0f) = 
  FlatStart(nstates = "3", useDictionary = true) ->: EMTrain(stage = "1g" + tag, iterations = 5, useDictionary = true) ->:
  FullSP(stage = "1g" + tag + "sp") ->: EMTrain(stage = "1g" + tag + "sp", iterations = 5, useDictionary = true) ->:
  Align(lcExpand = false, rcExpand = false) ->: Reco2Meta(writeTranscript = "1g" + tag + ".swb", writeFormat = "swb-phones") ->:
  EMTrain(stage = "1g" + tag  + "spa", iterations = 5) ->: MakeCDModels(stage = "1g" + tag + "spa-tph") ->: EMTrain(stage = "1g" + tag + "spa-tph", iterations = 5) ->:
  StateCluster(stage = "1g" + tag + "spa-sctm", phoneAttributes = pattr, questionSpec = qspec, likeThreshold = likeThreshold, occThreshold = 150.0f) ->:
  GaussMixUp(suffix = "g" + tag + "spa-sctm", iterations = 5, numSplits = 5, silFactor = 2.0f) ->: SyncModels(target = tag + "si-sctm") ->:
  HLDA(stage = "32g" + tag + "spa-sctm-hlda", iterations = 20) ->: EMTrain(stage = "32g" + tag + "spa-sctm-hlda", iterations = 5) ->: SyncModels(target = tag + "si-sctm-hlda")
def resegProcess(trans : String, model : String, output : String) = 
  Align(models = model, alignTranscript = trans, outputType = "words+sil") ->:
  ResegmentDecoderOutput(resegment=true, repad=true, splitThreshold=0.50f, silPadding=0.25f, silWords = silWords) ->:
  Reco2Meta(writeTranscript = output, writeFormat="reseg", removeContext=false)
def lmProcess    = NgramLM(ngMin = 1, ngMax = 8, discount = "-s ModKN")
def decProcess(latdir : String) = LVDecode(model = "models" / "si-sctm-hlda", lm = "models" / "conv.order-3.srilm", dictionary = "models" / "si-sctm-hlda" / "dict-wo-sp",
                                           pruneOptions = "-k 16 -t 350.0 250.0 -v 275.0 200.0", lmWeight = 15.0f, wordPenalty = 0.0f, latticeDir = latdir)


// -------------------------------------------------------------------------------------------------------------------------------------
// Resegment
// -------------------------------------------------------------------------------------------------------------------------------------
val initInput  = TrainInfo(transcript = initTrans, dictionary = initDict, srate = sampleRate, 
                          file2spk = initCorpus.file2spk, spk2file = initCorpus.spk2file) -> initCorpus.analist.values
val trainInput = TrainInfo(transcript = trainTrans, dictionary = trainDict, srate = sampleRate, 
                          file2spk = trainCorpus.file2spk, spk2file = trainCorpus.spk2file) -> trainCorpus.analist.values
val initRes    = initInput  -+>: (splitter ->: fExtractor ->: trainProcess(tag = "r"))
val resegRes   = trainInput -+>: (splitter ->: fExtractor ->: resegProcess(trans = prefix / "trans.lev.train", "models" / "rsi-sctm-hlda", "lev-train.reseg"))

//put decout print here. pass in new file
withPrint("decouts" / "lev-train.reseg.decout") { f =>
  for ((meta, part) <- resegRes; out <- part) 
    f.println(out.asInstanceOf[DecoderOutput].mkString("\n"));
}

val resegTrans  = Transcript("models" / "lev-train.reseg.trans", "swb")

withPrint("models" / "lev-train.reseg.trans.clean") { f =>
  for ((uid, utt) <- resegTrans)
    f.println("<s> " + utt.words.map(_.word).filter(w => !silWords(w)).mkString(" ") + " </s> (" + uid + ")")
}

//Train
val resegCorpus = new HTKCorpus(prefix = prefix / "audio.rawFiles", srate = sampleRate, spkfn = null, anafn = "models" / "lev-train.reseg.analist")
val cleanResegTrans  = Transcript("models" / "lev-train.reseg.trans.clean", "swb")
val resegInput  = TrainInfo(transcript = cleanResegTrans, dictionary = trainDict, srate = sampleRate, 
                          file2spk = trainCorpus.file2spk, spk2file = trainCorpus.spk2file) -> resegCorpus.analist.values
val trainRes    = resegInput -+>: (decsplit ->: fExtractor ->: trainProcess(likeThreshold = 500.0f))


// -------------------------------------------------------------------------------------------------------------------------------------
// Train Language Models
// -------------------------------------------------------------------------------------------------------------------------------------
def cleanSil(evs : Array[String]) = evs.drop(1).dropRight(1).filter(e => e != "sp").mkString("<s> ", " ", " </s>")
//val convTrans = for (utt <- Transcript(prefix / "trans.lev.all", "swb").subset { case (k, v) => !k.matches("""^.*_scripted.*$""") }.listView(wordsOnly))
//                yield cleanSil(utt.map(ev => ev.word))
val convTrans = for (utt <- Transcript(prefix / "trans.unique.all", "swb").subset { case (k, v) => !k.matches("""^.*_scripted.*$""") }.listView(wordsOnly))
                yield cleanSil(utt.map(ev => ev.word))
val uTrans = Transcript(prefix / "trans.unique.all", "swb")
//fix this
val ptrans = for (utt <- Transcript("models/1gr.swb", "swb").subset { case (k, v) => uTrans.contains(k) }.listView(wordsOnly))
                yield cleanSil(utt.map(ev => ev.word))

val lmInput = Vector("conv" -> convTrans, "phone" -> ptrans)
val lmRes   = lmInput =+>: lmProcess

// -------------------------------------------------------------------------------------------------------------------------------------
// resegment other subsets
// -------------------------------------------------------------------------------------------------------------------------------------
val devsets = Vector("test"          -> (prefix / "analist.lev.test", prefix / "trans.lev.test"))
for ((out, (analist, trans)) <- devsets) {
  val resegCorpus = new HTKCorpus(prefix = prefix / "audio.rawFiles", srate = sampleRate, spkfn = null, anafn = analist)
  val resegInput  = initRes.head._1 -> resegCorpus.analist.values
  val resegRes    = resegInput -+>: (decsplit ->: fExtractor ->: resegProcess(trans, "models" / "rsi-sctm-hlda", "lev-" + out + ".reseg"))

  withPrint("decouts" / "lev-" + out + ".reseg.decout") { f =>
    for ((meta, part) <- resegRes; out <- part) 
      f.println(out.asInstanceOf[DecoderOutput].mkString("\n"));
  }
}
val subResegTrans = Transcript("models" / "lev-test.reseg.trans", "swb")
withPrint("models" / "lev-test.reseg.trans.clean") { f =>
  for ((uid, utt) <- subResegTrans)
    f.println("<s> " + utt.words.map(_.word).filter(w => !silWords(w)).mkString(" ") + " </s> (" + uid + ")")
}

// -------------------------------------------------------------------------------------------------------------------------------------
// decode each of the devsets
// -------------------------------------------------------------------------------------------------------------------------------------
for ((out, (analist, trans)) <- devsets) {
  ("lattices." + out).mkdir

  val devCorpus  = new HTKCorpus(prefix = prefix / "audio.rawFiles", srate = sampleRate, spkfn = null, anafn = "models" / "lev-" + out + ".reseg.analist")
  val devInput   = trainRes.head._1 -> devCorpus.analist.values
  val devRes     = devInput -+>: (decsplit ->: fExtractor ->: decProcess("lattices." + out))

  withPrint("decouts" / "lev-" + out + ".decout") { f =>
    for ((meta, part) <- devRes; out <- part) 
      f.println(out.asInstanceOf[DecoderOutput].mkString("\n"));
  }

  val wer = devRes =+>: WER(referenceTranscript = "models/lev-test.reseg.trans.clean")
}

//---------------------------------------
// NN training/testing
//---------------------------------------

//give Align si-sctm-hlda/dict (has sp)
val aliRes      = trainRes =+>: Align(models = "models" / "si-sctm-hlda", dictionary = "models/si-sctm-hlda/dict", alignTranscript = "models/lev-train.reseg.trans.clean", outputType = "words+sil") ->: 
                  Reco2Meta(writeTranscript = "si-sctm-hlda-realign.mlf", writeFormat="mlf", removeContext=true)
val intrans    = aliRes.head._1.train411.transcript 
val nntrans    = new Transcript                                                                                                                             
for ((u, tg) <- intrans) nntrans(u) = tg.symbolTransform(s => if (s.name == "sp") 'sil else s)                                                               
val nnRes       = (aliRes.map(x => x._1.copy(train411 = x._1.train411.copy(transcript = nntrans))) -> aliRes.map(_._2) =+>: 
  NNTrain(lm = "models/phone.order-2.slf", stride = 1, cacheSize = 3900000, cvSize = 0.0f, threads = 2, frameWindow = 13,                                     
                          nHidden = nhidden.toString, bunchSize = 32, outType = "softmax", ignore = collection.mutable.Set()))

//create grammar.align from grammar
withPrint("models" / "grammar.align"){ f =>
  for(line <- FileLines("models" / "grammar")){
    if(!(line matches ".*Network.*")) f.println(line)
  }
}

val nnCorpus  = new HTKCorpus(prefix = prefix / "audio.rawFiles", srate = sampleRate, spkfn = null, anafn = "models" / "lev-test.reseg.analist")
val nnInput   = FEInfo(srate = sampleRate) -> nnCorpus.analist.values

val penalties = List(0.0f, -1.0f, -2.0f, -3.0f, -4.0f, -5.0f, -6.0f)
val weights = List(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f)
//add optSil = true to HA, change si
//will fail at grammar.align. cp grammar to grammar.align and remove the network file line
val nnTest =   nnInput -+>: (splitter ->: fExtractor ->: NNBatch(nOutput = 35, mlp = "models/nn-weights", nHidden = nhidden.toString, frameWindow = 13) ->: HybridAlign(grammar = "models/grammar.align", alignTranscript = "models/lev-test.reseg.trans.clean", dictionary = "models/si-sctm-hlda/dict-wo-sp", traceFlags = 0x3, optSil = true) ->: Reco2Meta(writeTranscript = "nnLevtest.align.trans", writeFormat="swb-phones") ->: NNScore(phoneIndex = "models/label-map"))

//output of the forced alignment using hybridAlign
withPrint("decouts" / "lev.nntrain-hybridAlign.decout") { f =>
  for ((meta, part) <- nnTest; out <-  part) 
    f.println(out.asInstanceOf[DecoderOutput].mkString("\n"));
}

for(penalty <- penalties; weight <- weights){
  val hDec = nnTest =+>: (HybridDecode(grammar = "models/grammar", lmWeight = weight, wordPenalty = penalty, traceFlags = 0x2, beamThreshold = 700.0f) ->: WER(referenceTranscript = "models/nnLevtest.align.trans"))
  //one for each combo of penalty and weight
  withPrint("decouts" / "lev.nntrain-pen" + penalty + "lmw-" + weight + ".decout") { f =>
    for ((meta, part) <- hDec; out <-  part) 
      f.println(out.asInstanceOf[DecoderOutput].mkString("\n"));
  }
}


withPrint("models" / "lev.all.reseg.analist"){ f => 
  for(line <- FileLines("models" / "lev-train.reseg.analist"))
    f.println(line)
  for(line <- FileLines("models" / "lev-test.reseg.analist"))
    f.println(line)
}

withPrint("models" / "lev.all.reseg.trans.clean"){ f => 
  for(line <- FileLines("models" / "lev-train.reseg.trans.clean"))
    f.println(line)
  for(line <- FileLines("models" / "lev-test.reseg.trans.clean"))
    f.println(line)
}



println("nnscore values for all data")
//need to cat together lev-train.reseg.trans.clean and lev-test.reseg.trans.clean into lev-all.reseg.trans.clean and lev-train.reseg.analist with lev-test.reseg.analist to create lev-all.reseg.analist
val corp = new HTKCorpus(prefix = prefix / "audio.rawFiles", srate = sampleRate, spkfn = null, anafn = "models" / "lev.all.reseg.analist")
val scoreInput = FEInfo(srate = sampleRate) -> corp.analist.values
val normOutput = scoreInput -+>: (splitter ->: fExtractor ->: NNBatch(nOutput = 35, mlp = "models/nn-weights", nHidden = nhidden.toString, frameWindow = 13) ->: HybridAlign(grammar = "models/grammar.align", alignTranscript = "models/lev.all.reseg.trans.clean", dictionary = "models/si-sctm-hlda/dict-wo-sp", traceFlags = 0x2, optSil = true) ->: Reco2Meta(writeTranscript = "nnLevAll-align.trans", writeFormat = "swb-phones") ->: NNScore(phoneIndex = "models/label-map"))

