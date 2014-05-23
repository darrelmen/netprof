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
mode  := "sge-mt.q@mt9"
jobs  := 40
//logfn = "test.log"

// -------------------------------------------------------------------------------------------------------------------------------------
// Input parameters
// -------------------------------------------------------------------------------------------------------------------------------------
val parts       = 30;
val sampleRate  = 16000.0f
val nhidden = "2000,2000"
val prefix          = "/data/corpora/dli/dli-urdu"

val initCorpus = new HTKCorpus(prefix = prefix / "audio.raw.npUrdu+urduCE", srate = sampleRate, spkfn = null, anafn = prefix / "HydecFiles" / "analist" / "npUrdu+urduCE.analist.all")
val initDict   = Dictionary(prefix / "HydecFiles" / "dictionary" / "npUrdu+urduCE.dict.all.htk", format = "htk")
val initTrans  = Transcript(prefix / "HydecFiles" / "trans" / "npUrdu+urduCE.trans.all", "swb")

val trainCorpus = new HTKCorpus(prefix = prefix / "audio.raw.npUrdu+urduCE", srate = sampleRate, spkfn = null, 
                                anafn = prefix / "HydecFiles" / "analist" / "npUrdu+urduCE.analist.train")
val trainDict   = Dictionary(prefix / "HydecFiles" / "dictionary" / "npUrdu+urduCE.dict.train.htk", format = "htk")
val trainTrans  = Transcript(prefix / "HydecFiles" / "trans" / "npUrdu+urduCE.trans.train", "swb")

val silWords = Set("sil", "sp", "<s>", "</s>", "<no-speech>", "<cough>", "<breath>", "<sta>", "<int>", "<lipsmack>")
// -------------------------------------------------------------------------------------------------------------------------------------
// Setup FE and other shared components
// -------------------------------------------------------------------------------------------------------------------------------------
def fExtractor = SPFE(sampleRate = 16000.0f, frameRate = 100.0f, lofreq = 120, hifreq = 8000, maxDelta = 3, numcep = 13, ctype = "PLP", lifter = 0, nmfb = 24, window = 25.0f, lpcorder = 12)
def splitter   = Splitter[SegmentedFile, FEInfo](parts = parts)
def decsplit   = Splitter[SegmentedFile, FEInfo](parts = 30)

// -------------------------------------------------------------------------------------------------------------------------------------
// Process Definitions
// -------------------------------------------------------------------------------------------------------------------------------------
val feProcess    = splitter ->: fExtractor
def trainProcess(tag : String = "", pattr : String = prefix / "HydecFiles" / "pattr", qspec : String = prefix / "HydecFiles" / "q-spec", likeThreshold : Float = 1000.0f) = 
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
val resegRes   = trainInput -+>: (splitter ->: fExtractor ->: resegProcess(trans = prefix / "HydecFiles" / "trans" / "npUrdu+urduCE.trans.train", "models" / "rsi-sctm-hlda", "npUrdu+urduCE.train.reseg"))

//put decout print here. pass in new file
withPrint("npUrdu+urduCE.train.reseg.decout") { f =>
    for ((meta, part) <- resegRes; out <- part) 
      f.println(out.asInstanceOf[DecoderOutput].mkString("\n"));
  }

val resegTrans  = Transcript("models" / "npUrdu+urduCE.train.reseg.trans", "swb")

withPrint("models" / "npUrdu+urduCE.train.reseg.trans.clean") { f =>
  for ((uid, utt) <- resegTrans)
    f.println("<s> " + utt.words.map(_.word).filter(w => !silWords(w)).mkString(" ") + " </s> (" + uid + ")")
}

// -------------------------------------------------------------------------------------------------------------------------------------
// Train
// -------------------------------------------------------------------------------------------------------------------------------------
val resegCorpus = new HTKCorpus(prefix = prefix / "audio.raw.npUrdu+urduCE", srate = sampleRate, spkfn = null, anafn = "models" / "npUrdu+urduCE.train.reseg.analist")
val cleanResegTrans  = Transcript("models" / "npUrdu+urduCE.train.reseg.trans.clean", "swb")
val resegInput  = TrainInfo(transcript = cleanResegTrans, dictionary = trainDict, srate = sampleRate, 
                          file2spk = trainCorpus.file2spk, spk2file = trainCorpus.spk2file) -> resegCorpus.analist.values
val trainRes    = resegInput -+>: (decsplit ->: fExtractor ->: trainProcess(likeThreshold = 750.0f))

// -------------------------------------------------------------------------------------------------------------------------------------
// Train Language Models
// -------------------------------------------------------------------------------------------------------------------------------------
def cleanSil(evs : Array[String]) = evs.drop(1).dropRight(1).filter(e => e != "sp").mkString("<s> ", " ", " </s>")

val convTrans = for (utt <- Transcript(prefix / "HydecFiles" / "trans" / "npUrdu+urduCE.trans.train.unique", "swb").listView(wordsOnly))
                yield cleanSil(utt.map(ev => ev.word))

val uTrans = Transcript(prefix / "HydecFiles" / "trans" / "npUrdu+urduCE.trans.train.unique", "swb")

val ptrans = for (utt <- Transcript("models/1gr.swb", "swb").subset { case (k, v) => uTrans.contains(k) }.listView(wordsOnly))
                yield cleanSil(utt.map(ev => ev.word))

val lmInput = Vector("conv" -> convTrans, "phone" -> ptrans)
val lmRes   = lmInput =+>: lmProcess

