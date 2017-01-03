package com.linpinger.novel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.linpinger.tool.ToolJava;

public class Stor {

	public String getValue(String text, String label) {
		String ret = "";
		Matcher mat = Pattern.compile("(?smi)<" + label + ">(.*?)</" + label + ">").matcher(text);
		while (mat.find()) {
			ret = mat.group(1);
			break;
		}
		return ret;
	}

	public List<Novel> load(File inFile) {
		String strBooks = ToolJava.readText(inFile.getPath(), "utf-8");
		List<Novel> lst = new ArrayList<Novel>();
		Novel book ;
		Map<String, Object> info;
		List<Map<String, Object>> chapters;
		Map<String, Object> page;

		String strBook;
		Matcher mat = Pattern.compile("(?smi)<novel>(.*?)</novel>").matcher(strBooks);
		while (mat.find()) {
			strBook = mat.group(1);
			book = new Novel();
			info = new HashMap<String, Object>();
			info.put(NV.BookName,   getValue(strBook, NV.BookName));
			info.put(NV.BookURL,    getValue(strBook, NV.BookURL));
			info.put(NV.DelURL,     getValue(strBook, NV.DelURL));
			info.put(NV.BookStatu,  Integer.valueOf(getValue(strBook, NV.BookStatu)));
			info.put(NV.QDID,       getValue(strBook, NV.QDID));
			info.put(NV.BookAuthor, getValue(strBook, NV.BookAuthor));
			book.setInfo(info);

			chapters = new ArrayList<Map<String, Object>>();
			String strPage ;
			Matcher matP = Pattern.compile("(?smi)<page>(.*?)</page>").matcher(strBook);
			while (matP.find()) {
				strPage = matP.group(1);

				page = new HashMap<String, Object>();
				page.put(NV.PageName, getValue(strPage, NV.PageName));
				page.put(NV.PageURL,  getValue(strPage, NV.PageURL));
				page.put(NV.Content,  getValue(strPage, NV.Content));
				page.put(NV.Size,     Integer.valueOf(getValue(strPage, NV.Size)));

				chapters.add(page);
			}
			book.setChapters(chapters);

			lst.add(book);
		}

		return lst ;
	}

	public void save(List<Novel> inList , File outFile) {
		StringBuilder oo = new StringBuilder();

		oo.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n\n<shelf>\n\n");
		Map<String, Object> info;
		for (Novel novel : inList) {
			info = novel.getInfo();
			oo.append("<novel>\n")
			.append("\t<").append(NV.BookName).append(">").append(info.get(NV.BookName)).append("</").append(NV.BookName).append(">\n")
			.append("\t<").append(NV.BookURL).append(">").append(info.get(NV.BookURL)).append("</").append(NV.BookURL).append(">\n")
			.append("\t<").append(NV.DelURL).append(">").append(info.get(NV.DelURL)).append("</").append(NV.DelURL).append(">\n")
			.append("\t<").append(NV.BookStatu).append(">").append(info.get(NV.BookStatu)).append("</").append(NV.BookStatu).append(">\n")
			.append("\t<").append(NV.QDID).append(">").append(info.get(NV.QDID)).append("</").append(NV.QDID).append(">\n")
			.append("\t<").append(NV.BookAuthor).append(">").append(info.get(NV.BookAuthor)).append("</").append(NV.BookAuthor).append(">\n")
			.append("<chapters>\n") ;

			for (Map<String, Object> page : novel.getChapters()) {
				oo.append("<page>\n")
				.append("\t<").append(NV.PageName).append(">").append(page.get(NV.PageName)).append("</").append(NV.PageName).append(">\n")
				.append("\t<").append(NV.PageURL).append(">").append(page.get(NV.PageURL)).append("</").append(NV.PageURL).append(">\n")
				.append("\t<").append(NV.Content).append(">").append(page.get(NV.Content)).append("</").append(NV.Content).append(">\n")
				.append("\t<").append(NV.Size).append(">").append(page.get(NV.Size)).append("</").append(NV.Size).append(">\n")
				.append("</page>\n") ;
			}
			oo.append("</chapters>\n</novel>\n\n");
		}
		oo.append("</shelf>\n");
		ToolJava.writeText(oo.toString(), outFile.getPath());
	}

}
