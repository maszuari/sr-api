package com.blomk.sr;

import static spark.Spark.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;

import java.util.Set;

import java.util.UUID;
import java.util.zip.ZipEntry;

import java.util.zip.ZipInputStream;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import net.coobird.thumbnailator.Thumbnails;
import spark.Request;
import spark.Response;

public class Main {

	// Set images and tmp folders in config.properties.
	// images folder for storing images. tmp folder for processing the images.
	private static String IMAGES;
	private static String TMP;
	private static Logger lgr = LoggerFactory.getLogger(Main.class);
	private static Set<String> db;
	private static File imagesDir, tmpDir;
	private static final int MAX_WIDTH = 128, MAX_HEIGHT = 128; 

	public static void main(String[] args) {

		IMAGES = new SrProperties().getImagesFolder();
		imagesDir = new File(IMAGES);
		if (!imagesDir.isDirectory()) {
			lgr.error("This is not an image directory");
			return;
		}
		staticFiles.externalLocation(IMAGES);

		TMP = new SrProperties().getTmpFolder();
		tmpDir = new File(TMP);
		if (!tmpDir.isDirectory()) {
			lgr.error("This is not a tmp directory");
			return;
		}

		db = new HashSet<String>();
		post("/attach", (req, res) -> getImageLink(req, res));
		post("/zip", (req, res) -> getZipFile(req, res));
		post("/thumbnails", (req, res) -> getThumbnails(req, res));
	}

