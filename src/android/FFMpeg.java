package tv.megacubo.ffmpeg;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import android.util.Log;
import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;

/*
import com.arthenica.mobileffmpeg.Level;
import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.Statistics;
import com.arthenica.mobileffmpeg.FFmpegExecution;
import com.arthenica.mobileffmpeg.LogMessage;
import com.arthenica.mobileffmpeg.StatisticsCallback;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.LogCallback;
import com.arthenica.mobileffmpeg.MediaInformation;
*/

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Statistics;
import com.arthenica.ffmpegkit.StatisticsCallback;
import com.arthenica.ffmpegkit.ExecuteCallback;
import com.arthenica.ffmpegkit.LogCallback;

import org.apache.cordova.PluginResult;

 // ref: https://github.com/tanersener/ffmpeg-kit/tree/main/android
public class FFMpeg extends CordovaPlugin {

    private String TAG = "FFMpeg";
    Map<Long, String> outputs = new HashMap<Long, String>();
	int outputLogMaxLength = 9999999;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
	}
	
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equals("exec")) {
			String cmd = data.getString(0);
			Log.d(TAG, "COMMAND: " + cmd);
			Session session = FFmpegKit.executeAsync(cmd, new ExecuteCallback() {
				@Override
				public void apply(Session session) {
					Long sessionId = session.getSessionId();
					ReturnCode returnCode = session.getReturnCode();
					setTimeout(() -> { // wait some last message to be collected
						cordova.getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								String output = "return-" + outputs.get(sessionId);
								Log.d(TAG, output);
								outputs.remove(sessionId);
								if (ReturnCode.isSuccess(returnCode) || ReturnCode.isCancel(returnCode)){
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
			}, new LogCallback() {

				@Override
				public void apply(com.arthenica.ffmpegkit.Log log) {
					Long sessionId = log.getSessionId();
					String messages = outputs.get(sessionId);
					messages += log.getMessage();
					int len = messages.length();
					if(len > outputLogMaxLength){
						messages = messages.substring(len - outputLogMaxLength);
					}
					outputs.put(sessionId, messages);
				}
			}, new StatisticsCallback() {
			
				@Override
				public void apply(Statistics statistics) {

				}
			});
			Long sessionId = session.getSessionId();
			outputs.put(sessionId, "");
			PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "start-" + sessionId);
			pluginResult.setKeepCallback(true);
			callbackContext.sendPluginResult(pluginResult);
            return true;
        } else if(action.equals("kill")) {
            long sessionId = data.getLong(0);
			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					FFmpegKit.cancel(sessionId);
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
					List<Session> sessions = FFmpegKitConfig.getSessions();
					for (int i = 0; i < sessions.size(); i++) {
						Session session = sessions.get(i);
						Long sessionId = session.getSessionId();
						Long startTime = session.getStartTime().getTime() / 1000;
						if(!keepIds.contains(sessionId)){
							long elapsed = now - startTime;
							if(elapsed > startTimeTolerance){
								FFmpegKit.cancel(sessionId);
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
					FFmpegKit.cancel();
				}
            });
            return true;
        } else {
			return false;
		}
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
