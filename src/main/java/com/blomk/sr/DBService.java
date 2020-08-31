package com.blomk.sr;

//TODO DB to store urls.
public class DBService {

	public void createTable() {
		String sql =  "CREATE TABLE images " + 
	            "(id INTEGER not NULL, " + 
	            " filename VARCHAR(255), " +
	            " PRIMARY KEY ( id ))";
	}
}
