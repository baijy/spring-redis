package com.jianyu.modle;

import java.io.Serializable;

public class User implements Serializable {
	private static final long serialVersionUID = 3206173574449880350L;
	private String id;
	private String createTime;
	private String name;

	public final static String PRO_ID = "id";
	public final static String PRO_CREATETIME = "createTime";
	public final static String PRO_NAME = "name";

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
