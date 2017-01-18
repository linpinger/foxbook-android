package com.linpinger.foxbook;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.tool.NanoHTTPD;
import com.linpinger.tool.ToolJava;

public class FoxHTTPD extends NanoHTTPD {
	private NovelManager nm ;
	private File foxRootDir ;  // nanohttpd 的那个变量是私有的，无法继承
	private String nowUserAgent = "kindle" ;
	private final String html_foot = "\n</body>\n</html>\n\n" ;

	private final String LIST_PAGES = "lp" ;
	private final String SHOW_CONTENT = "sc" ;
	private final String SHOW_PREV_CONTENT = "spc" ;
	private final String SHOW_NEXT_CONTENT = "snc" ;
	private final String DOWN_TXT = "dt" ;

	public FoxHTTPD(int port, File wwwroot, NovelManager nm) throws IOException {
		super(port, wwwroot);
		this.foxRootDir = wwwroot;
		this.nm = nm;
	}


	// 响应
	@Override
	public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {

		// 首页列出书籍
		if (uri.equalsIgnoreCase("/")) {
			nowUserAgent = header.getProperty("user-agent", "kindle");
			String action = parms.getProperty("a", "blank");
			String bookid = parms.getProperty("bid", "-1");
			String pageid = parms.getProperty("pid", "-1");
			String html = "";

			if ( action.equalsIgnoreCase("blank") )
				html = showBookList() ;  //书架
			if ( action.equalsIgnoreCase(LIST_PAGES)) { // 章节列表
				html = showPageList(bookid) ; // 章节列表
			}
			if ( action.equalsIgnoreCase(SHOW_CONTENT)) { // 内容
				html = showContent(bookid, pageid) ;
			}
			if ( action.equalsIgnoreCase(SHOW_PREV_CONTENT)) { // 内容
				Map<String, Object> page = nm.getPrevPage(Integer.valueOf(bookid), Integer.valueOf(pageid));
				if ( page == null ) {
					html = "<html><head><title>txt</title><meta http-equiv=\"refresh\" content=\"0; URL=?\"></head><body><a href=\"?\">NoPrev</a></body></html>";
				} else {
					String newURL = "?a=" + SHOW_CONTENT + "&bid=" + page.get(NV.BookIDX).toString() + "&pid=" + page.get(NV.PageIDX).toString();
					html = "<html><head><title>txt</title><meta http-equiv=\"refresh\" content=\"0; URL=" + newURL +"\"></head><body><a href=\"" + newURL + "\">ShowContent</a></body></html>";
				}
			}
			if ( action.equalsIgnoreCase(SHOW_NEXT_CONTENT)) { // 内容
				Map<String, Object> page = nm.getNextPage(Integer.valueOf(bookid), Integer.valueOf(pageid));
				if ( page == null ) {
					html = "<html><head><title>txt</title><meta http-equiv=\"refresh\" content=\"0; URL=?\"></head><body><a href=\"?\">NoNext</a></body></html>";
				} else {
					String newURL = "?a=" + SHOW_CONTENT + "&bid=" + page.get(NV.BookIDX).toString() + "&pid=" + page.get(NV.PageIDX).toString();
					html = "<html><head><title>txt</title><meta http-equiv=\"refresh\" content=\"0; URL=" + newURL +"\"></head><body><a href=\"" + newURL + "\">ShowContent</a></body></html>";
				}
			}
			if ( action.equalsIgnoreCase(DOWN_TXT)) { // 下载txt
				String txtPath = "/fox.txt" ;
				nm.exportAsTxt(new File(this.foxRootDir, txtPath)); // 所有txt
				html = "<html><head><title>txt</title><meta http-equiv=\"refresh\" content=\"0; URL=" + txtPath +"\"></head><body><a href=\"" + txtPath + "\">Download txt</a></body></html>";
			}

			return new Response( HTTP_OK, MIME_HTML, html ) ;
		}

		if (uri.equalsIgnoreCase("/L")) {  // 列出/sdcard/
			return serveFile( "/", header, foxRootDir, true );
		}

		if (uri.equalsIgnoreCase("/f")) {
			if ( method.equalsIgnoreCase("get") ) { // 上传页面
				String title = "上传萌萌哒的文件";
				StringBuilder html = new StringBuilder();
				html.append("<!DOCTYPE html>\n<html>\n<head>\n\t<META http-equiv=Content-Type content=\"text/html; charset=utf-8\">\n\t<meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; minimum-scale=0.1; maximum-scale=3.0; \"/>\n\t<meta http-equiv=\"X-UA-Compatible\" content=\"IE=Edge\">\n\t<title>");
				html.append(title).append("</title>\n</head>\n\n<body bgcolor=\"#eefaee\">\n\n");
				html.append("<h3>上传文件到 /sdcard/ ，支持中文文件名</h3>\n\n<form action=\"/f\" enctype=\"multipart/form-data\" method=\"post\">\n\t<input name=\"filename\" type=\"file\" />\n\t<input type=\"submit\" value=\"上传\" />\n</form>\n");
				html.append("\n</body>\n</html>\n");
				return new Response( HTTP_OK, MIME_HTML, html.toString() ) ;
			} else { // 处理文件上传
				// 汗，修改了nanohttpd.java 里面的临时路径到/sdcard/，到处都是硬编码，呵呵哒
				String tmpFilePath = files.getProperty("filename") ; // /sdcard/NanoHTTPD-nnn.upload /data/data/com.linpinger.foxudp/cache/NanoHTTPD-561991304.upload
				String fileName = parms.getProperty("filename", "NoName.upload") ;    // testUpload.exe
				File savePath = new File(this.foxRootDir, fileName);
				ToolJava.renameIfExist(savePath);
				(new File(tmpFilePath)).renameTo(savePath);
				return new Response( HTTP_OK, MIME_HTML, "<html>\n<head>\n\t<META http-equiv=Content-Type content=\"text/html; charset=utf-8\">\n\t<title>Return Msg</title>\n</head>\n\n<body bgcolor=\"#eefaee\">\n\n" + tmpFilePath + " -> " + savePath.getAbsolutePath() + "\n\n</body>\n</html>\n") ;
			}
		}

//		return new Response( HTTP_OK, MIME_PLAINTEXT, "hello" ) ;
		return serveFile( uri, header, foxRootDir, true );
	}

