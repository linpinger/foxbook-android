package com.linpinger.tool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class FoxHTTP {
    private String nowURL = "";
    private HashMap<String, String> heads = new HashMap<String, String>(); // HTTP 头部字段
    private Map<String, List<String>> resHeads ; // 响应头

    private boolean isPOST = false; // 默认为GET
    private byte[] PostData = null;
    private boolean isPOSTFILE = false; // 发送文件
    private String postFilePath = "";
    final String BOUNDARY = "---------------------------123821742118716"; //boundary就是request头和上传文件内容的分隔符

    private boolean isSaveToFile = false; // 默认保存到字节数组中
    private String  savePath = "FoxHTTP.save" ; // 默认保存路径

    public FoxHTTP(String iURL) {
        nowURL = iURL;

        setDefHead();
    }

    public FoxHTTP setHead(String iKey, String iValue) {
        heads.put(iKey, iValue);
        return this;
    }

    public Map<String, List<String>> getRespondHeads() {
    	return resHeads;
    }

    public String getHTML() {
        return getHTML("", "");
    }

    public String getHTML(String htmlCharSet) {
        return getHTML(htmlCharSet, "");
    }

    public String getHTML(String htmlCharSet, String iPostData) {
        if ( iPostData != "" ) {
            isPOST = true;
            try {
                PostData = iPostData.getBytes("UTF-8");
            } catch (Exception e) { // 错误 是神马
                System.err.println(e.toString());
                return "";
            }
        }

        return htmlBuf2Str( goGO(), htmlCharSet);
    }

    public static boolean testHtmlOK(String html) {
        boolean isOK = false;
        if ( html.length() > 3 ) {
            String lowCase = html.toLowerCase();
            if ( lowCase.contains("</html>") ) { // html 下载完毕
                isOK = true;
            } else {
                if ( ! lowCase.contains("doctype") ) { // json
                    isOK = true;
                }
            }
        }
        return isOK;
    }

    private String htmlBuf2Str(byte[] buf, String htmlCharSet) { // 根据下载的buf，获取正确编码的Str
        try {
            if (buf == null) { return ""; }

            String html = "";
            if (htmlCharSet == "") {
                html = new String(buf, "gbk");
                if ( testHtmlOK(html) ) {
                    if (html.matches("(?smi).*<meta[^>]*charset=[\"]?(utf8|utf-8)[\"]?.*")) // 探测编码
                        html = new String(buf, "utf-8");
                } else { // 网页下载不完整，直接返回空字串
                    return "";
                }
            } else {
                html = new String(buf, htmlCharSet);
            }
            return html;
        } catch (Exception e) { // 错误 是神马
            System.err.println(e.toString());
            return "";
        }
    }

    public long saveFile(String outPath) {
        isSaveToFile = true;
        savePath = outPath;

        File toFile = new File(savePath);
        if (toFile.exists())
            toFile.delete();

        byte[] ret = goGO();
        if ( null == ret ) {
            System.err.println("下载数组为null, URL: " + nowURL);
            return 0;
        } else {
            return byteArray2Long(ret);
        }
    }

    public String postOneFileToFoxServer(String iPostFilePath) {
        isPOST = true;
        isPOSTFILE = true;
        postFilePath = iPostFilePath;

        return htmlBuf2Str( goGO(), "");
    }

    private byte[] goGO() {
        HttpURLConnection conn = null;
        byte[] buf = null;
        try {
            URL url = new URL(nowURL);
            conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);	// 读取超时5s
            conn.setUseCaches(false);	 // Cache-Control: no-cache	 Pragma: no-cache

            if ( isPOST ) {
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                if ( isPOSTFILE ) {
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
                } else {
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); // 默认POST头部，也许有更合理的
                }
            }

            for ( Map.Entry<String, String> entry : heads.entrySet()) { // HTTP 头部字段
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }

            conn.connect();  // 开始连接

            if ( isPOST ) {  // 发送PostData
                OutputStream out = conn.getOutputStream();
                if ( isPOSTFILE ) { // 发送文件
                    String inputName = "f";
                    File file = new File(postFilePath);
                    String filename = file.getName();

                    StringBuffer strBuf = new StringBuffer();
                    strBuf.append("--").append(BOUNDARY).append("\r\n");  // 多一个\n会使nanohttpd崩溃的亲
                    strBuf.append("Content-Disposition: form-data; name=\"" + inputName + "\"; filename=\"" + filename + "\"\r\n");
                    strBuf.append("Content-Type: application/octet-stream\r\n\r\n");
                    out.write(strBuf.toString().getBytes("UTF-8")); // 转码么?

                    InputStream inTP = new FileInputStream(file);
                    int bytes = 0;
                    byte[] bufferOut = new byte[8192];
                    while ((bytes = inTP.read(bufferOut)) != -1)
                        out.write(bufferOut, 0, bytes);
                    inTP.close();
                    byte[] endData = ("\r\n--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"e\"\r\n\r\nUTF-8\r\n--" + BOUNDARY + "--\r\n").getBytes("UTF-8");
                    out.write(endData);
                } else {
                    out.write(PostData);
                }
                out.flush();
                out.close();
            }

            if ( 403 == conn.getResponseCode() ) { // 处理403错误
            	return conn.getURL().toString().getBytes();
            }

            resHeads = conn.getHeaderFields(); // 获取响应头

            // 判断返回的是否是gzip数据
            OutputStream outStream;
            if ( isSaveToFile ) {
                outStream = new FileOutputStream(savePath);
            } else {
                outStream = new ByteArrayOutputStream();
            }

            byte[] buffer = new byte[8192];
            int len = 0;
            long allLen = 0;
            // 返回的字段: Content-Encoding: gzip/null 判断是否是gzip
            if (null == conn.getContentEncoding()) { // 不是gzip数据
                InputStream in = conn.getInputStream();
                while ((len = in.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                    allLen += len;
                }
                in.close();
            } else { // gzip 压缩处理
                InputStream in = conn.getInputStream();
                GZIPInputStream gzin = new GZIPInputStream(in);
                while ((len = gzin.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                    allLen += len;
                }
                gzin.close();
                in.close();
            }

            if ( ! isSaveToFile ) {
                buf = ((ByteArrayOutputStream)outStream).toByteArray();
            } else {
                buf = long2ByteArray(allLen);
            }
            outStream.close();
        } catch ( Exception e ) {
            System.err.println( e.toString() );
        } finally {
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
        }
        return buf;
    }

    private void setDefHead() { // 默认Head
        heads.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:53.0) Gecko/20100101 Firefox/53.0");
        heads.put("Accept", "*/*");
        heads.put("Accept-Encoding", "*");
    }

    public static String getFullURL(String baseURL, String subURL) { // 获取完整路径
        String allURL = "";
        try {
            allURL = (new URL(new URL(baseURL), subURL)).toString();
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return allURL;
    }

    public byte[] long2ByteArray(long res) {
        byte[] buffer = new byte[8];
        for (int i = 0; i < 8; i++) {
            int offset = 64 - (i + 1) * 8;
            buffer[i] = (byte) ((res >> offset) & 0xff);
        }
        return buffer;
    }

    public long byteArray2Long(byte[] b){
        long values = 0;
        for (int i = 0; i < 8; i++) {
            values <<= 8; values|= (b[i] & 0xff);
        }
        return values;
    }

} // end of class
