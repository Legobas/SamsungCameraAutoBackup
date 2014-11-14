package com.legobas.autobackup;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Didl {
	String title;
	String date;
	long size;

	public Didl(String title, String date, long size) {
		super();
		this.title = title;
		this.date = date;
		this.size = size;
	}

	public String getTitle() {
		return title;
	}

	public String getDate() {
		return date;
	}

	public long getSize() {
		return size;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
