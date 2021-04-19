function addQuotes(value){
	return '"'+ value +'"'
}
module.exports = {
  exec: function (cmd, successCallback, errorCallback) {
	if(Array.isArray(cmd)){
		cmd = cmd.map(c => {
			c = String(c)
			if(c.length && c.charAt(0) != '-' && (c.indexOf(' ') != -1 || c.indexOf("'") != -1)){
				c = addQuotes(c)
			}
			return c
		}).join(' ')
	}
    cordova.exec(successCallback, errorCallback, "FFMpeg", "exec", [cmd]);
  },
  kill: function (executionId) {
    cordova.exec(() => {}, console.error, "FFMpeg", "kill", [executionId]);
  },
  cleanup: function (executionIdsList) {
    cordova.exec(() => {}, console.error, "FFMpeg", "cleanup", [executionIdsList]);
  },
  exit: function () {
    cordova.exec(() => {}, console.error, "FFMpeg", "exit", []);
  }
};
