package com.linpinger.foxbook;

import java.util.Map;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Activity_BookInfo extends Activity {
	private Button btn_save;
	private TextView tv_bid;
	private EditText edt_bname, edt_qdid, edt_burl, edt_delurl;
	
	private int bookid ; //, qidianid;
//	private String bookname, bookurl, bookdellist;
	
	private void init_controls() { // 初始化各控件
		btn_save = (Button) findViewById(R.id.btn_save);
		tv_bid = (TextView) findViewById(R.id.tv_bid);
		edt_bname = (EditText) findViewById(R.id.edt_bname);
		edt_qdid = (EditText) findViewById(R.id.edt_qdid);
		edt_burl = (EditText) findViewById(R.id.edt_burl);
		edt_delurl = (EditText) findViewById(R.id.edt_delurl);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) { // 界面初始化
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookinfo);
		
		// 通过intent获取数据
		Intent itt = getIntent();
		bookid = itt.getIntExtra("bookid", 0); // 必需
		
		init_controls() ; // 初始化各控件
		// 显示数据
		Map<String,String> info = FoxDB.getOneRow("select name as bn, url as bu, qidianid as qid, delurl as list from book where id=" + bookid);
		tv_bid.setText(String.valueOf(bookid)) ;
		edt_bname.setText(info.get("bn"));
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
				FoxDB.update_cell("Book", cc, "id=" + bookid) ; // 修改单个字段
				setResult(RESULT_OK, (new Intent()).setAction("已修改书籍信息"));
				finish();
			}
		});

	}

}
