package com.linpinger.foxbook;

import java.io.File;

import com.linpinger.tool.Activity_FileChooser;
import com.linpinger.tool.ToolAndroid;
import com.linpinger.tool.ToolJava;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

public class Activity_Setting extends PreferenceActivity {
	SharedPreferences settings;

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	protected void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isWhiteActionBar", false) )
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences); // 当使用PreferenceActivity时
		// getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragement()).commit(); 

		getActionBar().setDisplayHomeAsUpEnabled(true); // 标题栏中添加返回图标

	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if ( preference.getKey().equalsIgnoreCase("cleanCache") ) {
			boolean isCleanSuccess = ToolJava.deleteDir(this.getCacheDir());
			foxtip("已清理缓存，成功：" + isCleanSuccess);
		}
		if ( preference.getKey().equalsIgnoreCase("selectfont") ) {
			Intent itt = new Intent(Activity_Setting.this, Activity_FileChooser.class);
			itt.putExtra("dir", "/sdcard/fonts/");
			startActivityForResult(itt, 9);
			return true;
		}
		if ( preference.getKey().equalsIgnoreCase("exportEinkCFG") ) {
			ToolAndroid.myConfigImportExPort(this, true);
			foxtip("已导出到 FoxBook.cfg");
			return true;
		}
		if ( preference.getKey().equalsIgnoreCase("importEinkCFG") ) {
			ToolAndroid.myConfigImportExPort(this, false);
			foxtip("已从 FoxBook.cfg 导入");
			return true;
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 9: // 响应文件选择器的选择
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();
				String newFont = new File(uri.getPath()).getAbsolutePath();
				String nowPATH = newFont.toLowerCase() ;
				if ( nowPATH.endsWith(".ttf") | nowPATH.endsWith(".ttc") | nowPATH.endsWith(".otf") ) {
					SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
					editor.putString("selectfont", newFont);
					editor.commit();
					foxtip(newFont);
				} else {
					foxtip("要选择后缀为.ttf/.ttc/.otf的字体文件");
				}
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case android.R.id.home: // 返回图标
			this.finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
}
