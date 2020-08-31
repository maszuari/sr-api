package com.blomk.sr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SrProperties {

	private Logger lgr = LoggerFactory.getLogger(SrProperties.class);
	private String imagesFolder;
	private String tmpFolder;

	public SrProperties() {

		try (InputStream input = SrProperties.class.getClassLoader().getResourceAsStream("config.properties")) {

			Properties prop = new Properties();

			if (input == null) {
				lgr.error("Unable to find config.properties");
			}

			prop.load(input);
			imagesFolder = prop.getProperty("images");
			tmpFolder = prop.getProperty("tmp");
			//lgr.debug("STORAGE "+imagesFolder);

		} catch (IOException ex) {
			lgr.error(ex.getMessage());
		}
	}
	
	public String getImagesFolder() {
		return imagesFolder;
	}

	public String getTmpFolder() {
		return tmpFolder;
	}
	
}
