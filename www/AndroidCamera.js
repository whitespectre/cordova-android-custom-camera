var exec = require('cordova/exec');
var argscheck = require('cordova/argscheck');
var CustomCamera = function() {};

CustomCamera.prototype.recordVideo = function(success, fail, options) {
  var getValue = argscheck.getValue;

  var libraryFolder = getValue(options.libraryFolder, "My Challenge Tracker");
  var cancelText = getValue(options.cancelText, "Cancel");
  var tooltip = getValue(options.tooltip, "Max time limit 3 minutes");
  var errorStorage = getValue(options.errorStorage, "It seems that you don't have enought storage to record videos, please free up some memory");
  var errorGeneral = getValue(options.errorGeneral, "Oops, something went wrong.  Please try again.");

  return cordova.exec(
    success, 
    fail, 
    "AndroidCamera", 
    "recordVideo", [
      libraryFolder,
      cancelText,
      tooltip,
      errorStorage,
      errorGeneral
    ]);
};

window.AndroidCamera = new CustomCamera();