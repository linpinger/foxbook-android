package com.linpinger.foxbook;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FoxEpubReader {

    private File ePubFile;
    private FoxZipReader zipReader;

    public FoxEpubReader(File epub) {
        ePubFile = epub;
        zipReader = new FoxZipReader(ePubFile);

    }

    public void close() {
        zipReader.close();
    }

//    public void printFileList() {
//        for (HashMap<String, Object> hm : zipReader.getList()) {
//            System.out.println(hm.get("name") + "(" + hm.get("count") + ")");
//        }
//    }

    public ArrayList<Map<String, Object>> getFileList() {
        return zipReader.getList();
    }

    public String getFileContent(String itemName) {
        return getFileContent(itemName, "UTF-8");
    }

    public String getFileContent(String itemName, String Encoding) {
        return zipReader.getHtmlFile(itemName, Encoding);
    }

    public String getOPFName() {
        String xml = getFileContent("META-INF/container.xml");
        Matcher mat = Pattern.compile("(?smi)<rootfile .*?full-path=\"([^\" ]*?)\"").matcher(xml);
        while (mat.find()) {
            return mat.group(1);
        }
        return "";
    }

    public ArrayList<HashMap<String, Object>> getQiDianEpubTOC() {
        ArrayList<HashMap<String, Object>> xx = new ArrayList<HashMap<String, Object>>(80);
        String xml = getFileContent("toc.ncx");
// <navPoint id="content1003656831_312845178" playOrder="5"><navLabel><text>第一章 与天斗，其乐无穷！</text></navLabel><content src="content1003656831_312845178.html"/></navPoint>
        Matcher mat = Pattern.compile("(?smi)<navPoint.*?<text>(.*?)</text>.*?src=\"([^\"]*?)\"").matcher(xml);
        HashMap<String, Object> hm ;
        while (mat.find()) {
            if ( mat.group(2).contains("content")) {
                hm = new HashMap<String, Object>();
                hm.put("title", mat.group(1));
                hm.put("name", mat.group(2));
                xx.add(hm);
            }
         }
        return xx ;
    }
    
    public String getQiDianEpubPage(String itemName){
        String txt = "";
        String html = getFileContent(itemName);
        Matcher mat = Pattern.compile("(?smi)<div class=\"content\">(.*?)</div>").matcher(html);
        while (mat.find())
            txt = mat.group(1);
        txt = txt.replace("<p>手机用户请到m.qidian.com阅读。</p>", "")
                .replace("<p>手机阅读器、看书更方便。【<a href=\"http://download.qidian.com/apk/QDReader.apk?k=e\" target=\"_blank\">安卓版</a>】</p>", "");
        txt = txt.replace("\r", "")
                .replace("\n", "")
                .replace("<p>", "")
                .replace("</p>", "\n");
        return txt;
    }
    
    public HashMap<String, Object> getQiDianEpubInfo() {
        HashMap<String, Object> hm = new HashMap<String, Object>() ;
        String html = getFileContent("title.xhtml");
        Matcher mat = Pattern.compile("(?smi)<li><b>书名</b>：<a href=\"http://([0-9]*).qidian.com[^>]*?>([^<]*?)</a>.*<li><b>作者</b>：<a[^>]*?>([^<]*?)</a>.*<li><b>主题</b>：([^<]*?)<.*<li><b>简介</b>：<pre>(.*)</pre>").matcher(html);
        while (mat.find()) {
        	hm.put("qidianid", mat.group(1));
            hm.put("bookname", mat.group(2));
            hm.put("author", mat.group(3));
            hm.put("type", mat.group(4));
            hm.put("info", mat.group(5));
        }
        return hm;
    }

    public static void main(String[] args) {
        long sTime = System.currentTimeMillis();
        FoxEpubReader epub = new FoxEpubReader(new File("S:\\1003656831.epub"));
        
//        for (HashMap<String, Object> hm : epub.getQiDianEpubTOC())
//            System.out.println(hm.get("name") + ":" + hm.get("title") );
//        System.out.println(epub.getQiDianEpubPage("content1003656831_317820302.html"));
        HashMap<String, Object> hm = epub.getQiDianEpubInfo();
        System.out.println("BookName:" + hm.get("bookname"));
        System.out.println("author:" + hm.get("author"));
        System.out.println("Tyep:" + hm.get("type"));
        System.out.println("info:" + hm.get("info"));
        
        epub.close();
        System.out.println("Time: " + (System.currentTimeMillis() - sTime));
    }
}
