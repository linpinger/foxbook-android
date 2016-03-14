package com.linpinger.foxbook;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

public class FoxMemDB {
	private Context cc;
	private File fDB ;
	private int nowDBnum = 0 ;
	
	private boolean isMemDB = true ;  // 是否是内存数据库
	private boolean isIntDB = true ;  // 是否是内部存储数据库，即保存在/data目录下
	private SQLiteDatabase db ;
	
	FoxMemDB(boolean inisMemDB, boolean inisIntDB, Context ct) {  // 无参打开默认数据库
		this.cc = ct;
		this.isMemDB = inisMemDB;
		this.isIntDB = inisIntDB;
		if (isIntDB) {
			this.fDB = cc.getFileStreamPath("FoxBook.db3");
		} else {
			this.fDB = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "FoxBook.db3");
		}
		createMemDB(fDB);
	}
	FoxMemDB(File DBFile, Context ct) { // 打开DB3/起点txt
		this.cc = ct;
		this.isMemDB = true ;
		this.isIntDB = false;
		this.fDB = DBFile;
		createMemDB(fDB);
	}

	public SQLiteDatabase getDB() {
		return db;
	}
	
	public void closeMemDB() {
		vacuumMemDB();
		if ( this.isMemDB ) {
			FoxMemDBBackupAndRestore(db, fDB, true); // 将旧的数据库保存到磁盘上
		}
		db.close();
	}

	public File switchMemDB() { // 切换数据库路径
		File oldDBPath = fDB;
		ArrayList<File> dbList ;
		if ( this.isIntDB ) {
			dbList = getDBList(cc.getFilesDir());
		} else {
			dbList = getDBList(Environment.getExternalStorageDirectory());
		}
		
        int countDBs = dbList.size();
        ++nowDBnum;
        if ( nowDBnum >= countDBs ) {
            nowDBnum = 0 ;
        }
        fDB = dbList.get(nowDBnum) ;
        
        vacuumMemDB();
		if ( this.isMemDB ) {
        	FoxMemDBBackupAndRestore(db, oldDBPath, true); // 将旧的数据库保存到磁盘上
		}
        db.close();
        
        createMemDB(fDB); // 导入新的
        return fDB;
	}

	public void execSQL(String inSQL) {
		db.execSQL(inSQL);
	}
	
	public String getOneCell(String inSQL) { // 获取一个cell
		String outStr = "";
		Cursor cursor = db.rawQuery(inSQL, null);
		if (cursor.getCount() > 0 && cursor.moveToFirst()) {
			do {
				outStr = cursor.getString(0);
				if ( null == outStr ) { outStr = ""; }
			} while (cursor.moveToNext());
		}
		cursor.close();
		return outStr;
	}

	
	// 该方法使用需注意key的大小写，或者可以在SQL中使用 as 命名的形式
	public HashMap<String, String> getOneRow(String inSQL) { // 获取一行
		HashMap<String, String> ree = new HashMap<String, String>();
		Cursor cursor = db.rawQuery(inSQL, null);
		String nowValue = "";
		if ( cursor.getCount() > 0 && cursor.moveToFirst()) {
			do {
				for (int i = 0; i < cursor.getColumnCount(); i++) {
					nowValue = cursor.getString(i) ;
					if ( null == nowValue ) {
						ree.put(cursor.getColumnName(i), "");
					} else {
						ree.put(cursor.getColumnName(i), nowValue);
					}
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return ree;
	}

// ToDo 简化table的获取
/*
	public List<Map<String, Object>> getTable(String inSQL) { // 获取页面列表
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		Map<String, Object> item;
		Cursor cursor = db.rawQuery(inSQL, null);
		int colCount = cursor.getColumnCount();
		int i = 0 ;
		for ( i = 0; i < colCount; i++ ) {
			if ( FieldType.Null == cursor.getType(i) ) {
				
			}
		}		
		if (cursor.getCount() > 0 && cursor.moveToFirst()) {
			do {
				for ( i = 0; i < colCount; i++ ) {
					cursor.getType(i);

				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return data;
	}
*/
	private ArrayList<File> getDBList(File DBDir) {
        ArrayList<File> retList = new ArrayList<File>(9); // 最多9个路径，以后可以按需修改
        retList.add(new File(DBDir.getAbsolutePath() + File.separator + "FoxBook.db3"));

        File[] fff = DBDir.listFiles(new FileFilter() {
            public boolean accept(File ff) {
                if (ff.isFile()) {
                    if (ff.toString().endsWith(".db3")) {
                        if (ff.getName().equalsIgnoreCase("FoxBook.db3")) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        int fc = fff.length;
        for (int i = 0; i < fc; i++) {
            retList.add(fff[i]);
        }
        return retList;
    }
    
	private void vacuumMemDB() { // 释放数据库空闲空间
		db.execSQL("vacuum");
	}
	
	private void createMemDB(File fileDB) { // 创建数据库并导入文件数据库
		if ( this.isMemDB ) {
			db = SQLiteDatabase.create(null); // 创建空的内存数据库
		} else {
			db = SQLiteDatabase.openOrCreateDatabase(fDB, null);
		}

		if ( fileDB.exists() ) { // 存在就导入
			if ( this.isMemDB ) {
				FoxMemDBBackupAndRestore(db, fileDB, false) ; // File -> Mem
			}
		} else { // 不存在就创建表结构
			db.execSQL("CREATE TABLE Book (ID integer primary key, Name Text, URL text, DelURL text, DisOrder integer, isEnd integer, QiDianID text, LastModified text);");
			db.execSQL("CREATE TABLE config (ID integer primary key, Site text, ListRangeRE text, ListDelStrList text, PageRangeRE text, PageDelStrList text, cookie text);");
			db.execSQL("CREATE TABLE Page (ID integer primary key, BookID integer, Name text, URL text, CharCount integer, Content text, DisOrder integer, DownTime integer, Mark text);");
		}
	}
	
	public void SD2Int(boolean isSD2Int) {
		String SDRoot = Environment.getExternalStorageDirectory().getPath() + File.separator ;
		ArrayList<File> dbListSD = getDBList(Environment.getExternalStorageDirectory());
		ArrayList<File> dbListInt = getDBList(cc.getFilesDir());
		
		if ( isSD2Int ) { // SD -> 内部
			for (File f : dbListInt) {
				f.delete(); // 先删除SD卡上的旧数据库
			}
			for (File ff : dbListSD) {
				try {
					FileInputStream fosfrom = new FileInputStream(ff);

					FileOutputStream fosto = cc.openFileOutput(ff.getName(), Context.MODE_PRIVATE);
		            byte bt[] = new byte[1024];
		            int c;
		            while ((c = fosfrom.read(bt)) > 0) {
		                fosto.write(bt, 0, c);
		            }
		            fosfrom.close();
		            fosto.close();
				} catch (Exception e) {
					e.toString();
				}
			}
		} else { // 内部 -> SD
			for (File f : dbListSD) { // 先备份SD卡上的旧数据库
				File oldFileDB = new File(f.getAbsolutePath() + ".old");
				if (oldFileDB.exists()) {
					oldFileDB.delete();
				}
				f.renameTo(oldFileDB);
			}
			for (File ff : dbListInt) {
				try {
					FileInputStream fosfrom = cc.openFileInput(ff.getName());

					FileOutputStream fosto = new FileOutputStream(SDRoot + ff.getName());
		            byte bt[] = new byte[1024];
		            int c;
		            while ((c = fosfrom.read(bt)) > 0) {
		                fosto.write(bt, 0, c);
		            }
		            fosfrom.close();
		            fosto.close();
		            
				} catch (Exception e) {
					e.toString();
				}
			}
		}
		

	}
	
	private void FoxMemDBBackupAndRestore(SQLiteDatabase oDBMem, File fileDB, boolean isBackupMemDBToFileDB) { // 内存数据库导入导出
		String FromDB = "main" ;
		String ToDB = "FoxAttach" ;
		if ( isBackupMemDBToFileDB ) { // Mem -> File
			FromDB = "main" ;
			ToDB = "FoxAttach" ;
			
			if ( fileDB.exists() ) { // 数据库.db3覆盖为 .db3.old
				if (this.isIntDB) {
					fileDB.delete();
				} else {
					File oldFileDB = new File(fileDB.getAbsolutePath() + ".old");
					if (oldFileDB.exists()) {
						oldFileDB.delete();
					}
					fileDB.renameTo(oldFileDB);
				}
			}
		} else { // File -> Mem
			FromDB = "FoxAttach" ;
			ToDB = "main" ;
		}
		
		oDBMem.execSQL("Attach '" + fileDB.getAbsolutePath() + "' as FoxAttach");
		oDBMem.execSQL("drop table if exists main.android_metadata; drop table if exists FoxAttach.android_metadata;"); // 该表的存在会导致异常，该表已存在
		String newSQL = "";
		String tbName = "";
		String oldSQL = "";
		try {
			Cursor cursor = oDBMem.rawQuery("select tbl_name,sql from " + FromDB + ".sqlite_master", null);
			if (cursor.moveToFirst()) {
				do {
					tbName = cursor.getString(0); // 表名
					oldSQL = cursor.getString(1); // SQL : create table ...
					Matcher mat = Pattern.compile("(?i)^.*" + tbName + "(.*)$").matcher(oldSQL);
					while (mat.find()) {
						newSQL = "create table " + ToDB + "." + tbName + mat.group(1);
					}
					oDBMem.execSQL(newSQL); //根据原数据库表格创建新表
					oDBMem.execSQL("insert into " + ToDB + "." + tbName + " select * from " + FromDB + "." + tbName ) ; //插入原表数据到新表
				} while (cursor.moveToNext());
			}
			cursor.close();
		} catch (Exception e) {
			e.toString();
		}
		oDBMem.execSQL("Detach FoxAttach"); //反附加数据库
	}

	// 蛋疼，3.x 才能 getType
	/*
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public ArrayList<HashMap<String, String>> getTable(String inSQL) { // 获取一行
		ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>(200);
		
		Cursor cursor = db.rawQuery(inSQL, null);
		int nowColumnType = 0;
		String nowValue = "";
		int nowColumnCount = 0;
		if ( cursor.getCount() > 0 && cursor.moveToFirst()) {
			do {
				HashMap<String, String> ree = new HashMap<String, String>();
				nowColumnCount = cursor.getColumnCount();
				for (int i = 0; i < nowColumnCount; i++) {
					nowColumnType = cursor.getType(i);
					switch (nowColumnType) {
						case Cursor.FIELD_TYPE_NULL:
							nowValue = "";
							break;
						case Cursor.FIELD_TYPE_INTEGER:
							nowValue = String.valueOf(cursor.getInt(i));
							break;
						case Cursor.FIELD_TYPE_FLOAT:
							nowValue = String.valueOf(cursor.getFloat(i));
							break;
						case Cursor.FIELD_TYPE_STRING:
							nowValue = cursor.getString(i) ;
							break;
						default:
							nowValue = "";
							break;
					}
					ree.put(cursor.getColumnName(i), nowValue);
				}
				data.add(ree);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return data;
	}
	*/

}
