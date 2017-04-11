package io.github.sidf.documentreader.document;

import org.ini4j.Ini;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.logging.Logger;

public class Bookmark {
	private Page page;
	private int pageIndex;
	private Ini bookmarkIni;
	private int sentenceIndex;
	private Document sourceDocument;
	
	private static Logger logger = Logger.getLogger(Bookmark.class.getName());
	
	Bookmark(Page page, int pageIndex, int sentenceIndex, Ini ini, Document sourceDocument) {
		this.page = page;
		this.bookmarkIni = ini;
		this.pageIndex = pageIndex;
		this.sentenceIndex = sentenceIndex;
		this.sourceDocument = sourceDocument;
	}
	
	Page getPage() {
		return page;
	}

	void setPage(Page page) {
		this.page = page;
	}

	int getPageIndex() {
		return pageIndex;
	}
	
	void setPageIndex(int pageIndex) throws IOException {
		this.pageIndex = pageIndex;
		updateBookmarkIni();
	}
	
	int getSentenceIndex() {
		return sentenceIndex;
	}
	
	void setSentenceIndex(int sentenceIndex) throws IOException {
		this.sentenceIndex = sentenceIndex;
		updateBookmarkIni();
	}
	
	boolean endReached() {
		BreakIterator iterator = BreakIterator.getSentenceInstance();
		iterator.setText(page.getContent());
		return onLastPage() && iterator.preceding(iterator.last()) == sentenceIndex;
	}
	
	boolean onLastPage() {
		return pageIndex == sourceDocument.getPageCount() - 1;
	}
	
	private void updateBookmarkIni() throws IOException {
		bookmarkIni.put(sourceDocument.getId(), "pageIndex", pageIndex);
		bookmarkIni.put(sourceDocument.getId(), "sentenceIndex", sentenceIndex);
		bookmarkIni.store();
		
		logger.info(String.format("Updated bookmark history file with pageIndex: %d and sentenceIndex: %d", pageIndex, sentenceIndex));
	}
	
	void delete() throws IOException {
		bookmarkIni.remove(sourceDocument.getId());
		bookmarkIni.store();
	}
}
