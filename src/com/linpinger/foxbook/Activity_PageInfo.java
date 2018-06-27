package com.linpinger.foxbook;

import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.tool.ToolAndroid;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_PageInfo extends Activity {
	private TextView info;
	private NovelManager nm;
	private int bookIDX = -1;
	private int pageIDX = -1;
	private TextView pi_bid, pi_pid;
	private EditText pi_pname, pi_purl, pi_content;

	private void init_controls() { // 初始化各控件
		info = (TextView)this.findViewById(R.id.testTV);
		info.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		pi_bid = (TextView) findViewById(R.id.pi_bid);
		pi_pid = (TextView) findViewById(R.id.pi_pid);
		pi_pname = (EditText) findViewById(R.id.pi_pname);
		pi_purl = (EditText) findViewById(R.id.pi_purl);
		pi_content = (EditText) findViewById(R.id.pi_content);
	}

	public void onCreate(Bundle savedInstanceState) { // 界面初始化
//		this.setTheme(android.R.style.Theme_Holo_Light_DarkActionBar); // tmp: ActionBar
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pageinfo);

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

		foxtipL("返回，保存，复制，粘贴，清空内容");
	}

	public void onBtnClick(View v) {
		switch ( v.getId() ) {
		case R.id.btnSave:
			Map<String, Object> info = nm.getBlankPage();
			info.put(NV.PageName, pi_pname.getText().toString());
			info.put(NV.PageURL, pi_purl.getText().toString());
			info.put(NV.Content, pi_content.getText().toString());
			info.put(NV.Size, pi_content.getText().toString().length());
			nm.setPage(info, bookIDX, pageIDX);

			onBackPressed();
			break;
		case R.id.btnCopyFocus:
			String toCopy = "";
			if ( pi_pname.isFocused() ) { toCopy = pi_pname.getText().toString() ; }
			if ( pi_purl.isFocused() ) { toCopy = pi_purl.getText().toString() ; }
			if ( pi_content.isFocused() ) { toCopy = pi_content.getText().toString() ; }
			ToolAndroid.setClipText(toCopy, this);
			foxtip("剪贴板: " + toCopy);
			break;
		case R.id.btnPasteFocus:
			String toPaste = ToolAndroid.getClipText(this);
			if ( pi_pname.isFocused() ) { pi_pname.setText(toPaste) ; }
			if ( pi_purl.isFocused() ) { pi_purl.setText(toPaste) ; }
			if ( pi_content.isFocused() ) { pi_content.setText("") ; }
			break;
		case R.id.btnClearContent:
			pi_content.setText("");
			break;
		}
	}

	@Override
	public void onBackPressed() { // 返回键被按
		setResult(RESULT_OK);
		finish();
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}
	private void foxtipL(String sinfo) {
		info.setText(sinfo);
	}
}
