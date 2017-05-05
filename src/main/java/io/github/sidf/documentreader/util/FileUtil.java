package io.github.sidf.documentreader.util;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.io.FileNotFoundException;
import javax.xml.bind.DatatypeConverter;
import java.nio.file.StandardCopyOption;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public class FileUtil {
	private static ClassLoader classLoader;
	
	static {
		classLoader = FileUtil.class.getClassLoader();
	}
	
	private FileUtil() {
		
	}
	
	public static String getMd5Hash(String filePath) throws IOException {
		byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
		byte[] md5Hash = null;
		
		try {
			 md5Hash = MessageDigest.getInstance("MD5").digest(fileContent);
		} catch (NoSuchAlgorithmException e) {
			// it won't reach here since MD5 is valid
		}
		
		// http://stackoverflow.com/questions/5470219/get-md5-string-from-message-digest
		return DatatypeConverter.printHexBinary(md5Hash);
	}
	
	public static String fileToString(String filePath) throws IOException {
		StringBuilder sb = new StringBuilder();
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(String.format("%s%n", line));
			}
		}
		
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	public static void stringToFile(String source, String locationPath) throws FileNotFoundException {
		try(PrintWriter writer = new PrintWriter(locationPath, "UTF-8")) {
			writer.print(source);
		} catch (UnsupportedEncodingException e) {
			//
		}
	}
	
	public static void copyFile(String sourcePath, String destinationPath) throws IOException {
		Files.copy(Paths.get(sourcePath), Paths.get(destinationPath), StandardCopyOption.REPLACE_EXISTING);
	}
	
	public static String resourcePathToFile(String relativeResourcePath) throws IOException {
		InputStream inputStream = classLoader.getResourceAsStream(relativeResourcePath);
		File file = File.createTempFile("temp", null);
		String filePath = file.getPath();
		
		StreamUtil.inputStreamToFile(inputStream, filePath);
		return filePath;
	}
}
