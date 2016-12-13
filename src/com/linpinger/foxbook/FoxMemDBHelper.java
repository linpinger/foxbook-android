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

	public static String importQidianEpub(FoxEpubReader epub, FoxMemDB oDB) { // 导入起点epub
		HashMap<String, Object> qhm = epub.getQiDianEpubInfo();
		String qidianid = qhm.get("qidianid").toString();
		String sBookid = String.valueOf(FoxMemDBHelper.insertbook(qhm.get("bookname").toString()
				, site_qidian.qidian_getIndexURL_Mobile(Integer.valueOf(qidianid)), qidianid, oDB));

		// 导入内容到RamDB
		String fileName ;
		String pageTitle ;
		String pageText ;
		SQLiteDatabase db = oDB.getDB();
		db.beginTransaction();// 开启事务
		for ( HashMap<String, Object> hm : epub.getQiDianEpubTOC() ) {
			fileName = hm.get("name").toString();
			pageTitle = hm.get("title").toString();
			pageText = epub.getQiDianEpubPage(fileName);
			db.execSQL("insert into page(bookid,name, url,content,CharCount) values(?,?,?,?,?)",
					new Object[] { sBookid, pageTitle
					, site_qidian.qidian_getPageURL(Integer.valueOf(hm.get("pageid").toString()), Integer.valueOf(hm.get("bookid").toString()))
					, pageText, String.valueOf(pageText.length()) });
		}
		db.setTransactionSuccessful();// 设置事务的标志为True
		db.endTransaction();
		return qhm.get("bookname").toString();
	}
	public static String importQidianTxt(String txtPath, FoxMemDB oDB) {
		return importQidianTxt(txtPath, oDB, true); // 使用新方式，更快 
	}
    public static String importQidianTxt(String txtPath, FoxMemDB oDB, boolean isNew) {
    	// boolean isNew = true;
    	//long sTime = System.currentTimeMillis();
        // 第一步检测编码，非GBK就是UTF-8，其他不予考虑
        String txtEnCoding = ToolJava.detectTxtEncoding(txtPath) ; // 猜测中文文本编码 返回: "GBK" 或 "UTF-8"
        String txt = ToolJava.readText(txtPath, txtEnCoding).replace("\r", "").replace("　", ""); // 为起点txt预处理

        if ( ! txt.contains("更新时间") ) // 非起点文本
			return importNormalTxt(txtPath, oDB, txtEnCoding);

        SQLiteDatabase db = oDB.getDB();
        String sQidianid = (new File(txtPath)).getName().replace(".txt", ""); // 文件名
        String sQidianURL = site_qidian.qidian_getIndexURL_Mobile(Integer.valueOf(sQidianid)); // URL
        String sBookName = sQidianid;

        // 新版要快很多，而且少了头部的无用章节
        // 不过如果以后起点txt有结构变动的话，适应性可能不如旧版，故保留旧版
        if ( isNew ) { // 新版，可能有bug
    		String line[] = txt.split("\n");
    		int lineCount = line.length;
    		sBookName = line[0] ;
    		String sBookid = String.valueOf(insertbook(sBookName, sQidianURL, sQidianid, oDB)); // 新增书籍 并 获取id
    		int titleNum = 0 ; // base:0 包含
    		int headNum = 0 ; // base:0  包含
			int lastEndNum = 0 ;
    		db.beginTransaction();// 开启事务
    		for ( int i=3; i<lineCount; i++) { // 从第四行开始
    			if (line[i].startsWith("更新时间")) { // 上一行为标题行
    				titleNum = i - 1 ;
    				headNum = i + 2 ;
    			} else { // 非标题行
    				if ( line[i].startsWith("<a href=") ) {
						if ( i - lastEndNum < 5 ) // 有些txt章节尾部有两行链接，醉了
							continue;
    					// 在这里获取标题行，内容行
    					// System.out.println(titleNum + " : " + headNum + " - " + i );
    					StringBuilder sbd = new StringBuilder();
    					for ( int j=headNum; j<i; j++)
    						sbd.append(line[j]).append("\n");
    					sbd.append("\n");
    					db.execSQL("insert into page(bookid,name,content,CharCount) values(?,?,?,?)",
            					new Object[] { sBookid, line[titleNum], sbd.toString(), String.valueOf(sbd.length()) });
						lastEndNum = i;
    				}
    			}
    		}
    		db.setTransactionSuccessful();// 设置事务的标志为True
    		db.endTransaction();
        } else { // 旧版，稍微慢一点
        	try {  // 第一行书名
        		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(txtPath), "GBK"));
        		sBookName =  br.readLine() ;
        		br.close();
        	} catch (Exception e) {
        		e.toString();
        	}
        	String sBookid = String.valueOf(insertbook(sBookName, sQidianURL, sQidianid, oDB)); // 新增书籍 并 获取id

        	String txtContent = site_qidian.qidian_getTextFromPageJS(txt) + "\n<end>\n" ;
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
        }
        // Log.e("XX", "耗时: " + (System.currentTimeMillis() - sTime));
        return sBookName;
    }

	private static String importNormalTxt(String txtPath, FoxMemDB oDB, String txtEnCoding) {
//        String txtEnCoding = ToolBookJava.detectTxtEncoding(txtPath) ; // 猜测中文文本编码 返回: "GBK" 或 "UTF-8"
//        String txt = ToolBookJava.readText(txtPath, txtEnCoding) ;

        String fileName = (new File(txtPath)).getName().replace(".txt", ""); // 文件名
        SQLiteDatabase db = oDB.getDB();
        String sBookid = String.valueOf(insertbook(fileName, "txt", "00", oDB)); // 新增书籍 并 获取id

        db.beginTransaction();// 开启事务
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(txtPath), txtEnCoding));
            StringBuilder chunkStr = new StringBuilder(65536);
            int chunkLen = 0;
            int chunkCount = 0;
            String line = null;
			int lineLen = 0;
            while ((line = br.readLine()) != null) {
                if ( line.startsWith("　　") ) // 去掉开头的空白
                    line = line.replaceFirst("　　*", "");

				lineLen = line.length() ;
                chunkLen = chunkStr.length();
                if ( chunkLen > 2200 && lineLen < 22 && ( line.startsWith("第") || line.contains("卷") || line.contains("章") || line.contains("节") || line.contains("回") || line.contains("品") || lineLen > 2 ) ) {
                    ++ chunkCount;
                      db.execSQL("insert into page(bookid,name,content,CharCount) values(?,?,?,?)",
                          new Object[] { sBookid, txtEnCoding + "_" + String.valueOf(chunkCount), chunkStr.toString(), String.valueOf(chunkStr.length()) });
                    chunkStr = new StringBuilder(65536);
                 }
                 chunkStr.append(line).append("\n");
            }
            if ( chunkStr.length() > 0 ) {
                 ++ chunkCount;
                  db.execSQL("insert into page(bookid,name,content,CharCount) values(?,?,?,?)",
                          new Object[] { "1", txtEnCoding + "_" + String.valueOf(chunkCount), chunkStr.toString(), String.valueOf(chunkStr.length()) });
            }
            br.close();
            db.setTransactionSuccessful();// 设置事务的标志为True
        } catch (IOException e) {
            e.toString();
         } finally {
            db.endTransaction();// 结束事务,有两种情况：commit,rollback,  事务的提交或回滚是由事务的标志决定的,如果事务的标志为True，事务就会提交，否侧回滚,默认情况下事务的标志为False
        }
        return fileName;
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

    public static int insertbook(String bookname, String bookurl, String qidianid, FoxMemDB db) { // 插入一本新书，并返回bookid
        ContentValues xxx = new ContentValues();
        xxx.put("Name", bookname);
        xxx.put("URL", bookurl);
		if (qidianid != null) {
        	xxx.put("QiDianID", qidianid);
		}
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

    // 获取有新章的书列表,　返回的数组元素: bookid, bookname, bookurl
    public static ArrayList<HashMap<String, Object>> compareShelfToGetNew(FoxMemDB db) {
        // 下载书架网页, 需要 书架地址，cookie
        // 正则分析网页，合成 得到 书地址, 书名, 新章节地址, 新章节名
        // 查数据库得到 bookid, bookname, bookurl, pageUrlList
        // 比较书籍的 新章节地址是否在 pageUrlList 中，否就加入返回列表中
        ArrayList<HashMap<String, Object>> book = getBookListForShelf(db);

        String SiteURL = (String) book.get(0).get("url");
        int SiteType = 0 ;
        String cookieSQL = "";
        String urlShelf = "";
        String reShelf  = "(?smi)<tr>.*?(aid=[^\"]*)\"[^>]*>([^<]*)<.*?<td class=\"odd\"><a href=\"([^\"]*)\"[^>]*>([^<]*)<";
        if ( SiteURL.contains(".13xs.com") ) {
            SiteType = 13 ;
            cookieSQL = ".13xs." ;
            urlShelf = "http://www.13xs.com/shujia.aspx";
            reShelf  = "(?smi)<tr>.*?(aid=[^\"]*)&index.*?\"[^>]*>([^<]*)<.*?<td class=\"odd\"><a href=\"[^\"]*cid=([0-9]*)\"[^>]*>([^<]*)<" ;
        }
        if ( SiteURL.contains(".biquge.com.tw") ) {
            SiteType = 2 ;
            cookieSQL = ".biquge." ;
            urlShelf = "http://www.biquge.com.tw/modules/article/bookcase.php";
            reShelf  = "(?smi)<tr>.*?(aid=[^\"]*)\"[^>]*>([^<]*)<.*?<td class=\"odd\"><a href=\"([^\"]*)\"[^>]*>([^<]*)<";
        }
        if ( SiteURL.contains(".dajiadu.net") ) {
            SiteType = 4 ;
            cookieSQL = ".dajiadu." ;
            urlShelf = "http://www.dajiadu.net/modules/article/bookcase.php" ;
            reShelf  = "(?smi)<tr>.*?(aid=[^\"]*)&index.*?\"[^>]*>([^<]*)<.*?<td class=\"odd\"><a href=\"[^\"]*cid=([0-9]*)\"[^>]*>([^<]*)<" ;
        }
        if ( SiteURL.contains("m.qreader.me") ) {
            SiteType = 99 ;
            urlShelf = "http://m.qreader.me/update_books.php" ;
            reShelf = "(?smi)\"id\":([0-9]*),\"status\":([0-9]*).*?\"chapter_i\":([0-9]*),\"chapter_n\":\"([^\"]*)\"";
        }

        if ( 0 == SiteType ) {
            return null;
        }

        String html = "";
        if ( 99 == SiteType ) {
            Iterator<HashMap<String, Object>> itrQQ = book.iterator();
            String qindexURL ;
            String postData = "{\"books\":[" ;
            Pattern p = Pattern.compile("bid=([0-9]+)");
            while (itrQQ.hasNext()) {
                qindexURL = (String) ( (HashMap<String, Object>) itrQQ.next() ).get("url");
                Matcher m = p.matcher(qindexURL) ;
                while(m.find())
                    postData = postData + "{\"t\":0,\"i\":" + m.group(1) + "},";
            }
            if ( postData.endsWith(",") )
                postData = postData.substring(0, postData.length()-1) ;
            postData = postData + "]}";
            html = ToolBookJava.downhtml(urlShelf, "", postData);
        } else {
            String cookie = db.getOneCell("SELECT cookie from config where site like '%" + cookieSQL + "%' ") ;
            html = ToolBookJava.downhtml(urlShelf, "gbk", "GET", ToolBookJava.cookie2Field(cookie)) ;
        }
        if ( html.length() < 5 )
            return null ;

        HashMap<String, String> shelfBook = new HashMap<String, String>(30); // 书名 -> 新章节地址
        Matcher mat = Pattern.compile(reShelf).matcher(html);
        while (mat.find()) {
            switch (SiteType) {
            case 13:
                shelfBook.put(mat.group(2), mat.group(3) + ".html");
                break;
            case 2:
                shelfBook.put(mat.group(2), mat.group(3));
                break;
            case 4:
                shelfBook.put(mat.group(2), mat.group(3) + ".html");
                break;
            case 99: // BID -> 新章节地址
                shelfBook.put("BID" + mat.group(1), "#" + mat.group(3));
                break;
            }
        }

        ArrayList<HashMap<String, Object>> newPages = new ArrayList<HashMap<String, Object>>(30);
        Iterator<HashMap<String, Object>> itr = book.iterator();
        HashMap<String, Object> mm;
        String nowName, nowURL, nowPageList;
        Pattern pp = Pattern.compile("bid=([0-9]+)");
        while (itr.hasNext()) {
            mm = (HashMap<String, Object>) itr.next();
//            nowBID =  String.valueOf((Integer)mm.get("id"));
            nowName = (String) mm.get("name");
            nowPageList = (String) mm.get("pagelist");
            if ( 99 == SiteType ) {
                nowURL = (String) mm.get("url");
                Matcher m = pp.matcher(nowURL) ;
                while(m.find()) {
                    if ( ! nowPageList.contains("\n" + (String)shelfBook.get("BID" + m.group(1)) + "|") ) {
                        newPages.add(mm);
                    }
                }
            } else {
                if ( ! nowPageList.contains("\n" + (String)shelfBook.get(nowName) + "|") ) {
                    newPages.add(mm);
                }
            }
        }
        return newPages;
    }

    public static ArrayList<HashMap<String, Object>> getBookListForShelf(FoxMemDB db) { // 获取比较书架需要的数据
        ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>(30);
        String sql = "select id,name,url,DelURL from book where ( isEnd isnull or isEnd = '' or isEnd < 1 )" ;

        Cursor cursor = db.getDB().rawQuery(sql, null);
        HashMap<String, Object> item;
        if (cursor.moveToFirst()) {
            do {
                item = new HashMap<String, Object>();
                item.put("id", cursor.getInt(0));
                item.put("name", cursor.getString(1));
                item.put("url", cursor.getString(2));
                item.put("pagelist", (cursor.getString(3) + "\n" + getPageListStr_notDel("where bookid = " + cursor.getInt(0), db)).replace("\n\n", "\n"));
                data.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return data;
    }

    public static List<Map<String, Object>> getBookList(FoxMemDB db) { // 获取书籍列表
//        String sql = "select book.Name,count(page.id) as count,book.ID,book.URL,book.isEnd from Book left join page on book.id=page.bookid group by book.id order by count Desc";
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
		return getPageList(sqlWhereStr, 0, db) ;
	}
    public static List<Map<String, Object>> getPageList(String sqlWhereStr, int sMode, FoxMemDB db) { // 获取页面列表
        String sql = "select name, ID, URL,Bookid, length(content) from page " + sqlWhereStr ;
		if ( sMode == 26 ) { // ZIP专用，显示字数
			sql = "select name, ID, URL,Bookid, CharCount from page " + sqlWhereStr ;
			sMode = 0;
		}
        Cursor cursor = db.getDB().rawQuery(sql, null);

		int lastBID = 0 ;
		int nowBID = 0 ;
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(25);
        Map<String, Object> item;
        if (cursor.moveToFirst()) {
            do {
                item = new HashMap<String, Object>();
				nowBID = cursor.getInt(3) ;
				if ( 0 == sMode ) {
                	item.put("name", cursor.getString(0));
				} else {
					if ( nowBID == lastBID ) {
                		item.put("name", cursor.getString(0));
					} else {
                		item.put("name", "★" + cursor.getString(0));
					}
				}
                item.put("id", cursor.getInt(1));
                item.put("url", cursor.getString(2));
                item.put("bookid", nowBID);
                item.put("count", cursor.getInt(4));
                data.add(item);
				lastBID = nowBID ;
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
                    item.put("url", ToolBookJava.getFullURL(bookurl,cursor.getString(1)));
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

    // 精简所有书的DelURL
    public static void simplifyAllDelList(FoxMemDB oDB) {
        SQLiteDatabase db = oDB.getDB();
        db.beginTransaction();// 开启事务
        try {
            Cursor cursor = db.rawQuery("select ID, DelURL from book where length(DelURL) > 128", null);
            if (cursor.moveToFirst()) {
                do {
                    db.execSQL("update Book set DelURL=? where id=" + String.valueOf(cursor.getInt(0)),
                            new Object[] { ToolBookJava.simplifyDelList(cursor.getString(1)) });
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.setTransactionSuccessful();// 设置事务的标志为True
        } finally {
            db.endTransaction();
        }
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
        String fullPageURL = ToolBookJava.getFullURL(xx.get("bu"),xx.get("pu")); // 获取bookurl, pageurl 合成得到url

        updatepage(pageid, fullPageURL, db) ;
    }

    public static String updatepage(int pageid, String pageFullURL, FoxMemDB db) {
        String text = "";
        String html = "" ;
        int site_type = 0 ; // 特殊页面处理 

        if ( pageFullURL.contains(".qidian.com") ) { site_type = 99 ; }
        if ( pageFullURL.contains("files.qidian.com") ) { site_type = 98; }   // 起点手机站直接用txt地址好了

        switch(site_type) {
            case 98:
                html = ToolBookJava.downhtml(pageFullURL, "GBK"); // 下载json
                text = site_qidian.qidian_getTextFromPageJS(html);
                break;
            case 99:
                String nURL = site_qidian.qidian_toTxtURL_FromPageContent(ToolBookJava.downhtml(pageFullURL)) ; // 2015-11-17: 起点地址变动，只能下载网页后再获取txt地址
                if ( nURL.equalsIgnoreCase("") ) {
                    text = "" ;
                } else {
                    html = ToolBookJava.downhtml(nURL);
                    text = site_qidian.qidian_getTextFromPageJS(html);
                }
                break;
            default:
                html = ToolBookJava.downhtml(pageFullURL); // 下载url
                text = ToolBookJava.pagetext(html);       // 分析得到text
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

            ToolJava.writeText(txt.toString(), txtPath);
            return "/fox.txt"; // 给foxHTTPD当下载路径使用
        }

        public static void all2epub(FoxMemDB db) {// 所有书籍转为epub
            FoxEpubWriter oEpub = new FoxEpubWriter(new File(Environment.getExternalStorageDirectory(), "fox.epub"));

            List<Map<String, Object>> data = FoxMemDBHelper.getEbookChaters(true, db);
            Iterator<Map<String, Object>> itr = data.iterator();
            HashMap<String, Object> mm;
            while (itr.hasNext()) {
                mm = (HashMap<String, Object>) itr.next();
                oEpub.addChapter((String)mm.get("title"), (String)mm.get("content"), -1);
            }
            oEpub.saveAll();
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

