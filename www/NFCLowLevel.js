var exec = require('cordova/exec');

function initializeNFC() {
    console.log("Initializing NFC plugin");
    setTimeout(function() {
      cordova.exec(
        function() {
          console.log("Initialized the NfcPlugin");
        },
        function(reason) {
          console.log("Failed to initialize the NfcPlugin " + reason);
        },
        "NFCLowLevel",
        "init",
        []
      );
    }, 10);
  }
  
  document.addEventListener("deviceready", initializeNFC, false);
  
  // add all public functions to a single object which then can be exported
  var PluginNFCObject = {
  
    // listener for tag discovery
    addTagDiscoveredListener: function(success, error) {
      console.log("addTagDiscoveredListener invoked");
      exec(success, error, "NFCLowLevel", "addTagDiscoveredListener", []);
    },
  
    // connect
    connect: function(success, error) {
      console.log("connect invoked");
      exec(success, error, "NFCLowLevel", "connect", []);
    },
  
    // transceive
    transceive: function(data, success, error) {
      console.log("transceive invoked");
      exec(success, error, "NFCLowLevel", "transceive", [data]);
    },
  
    // close
    close: function(success, error) {
      console.log("close invoked");
      exec(success, error, "NFCLowLevel", "close", []);
    }
  };
  
  module.exports = PluginNFCObject;
  