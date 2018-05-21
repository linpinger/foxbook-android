package com.linpinger.foxbook;

import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.tool.ToolAndroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_PageInfo extends Activity {
	private NovelManager nm;
	private int bookIDX = -1;
	private int pageIDX = -1;
	private TextView pi_bid, pi_pid;
	private EditText pi_pname, pi_purl, pi_content;

	SharedPreferences settings;

	private void init_controls() { // 初始化各控件
		pi_bid = (TextView) findViewById(R.id.pi_bid);
		pi_pid = (TextView) findViewById(R.id.pi_pid);
		pi_pname = (EditText) findViewById(R.id.pi_pname);
		pi_purl = (EditText) findViewById(R.id.pi_purl);
		pi_content = (EditText) findViewById(R.id.pi_content);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true); // 标题栏中添加返回图标
	}

	public void onCreate(Bundle savedInstanceState) { // 界面初始化
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		if ( settings.getBoolean("isWhiteActionBar", false) )
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pageinfo);

		showHomeUp();
		init_controls() ; // 初始化各控件

		this.nm = ((FoxApp)this.getApplication()).nm ;

		// 通过intent获取数据
		bookIDX = getIntent().getIntExtra(NV.BookIDX, -1);
		pageIDX = getIntent().getIntExtra(NV.PageIDX, -1);
		Map<String, Object> info = nm.getPage(bookIDX, pageIDX);

		// 显示数据
		pi_bid.setText(String.valueOf(bookIDX)) ;
		pi_pid.setText(String.valueOf(pageIDX)) ;
		pi_pname.setText(info.get(NV.PageName).toString()) ;
		pi_purl.setText(info.get(NV.PageURL).toString()) ;
		pi_content.setText(info.get(NV.Content).toString()) ;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.pageinfo, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case R.id.pi_clearContent:
			pi_content.setText("");
			break;
		case R.id.pi_copyPageName: // 复制
			String pn = pi_pname.getText().toString();
			ToolAndroid.setClipText(pn, this);
			foxtip("剪贴板: " + pn);
			break;
		case R.id.pi_copyURL: // 复制
			String pu = pi_purl.getText().toString();
			ToolAndroid.setClipText(pu, this);
			foxtip("剪贴板: " + pu);
			break;
		case R.id.pi_copyContent: // 复制
			String pc = pi_content.getText().toString();
			ToolAndroid.setClipText(pc, this);
			foxtip("剪贴板: " + pc);
			break;
		case R.id.pi_pastePageName: // 粘贴
			pi_pname.setText(ToolAndroid.getClipText(this));
			break;
		case R.id.pi_pasteURL: // 粘贴
			pi_purl.setText(ToolAndroid.getClipText(this));
			break;
		case R.id.pi_pasteContent: // 粘贴
			this.pi_content.setText(ToolAndroid.getClipText(this));
			break;
		case android.R.id.home: // 返回图标
			onBackPressed();
			break;
		case R.id.pi_save_exit:
			Map<String, Object> info = nm.getBlankPage();
			info.put(NV.PageName, pi_pname.getText().toString());
			info.put(NV.PageURL, pi_purl.getText().toString());
			info.put(NV.Content, pi_content.getText().toString());
			info.put(NV.Size, pi_content.getText().toString().length());
			nm.setPage(info, bookIDX, pageIDX);

			onBackPressed();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() { // 返回键被按
		setResult(RESULT_OK);
		finish();
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
}
