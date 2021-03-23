package tv.megacubo.ffmpeg;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import android.util.Log;
import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import com.arthenica.mobileffmpeg.Level;
import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.Statistics;
import com.arthenica.mobileffmpeg.StatisticsCallback;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpegExecution;
import com.arthenica.mobileffmpeg.LogMessage;
import com.arthenica.mobileffmpeg.LogCallback;
import com.arthenica.mobileffmpeg.MediaInformation;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import org.apache.cordova.PluginResult;

 // ref: https://github.com/tanersener/mobile-ffmpeg/wiki/Android
public class FFMpeg extends CordovaPlugin {

    private String TAG = "FFMpeg";
    Map<Long, String> outputs = new HashMap<Long, String>();
	int outputLogMaxLength = 9999999;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
		Config.setLogLevel(Level.AV_LOG_INFO); // must be "info" to get codecs data and bitrate
		Config.enableLogCallback(new LogCallback() {
			public void apply(LogMessage message) {
				CollectLogMessages(message);
			}
		});
	}
	
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equals("exec")) {
			String cmd = data.getString(0);
			long executionId = FFmpeg.executeAsync(cmd, new ExecuteCallback() {
				@Override
				public void apply(long executionId, int returnCode) {
					setTimeout(() -> {                            
						cordova.getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								String output = "return-" + outputs.get(executionId);
								Log.d(TAG, output);
								outputs.remove(executionId);
								if (returnCode == RETURN_CODE_SUCCESS){
									PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, output);
									pluginResult.setKeepCallback(false);
									callbackContext.sendPluginResult(pluginResult);
								} else {
									output += "; Xerrorcode: " + returnCode;
									PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, output);
									pluginResult.setKeepCallback(false);
									callbackContext.sendPluginResult(pluginResult);
								}
							}
						});
					}, 100);
				}
			});
			outputs.put(executionId, "");
			PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "start-" + executionId);
			pluginResult.setKeepCallback(true);
			callbackContext.sendPluginResult(pluginResult);
            return true;
        } else if(action.equals("kill")) {
            long executionId = data.getLong(0);
			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					FFmpeg.cancel(executionId);
				}
			});
            return true;
        } else if(action.equals("cleanup")) {
            List<Long> keepIds = new ArrayList<Long>();
			if(data.length() > 1){
            	for (String s : data.getString(0).split(",")){
	                keepIds.add(Long.parseLong(s));
            	}
			}
			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					long startTimeTolerance = 15;
					long now = System.currentTimeMillis() / 1000;
					final List<FFmpegExecution> ffmpegExecutions = FFmpeg.listExecutions();
					for (int i = 0; i < ffmpegExecutions.size(); i++) {
						FFmpegExecution execution = ffmpegExecutions.get(i);
						Long executionId = execution.getExecutionId();
						Long startTime = execution.getStartTime().getTime() / 1000;
						if(!keepIds.contains(executionId)){
							long elapsed = now - startTime;
							if(elapsed > startTimeTolerance){
								FFmpeg.cancel(executionId);
							}
						}
					}
				}
            });
            return true;
        } else if(action.equals("exit")) {
			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					FFmpeg.cancel();
				}
            });
            return true;
        } else {
			return false;
		}
    }    
    public void CollectLogMessages(LogMessage logMessage){
		long eid = logMessage.getExecutionId();
		String log = outputs.get(eid);
		log += logMessage.getText();
		int len = log.length();
		if(len > outputLogMaxLength){
			log = log.substring(len - outputLogMaxLength);
		}
		outputs.put(eid, log);
	}
    public static void setTimeout(Runnable runnable, int delay){
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            }
            catch (Exception e){
                System.err.println(e);
                Log.d("FFmpeg", "setTimeout error "+ e.getMessage());
            }
        }).start();
    }
}
