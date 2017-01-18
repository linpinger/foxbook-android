package com.linpinger.novel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.linpinger.tool.ToolJava;

public class StorTxt extends Stor {

	public List<Novel> load(File inFile) {
		// boolean isNew = true;
		//long sTime = System.currentTimeMillis();
		// 第一步检测编码，非GBK就是UTF-8，其他不予考虑
		String txtEnCoding = ToolJava.detectTxtEncoding(inFile.getPath()) ; // 猜测中文文本编码 返回: "GBK" 或 "UTF-8"
		String txt = ToolJava.readText(inFile.getPath(), txtEnCoding).replace("\r", "").replace("　", ""); // 为起点txt预处理

		if ( ! txt.contains("更新时间") ) // 非起点文本
			return importNormalTxt(inFile, txtEnCoding);

		String sQidianid = inFile.getName().replace(".txt", ""); // 文件名
		String sQidianURL = new SiteQiDian().getTOCURL_Android7(sQidianid); // URL
		String sBookName = sQidianid;

		List<Novel> lst = new ArrayList<Novel>();
		Novel book = new Novel();
		Map<String, Object> info = new HashMap<String, Object>();
		List<Map<String, Object>> chapters = new ArrayList<Map<String, Object>>();
		Map<String, Object> page;

		// 新版要快很多，而且少了头部的无用章节
		// 不过如果以后起点txt有结构变动的话，适应性可能不如旧版，故保留旧版
		String line[] = txt.split("\n");
		int lineCount = line.length;
		sBookName = line[0] ;
		int titleNum = 0 ; // base:0 包含
		int headNum = 0 ; // base:0  包含
		int lastEndNum = 0 ;
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

					page = new HashMap<String, Object>();
					page.put(NV.PageName, line[titleNum]);
					page.put(NV.PageURL,  "");
					page.put(NV.Content,  sbd.toString());
					page.put(NV.Size,	 sbd.length());
					chapters.add(page);

					lastEndNum = i;
				}
			}
		}
		book.setChapters(chapters);

		info.put(NV.BookName,   sBookName);
		info.put(NV.BookURL,	sQidianURL);
		info.put(NV.DelURL,	 "");
		info.put(NV.BookStatu,  0);
		info.put(NV.QDID,	   sQidianid);
		info.put(NV.BookAuthor, "");
		book.setInfo(info);

		// Log.e("XX", "耗时: " + (System.currentTimeMillis() - sTime));
		lst.add(book);
		return lst ;
	}
	
	private List<Novel> importNormalTxt(File inFile, String txtEnCoding) {
//		String txtEnCoding = ToolBookJava.detectTxtEncoding(txtPath) ; // 猜测中文文本编码 返回: "GBK" 或 "UTF-8"
//		String txt = ToolBookJava.readText(txtPath, txtEnCoding) ;

		String fileName = inFile.getName().replace(".txt", ""); // 文件名

		List<Novel> lst = new ArrayList<Novel>();
		Novel book = new Novel();
		Map<String, Object> info = new HashMap<String, Object>();
		List<Map<String, Object>> chapters = new ArrayList<Map<String, Object>>();
		Map<String, Object> page;

		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), txtEnCoding));

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

					page = new HashMap<String, Object>();
					page.put(NV.PageName, txtEnCoding + "_" + String.valueOf(chunkCount));
					page.put(NV.PageURL,  "");
					page.put(NV.Content,  chunkStr.toString());
					page.put(NV.Size,	 chunkStr.length());
					chapters.add(page);

					chunkStr = new StringBuilder(65536);
				}
				chunkStr.append(line).append("\n");
			}

			if ( chunkStr.length() > 0 ) {
				++ chunkCount;
				page = new HashMap<String, Object>();
				page.put(NV.PageName, txtEnCoding + "_" + String.valueOf(chunkCount));
				page.put(NV.PageURL,  "");
				page.put(NV.Content,  chunkStr.toString());
				page.put(NV.Size,	 chunkStr.length());
				chapters.add(page);
			}
		} catch (Exception e) {
			System.err.println(e.toString());
		} finally {
			try {
				br.close();
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
		book.setChapters(chapters);

		info.put(NV.BookName,   fileName);
		info.put(NV.BookURL,	"");
		info.put(NV.DelURL,	 "");
		info.put(NV.BookStatu,  0);
		info.put(NV.QDID,	   "");
		info.put(NV.BookAuthor, "");
		book.setInfo(info);

		lst.add(book);
		return lst ;
	}

	public void save(List<Novel> inList , File outFile) {
		StringBuilder oo = new StringBuilder();

		Map<String, Object> info;
		for (Novel novel : inList) {
			if ( novel.getChapters().size() == 0 )
				continue ;
			info = novel.getInfo();
			oo.append(info.get(NV.BookName)).append("\n")
			.append(info.get(NV.BookAuthor)).append("\n\n");

			for (Map<String, Object> page : novel.getChapters()) {
				oo.append(page.get(NV.PageName)).append("\n\n")
				.append(page.get(NV.Content)).append("\n\n\n");
			}
		}

		ToolJava.writeText(oo.toString(), outFile.getPath());
	}

}

