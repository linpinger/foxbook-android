package com.linpinger.foxbook;

import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_ShowPage extends Activity {
	private static int FROM_DB = 1 ;
	private static int FROM_NET = 2 ; 

	private int foxfrom = 0 ;  // 1=DB, 2=search 
	private TextView tv ;
	private int pageid = 0 ;
	private String pagetext = "暂缺" ;
	private String pagename = "" ;
	private String pageurl = "" ;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_showpage);

		tv = (TextView) findViewById(R.id.tv_page);
		
		Intent itt = getIntent();
		foxfrom = itt.getIntExtra("iam", 0);       // 必需 表明数据从哪来的
		pagename = itt.getStringExtra("chapter_name");
		pageurl = itt.getStringExtra("chapter_url");

		setTitle(pagename + " : " + pageurl );

		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				Bundle data = msg.getData();
				pagetext = data.getString("text");
				
				tv.setText(pagetext.replace("\n", "\n　　"));
			}
		};

		final Runnable down_page = new Runnable() {
			@Override
			public void run() {
				String text = FoxBookLib.updatepage(-1, pageurl) ;
		
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putString("text", text); 
				msg.setData(data);
				handler.sendMessage(msg);
			}
		};
		
		if ( FROM_DB == foxfrom ){ // DB
			pageid =  itt.getIntExtra("chapter_id", 0);
			pagetext = FoxDB.getOneCell("select Content from page where id = " + pageid + " and Content is not null" );
	 
			tv.setText(pagetext.replace("\n", "\n　　"));
		} 
		if ( FROM_NET == foxfrom ){ // NET
			setTitle("下载中...");
			new Thread(down_page).start();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // 创建菜单
		getMenuInflater().inflate(R.menu.showpage, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) { // 响应选择菜单的动作
		switch (item.getItemId()) {
		case R.id.show_prev: // 上一页
			Map<String,String> pp = FoxDB.getOneRow("select id as id, name as name, url as url, content as content from page where id < " + pageid + " and content is not null order by bookid, id limit 1");
			if ( null == pp.get("name") ) {
				foxtip("亲，没有上一页了");
				break;
			}
			
			setTitle(pp.get("name") + " : " + pp.get("url") );
			pagetext = pp.get("content");
			tv.setText(pagetext.replace("\n", "\n　　"));
			pageid = Integer.valueOf(pp.get("id"));
			break;
		case R.id.show_next: // 下一页
			Map<String,String> nn = FoxDB.getOneRow("select id as id, name as name, url as url, content as content from page where id > " + pageid + " and content is not null order by bookid, id limit 1");
			if ( null == nn.get("name") ) {
				foxtip("亲，没有下一页了");
				break;
			}
			setTitle(nn.get("name") + " : " + nn.get("url") );
			pagetext = nn.get("content");
			tv.setText(pagetext.replace("\n", "\n　　"));
			pageid = Integer.valueOf(nn.get("id"));
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

}
