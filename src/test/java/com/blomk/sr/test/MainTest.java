package com.blomk.sr.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.blomk.sr.Main;

public class MainTest {

	@Test
	public void checkFilename() {
		
		String rs = Main.generateFilename("test");
		assertNotNull(rs);
		
	}
}
