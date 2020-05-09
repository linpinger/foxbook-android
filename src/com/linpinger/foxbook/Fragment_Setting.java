package com.linpinger.foxbook;

import java.io.File;

import com.linpinger.tool.ToolAndroid;
import com.linpinger.tool.ToolJava;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Fragment_Setting extends PreferenceFragment {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences); // 若是自定义布局，必须包含ID为"@android:id/list"的ListView
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ctx = container.getContext();

		View v = inflater.inflate(R.layout.fragment_setting, container, false); // 这个false很重要，不然会崩溃
		if ( ToolAndroid.isEink() ) {
			v.setBackgroundColor(Color.WHITE);
			v.findViewById(R.id.btnToLVBottom).setVisibility(View.VISIBLE);
			v.findViewById(R.id.btnToLVTop).setVisibility(View.VISIBLE);
		} else {
			v.setBackgroundResource(R.drawable.background_color);
		}

		lv = (ListView) v.findViewById(android.R.id.list);

		tv = (TextView) v.findViewById(R.id.testTV);
		tv.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				back();
			}
		});

		// 点击事件
		v.findViewById(R.id.btnToLVBottom).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ToolAndroid.jump2ListViewPos(lv, -66) ;
			}
		});
		v.findViewById(R.id.btnToLVBottom).setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				ToolAndroid.jump2ListViewPos(lv, -1) ;
				return true;
			}
		});
		v.findViewById(R.id.btnToLVTop).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ToolAndroid.jump2ListViewPos(lv, -99) ;
			}
		});
		v.findViewById(R.id.btnToLVTop).setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				ToolAndroid.jump2ListViewPos(lv, 0) ;
				return true;
			}
		});

		this.findPreference("cleanCache").setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference pf) {
				cleanAppCache();
				return true;
			}
		});
		this.findPreference("selectfont").setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference pf) {
				selectFile();
				return true;
			}
		});
		this.findPreference("exportEinkCFG").setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference pf) {
				ToolAndroid.myConfigImportExPort(ctx, true);
				foxtip("已导出到 FoxBook.cfg");
				return true;
			}
		});
		this.findPreference("importEinkCFG").setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference pf) {
				ToolAndroid.myConfigImportExPort(ctx, false);
				foxtip("已从 FoxBook.cfg 导入");
				return true;
			}
		});
		return v;
	}

	void cleanAppCache() {
		if ( ToolJava.deleteDir( ctx.getCacheDir() ) ) {
			foxtip("已成功清理缓存");
			back();
		} else {
			foxtip("清理缓存失败");
		}
	}

	public void selectFile() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, 9);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 9: // 响应文件选择器的选择
			if (resultCode == Activity.RESULT_OK) {
				Uri uri = data.getData();
				String newFont = new File(uri.getPath()).getAbsolutePath();
				if ( newFont.contains("/document/primary:") ) { // 神奇的路径
					newFont = "/sdcard/" + newFont.split(":")[1];
				}
				String nowPATH = newFont.toLowerCase() ;
				if ( nowPATH.endsWith(".ttf") | nowPATH.endsWith(".ttc") | nowPATH.endsWith(".otf") ) {
					PreferenceManager.getDefaultSharedPreferences(ctx).edit().putString("selectfont", newFont).commit();
					foxtip("\n" + uri.getPath() + "\n" + newFont);
				} else {
					foxtip("要选择后缀为.ttf/.ttc/.otf的字体文件");
				}
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(ctx, sinfo, Toast.LENGTH_SHORT).show();
	}

	private void back() {
		getActivity().onBackPressed();
	}
//	void startFragment(Fragment fragmt) {
//		getFragmentManager().beginTransaction().hide(this).add(android.R.id.content, fragmt).addToBackStack(null).commit();
//	}

	Context ctx;
	private TextView tv;
	private ListView lv;

}
