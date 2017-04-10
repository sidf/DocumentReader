package io.github.sidf.documentreader.reader;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.sidf.documentreader.document.Page;
import io.github.sidf.documentreader.util.ArrayUtil;
import io.github.sidf.documentreader.util.enums.Speed;
import io.github.sidf.documentreader.document.Document;
import io.github.sidf.documentreader.util.enums.Language;

public abstract class Reader implements Runnable {
	private Speed speed;
	private boolean reading;
	private Language language;
	private Document document;
	private static Logger logger = Logger.getLogger(Reader.class.getName());
	
	Reader(Document document, Language language, Speed speed) throws IOException {
		this.document = document;
		
		if (!ArrayUtil.arrayContains(getSupportedLanguages(), language.getDisplayName())) {
			String message = String.format("The reader does not support %s", language.getDisplayName());
			throw new IOException(message);
		}
		
		this.language = language;
		
		if (!ArrayUtil.arrayContains(getSupportedSpeed(), speed.getDisplayName())) {
			String message = String.format("The reader does not support %s speed", speed.getDisplayName());
			throw new IOException(message);
		}
		
		this.speed = speed;
	}

	public Language getLanguage() {
		return language;
	}
	
	public Speed getSpeed() {
		return speed;
	}

	@Override
	public void run() {
		readerLoop();
	}
	
	private void readerLoop() {
		reading = true;
		
		logger.info("Entered reader loop");
		
		int pageIndex = 0;
		
		outerLoop:
		for (Page page : document) {
			logger.info(String.format("Reading page with the session index of %d", pageIndex));
			for (String sentence : page) {
				try {
					read(sentence);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Couldn't read sentence", e);
					throw new RuntimeException(e.getMessage());
				}
				
				if (!reading) {
					break outerLoop;
				}
			}
			pageIndex++;
		}
		
		document.postReadingOperations();
		stop();
	}
	
	public void stop() {
		reading = false;
		
		try {
			stopInternal();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Could not stop fully stop the reader", e);
		}
	}
	
	public boolean isReading() {
		return reading;
	}
	
	public abstract String[] getSupportedSpeed();
	public abstract String[] getSupportedLanguages();
	abstract void stopInternal() throws Exception;
	abstract void read(String text) throws Exception;
}
