package com.blomk.sr.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.blomk.sr.ImageFile;
import com.blomk.sr.Main;

public class MainTest {
	
	private static File tmpImgFolder;
	private static String zipFilename = "sample_images.zip";
	
	@BeforeAll
	public static void setUp() {
		System.out.println("SetUp start");
		String [] arg = {};
		Main.main(arg);
	}
	
	@Test
	public void checkFilename() {
		
		String rs = Main.generateFilename("test");
		assertNotNull(rs);
	}
	
	@Test
	public void checkIfImageSizeIsValid() {
		
		String sampleFile = "sample.jpg";
		ImageFile imageFile = Main.checkImageFileDimension(sampleFile);
		assertNotNull(imageFile);
		assertTrue(imageFile.isValidImage());
		
	}
	
	@Test
	public void testReadContentInZipFile() {
		
		zipFilename = "sample_images.zip";
		tmpImgFolder = Main.readContentInZipFile(zipFilename);
		assertNotNull(tmpImgFolder);
	}
	
	
	@AfterAll
	public static void done() throws IOException {
		Main.deleteFileAndFolder(zipFilename, tmpImgFolder);
	}
}
