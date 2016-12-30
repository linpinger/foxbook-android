package com.linpinger.novel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Novel implements Serializable {
	private static final long serialVersionUID = 8721548324846L;

	private Map<String, Object> info ;				// 本书信息
	private List<Map<String, Object>> chapters ;	// 章节列表

	public Novel() {
		info = new HashMap<String, Object>();
		chapters = new ArrayList<Map<String, Object>>();
	}

	public Map<String, Object> getInfo() {
		return info;
	}

	public void setInfo(Map<String, Object> info) {
		this.info = info;
	}

	public List<Map<String, Object>> getChapters() {
		return chapters;
	}

	public void setChapters(List<Map<String, Object>> chapters) {
		this.chapters = chapters;
	}

}
