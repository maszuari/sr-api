package com.blomk.sr;

public class DBService {

	public void createTable() {
		String sql =  "CREATE TABLE images " + 
	            "(id INTEGER not NULL, " + 
	            " filename VARCHAR(255), " +
	            " PRIMARY KEY ( id ))";
	}
}