	// 内容
	private String showContent(String iBookIDX, String iPageIDX) {
		String fontSize = "22px" ;
		if ( nowUserAgent.contains("Android") )
			fontSize = "18px" ;

		Map<String, Object> page = nm.getPage(Integer.valueOf(iBookIDX), Integer.valueOf(iPageIDX));
		if ( page.isEmpty() )
			return "<html><head><title>txt</title><meta http-equiv=\"refresh\" content=\"0; URL=?\"></head><body>no this ID</body></html>";

		String title = page.get(NV.PageName).toString() ;
		String content = page.get(NV.Content).toString() ;
		StringBuilder html = new StringBuilder();
		html.append(html_head("utf-8", title, true));
		html.append("<h2>").append(title).append("</h2>\n")
		.append("<div class=\"content\" style=\"font-size:").append(fontSize).append("; line-height:150%;font-family: Microsoft YaHei;\">\n")
		.append("<p>").append(content.replace("\n", "</p>\n<p>"))
		.append("</p>\n</div>\n")
		.append("<p>　　").append(title).append("</p>\n")
		.append("<div class=\"book_switch\">\n<ul>")
		.append("\t<li><a href=\"?a=").append(SHOW_PREV_CONTENT).append("&bid=").append(iBookIDX).append("&pid=").append(iPageIDX).append("\">上一章</a></li>\n")
		.append("\t<li><a href=\"?a=").append(LIST_PAGES).append("&bid=").append(iBookIDX).append("\">返回目录</a></li>\n")
		.append("\t<li><a href=\"?\">返回书架</a></li>\n")
		.append("\t<li><a href=\"?a=").append(SHOW_NEXT_CONTENT).append("&bid=").append(iBookIDX).append("&pid=").append(iPageIDX).append("\">下一章</a></li>\n")
		.append("</ul>\n</div>\n\n").append(html_foot);
		return html.toString() ;
	}

