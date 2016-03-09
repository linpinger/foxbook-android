package com.linpinger.foxbook;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ray.tools.umd.builder.Umd;
import com.ray.tools.umd.builder.UmdChapters;
import com.ray.tools.umd.builder.UmdHeader;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

public class FoxMemDBHelper {

    public static String importQidianTxt(String txtPath, FoxMemDB oDB) {
    	// 第一步检测编码，非GBK就是UTF-8，其他不予考虑
    	String txtEnCoding = "GBK" ;
    	String txt = FoxBookLib.readText(txtPath, txtEnCoding) ;
    	if ( ! txt.contains("的") ) {
    		txtEnCoding = "UTF-8" ;
    		txt = FoxBookLib.readText(txtPath, txtEnCoding);
    	}
    	
    	SQLiteDatabase db = oDB.getDB();
        String sQidianid = (new File(txtPath)).getName().replace(".txt", ""); // 文件名

        if ( ! txt.contains("更新时间") ) { // 非起点文本
        	StringBuilder chunkStr = new StringBuilder(65536);
        	db.beginTransaction();// 开启事务
    		try {
    	        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(txtPath), txtEnCoding));
    	        String line = null;
    	        int chunkLen = 0;
    	        int chunkCount = 0;
				String chunkNow ;
    	        while ((line = br.readLine()) != null) {
					if ( line.startsWith("　　") ) { // 去掉开头的空白
						line = line.replace("　　", "");
					}
    	            chunkLen = chunkStr.length();
    	            if ( chunkLen > 3000 && ( line.length() == 0 || chunkLen > 6000 || line.startsWith("第") || line.contains("卷") || line.contains("章") || line.contains("节") ) ) {
    	            	++ chunkCount;
						chunkNow = chunkStr.toString().replace("\n\n", "\n").replace("\n\n", "\n");
    	              	db.execSQL("insert into page(bookid,name,content,CharCount) values(?,?,?,?)",
    	              		new Object[] { "1", txtEnCoding + "_" + String.valueOf(chunkCount), chunkNow, String.valueOf(chunkNow.length()) });
    	                chunkStr = new StringBuilder(65536);
    	             }
    	             chunkStr.append(line).append("\n");
    	        }
    	        if ( chunkStr.length() > 0 ) {
    	         	++ chunkCount;
					chunkNow = chunkStr.toString().replace("\n\n", "\n").replace("\n\n", "\n");
	              	db.execSQL("insert into page(bookid,name,content,CharCount) values(?,?,?,?)",
	              			new Object[] { "1", txtEnCoding + "_" + String.valueOf(chunkCount), chunkNow, String.valueOf(chunkNow.length()) });
    	        }
    	        br.close();
    	        db.setTransactionSuccessful();// 设置事务的标志为True
    	    } catch (IOException e) {
    	        e.toString();
     		} finally {
    			db.endTransaction();// 结束事务,有两种情况：commit,rollback,  事务的提交或回滚是由事务的标志决定的,如果事务的标志为True，事务就会提交，否侧回滚,默认情况下事务的标志为False
    		}
        	return sQidianid;
    	}

        String sQidianURL = site_qidian.qidian_getIndexURL_Desk(Integer.valueOf(sQidianid)); // URL
        String sBookName = sQidianid;
        try {  // 第一行书名
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(txtPath), "GBK"));
            sBookName =  br.readLine() ;
            br.close();
        } catch (Exception e) {
            e.toString();
        }
        String sBookid = String.valueOf(insertbook(sBookName, sQidianURL, oDB)); // 新增书籍 并 获取id
        
    	String txtContent = site_qidian.qidian_getTextFromPageJS(txt.replace("\r\n", "\n")) + "\n<end>\n" ;
		db.beginTransaction();// 开启事务
		try {
			Matcher mat = Pattern.compile("(?mi)^([^\\r\\n]+)[\\r\\n]{1,2}更新时间.*$[\\r\\n]{2,4}([^\\a]+?)(?=(^([^\\r\\n]+)[\\r\\n]{1,2}更新时间)|^<end>$)").matcher(txtContent);
			while (mat.find()) {
				db.execSQL("insert into page(bookid,name,content,CharCount) values(?,?,?,?)",
						new Object[] { sBookid, mat.group(1), mat.group(2).replace("\n\n", "\n"), String.valueOf(mat.group(2).length()) });
			}
			db.setTransactionSuccessful();// 设置事务的标志为True
		} finally {
			db.endTransaction();// 结束事务,有两种情况：commit,rollback,// 事务的提交或回滚是由事务的标志决定的,如果事务的标志为True，事务就会提交，否侧回滚,默认情况下事务的标志为False
		}
        return sBookName;
    }

    public static List<Map<String, Object>> getEbookChaters(boolean isHTMLOut, FoxMemDB db){
    	return getEbookChaters(isHTMLOut, "all", db);
    }
	public static List<Map<String, Object>> getEbookChaters(boolean isHTMLOut, String iBookID, FoxMemDB db){
		String addSQL = "" ;
		if ( ! iBookID.equalsIgnoreCase("all") ) {
			addSQL = " and page.bookid = " + iBookID ;
		}
		Cursor cursor = db.getDB().rawQuery("select page.name, page.content, book.name, book.id from book,page where book.id = page.bookid and page.content is not null " + addSQL + " order by page.bookid,page.id", null);
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(200);
		Map<String, Object> item;
		long preBookID = 0 ;
		long nowBookID = 0 ;
		if (cursor.moveToFirst()) {
			do {
				item = new HashMap<String, Object>();
				nowBookID = cursor.getLong(3) ;
				if ( preBookID != nowBookID ) { // 本次ID和上次不同，说明是书的开头
					item.put("title", "●" + cursor.getString(2) + "●" + cursor.getString(0));
					preBookID = nowBookID ;
				} else {
					item.put("title", cursor.getString(0));
				}
				if (isHTMLOut) {
					item.put("content", "\n　　" + cursor.getString(1).replace("\n", "<br/>\n　　"));
				} else {
					item.put("content", "　　" + cursor.getString(1).replace("\n", "\n　　"));
				}
				data.add(item);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return data;
	}
	
	public static void deleteBook(int bookid, FoxMemDB db) { // 删除一本书
		db.execSQL("Delete From Page where BookID = " + bookid);
		db.execSQL("Delete From Book where ID = " + bookid);
	}
	
	public static int insertbook(String bookname, String bookurl, FoxMemDB db) { // 插入一本新书，并返回bookid
		ContentValues xxx = new ContentValues();
		xxx.put("Name", bookname);
		xxx.put("URL", bookurl);
		long bookid = db.getDB().insert("book", null, xxx);
		
		if ( -1 == bookid ){
			return 0;
		} else {
			return (int)bookid;
		}
	}
	
	public static void regenID(int sortmode, FoxMemDB oDB) { // 重新排序bookid ,pageid
		String sSQL = "";
		switch ( sortmode )
		{
		case 1: // 书籍页数顺序
			sSQL = "select book.ID from Book left join page on book.id=page.bookid group by book.id order by count(page.id),book.isEnd,book.ID" ;
			break;
		case 2: // 书籍页数倒序
			sSQL = "select book.ID from Book left join page on book.id=page.bookid group by book.id order by count(page.id) desc,book.isEnd,book.ID" ;
			break;
		case 9:  // 根据bookid重新生成pageid
			sSQL = "select id from page order by bookid,id";
			break;
		}
		
		// 获取id列表到数组中
		SQLiteDatabase db = oDB.getDB();
		Cursor cursor = db.rawQuery(sSQL, null);
		int nRow = cursor.getCount();
		if ( nRow == 0 ) {
            return;
        }
		int [] ids = new int[nRow];
		int i = 0 ;
		if (cursor.moveToFirst()) {
			do {
				ids[i] = cursor.getInt(0) ;
				++ i;
			} while (cursor.moveToNext());
		}
		cursor.close();	
		
		// 最大ID
		int nStartID = 99999 ;
		if ( 9 == sortmode ) {
			nStartID = 5 + Integer.valueOf(oDB.getOneCell("select max(id) from page"));
		} else {
			nStartID = 5 + Integer.valueOf(oDB.getOneCell("select max(id) from book"));
		}
		int nStartID1 = nStartID;
		int nStartID2 = nStartID;

		
		db.beginTransaction();// 开启事务
		try {
			for (i=0; i<nRow; i++) {
				++nStartID1;
				if ( 9 == sortmode ) {
					db.execSQL("update page set id=" + nStartID1 + " where id=" + ids[i]);
				} else {
					db.execSQL("update page set bookid=" + nStartID1 + " where bookid=" + ids[i]);
					db.execSQL("update book set id=" + nStartID1 + " where id=" + ids[i]);
				}
			}
			db.setTransactionSuccessful(); 
		} finally {
			db.endTransaction(); 
		}
		
		db.beginTransaction();
		try {
			for (i=1; i<=nRow; i++) {
				++nStartID2;
				if ( 9 == sortmode ) {
					db.execSQL("update page set id=" + i + " where id=" + nStartID2);
				} else {
					db.execSQL("update page set bookid=" + i + " where bookid=" + nStartID2);
					db.execSQL("update book set id=" + i + " where id=" + nStartID2);
				}
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction(); 
		}
		if ( 9 != sortmode ) {
			db.execSQL("update Book set Disorder=ID");
		}
	}
	
	public static List<Map<String, Object>> getBookList(FoxMemDB db) { // 获取书籍列表
//		String sql = "select book.Name,count(page.id) as count,book.ID,book.URL,book.isEnd from Book left join page on book.id=page.bookid group by book.id order by count Desc";
		String sql ="select book.Name,count(page.id) as count,book.ID,book.URL,book.isEnd from Book left join page on book.id=page.bookid group by book.id order by book.DisOrder";
		Cursor cursor = db.getDB().rawQuery(sql, null);

		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(25);
		Map<String, Object> item;
		if (cursor.moveToFirst()) {
			do {
				item = new HashMap<String, Object>();
				item.put("name", cursor.getString(0));
				item.put("count", String.valueOf(cursor.getInt(1)));
				item.put("id", cursor.getInt(2));
				item.put("url", cursor.getString(3));
				item.put("isend", cursor.getInt(4));
				data.add(item);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return data;
	}

	public static List<Map<String, Object>> getPageList(String sqlWhereStr, FoxMemDB db) { // 获取页面列表
		String sql = "select name, ID, URL,Bookid, length(content) from page " + sqlWhereStr ;
		Cursor cursor = db.getDB().rawQuery(sql, null);

		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(25);
		Map<String, Object> item;
		if (cursor.moveToFirst()) {
			do {
				item = new HashMap<String, Object>();
				item.put("name", cursor.getString(0));
				item.put("id", cursor.getInt(1));
				item.put("url", cursor.getString(2));
				item.put("bookid", cursor.getInt(3));
				item.put("count", cursor.getInt(4));
				data.add(item);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return data;
	}

	public static List<Map<String, Object>> getBookNewPages(int bookid, FoxMemDB db) { // 获取数据库中新增的章节
		List<Map<String, Object>> xx = new ArrayList<Map<String, Object>>(100);
		Map<String, Object> item;

		String bookurl = db.getOneCell("select url from book where id = " + bookid);
		try {
			Cursor cursor = db.getDB().rawQuery("select id,url from page where ( bookid="
				+ String.valueOf(bookid)
				+ " ) and ( (content is null) or ( length(content) < 9 ) )",
				null);
			if (cursor.moveToFirst()) {
				do {
					item = new HashMap<String, Object>();
					item.put("id", cursor.getInt(0));
					item.put("url", FoxBookLib.getFullURL(bookurl,cursor.getString(1)));
					xx.add(item);
				} while (cursor.moveToNext());
			}
			cursor.close();
		} catch (Exception e) {
			e.toString();
		}
		return xx;
	}
		
	public static synchronized void inserNewPages(ArrayList<HashMap<String, Object>> data, int bookid, FoxMemDB oDB) { // 新增章节到数据库
		SQLiteDatabase db = oDB.getDB();
		db.beginTransaction();// 开启事务
		try {
			String sbookid = String.valueOf(bookid);
			Iterator<HashMap<String, Object>> itr = data.iterator();
			HashMap<String, Object> mm;
			String nowName, nowURL;
			while (itr.hasNext()) {
				mm = (HashMap<String, Object>) itr.next();
				nowName = (String) mm.get("name");
				nowURL = (String) mm.get("url");
				// Log.e("FoxDB1", "new : " + nowURL);
				db.execSQL("insert into page(bookid,url,name) values(?,?,?)",
						new Object[] { sbookid, nowURL, nowName });
			}
			db.setTransactionSuccessful();// 设置事务的标志为True
		} finally {
			db.endTransaction();// 结束事务,有两种情况：commit,rollback,
			// 事务的提交或回滚是由事务的标志决定的,如果事务的标志为True，事务就会提交，否侧回滚,默认情况下事务的标志为False
		}
	}

	public static void delete_nowupdown_Pages(int pageid, boolean bLE, boolean bUpdateDelList, FoxMemDB oDB) { // 删除某章节以上章节
		SQLiteDatabase db = oDB.getDB();
		int bookid = Integer.valueOf(oDB.getOneCell("select bookid from page where id=" + pageid));
		if (bUpdateDelList) { // 修改 DelURL
			String oldDelStr = oDB.getOneCell("select DelURL from book where id = " + bookid);
			String newDelStr = "";
			if ( bLE ) {
				newDelStr = getPageListStr_notDel(" where bookid=" + bookid + " and id <= " + pageid, oDB);
			} else {
				newDelStr = getPageListStr_notDel(" where bookid=" + bookid + " and id >= " + pageid, oDB);
			}
			ContentValues args = new ContentValues();
			args.put("DelURL", oldDelStr.replace("\n\n", "\n") + newDelStr);
			db.update("book", args, "id=" + bookid, null);
		}
		if ( bLE ) {
			db.execSQL("Delete From Page where bookid = " + bookid + " and ID <= " + pageid);
		} else {
			db.execSQL("Delete From Page where bookid = " + bookid + " and ID >= " + pageid);
		}
	}
	
	public static void delete_Pages(int[] pageidlist, boolean bUpdateDelList, FoxMemDB db) { // 删除选定章节
		for (int i = 0; i < pageidlist.length; i++) { // 循环pageid
			delete_Pages(pageidlist[i], bUpdateDelList, db);
		}
	}

	public static void delete_Pages(int pageid, boolean bUpdateDelList, FoxMemDB db) { // 删除单章节
		if (bUpdateDelList) { // 修改 DelURL
			Map<String, String> xx = db.getOneRow("select book.DelURL as old, page.bookid as bid, page.url as url, page.name as name from book,page where page.id=" + pageid + " and book.id = page.bookid") ;
			ContentValues args = new ContentValues();
			args.put("DelURL", xx.get("old").replace("\n\n", "\n") + xx.get("url") + "|" + xx.get("name") + "\n");
			db.getDB().update("book", args, "id=" + xx.get("bid"), null);
		}
		db.execSQL("Delete From Page where ID = " + pageid);
	}
	
	public static void update_cell(String tbn, ContentValues cv, String argx, FoxMemDB db) { // 修改单个字段
		db.getDB().update(tbn, cv, argx, null);
	}

	public static void delete_Book_All_Pages(int bookid, boolean bUpdateDelList, FoxMemDB db) { // 清空book中章节列表
		String sbookid = String.valueOf(bookid);
		if (bUpdateDelList) { // 修改 DelURL
			ContentValues args = new ContentValues();
			args.put("DelURL", getPageListStr(bookid, db));
			db.getDB().update("book", args, "id=" + sbookid, null);
		}
		db.execSQL("Delete From Page where BookID = " + sbookid);
	}

	public static synchronized void setPageContent(int pageid, String text, FoxMemDB db) { // 修改指定章节的内容
		String aNow = (new java.text.SimpleDateFormat("yyyyMMddHHmmss")).format(new java.util.Date()) ;
		ContentValues args = new ContentValues();
		args.put("CharCount", text.length());
		args.put("Mark", "text");
		args.put("Content", text);
		args.put("DownTime", aNow);
		db.getDB().update("page", args, "id=" + String.valueOf(pageid), null);
	}

	public static String getPageListStr(int bookid, FoxMemDB db) { // 获取 url,name 列表
		return db.getOneCell("select DelURL from book where id = " + bookid).replace("\n\n", "\n") + getPageListStr_notDel("where bookid = " + bookid, db);
	}

	private static String getPageListStr_notDel(String sqlWhereStr, FoxMemDB db) { // 私有: 获取 未删除url,name列表
		String addDelList = "";
		Cursor cursor = db.getDB().rawQuery("select url, name from page " + sqlWhereStr, null);
		if (cursor.moveToFirst()) {
			do {
				addDelList += cursor.getString(0) + "|" + cursor.getString(1) + "\n";
			} while (cursor.moveToNext());
		}
		cursor.close();
		return addDelList;
	}

	public static void updatepage(int pageid, FoxMemDB db) {
		Map<String, String> xx = db.getOneRow("select book.url as bu,page.url as pu from book,page where page.id=" + String.valueOf(pageid) + " and  book.id in (select bookid from page where id=" + String.valueOf(pageid) + ")");
		String fullPageURL = FoxBookLib.getFullURL(xx.get("bu"),xx.get("pu"));		// 获取bookurl, pageurl 合成得到url

		updatepage(pageid, fullPageURL, db) ;
	}
	
	public static String updatepage(int pageid, String pageFullURL, FoxMemDB db) {
		String text = "";
		String html = "" ;
		int site_type = 0 ; // 特殊页面处理 

		if ( pageFullURL.contains(".qidian.com") ) { site_type = 99 ; }
		if ( pageFullURL.contains("files.qidian.com") ) { site_type = 98; }   // 起点手机站直接用txt地址好了
		if ( pageFullURL.contains(".qreader.") ) { site_type = SITES.SITE_QREADER ; }
		if ( pageFullURL.contains("zhuishushenqi.com") ) { site_type = SITES.SITE_ZSSQ ; } // 这个得放在qidian后面，因为有时候zssq地址会包含起点的url

		switch(site_type) {
			case SITES.SITE_ZSSQ:
				String json = FoxBookLib.downhtml(pageFullURL, "utf-8"); // 下载json
				text = site_zssq.json2Text(json);
				break;
			case SITES.SITE_QREADER:
				text = site_qreader.qreader_GetContent(pageFullURL);
				break;
			case 98:
				html = FoxBookLib.downhtml(pageFullURL, "GBK"); // 下载json
				text = site_qidian.qidian_getTextFromPageJS(html);
				break;
			case 99:
				String nURL = site_qidian.qidian_toTxtURL_FromPageContent(FoxBookLib.downhtml(pageFullURL)) ; // 2015-11-17: 起点地址变动，只能下载网页后再获取txt地址
				if ( nURL.equalsIgnoreCase("") ) {
					text = "" ;
				} else {
					html = FoxBookLib.downhtml(nURL);
					text = site_qidian.qidian_getTextFromPageJS(html);
				}
                break;
			default:
				html = FoxBookLib.downhtml(pageFullURL); // 下载url
				text = FoxBookLib.pagetext(html);   	// 分析得到text
		}

		if ( pageid > 0 ) { // 当pageid小于0时不写入数据库，主要用于在线查看
			FoxMemDBHelper.setPageContent(pageid, text,db); // 写入数据库
			return String.valueOf(0);
		} else {
			return text;
		}
	}
 
	   public static String all2txt(FoxMemDB db) {
	    	return all2txt("all", db);
	    }
		public static String all2txt(String iBookID, FoxMemDB db) { // 所有书籍转为txt
			String txtPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "fox.txt";
	    	return all2txt(iBookID, db, txtPath);
		}
		public static String all2txt(String iBookID, FoxMemDB db, String txtPath) { // 所有书籍转为txt
			StringBuilder txt = new StringBuilder(81920);
			List<Map<String, Object>> data ;
			if ( iBookID.equalsIgnoreCase("all") ) {
				data = FoxMemDBHelper.getEbookChaters(false, db);
			} else {
				data = FoxMemDBHelper.getEbookChaters(false, iBookID, db);
			}
			Iterator<Map<String, Object>> itr = data.iterator();
			while (itr.hasNext()) {
				HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
				txt.append(mm.get("title")).append("\n\n").append(mm.get("content")).append("\n\n\n");
			}

			FoxBookLib.writeText(txt.toString(), txtPath, "utf-8");
			return "/fox.txt"; // 给foxHTTPD当下载路径使用
		}
		
		public static void all2epub(FoxMemDB db) {// 所有书籍转为epub
			String epubPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "fox.epub";
			FoxEpub oEpub = new FoxEpub("FoxBook", epubPath);
			
			List<Map<String, Object>> data = FoxMemDBHelper.getEbookChaters(true, db);
			Iterator<Map<String, Object>> itr = data.iterator();
			HashMap<String, Object> mm;
			while (itr.hasNext()) {
				mm = (HashMap<String, Object>) itr.next();
				oEpub.AddChapter((String)mm.get("title"), (String)mm.get("content"), -1);
			}
			oEpub.SaveTo();
		}

		public static void all2umd(FoxMemDB db) { // 所有书籍转为umd
			String umdPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "fox.umd";
			Umd umd = new Umd();
			
			UmdHeader uh = umd.getHeader(); // 设置书籍信息
			uh.setTitle("FoxBook");
			uh.setAuthor("爱尔兰之狐");
			uh.setBookType("小说");
			uh.setYear("2014");
			uh.setMonth("04");
			uh.setDay("01");
			uh.setBookMan("爱尔兰之狐");
			uh.setShopKeeper("爱尔兰之狐");

			
			UmdChapters  cha = umd.getChapters(); // 设置内容
			List<Map<String, Object>> data = FoxMemDBHelper.getEbookChaters(false, db);
			Iterator<Map<String, Object>> itr = data.iterator();
			while (itr.hasNext()) {
				HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
				cha.addChapter((String) mm.get("title"), (String) mm.get("content"));
			}

	        File file = new File(umdPath); // 生成
	        try {
	            FileOutputStream fos = new FileOutputStream(file);
	            try {
	                BufferedOutputStream bos = new BufferedOutputStream(fos);
	                umd.buildUmd(bos);
	                bos.flush();
	             } finally {
	                fos.close();
	            }
	        } catch (Exception e) {
	        	e.toString();
	        }
		}

	
}
