package com.linpinger.foxbook;

import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Activity_BookInfo extends Activity {
	public static FoxMemDB oDB;
	private Button btn_save;
	private TextView tv_bid;
	private EditText edt_bname, edt_isend, edt_qdid, edt_burl, edt_delurl;
	
	SharedPreferences settings;
	public static final String FOXSETTING = "FOXSETTING";
	private boolean isEink = false; // 是否E-ink设备

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
		settings = getSharedPreferences(FOXSETTING, 0);
		isEink = settings.getBoolean("isEink", isEink);
		if ( isEink ) {
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
	
	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case android.R.id.home: // 返回图标
			this.finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
