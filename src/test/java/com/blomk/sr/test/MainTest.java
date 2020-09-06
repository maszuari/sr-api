package com.blomk.sr.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.blomk.sr.ImageFile;
import com.blomk.sr.Main;

public class MainTest {

	@Test
	public void checkFilename() {
		
		String rs = Main.generateFilename("test");
		assertNotNull(rs);
		
	}
	
	@Test
	public void checkIfImageSizeIsValid() {
		
		String [] arg = {};
		Main.main(arg);
		String sampleFile = "sample.jpg";
		ImageFile imageFile = Main.checkImageFileDimension(sampleFile);
		assertNotNull(imageFile);
		assertTrue(imageFile.isValidImage());
		
	}
}
