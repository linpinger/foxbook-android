package com.linpinger.novel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.tool.ToolJava;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class StorDB3 extends Stor {

	public List<Novel> load(File inFile) {
		File tmpBakFile = new File(inFile.getPath() + ".faksdj");
		ToolJava.copyFile(inFile, tmpBakFile);

		List<Novel> lst = new ArrayList<Novel>();
		Novel book ;
		Map<String, Object> info;
		List<Map<String, Object>> chapters;
		Map<String, Object> page;

		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(inFile, null);
		Cursor cursorB = db.rawQuery("select * from book", null);
		if (cursorB.moveToFirst()) {
			do {
				book = new Novel();

				info = new HashMap<String, Object>();
				info.put(NV.BookName, cursorB.getString(1));
				info.put(NV.BookURL, cursorB.getString(2));
				info.put(NV.DelURL, cursorB.getString(3));
				info.put(NV.BookStatu, cursorB.getInt(5));
				info.put(NV.QDID, cursorB.getString(6));
				info.put(NV.BookAuthor, "noname");
				book.setInfo(info);

				chapters = new ArrayList<Map<String, Object>>();
				Cursor cursorP = db.rawQuery("select name, url, content, charcount from page where bookid=" + cursorB.getInt(0), null);
				if (cursorP.moveToFirst()) {
					do {
						page = new HashMap<String, Object>();
						page.put(NV.PageName, cursorP.getString(0));
						page.put(NV.PageURL, cursorP.getString(1));
						page.put(NV.Content, cursorP.getString(2));
						page.put(NV.Size, Integer.valueOf(cursorP.getString(3)));
						chapters.add(page);
					} while (cursorP.moveToNext());
				}
				cursorP.close();
				book.setChapters(chapters);

				lst.add(book);
			} while (cursorB.moveToNext());
		}
		cursorB.close();

//		db.execSQL("drop table if exists main.android_metadata;");
		db.close();
		new File(inFile.getPath() + "-journal").delete();
		inFile.delete();
		tmpBakFile.renameTo(inFile);
		return lst ;
	}

	public void save(List<Novel> inList , File outFile) {
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(outFile, null);
		db.execSQL("CREATE TABLE Book (ID integer primary key, Name Text, URL text, DelURL text, DisOrder integer, isEnd integer, QiDianID text, LastModified text);");
		db.execSQL("CREATE TABLE config (ID integer primary key, Site text, ListRangeRE text, ListDelStrList text, PageRangeRE text, PageDelStrList text, cookie text);");
		db.execSQL("CREATE TABLE Page (ID integer primary key, BookID integer, Name text, URL text, CharCount integer, Content text, DisOrder integer, DownTime integer, Mark text);");

		Map<String, Object> info;
		ContentValues vv ;
		long bookid = 0 ;
		db.beginTransaction();// 开启事务
		try {
			for (Novel novel : inList) {
				info = novel.getInfo();
				vv = new ContentValues();
				vv.put("Name", info.get(NV.BookName).toString());
				vv.put("URL", info.get(NV.BookURL).toString());
				vv.put("DelURL", info.get(NV.DelURL).toString());
				vv.put("isEnd", info.get(NV.BookStatu).toString());
				vv.put("QiDianID", info.get(NV.QDID).toString());

				bookid = db.insert("book", null, vv);
				for (Map<String, Object> page : novel.getChapters()) {
					db.execSQL("insert into page(bookid, name, url, content, CharCount, DownTime, Mark) values(?,?,?,?,?, 20161225122500, 'text')",
							new Object[] { bookid
							, page.get(NV.PageName)
							, page.get(NV.PageURL)
							, page.get(NV.Content)
							, page.get(NV.Size) });
				}
			}
			db.setTransactionSuccessful();// 设置事务的标志为True
		} finally {
			db.endTransaction();
		}
		db.execSQL("drop table if exists main.android_metadata;");
		db.close();
		new File(outFile.getPath() + "-journal").delete();
	}

}
