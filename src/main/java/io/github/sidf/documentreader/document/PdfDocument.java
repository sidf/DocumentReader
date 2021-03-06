package io.github.sidf.documentreader.document;

import java.io.File;

import io.github.sidf.documentreader.util.IoUtil;
import io.github.sidf.documentreader.util.CommandUtil;
import io.github.sidf.documentreader.util.CommandResult;

public class PdfDocument extends Document {
	private int pageCount;
	
	public PdfDocument(String filePath, File bookmarkFile) throws Exception {
		super(filePath, bookmarkFile);
		
		pageCount = fetchPageCount();
	}

	@Override
	Page fetchPage(int index) throws Exception {
		File tempFile = File.createTempFile("tempContent", null);
		CommandUtil.executeCommand(String.format("pdftotext -layout -nopgbrk -f %d -l %d \"%s\" %s", index + 1, index + 1,
												 getPath(), tempFile.getPath()));
		
		Page page = new Page(getBookmark(), IoUtil.fileToString(tempFile.getPath()));
		return page;
	}
	
	private int fetchPageCount() throws Exception {
		CommandResult commandResult = CommandUtil.executeCommand(String.format("pdfinfo \"%s\" | grep Pages", getPath()));
		int pageCount = Integer.valueOf(commandResult.getStdout().split("\\s+")[1]);
		return pageCount;
	}

	@Override
	public int getPageCount() {
		return pageCount;
	}
}