	// 章节列表
	private String showPageList(String iBookIDX) {
		StringBuilder html = new StringBuilder();
		html.append(html_head("萌萌哒的章节列表"));

		List<Map<String, Object>> data ;
		if ( Integer.valueOf(iBookIDX) >= 0 )
			data = nm.getBookPageList(Integer.valueOf(iBookIDX));
		else
			data = nm.getPageList(1);

		html.append("<ol>\n");
		for(Map<String, Object> page : data ) {
			html.append("\t<li><a href=\"?a=").append(SHOW_CONTENT)
			.append("&bid=").append(page.get(NV.BookIDX))
			.append("&pid=").append(page.get(NV.PageIDX))
			.append("\" title=\"").append(page.get(NV.PageURL)).append("\">")
			.append(page.get(NV.PageName)).append("</a> (")
			.append(page.get(NV.Size)).append(")</li>\n");
		}
		html.append("\n</ol>\n").append(html_foot);
		return html.toString() ;
	}
	// 书架
	private String showBookList() {
		StringBuilder html = new StringBuilder();
		html.append(html_head("萌萌哒的书籍列表"));
		
		html.append("<br>　　<a href=\"?a=").append(LIST_PAGES)
			.append("&bid=-1\">显示所有章节</a>  <a href=\"?a=")
			.append(DOWN_TXT).append("&bid=-1\">下载txt</a><br>\n<ol>\n");
		List<Map<String, Object>> data = nm.getBookList(); // 获取书籍列表
		for(Map<String, Object> mm : data ) {
			html.append("<li><a href=\"?a=").append(LIST_PAGES).append("&bid=")
				.append(mm.get(NV.BookIDX)).append("\" title=\"").append(mm.get(NV.BookURL)).append("\">")
				.append(mm.get(NV.BookName)).append("</a> (").append(mm.get(NV.PagesCount)).append(")")
				// .append(" <a href=\"?a=").append(DOWN_TXT).append("&bid=").append(mm.get(NV.BookIDX)).append("\" class=\"yy\">T</a>")
				.append("</li>\n");
		}
		html.append("\n</ol>\n").append(html_foot);
		return html.toString() ;
	}

	private String html_head(String title) {
		return html_head("utf-8", title, false);
	}
	private String html_head(String charset, String title, boolean addJS) {
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\n")
			.append("<html>\n")
			.append("<head>\n")
			.append("\t<META http-equiv=Content-Type content=\"text/html; charset=").append(charset).append("\">\n")
			.append("\t<meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; minimum-scale=0.1; maximum-scale=3.0; \"/>\n")
			.append("\t<meta http-equiv=\"X-UA-Compatible\" content=\"IE=Edge\">\n")
			.append("\t<title>").append(title).append("</title>\n")
			.append("\t<style>\n\t\tli {line-height:200%;}\n\t\tli a:link { text-decoration: none; }\n\t\t.content { padding: 1em; }\n\t\t.content p { text-indent: 2em; }\n\t\tol {list-style-type: decimal-leading-zero;}\nol li {\n\tborder-bottom-width: 1px;\n\tborder-bottom-style: dashed;\n\tborder-bottom-color: #CCCCCC;\n}\n.yy {\n\tpadding:3px;\n\tbackground-color:#99cc66;\n\tcolor: #fff;\n\ttext-align: center;\n\tborder-radius: 4px;\n}\nbody, div, ul {\n\tmargin: 0;\n\tpadding: 0;\n\tlist-style: none;\n}\n.book_switch a, .book_switch a:active {\n\tcolor: #09b396;\n\tborder: 1px solid #09b396;\n\tborder-radius: 5px;\n\tdisplay: block;\n\tfont-size: .875rem;\n\tmargin-right: 10px;\n}\n.book_switch {\n\theight: 35px;\n\tline-height: 35px;\n\tpadding: 8px 10px 10px 10px;\n\ttext-align: center;\n}\n.book_switch ul li {\n\tfloat: left;\n\twidth: 25%;\n}\n\t</style>\n");
		if ( addJS ) {
			html.append("<script language=javascript>\n")
			.append("function BS(colorString) {document.bgColor=colorString;}\n")
			.append("function mouseClick(ev){\n")
			.append("\tev = ev || window.event;\n")
			.append("\ty = ev.clientY;\n")
			.append("\th = window.innerHeight || document.documentElement.clientHeight;\n")
			.append("\tif ( y > ( 0.3 * h ) ) {\n")
			.append("\t\twindow.scrollBy(0, h - 20);\n")
			.append("\t} else {\n")
			.append("\t\twindow.scrollBy(0, -h + 20);\n")
			.append("\t}\n")
			.append("}\n")
			.append("document.onmousedown = mouseClick;\n")
			.append("document.onkeydown=function(event){\n")
			.append("\tvar e = event || window.event || arguments.callee.caller.arguments[0];\n")
			.append("\tif(e && (e.keyCode==32 || e.keyCode==81 )){\n")
			.append("\t\th = window.innerHeight || document.documentElement.clientHeight;\n")
			.append("\t\twindow.scrollBy(0, h - 20);\n")
			.append("\t}\n")
			.append("};\n")
			.append("</script>\n");
		}
		html.append("</head>\n")
			.append("<body bgcolor=\"#eefaee\">\n\n");
		return html.toString();
	}
	
}