// -------------------------------------------------------------------------------------------------------------------------------------
// resegment other subsets
// -------------------------------------------------------------------------------------------------------------------------------------
val devsets = Vector("npUrdu+urduCE.test"          -> (prefix / "HydecFiles" / "analist" / "npUrdu+urduCE.analist.test", prefix / "HydecFiles" / "trans" / "npUrdu+urduCE.trans.test"))
val out = "npUrdu+urduCE.test"
for ((out, (analist, trans)) <- devsets) {
  val resegCorpus = new HTKCorpus(prefix = prefix / "audio.raw.npUrdu+urduCE", srate = sampleRate, spkfn = null, anafn = analist)
  val resegInput  = initRes.head._1 -> resegCorpus.analist.values
  val resegRes    = resegInput -+>: (decsplit ->: fExtractor ->: resegProcess(trans, "models" / "rsi-sctm-hlda", out + ".reseg"))

  withPrint(out + ".reseg.decout") { f =>
    for ((meta, part) <- resegRes; out <- part) 
      f.println(out.asInstanceOf[DecoderOutput].mkString("\n"));
  }
}
val subResegTrans = Transcript("models" / out + ".reseg.trans", "swb")
withPrint("models" / out + ".reseg.trans.clean") { f =>
  for ((uid, utt) <- subResegTrans)
    f.println("<s> " + utt.words.map(_.word).filter(w => !silWords(w)).mkString(" ") + " </s> (" + uid + ")")
}

// -------------------------------------------------------------------------------------------------------------------------------------
// decode each of the devsets
// -------------------------------------------------------------------------------------------------------------------------------------
for ((out, (analist, trans)) <- devsets) {
  ("lattices." + out).mkdir

  val devCorpus  = new HTKCorpus(prefix = prefix / "audio.raw.npUrdu+urduCE", srate = sampleRate, spkfn = null, anafn = "models" / out + ".reseg.analist")
  val devInput   = resegRes.head._1 -> devCorpus.analist.values
  val devRes     = devInput -+>: (decsplit ->: fExtractor ->: decProcess("lattices." + out))

  withPrint(out + ".decout") { f =>
    for ((meta, part) <- devRes; out <- part) 
      f.println(out.asInstanceOf[DecoderOutput].mkString("\n"));
  }

  val wer = devRes =+>: WER(referenceTranscript = "models" / out + ".reseg.trans.clean")
}

//---------------------------------------
// NN training/testing
//---------------------------------------

//give Align rsi-sctm-hlda/dict (has sp)
val aliRes      = resegRes =+>: Align(models = "models" / "si-sctm-hlda", dictionary = "models/si-sctm-hlda/dict", alignTranscript = "models" / "npUrdu+urduCE.train.reseg.trans.clean", outputType = "words+sil") ->: 
                  Reco2Meta(writeTranscript = "si-sctm-hlda-realign.mlf", writeFormat="mlf", removeContext=true) 
val intrans    = aliRes.head._1.train411.transcript 
val nntrans    = new Transcript                                                                                                                             
for ((u, tg) <- intrans) nntrans(u) = tg.symbolTransform(s => if (s.name == "sp") 'sil else s)                                                               
val nnRes       = (aliRes.map(x => x._1.copy(train411 = x._1.train411.copy(transcript = nntrans))) -> aliRes.map(_._2) =+>: 
  NNTrain(lm = "models/phone.order-2.slf", stride = 1, cacheSize = 3900000, cvSize = 0.0f, threads = 2, frameWindow = 13,                                     
                          nHidden = nhidden.toString, bunchSize = 64, outType = "softmax", ignore = Set("unk")))


//create grammar.align from grammar
withPrint("models" / "grammar.align"){ f =>
  for(line <- FileLines("models" / "grammar")){
    if(!(line matches ".*Network.*")) f.println(line)
  }
}

val nnCorpus  = new HTKCorpus(prefix = prefix / "audio.raw.npUrdu+urduCE", srate = sampleRate, spkfn = null, anafn = "models" / "npUrdu+urduCE.test.reseg.analist")
val nnInput   = FEInfo(srate = sampleRate) -> nnCorpus.analist.values

val penalties = List(0.0f, -1.0f, -2.0f, -3.0f, -4.0f, -5.0f, -6.0f, -7.0f, -8.0f, -9.0f)
val weights = List(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f)

val nnTest =   nnInput -+>: (splitter ->: fExtractor ->: NNBatch(nOutput = 37, mlp = "models/nn-weights", nHidden = nhidden.toString, frameWindow = 13) ->: HybridAlign(grammar = "models/grammar.align", alignTranscript = "models/npUrdu+urduCE.test.reseg.trans.clean", dictionary = "models" / "rsi-sctm-hlda" / "dict-wo-sp", traceFlags = 0x3, optSil = true) ->: Reco2Meta(writeTranscript = "nnTest-align.trans", writeFormat="swb-phones") ->: NNScore(phoneIndex = "models/label-map"))

for(penalty <- penalties; weight <- weights){
  val hDec = nnTest =+>: (HybridDecode(processDictionary = false, dictionary = "models" / "rsi-sctm-hlda" / "dict.phones+null", lm = "models/phone.order-2.slf", grammar = "models/grammar", lmWeight = weight, wordPenalty = penalty, traceFlags = 0x2, beamThreshold = 700.0f) ->: WER(referenceTranscript = "models/nnTest-align.trans"))
}

