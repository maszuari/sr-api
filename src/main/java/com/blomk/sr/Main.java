package com.blomk.sr;

import static spark.Spark.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;
import spark.Request;
import spark.Response;

public class Main {

	// Create config.properties and set the directory on the storage property.
	// For example; storage=/tmp/public
	private static String IMAGES;
	private static String TMP;
	private static Logger lgr = LoggerFactory.getLogger(Main.class);
	private static Set<String> db;
	private static File imagesDir, tmpDir;

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
	}

	private static String getImageLink(Request req, Response res) {
		req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));

		try {

			Part filePart = req.raw().getPart("file");

			String uploadedFileName = filePart.getSubmittedFileName();
			String cntType = filePart.getContentType();
			if (cntType.startsWith("image/")) {
				InputStream stream = filePart.getInputStream();
				String newFilename = generateFilename(uploadedFileName);
				//String url = req.scheme() + "://" + req.host() + "/" + newFilename;
				String url = generateURL(req, newFilename);
				db.add(url);
				Files.copy(stream, Paths.get(IMAGES).resolve(newFilename), StandardCopyOption.REPLACE_EXISTING);
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

		/*
		 * TODO: 
		 * 1. Get the zip file from client.
		 * 2. Create a unique folder in temp file to store the image files.
		 * 3. Extract zip file and save img files to the folder.
		 * 4. Get all img files from the folder.
		 * 5. Rename img file and save it to main folder.
		 * 6. Delete zip file and the tmp folder.
		 */
		req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));

		try {
			Part filePart = req.raw().getPart("file");
			String uploadedFileName = filePart.getSubmittedFileName();

			String cntType = filePart.getContentType();

			if (cntType.equals("application/zip")) {
				InputStream stream = filePart.getInputStream();

				String zipFilename = generateFilename(uploadedFileName);
				Files.copy(stream, Paths.get(TMP).resolve(zipFilename), StandardCopyOption.REPLACE_EXISTING);
				File tmpImgFolder = readContentInZipFile(req, zipFilename);
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
	
	private static void renameAndMoveFiles(Request req, File folder) throws IOException {
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isFile()) {
		          String fName = fileEntry.getName();
		          String newFilename = generateFilename(fileEntry.getName());
		          Path srvFile = Paths.get(fileEntry.getAbsoluteFile().toURI());
		          Files.copy(srvFile, Paths.get(IMAGES).resolve(newFilename), StandardCopyOption.REPLACE_EXISTING);
		          String url = generateURL(req, newFilename);
		          db.add(url);
			}
		}
	}

	private static File readContentInZipFile(Request req, String filename) {

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
				
				//String newFilename = generateFilename(entry.getName());
				lgr.debug("Image file "+ entry.getName());
				
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
	        //Delete tmpImgFolder
		} catch (IOException ex) {
			lgr.error(ex.getMessage());
			return null;
		}

	}
	
	private static void deleteFileAndFolder(String zipFilename, File tmpImgFolder) throws IOException {
		
		String zipFile = TMP + File.separator + zipFilename;
		FileUtils.forceDelete(new File(zipFile));
		FileUtils.deleteDirectory(tmpImgFolder);
	}
	
	private static String generateURL(Request req, String newFilename) {
		String url = req.scheme() + "://" + req.host() + "/" + newFilename;
		return url;
	}

	private static String generateFilename(String filename) {
		String uniqueID = UUID.randomUUID().toString();
		String ext = FilenameUtils.getExtension(filename);
		filename = uniqueID + "." + ext;
		return filename;
	}

	public static File newFile(File tmpImgFolder, String nFilename) throws IOException {

		File destFile = new File(tmpImgFolder, nFilename);

		String destDirPath = tmpImgFolder.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + nFilename);
		}

		return destFile;
	}

	// Check if the file is an image.
	private static boolean isImageFile(byte[] in) {

		try {
			String mimeType = Magic.getMagicMatch(in, false).getMimeType();
			lgr.debug("MIME TYPE " + mimeType);
			if (mimeType.startsWith("image/")) {
				return true;
			} else {
				return false;
			}
		} catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
			// TODO Auto-generated catch block
			lgr.error(e.toString());
			return false;
		}

	}
	
	private static void renameFileInZipFile(URI zipFile, Map<String, String> zipProperties, String entryName, String nZipFilename) throws IOException {
		
        
        //FileSystem fs = FileSystems.newFileSystem(zipfile, zip_properties, null);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, zipProperties, null)) {
            /* Access file that needs to be renamed */
            Path pathInZipfile = zipfs.getPath(entryName);
            /* Specify new file name */
            Path renamedZipEntry = zipfs.getPath(nZipFilename);
            /* Execute rename */
            Files.move(pathInZipfile,renamedZipEntry,StandardCopyOption.ATOMIC_MOVE);  
        } 
	}

}
