package com.linpinger.foxbook;

import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_BookInfo extends Activity {
	public static FoxMemDB oDB;
	private Button btn_save;
	private TextView tv_bid;
	private EditText edt_bname, edt_isend, edt_qdid, edt_burl, edt_delurl;
	
	SharedPreferences settings;
	private boolean isWhiteActionBar = false; // 白色动作栏

	private int bookid ;
	
	private void init_controls() { // 初始化各控件
		btn_save = (Button) findViewById(R.id.btn_save);
		tv_bid = (TextView) findViewById(R.id.tv_bid);
		edt_bname = (EditText) findViewById(R.id.edt_bname);
		edt_isend = (EditText) findViewById(R.id.edt_isend);
		edt_qdid = (EditText) findViewById(R.id.edt_qdid);
		edt_burl = (EditText) findViewById(R.id.edt_burl);
		edt_delurl = (EditText) findViewById(R.id.edt_delurl);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // 标题栏中添加返回图标
//		getActionBar().setDisplayShowHomeEnabled(false); // 隐藏程序图标
	}		// 响应点击事件在onOptionsItemSelected的switch中加入 android.R.id.home   this.finish();
	
	public void onCreate(Bundle savedInstanceState) { // 界面初始化
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isWhiteActionBar = settings.getBoolean("isWhiteActionBar", isWhiteActionBar);
		if ( isWhiteActionBar ) {
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookinfo);
		
		showHomeUp();
		
		// 通过intent获取数据
		Intent itt = getIntent();
		bookid = itt.getIntExtra("bookid", 0); // 必需
		
		init_controls() ; // 初始化各控件
		// 显示数据
		Map<String,String> info = oDB.getOneRow("select name as bn, url as bu, isEnd as bend, qidianid as qid, delurl as list from book where id=" + bookid);
		tv_bid.setText(String.valueOf(bookid)) ;
		edt_bname.setText(info.get("bn"));
		edt_isend.setText(info.get("bend"));
		edt_qdid.setText(info.get("qid"));
		edt_burl.setText(info.get("bu"));
		edt_delurl.setText(info.get("list"));
		
		// 点击保存按钮保存数据
		btn_save.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ContentValues cc = new ContentValues();
				cc.put("Name", edt_bname.getText().toString());
				cc.put("URL", edt_burl.getText().toString());
				cc.put("DelURL", edt_delurl.getText().toString());
				cc.put("QiDianID", edt_qdid.getText().toString());
				cc.put("isEnd", edt_isend.getText().toString());
				FoxMemDBHelper.update_cell("Book", cc, "id=" + bookid, oDB) ; // 修改单个字段
				setResult(RESULT_OK, (new Intent()).setAction("已修改书籍信息"));
				finish();
			}
		});

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.bookinfo, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case R.id.bi_getQDidFromURL:
			String url_now = edt_burl.getText().toString();
			if ( url_now.contains(".qidian.com/") ) {
				edt_qdid.setText(String.valueOf(site_qidian.qidian_getBookID_FromURL(url_now)));
			} else {
				foxtip("URL不包含 .qidian.com/");
			}
			break;
		case R.id.bi_copyBookName:
			String bn = edt_bname.getText().toString();
			TOOLS.setcliptext(bn, this);
			foxtip("剪贴板: " + bn);
			break;
		case R.id.bi_copyQidianID:
			String bq = edt_qdid.getText().toString();
			TOOLS.setcliptext(bq, this);
			foxtip("剪贴板: " + bq);
			break;
		case R.id.bi_copyURL: // 复制
			String bu = edt_burl.getText().toString();
			TOOLS.setcliptext(bu, this);
			foxtip("剪贴板: " + bu);
			break;
		case R.id.bi_pasteBookName: // 粘贴
			edt_bname.setText(TOOLS.getcliptext(this));
			break;
		case R.id.bi_pasteQidianID:
			edt_qdid.setText(TOOLS.getcliptext(this));
			break;
		case R.id.bi_pasteURL:
			edt_burl.setText(TOOLS.getcliptext(this));
			break;
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
