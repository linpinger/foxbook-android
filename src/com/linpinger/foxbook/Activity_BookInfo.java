package com.linpinger.foxbook;

import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.SiteQiDian;
import com.linpinger.tool.ToolAndroid;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_BookInfo extends Activity {
	private TextView info;
	private NovelManager nm;
	private int bookIDX = -1;
	private TextView tv_bid;
	private EditText edt_bname, edt_bauthor, edt_isend, edt_qdid, edt_burl, edt_delurl;

	public void onCreate(Bundle savedInstanceState) { // 界面初始化
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookinfo);
	
		init_controls() ; // 初始化各控件
	
		this.nm = ((FoxApp)this.getApplication()).nm ;
	
		// 通过intent获取数据
		bookIDX = getIntent().getIntExtra(NV.BookIDX, -1);
		Map<String, Object> info = nm.getBookInfo(bookIDX);
	
		// 显示数据
		tv_bid.setText(String.valueOf(bookIDX)) ;
		edt_bname.setText(info.get(NV.BookName).toString()) ;
		edt_bauthor.setText(info.get(NV.BookAuthor).toString()) ;
		edt_isend.setText(String.valueOf(info.get(NV.BookStatu))) ;
		edt_qdid.setText(info.get(NV.QDID).toString()) ;
		edt_burl.setText(info.get(NV.BookURL).toString()) ;
		edt_delurl.setText(info.get(NV.DelURL).toString()) ;
	
		foxtipL("返回，保存，复制，粘贴，网址获取ID");
	}

	public void onBtnClick(View v) {
		switch ( v.getId() ) {
		case R.id.btnSave:
			Map<String, Object> info = nm.getBlankBookInfo();
			info.put(NV.BookName, edt_bname.getText().toString());
			info.put(NV.BookAuthor, edt_bauthor.getText().toString());
			info.put(NV.BookURL, edt_burl.getText().toString());
			info.put(NV.DelURL, edt_delurl.getText().toString());
			info.put(NV.QDID, edt_qdid.getText().toString());
			info.put(NV.BookStatu, Integer.valueOf(edt_isend.getText().toString()));
			nm.setBookInfo(info, bookIDX);
	
			onBackPressed();
			break;
		case R.id.btnCopyFocus:
			String toCopy = "";
			if ( edt_bname.isFocused() ) { toCopy = edt_bname.getText().toString() ; }
			if ( edt_bauthor.isFocused() ) { toCopy = edt_bauthor.getText().toString() ; }
			if ( edt_qdid.isFocused() ) { toCopy = edt_qdid.getText().toString() ; }
			if ( edt_burl.isFocused() ) { toCopy = edt_burl.getText().toString() ; }
			if ( edt_delurl.isFocused() ) { toCopy = edt_delurl.getText().toString() ; }
			if ( edt_isend.isFocused() ) {
				toCopy = "FoxBook>" + edt_bname.getText().toString() + ">"
						+ edt_bauthor.getText().toString() + ">"
						+ edt_qdid.getText().toString() + ">"
						+ edt_burl.getText().toString() + ">"
						+ edt_delurl.getText().toString(); // 复制全部
			}
			ToolAndroid.setClipText(toCopy, this);
			foxtip("剪贴板: " + toCopy);
			break;
		case R.id.btnPasteFocus:
			String toPaste = ToolAndroid.getClipText(this);
			if ( edt_bname.isFocused() ) { edt_bname.setText(toPaste) ; }
			if ( edt_bauthor.isFocused() ) { edt_bauthor.setText(toPaste) ; foxtip("若作者留空，表示是新书，更新时不会只下载最后55章"); }
			if ( edt_qdid.isFocused() ) { edt_qdid.setText(toPaste) ; }
			if ( edt_burl.isFocused() ) { edt_burl.setText(toPaste) ; }
			if ( edt_delurl.isFocused() ) { edt_delurl.setText("") ; }
			if ( edt_isend.isFocused() ) {
				if ( ! toPaste.contains("FoxBook>") ) { // 粘贴全部
					foxtip("剪贴板中的内容格式不对哟");
					break;
				}
				String xx[] = toPaste.split(">");
				edt_bname.setText(xx[1]);
				edt_bauthor.setText(xx[2]);
				edt_qdid.setText(xx[3]);
				if ( edt_burl.getText().toString().contains("http") ) {
					ToolAndroid.setClipText(xx[4], this);
					foxtip("剪贴板: " + xx[4]);
				} else {
					edt_burl.setText(xx[4]);
					edt_delurl.setText(xx[5]);
				}
			}
			break;
		case R.id.btnGetQDidFromURL:
			String url_now = edt_burl.getText().toString();
			if ( url_now.contains(".qidian.com/") ) {
				edt_qdid.setText(new SiteQiDian().getBookID_FromURL(url_now));
			} else {
				foxtip("URL不包含 .qidian.com/");
			}
			break;
		}
	}


	private void init_controls() { // 初始化各控件
		info = (TextView)this.findViewById(R.id.testTV);
		info.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		tv_bid = (TextView) findViewById(R.id.tv_bid);
		edt_bname = (EditText) findViewById(R.id.edt_bname);
		edt_bauthor = (EditText) findViewById(R.id.edt_bauthor);
		edt_isend = (EditText) findViewById(R.id.edt_isend);
		edt_qdid = (EditText) findViewById(R.id.edt_qdid);
		edt_burl = (EditText) findViewById(R.id.edt_burl);
		edt_delurl = (EditText) findViewById(R.id.edt_delurl);
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
