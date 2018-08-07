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
//        console.log("making recorder  at " + new Date());

        var config = cfg || {};
        var bufferLen = config.bufferLen || 4096;
        this.context = source.context;
        this.node = (this.context.createScriptProcessor ||
            this.context.createJavaScriptNode).call(this.context, bufferLen, 2, 2);
        var worker = new Worker(config.workerPath || WORKER_PATH);


        var silenceDetectionConfig =  {};

        // how long of a silence before we start saying we've found silence
        silenceDetectionConfig.time =  1000;
        silenceDetectionConfig.amplitude =  0.2;


        worker.postMessage({
            command: 'init',
            config: {
                sampleRate: this.context.sampleRate
            }
        });

        var recording = false,
            currCallback, start, silenceCallback;

        this.node.onaudioprocess = function (e) {
            if (!recording) return;
            worker.postMessage({
                command: 'record',
                buffer: [
                    e.inputBuffer.getChannelData(0),
                    e.inputBuffer.getChannelData(1)
                ]
            });
            analyse();
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
            start=Date.now();
//      console.log("record " + "  at " + new Date().getTime());
        };

        this.stop = function () {
            //    source.disconnect(this.node);
            //    this.node.disconnect(this.context.destination);    //this should not be necessary
            recording = false;
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
//      console.log("exportMonoWAV " + "  at " + new Date().getTime());
            if (!currCallback) throw new Error('Callback not set');
            worker.postMessage({
                command: 'exportMonoWAV',
                type: type
            });
        };

        this.getAllZero = function (cb) {
            currCallback = cb || config.callback;
            worker.postMessage({command: 'getAllZero'})
        };

        worker.onmessage = function (e) {
            var blob = e.data;
            currCallback(blob);
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
            var time = silenceDetectionConfig.time;

            analyser.getByteTimeDomainData(dataArray);

            for (var i = 0; i < bufferLength; i++) {
                // Normalize between -1 and 1.
                var curr_value_time = (dataArray[i] / 128) - 1.0;
                if (curr_value_time > amplitude || curr_value_time < (-1 * amplitude)) {
                    start = Date.now();
                }
            }
            var newtime = Date.now();
            var elapsedTime = newtime - start;
            if (elapsedTime > time) {
                silenceDetected();
            }
        };


        var analyser = source.context.createAnalyser();
        analyser.minDecibels = -90;
        analyser.maxDecibels = -10;
        analyser.smoothingTimeConstant = 0.85;

        source.connect(analyser);
        analyser.connect(this.node);

//        source.connect(this.node);
        this.node.connect(this.context.destination);    //this should not be necessary
    };

})(window);