	private static String getImageLink(Request req, Response res) {
		
		db.clear();
		req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));

		try {

			Part filePart = req.raw().getPart("file");
			String uploadedFileName = filePart.getSubmittedFileName();
			String cntType = filePart.getContentType();
			
			if (cntType.startsWith("image/")) {
				
				InputStream stream = filePart.getInputStream();
				String newFilename = renameAndMoveImageFile(stream, uploadedFileName);
				String url = generateURL(req, newFilename);
				db.add(url);
				return new Gson()
						.toJson(new StandardResponse(StatusResponse.SUCCESS, "Successfully uploaded file.", db));

			} else {
				return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, "This is not an image file"));
			}

		} catch (IOException | ServletException e) {
			return new Gson().toJson(new StandardResponse(StatusResponse.ERROR,
					"Exception occurred while uploading file" + e.toString()));
		}
	}

	private static String getZipFile(Request req, Response res) {

		db.clear();
		req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));

		try {
			
			Part filePart = req.raw().getPart("file");
			String uploadedFileName = filePart.getSubmittedFileName();
			String cntType = filePart.getContentType();

			if (cntType.equals("application/zip")) {
				InputStream stream = filePart.getInputStream();

				String zipFilename = generateFilename(uploadedFileName);
				Files.copy(stream, Paths.get(TMP).resolve(zipFilename), StandardCopyOption.REPLACE_EXISTING);
				File tmpImgFolder = readContentInZipFile(zipFilename);
				renameAndMoveFiles(req, tmpImgFolder);
				deleteFileAndFolder(zipFilename, tmpImgFolder);
				return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, "Successfully uploaded file", db));
			} else {
				// Not zip file.
				return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, "Not a zip file"));
			}

		} catch (IOException | ServletException e) {
			return new Gson().toJson(new StandardResponse(StatusResponse.ERROR,
					"Exception occurred while uploading file" + e.toString()));
		}
	}
	
	private static String getThumbnails(Request req, Response res) {
		
		db.clear();
		req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));

		try {
			
			Part filePart = req.raw().getPart("file");
			String uploadedFileName = filePart.getSubmittedFileName();
			String cntType = filePart.getContentType();		
			
			if (cntType.startsWith("image/")) {
				
				InputStream stream = filePart.getInputStream();
				String newFilename = renameAndMoveImageFile(stream, uploadedFileName);
				ImageFile imgFile = checkImageFileDimension(newFilename);
				if( imgFile != null && imgFile.isValidImage() ) {
					
					String file32 = createThumbnail(imgFile.getWidth(), imgFile.getHeight(), 32, newFilename);
					String url32 = generateURL(req, file32);
					
					String file64 = createThumbnail(imgFile.getWidth(), imgFile.getHeight(), 64, newFilename);
					String url64 = generateURL(req, file64);
					
					db.add(url32);
					db.add(url64);
					
					return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, "Success.", db));
				}else {
					String url = generateURL(req, newFilename);
					db.add(url);
					return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, "Image file width and height are less than 128px.", db));
				}
			}else {
				return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, "Not an image file"));
			}
			
		} catch (IOException | ServletException e) {	
			return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, "Not a zip file"));
		}
	}
	
	private static String renameAndMoveImageFile(InputStream stream, String uploadedFilename) throws IOException {
		
		String newFilename = generateFilename(uploadedFilename);
		Files.copy(stream, Paths.get(IMAGES).resolve(newFilename), StandardCopyOption.REPLACE_EXISTING);
		return newFilename;
	}
	
	public static ImageFile checkImageFileDimension(String filename) {
		
		try {
			
			String fullPath = IMAGES + File.separator + filename;
			ImageInfo imgInfo = Imaging.getImageInfo(new File(fullPath));
			
			if( imgInfo.getHeight() >= MAX_HEIGHT || imgInfo.getWidth() >= MAX_WIDTH ){	
				return new ImageFile(imgInfo.getWidth(), imgInfo.getHeight(), true);
			}else {
				return new ImageFile(imgInfo.getWidth(), imgInfo.getHeight(), false);
			}
			
		} catch (ImageReadException | IOException e) {
			lgr.error(e.getMessage());
			return null;
		}
	}
	
	private static void renameAndMoveFiles(Request req, File folder) throws IOException {
		
		for (final File fileEntry : folder.listFiles()) {
			
			if (fileEntry.isFile()) {
		          String newFilename = generateFilename(fileEntry.getName());
		          Path srvFile = Paths.get(fileEntry.getAbsoluteFile().toURI());
		          Files.copy(srvFile, Paths.get(IMAGES).resolve(newFilename), StandardCopyOption.REPLACE_EXISTING);
		          String url = generateURL(req, newFilename);
		          db.add(url);
			}
		}
	}

	public static File readContentInZipFile(String filename) {

		try {
			
			String tmpImgFolder = TMP + File.separator +UUID.randomUUID().toString();
			File tmpImgFolderFile = new File(tmpImgFolder);
			if( !tmpImgFolderFile.exists() ) {
				tmpImgFolderFile.mkdir();
			}
			
			String zipFile = TMP + File.separator + filename;
			byte[] buffer = new byte[1024];
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry entry = zis.getNextEntry();			
			
			while (entry!=null) {
								
				File nFile = newFile(tmpImgFolderFile, entry.getName());
				FileOutputStream fos = new FileOutputStream(nFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
				entry = zis.getNextEntry();
			}
			
			zis.closeEntry();
	        zis.close();
	        return tmpImgFolderFile;
	        
		} catch (IOException ex) {
			lgr.error(ex.getMessage());
			return null;
		}

	}
	
	public static void deleteFileAndFolder(String zipFilename, File tmpImgFolder) throws IOException {
		
		String zipFile = TMP + File.separator + zipFilename;
		FileUtils.forceDelete(new File(zipFile));
		FileUtils.deleteDirectory(tmpImgFolder);
	}
	
	private static String generateURL(Request req, String newFilename) {
		String url = req.scheme() + "://" + req.host() + "/" + newFilename;
		return url;
	}

	public static String generateFilename(String filename) {
		
		String uniqueID = UUID.randomUUID().toString();
		String ext = FilenameUtils.getExtension(filename);
		
		filename = uniqueID + "." + ext;
		return filename;
	}

	//Check if there is a Zip Slip.
	private static File newFile(File tmpImgFolder, String nFilename) throws IOException {

		File destFile = new File(tmpImgFolder, nFilename);

		String destDirPath = tmpImgFolder.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + nFilename);
		}
		return destFile;
	}
	
	private static String createThumbnail(int srcWidth, int srcHeight, int targetWidth, String srcFilename) {
		
		double scale = (double) srcWidth / targetWidth;
		int targetHeight = (int) (srcHeight / scale);
		
		String fullPath = IMAGES + File.separator + srcFilename;
		String scaledFilename = targetWidth+"px_"+srcFilename;
		String scaledFilePath = IMAGES + File.separator + scaledFilename;
		
		try {
			
			Thumbnails.of(new File(fullPath))
			.size(targetWidth, targetHeight)
			.toFile(new File(scaledFilePath));
			
			return scaledFilename;
			
		} catch (IOException e) {
			lgr.error(e.getMessage());
			return null;
		}
		
	}
	
}
