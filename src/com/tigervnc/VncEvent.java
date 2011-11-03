package com.tigervnc;

public enum VncEvent {
	INIT("init"),
	CONNECTION_ERROR("connection_error"),
	DESTROY("destroy");

	private String name;

	VncEvent(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}
}
