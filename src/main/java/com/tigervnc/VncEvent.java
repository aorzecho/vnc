package com.tigervnc;

public enum VncEvent {
	INIT("init"),
	CONNECTION_ERROR("connection_error"),
	DESTROY("destroy"),
	UPD_SETUP("upd_setup");

	private String name;

	VncEvent(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}
}
