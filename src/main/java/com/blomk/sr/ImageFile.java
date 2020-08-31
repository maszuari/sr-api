package com.blomk.sr;

public class ImageFile {

	private int width;
	private int height;
	private boolean isValidImage;
	
	public ImageFile(int width, int height, boolean isValidImage) {
		super();
		this.width = width;
		this.height = height;
		this.isValidImage = isValidImage;
	}
	
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public boolean isValidImage() {
		return isValidImage;
	}
	public void setValidImage(boolean isValidImage) {
		this.isValidImage = isValidImage;
	}
}
