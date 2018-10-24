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

(function (window) {

    var WORKER_PATH = 'langtest/js/recorderWorker.js';

    window.Recorder = function (source, cfg) {
        console.log("window.Recorder : making recorder at " + new Date());

        var config = cfg || {};
        var bufferLen = config.bufferLen || 4096;

        var recording = false,
            currCallback, start, totalSamples, didStream;


        this.context = source.context;
        this.node = (this.context.createScriptProcessor ||
            this.context.createJavaScriptNode).call(this.context, bufferLen, 2, 2);
        var worker = new Worker(config.workerPath || WORKER_PATH);


        var silenceDetectionConfig = {};

        // how long of a silence before we start saying we've found silence
        silenceDetectionConfig.time = 1000;
        silenceDetectionConfig.amplitude = 0.2;


        worker.postMessage({
            command: 'init',
            config: {
                sampleRate: this.context.sampleRate
            }
        });


        // when audio samples come in, they come in here and passed to the worker
        this.node.onaudioprocess = function (e) {
            if (recording) {
                //      console.log("onaudioprocess recording...");
                var mytype = config.type || 'audio/wav';
                worker.postMessage({
                    command: 'record',
                    buffer: [
                        e.inputBuffer.getChannelData(0),
                        e.inputBuffer.getChannelData(1)
                    ],
                    type: mytype
                });
                analyse();
            }
            else {
                //console.log("onaudioprocess not recording...");
            }
        };

        this.configure = function (cfg) {
            for (var prop in cfg) {
                if (cfg.hasOwnProperty(prop)) {
                    config[prop] = cfg[prop];
                }
            }
        };

        this.record = function () {
            // source.connect(this.node);
            //  this.node.connect(this.context.destination);    //this should not be necessary
            //source.start();
            recording = true;
            start = Date.now();
            totalSamples = 0;
            //   console.log("Recorder.record at " + new Date().getTime());
        };

        this.stop = function () {
            //    source.disconnect(this.node);
            //    this.node.disconnect(this.context.destination);    //this should not be necessary
            recording = false;
            // didStream = false;
//      console.log("stop " + "  at " + new Date().getTime());
        };

        this.clear = function () {
            worker.postMessage({command: 'clear'});
        };

        this.getBuffers = function (cb) {
            currCallback = cb || config.callback;
            if (!currCallback) throw new Error('Callback not set');
            worker.postMessage({command: 'getBuffers'})
        };

        this.exportWAV = function (cb, type) {
            currCallback = cb || config.callback;
            type = type || config.type || 'audio/wav';
            if (!currCallback) throw new Error('Callback not set');
            worker.postMessage({
                command: 'exportWAV',
                type: type
            });
        };

        // called from webaudiorecorder.grabWav
        this.exportMonoWAV = function (cb, type) {
            currCallback = cb || config.callback;
            type = type || config.type || 'audio/wav';
            console.log("exportMonoWAV " + "  at " + new Date().getTime());
            //  if (!currCallback) throw new Error('Callback not set');
            worker.postMessage({
                command: 'exportMonoWAV',
                type: type
            });
        };

        // see webaudiorecorder serviceStartStream
        this.serviceStartStream = function (url, exid, reqid, isreference, audiotype, dialogSessionID, recordingSession, cb) {
            currCallback = cb || config.callback;

            if (url) {
                // console.log('service.startStream url ' + url);
            }
            else {
                console.log('service.startStream url undefined');
            }
            //  if (!currCallback) throw new Error('Callback not set');
            didStream = true;

            worker.postMessage({
                command: 'startStream',
                url: url,
                exid: exid,
                reqid: reqid,
                isreference: isreference,
                audiotype: audiotype,
                dialogSessionID: dialogSessionID,
                recordingSession: recordingSession
            });
        };

        this.serviceStopStream = function (abort, cb) {
            currCallback = cb || config.callback;

            if (didStream) {
                //   source.disconnect(analyser);
                didStream = false;
                currCallback = cb || config.callback;

                //  console.log("serviceStopStream " + " abort " + abort + " at " + new Date().getTime());

                if (!currCallback) throw new Error('Callback not set');
                worker.postMessage({
                    command: 'stopStream',
                    type: 'audio/wav',
                    abort: abort
                });
            }
            else console.log("stopStream - never started.")
        };

        // get reply from worker
        worker.onmessage = function (e) {
            if (currCallback) {

                if (e.data.startsWith("{\"status\"")) { // stop recording!
                    console.log("stop recording! ", e.data);
                    recording = false;

                    // if (source.context && source.context.state === 'running') {
                    //     source.context.suspend().then(function () {
                    //         __log('worker.onmessage suspended recording...');
                    //         source.stop();
                    //     });
                    // }
                }
                else {
                    //  console.log("worker.onmessage ", e.data);

                }
                if (typeof currCallback === 'function') {
                    currCallback(e.data);
                }
                else {
                    console.log("currCallback not a function")
                }
            }
            else {
                console.log("currCallback not set - maybe that's ok...?")
            }
        };

        this.getAllZero = function (cb) {
            currCallback = cb || config.callback;
            worker.postMessage({command: 'getAllZero'})
        };

        /**
         * Checks the time domain data to see if the amplitude of the audio waveform is more than
         * the silence threshold. If it is, "noise" has been detected and it resets the start time.
         * If the elapsed time reaches the time threshold the silence callback is called. If there is a
         * visualizationCallback it invokes the visualization callback with the time domain data.
         */
        var analyse = function () {
            analyser.fftSize = 2048;
            var bufferLength = analyser.fftSize;
            var dataArray = new Uint8Array(bufferLength);
            var amplitude = silenceDetectionConfig.amplitude;

            var max = 0;
            var nonzero = 0;
            analyser.getByteTimeDomainData(dataArray);

            for (var i = 0; i < bufferLength; i++) {
                // Normalize between -1 and 1.
                var curr_value_time = (dataArray[i] / 128) - 1.0;
                if (curr_value_time > amplitude || curr_value_time < (-1 * amplitude)) {
                    start = Date.now();
                    if (curr_value_time > max) {
                        max = curr_value_time;
                    }
                }
                else if (curr_value_time !== 0) {
                    nonzero = curr_value_time;
                }
            }
            var newtime = Date.now();
            var elapsedTime = newtime - start;

            var time = silenceDetectionConfig.time;
            if (elapsedTime > time) {
                /*                console.log("analyze : SILENCE! elapsedTime is " + elapsedTime + " vs " + time + " max " + max + "/" + nonzero +
                                    " start " + start);*/
                silenceDetected();
            }
            /*            else if (max > 0) {
                            console.log("analyze : VAD      elapsedTime is " + elapsedTime + " vs " + time + " max " + max + " start " + start);
                        }*/
            /*         else {
                         console.log("analyze : SHORT SL elapsedTime is " + elapsedTime + " vs " + time + " max " + max + "/" +nonzero+
                             " start " + start);
                     }*/

        };


        var analyser = source.context.createAnalyser();
        analyser.minDecibels = -90;
        analyser.maxDecibels = -10;
        analyser.smoothingTimeConstant = 0.85;

        source.connect(analyser);
        analyser.connect(this.node);

        this.node.connect(this.context.destination);    //this should not be necessary
    };

})(window);
