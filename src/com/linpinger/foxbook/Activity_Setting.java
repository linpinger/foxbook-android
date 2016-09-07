package com.linpinger.foxbook;


import java.io.File;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

public class Activity_Setting extends Activity {
	SharedPreferences settings;
	private boolean isWhiteActionBar = false; // 白色动作栏
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isWhiteActionBar = settings.getBoolean("isWhiteActionBar", isWhiteActionBar);
		if ( isWhiteActionBar ) {
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);
		}

		super.onCreate(savedInstanceState);
		// addPreferencesFromResource(R.xml.preferences); // 当使用PreferenceActivity时
		getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragement()).commit(); 

		getActionBar().setDisplayHomeAsUpEnabled(true);  // 标题栏中添加返回图标
		
	}
	

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class PrefsFragement extends PreferenceFragment{  
		@Override  
		public void onCreate(Bundle savedInstanceState) {  
			super.onCreate(savedInstanceState);  
			addPreferencesFromResource(R.xml.preferences);  
		}

		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
			if ( preference.getKey().equalsIgnoreCase("selectfont") ) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("*/*"); 
				intent.addCategory(Intent.CATEGORY_OPENABLE);
			    try {
			        startActivityForResult( Intent.createChooser(intent, "选择一个字体文件: *.ttf/*.ttc"), 99);
			    } catch (android.content.ActivityNotFoundException ex) {
			    	Toast.makeText(this.getActivity(), "安装一个文件管理器好吧", Toast.LENGTH_SHORT).show();
			    }
				return true;
			}
			return super.onPreferenceTreeClick(preferenceScreen, preference);
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			switch (requestCode) {
			case 99:  // 响应文件选择器的选择
				if (resultCode == RESULT_OK) {
					Uri uri = data.getData();
					String newFont = new File(uri.getPath()).getAbsolutePath();
					if ( newFont.toLowerCase().endsWith(".ttf") | newFont.toLowerCase().endsWith(".ttc") ) {
						SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this.getActivity()).edit();
						editor.putString("selectfont", newFont);
						editor.commit();
						Toast.makeText(this.getActivity(), newFont, Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(this.getActivity(), "要选择后缀为.ttf/.ttc的字体文件", Toast.LENGTH_SHORT).show();
					}
				}
				break;
			}
			super.onActivityResult(requestCode, resultCode, data);
		}
		
	}
	
	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case android.R.id.home: // 返回图标
			this.finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	

}
