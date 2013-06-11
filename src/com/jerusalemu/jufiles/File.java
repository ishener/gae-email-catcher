package com.jerusalemu.jufiles;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Entity
public class File {
	@Id Long id;
	@Unindex String body;
	
	public File() {}
	public File (String b) {
		this.body = b;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
}
