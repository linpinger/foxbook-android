package com.linpinger.novel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.linpinger.tool.FoxEpubReader;
import com.linpinger.tool.FoxEpubWriter;

public class StorEpub extends Stor {

	public List<Novel> load(File inFile) {
		FoxEpubReader epub = new FoxEpubReader(inFile);

		int epubType = 0 ;  // 0: unknown  1: FoxMake  2: Qidian
		// 判断epub类型
		if ( epub.getTextFile("catalog.html").length() == 0 ) { // 非起点 epub
			if ( epub.getTextFile("FoxMake.htm").length() == 0 ) { // 非 FoxMake epub
				System.err.println("Todo: 暂不支持解析该Epub类型");
				epub.close();
				return null;
			} else {
				epubType = 1 ; //FoxMake epub
			}
		} else {
			epubType = 2 ; // 起点epub
		}

		List<Novel> lst = new ArrayList<Novel>();
		Novel book = new Novel();
		Map<String, Object> info = new HashMap<String, Object>();
		List<Map<String, Object>> chapters = new ArrayList<Map<String, Object>>();
		Map<String, Object> page;

		switch (epubType) {
		case 1:
			info.put(NV.BookName,   "FoxEpub");
			info.put(NV.BookURL,    inFile.getName());
			info.put(NV.QDID,       "");
			info.put(NV.BookAuthor, "");
			break;
		case 2:
			HashMap<String, Object> qhm = epub.getQiDianEpubInfo();
			info.put(NV.BookName,   qhm.get("bookname"));
			info.put(NV.BookURL,    new SiteQiDian().getIndexURL_Mobile(qhm.get("qidianid").toString()));
			info.put(NV.QDID,       qhm.get("qidianid"));
			info.put(NV.BookAuthor, qhm.get("author"));
			break;
		default:
			break;
		}
		info.put(NV.DelURL,     "");
		info.put(NV.BookStatu,  0);
		book.setInfo(info);

		String fileName ;
		String pageTitle ;
		String pageText ;

		switch (epubType) {
		case 1:
			String html = epub.getTextFile("FoxMake.htm");
			Matcher mat = Pattern.compile("(?smi)<a href=\"([^\"]*)\">([^<]*)</a>").matcher(html); // path, title
			while (mat.find()) {
				fileName = mat.group(1);
				pageTitle = mat.group(2);
				pageText = new NovelSite().getContent(epub.getTextFile(fileName));

				page = new HashMap<String, Object>();
				page.put(NV.PageName, pageTitle);
				page.put(NV.PageURL,  fileName);
				page.put(NV.Content,  pageText);
				page.put(NV.Size,     pageText.length());

				chapters.add(page);
			}
			break;
		case 2:
			for ( HashMap<String, Object> hm : epub.getQiDianEpubTOC() ) {
				fileName = hm.get("name").toString();
				pageTitle = hm.get("title").toString();
				pageText = epub.getQiDianEpubPage(fileName);

				page = new HashMap<String, Object>();
				page.put(NV.PageName, pageTitle);
				page.put(NV.PageURL,  new SiteQiDian().getContentURLFromIDs(hm.get("pageid").toString(), hm.get("bookid").toString()));
				page.put(NV.Content,  pageText);
				page.put(NV.Size,     pageText.length());

				chapters.add(page);
			}
			break;
		default:
			break;
		}
    	book.setChapters(chapters);

    	lst.add(book);
    	epub.close();
		return lst ;
	}

	public void save(List<Novel> inList , File outFile) {
		FoxEpubWriter oEpub = new FoxEpubWriter(outFile);

		for (Novel novel : inList)
			for (Map<String, Object> page : novel.getChapters())
				oEpub.addChapter(page.get(NV.PageName).toString(), page.get(NV.Content).toString(), -1);

		oEpub.saveAll();
	}

}
