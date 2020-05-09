package com.linpinger.foxbook;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.linpinger.misc.BackHandledFragment;
import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.novel.NovelSite;
import com.linpinger.tool.FoxHTTP;
import com.linpinger.tool.ToolAndroid;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Fragment_QuickSearch extends BackHandledFragment {

	public static Fragment_QuickSearch newInstance(NovelManager novelMgr, int typeOfSE, String bookName) {
		Fragment_QuickSearch frgmt = new Fragment_QuickSearch();
		frgmt.nm = novelMgr;
		Bundle bd = new Bundle();
		bd.putInt(AC.searchEngine, typeOfSE);
		bd.putString(NV.BookName, bookName);
		frgmt.setArguments(bd);
		return frgmt;
	}
	public static Fragment_QuickSearch newInstance(NovelManager novelMgr, Bundle bd) {
		Fragment_QuickSearch frgmt = new Fragment_QuickSearch();
		frgmt.nm = novelMgr;
		frgmt.setArguments(bd);
		return frgmt;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ctx = container.getContext();
		View v = inflater.inflate(R.layout.fragment_quicksearch, container, false); // 这个false很重要，不然会崩溃

		tv = (TextView)v.findViewById(R.id.testTV);
		lv = (ListView)v.findViewById(R.id.testLV); // 获取LV
		if ( ! ToolAndroid.isEink() ) {
			lv.setBackgroundColor(Color.parseColor("#EEFAEE"));
		}

		Bundle bd = this.getArguments();
		book_name = bd.getString(NV.BookName); // 必需
		SE_TYPE = bd.getInt(AC.searchEngine, 1) ;
		if ( SE_TYPE == AC.SE_NONE ) { // 非搜索引擎
			seURL = bd.getString(NV.BookURL);
			foxtipL("搜索: " + book_name + "  点此返回，列表左侧目录页，右侧普通页，普通页面地址:\n" + seURL);
		} else {
			foxtipL("搜索: " + book_name + "  点此返回，列表左侧目录页，右侧普通页");
		}
		
		try {
			switch (SE_TYPE) { // 1:sogou 2:yahoo 3:bing
			case AC.SE_SOGOU:
				seURL = "http://www.sogou.com/web?query=" + URLEncoder.encode(book_name, "GB2312") + "&num=50" ;
				break;
			case AC.SE_YAHOO:
				seURL = "http://search.yahoo.com/search?n=40&p=" + URLEncoder.encode(book_name, "UTF-8") ;
				break;
			case AC.SE_BING:
				seURL = "http://cn.bing.com/search?q=" + URLEncoder.encode(book_name, "UTF-8") ;
				break;
			case AC.SE_MEEGOQ:
				seURL = "https://www.meegoq.com/search.htm?keyword=" + URLEncoder.encode(book_name, "UTF-8");
				break;
			}
		} catch (Exception e) {
			System.err.println(e.toString());
		}

		(new Thread(){
			public void run(){
				if ( SE_TYPE == AC.SE_NONE ) {
					data = getSearchEngineHref( new FoxHTTP(seURL).getHTML() , "");
				} else if ( SE_TYPE == AC.SE_MEEGOQ ) { // 下载并处理页面
					data = getSearchResultHref( new FoxHTTP(seURL).getHTML() , book_name);
				} else {
					data = getSearchEngineHref( new FoxHTTP(seURL).getHTML() , book_name); // 搜索引擎网页分析放在这里
				}
				handler.sendEmptyMessage(IS_REFRESH);
			}
		}).start();

		init_handler() ; // 初始化一个handler 用于处理后台线程的消息
		initViewAction() ; // 初始化 点击 的行为

		return v;
	}

	public List<Map<String, Object>> getSearchEngineHref(String html, String KeyWord) { // String KeyWord = "三界血歌" ;
		boolean isNormalSite = false ;
		if ( "".equalsIgnoreCase(KeyWord) ) {
			isNormalSite = true;
		}
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(64);
		Map<String, Object> item;

		html = html.replace("\t", "");
		html = html.replace("\r", "");
		html = html.replace("\n", "");
		html = html.replaceAll("(?i)<!--[^>]+-->", "");
		html = html.replace("<em>", "");
		html = html.replace("</em>", "");
		html = html.replace("<b>", "");
		html = html.replace("</b>", "");
		html = html.replace("<strong>", "");
		html = html.replace("</strong>", "");

		// 获取链接 并存入结构中
		Matcher mat = Pattern.compile("(?smi)href *= *[\"']?([^>\"']+)[\"']?[^>]*> *([^<]+)<").matcher(html);
		while (mat.find()) {
			if (2 == mat.groupCount()) {
				if ( ! isNormalSite ) {
					if (mat.group(1).length() < 5)
						continue;
					if (!mat.group(1).startsWith("http"))
						continue;
					if (mat.group(1).contains("www.sogou.com/web"))
						continue;
					if (!mat.group(2).contains(KeyWord))
						continue;
				}

				item = new HashMap<String, Object>(2);
				item.put(NV.BookURL, mat.group(1));
				item.put(NV.BookName, mat.group(2));
				data.add(item);
			}
		}

		return data;
	}

	public List<Map<String, Object>> getSearchResultHref(String html, String KeyWord) { // String KeyWord = "三界血歌" ;
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(64);
		Map<String, Object> item;

		// meegoq
		String reislist = "(?smi)<section class=\"lastest\">(.*?)</section>";
		String relistitem = "(?smi)<li>(.*?)</li>";
		String itemintrourl = "(?smi)<span class=\"n2\"><a href=\"([^\"]*)\"";
		String itembookname = "(?smi)<span class=\"n2\"><a[^>]*>([^<]*)</a>";

		String listStr = getMatch(html, reislist);
		if ( ! "".equalsIgnoreCase(listStr) ) {
			String itemStr = "";
			String nowURL = "";
			Matcher mat = Pattern.compile(relistitem).matcher(listStr);
			while (mat.find()) {
				itemStr = mat.group(1);
				item = new HashMap<String, Object>(2);

				nowURL = getMatch(itemStr, itemintrourl);
				// //www.meegoq.com/info62275.html
				// https://www.meegoq.com/book62275.html
				if ( nowURL.startsWith("//www.meegoq.com/info") ) { nowURL = "https:" + nowURL.replace("/info", "/book"); } // 2019-11-12

				item.put(NV.BookURL, nowURL);
				item.put(NV.BookName, getMatch(itemStr, itembookname));
				data.add(item);
			}
		}
		return data;
	}

	public String getMatch(String text, String iREstr) {
		String ret = "";
		Matcher mat = Pattern.compile(iREstr).matcher(text);
		while (mat.find()) {
			ret = mat.group(1);
			break;
		}
		return ret;
	}

	void initViewAction() {

		lv.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent arg1) {
				clickX = arg1.getX(); // 点击横坐标
				return false;
			}
		});
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Map<String, Object> book = (HashMap<String, Object>) parent.getItemAtPosition(position);
				book_url = (String) book.get(NV.BookURL);
				
				if ( clickX > lv.getWidth() * 0.8 ) { // 右边1/5处: 表示进入信息页，需要手工选择链接
					String nowFullURL = FoxHTTP.getFullURL(seURL, book_url);
//					ToolAndroid.setClipText(nowFullURL, ctx);
//					foxtip("网址已复制到剪贴板\n" + nowFullURL);

					Bundle bd = new Bundle();

					bd.putInt(AC.action, AC.aSearchBookOnSite);
					bd.putString(NV.BookName, book_name);
					bd.putInt(AC.searchEngine, AC.SE_NONE);
					bd.putString(NV.BookURL, nowFullURL);

					startFragment( Fragment_QuickSearch.newInstance(nm, bd) );
				} else {
//					ToolAndroid.setClipText(book_url, ctx);
//					foxtip("网址已复制到剪贴板\n" + book_url);
					Bundle bd = new Bundle();

					bd.putInt(AC.action, AC.aSearchBookOnSite);
					bd.putString(NV.BookURL, book_url);
					bd.putString(NV.BookName, book_name);

					startFragment( Fragment_PageList.newInstance(nm, bd) );
				}
				foxtipL("搜索: " + book_name + "\n" + book_url);
			}
		});

		tv.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				back();
			}
		});

	} // initViewAction end

	private void refreshLVAdapter() {
		adapter = new SimpleAdapter(ctx, data,
				android.R.layout.simple_list_item_2, new String[] { NV.BookName, NV.BookURL },
				new int[] { android.R.id.text1, android.R.id.text2 });
		lv.setAdapter(adapter);
	}

	private void init_handler() { // 初始化一个handler 用于处理后台线程的消息
		handler = new Handler() {
			public void handleMessage(Message msg) {
				if ( msg.what == IS_REFRESH ) { // 下载完毕
					tv.setText( "D." + tv.getText() );
					refreshLVAdapter();
				}
			}
		};
	}

	private OnFinishListener lsn;
	public Fragment setOnFinishListener(OnFinishListener ofl) {
		lsn = ofl;
		return this;
	}
	@Override
	public void onDestroy() {
		if ( lsn != null) {
			lsn.OnFinish();
		}
		super.onDestroy();
	}

//	private void foxtip(String sinfo) { // Toast消息
//		Toast.makeText(ctx, sinfo, Toast.LENGTH_SHORT).show();
//	}
	private void foxtipL(String sinfo) {
		tv.setText(sinfo);
	}

	private float clickX = 0 ; // 用来确定点击横坐标以实现不同LV不同区域点击效果
	private ListView lv ;
	private TextView tv;
	SimpleAdapter adapter;
	private List<Map<String, Object>> data;
	private Handler handler;
	private static int IS_REFRESH = 5 ;

	private NovelManager nm;
	private String book_name = "" ;
	private String book_url = "" ;

	private int SE_TYPE = 1; // 搜索引擎
	private String seURL = "" ;

	private Context ctx;
}
