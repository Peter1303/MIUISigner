package com.miui.dosign;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.miui.dosign.R;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
	
	private String cUserId = "";
	private String serviceToken = "";
	private String cookie = "";
	
	private TextView tip;
	private Button button;
	
	private SharedPreferences sp;
	
	private Context con = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
		sp = getSharedPreferences("Cookies", Context.MODE_PRIVATE);
		getCookies();
		tip = (TextView) findViewById(R.id.activity_main_TextView);
		button = (Button) findViewById(R.id.activity_main_Button);
		button.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1) {
					if(cUserId.isEmpty() || serviceToken.isEmpty()){
						ToastShow("Cookies还没有填好呢");
					} else {
						new Thread(networkTask).start();
					}
				}
			});
    }
	
	private void getCookies(){
		cUserId = sp.getString("cUserId", "");
		serviceToken = sp.getString("serviceToken", "");
		cookie = "cUserId=" + cUserId + ";serviceToken=" + serviceToken;
	}
	
	private void ToastShow(String string){
		LayoutInflater inflater = getLayoutInflater();
		View view = inflater.inflate(R.layout.toast_layout, null);
		TextView txt = (TextView) view.findViewById(R.id.toast_layout_TextView);
		txt.setText(string);
		Toast toast = new Toast(con);
		toast.setGravity(Gravity.BOTTOM, 0, 80);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(view);
		toast.show();
	}

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Bundle data = msg.getData();
			String val = data.getString("value");
			tip.setText(val);
		}
	};

	/**
	 * 网络操作相关的子线程
	 */
	Runnable networkTask = new Runnable() {

		@Override
		public void run() {
			String buf = "";
			try {
				URL url = new URL("http://api.bbs.miui.com/common/sign/doSign");
				HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
				httpCon.setConnectTimeout(5000);
				httpCon.setRequestProperty("Cookie", cookie);
				//String response = httpCon.getResponseMessage();
				//buf += ("HTTP/1.x " + httpCon.getResponseCode() + " " + response + "\n");
				InputStream in = new BufferedInputStream(httpCon.getInputStream());
				Reader r = new InputStreamReader(in);
				int c;
				while ((c = r.read()) != -1) {
					buf += (String.valueOf((char) c));
				}
				in.close();
				if (buf != null && !buf.isEmpty()) {
					JSONArray jsonArray = new JSONArray("[" + buf + "]");
					JSONObject jsonObj = jsonArray.getJSONObject(0);
					//int error = jsonObj.optInt("error");
					String desc = jsonObj.optString("desc");
					String data = jsonObj.optString("data");
					JSONArray jsonArray2 = new JSONArray("[" + data + "]");
					JSONObject jsonObj2 = jsonArray2.getJSONObject(0);
					String copyright = jsonObj2.optString("copyright");
					JSONArray jsonArray3 = new JSONArray("[" + copyright + "]");
					JSONObject jsonObj3 = jsonArray3.getJSONObject(0);
					String word = jsonObj3.optString("word");
					buf = desc + "\n" + word;
				}
			} catch (Exception e) {
				buf = e.toString();
			}
			// 在这里进行 http request.网络请求相关操作
			Message msg = new Message();
			Bundle data = new Bundle();
			data.putString("value", decode(buf));
			msg.setData(data);
			handler.sendMessage(msg);
		}
	};

	public static String decode(String unicodeStr) {
        if (unicodeStr == null) {
            return null;
        }
        StringBuffer retBuf = new StringBuffer();
        int maxLoop = unicodeStr.length();
        for (int i = 0; i < maxLoop; i++) {
            if (unicodeStr.charAt(i) == '\\') {
                if ((i < maxLoop - 5) && ((unicodeStr.charAt(i + 1) == 'u') || (unicodeStr.charAt(i + 1) == 'U')))
                    try {
                        retBuf.append((char) Integer.parseInt(unicodeStr.substring(i + 2, i + 6), 16));
                        i += 5;
                    } catch (NumberFormatException localNumberFormatException) {
                        retBuf.append(unicodeStr.charAt(i));
                    } else
                    retBuf.append(unicodeStr.charAt(i));
            }
			else {
                retBuf.append(unicodeStr.charAt(i));
            }
        }
        return retBuf.toString();
    }
	
	private void setCookies(){
		LayoutInflater inflater = LayoutInflater.from(con);
		View view = inflater.inflate(R.layout.dialog_cookies, null);
		final EditText ed1 = (EditText) view.findViewById(R.id.dialog_edit_EditText1);
		final EditText ed2 = (EditText) view.findViewById(R.id.dialog_edit_EditText2);
		ed1.setText(cUserId);
		ed2.setText(serviceToken);
		new AlertDialog.Builder(con)
			.setView(view)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface p1, int p2)
				{
					String temp1 = ed1.getText().toString();
					String temp2 = ed2.getText().toString();
					if(!temp1.isEmpty() && !temp2.isEmpty()){
						SharedPreferences.Editor sped = sp.edit();
						sped.putString("cUserId", temp1).commit();
						sped.putString("serviceToken", temp2).commit();
						getCookies();
					}
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case R.id.action_cookies:
				setCookies();
				break;
			case R.id.action_about:
				new AlertDialog.Builder(con).setMessage("Copyrights©2018 Peter\nAll Rights Received.").setPositiveButton(android.R.string.ok, null).show();
				break;
		}
        return super.onOptionsItemSelected(item);
    }
}
