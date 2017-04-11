package io.github.sidf.documentreader.document;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.function.Consumer;
import java.io.FileNotFoundException;

public class DocumentLibrary {
	private File libraryFile;
	private File bookmarkFile;
	private List<Document> documents = new ArrayList<Document>();
	private static Logger logger = Logger.getLogger(DocumentLibrary.class.getName());
	
	public DocumentLibrary(String libraryPath) throws Exception {
		libraryFile = new File(libraryPath);
		
		if (!libraryFile.isDirectory()) {
			throw new FileNotFoundException(String.format("%s does not exist as a directory", libraryPath));
		} 
		
		bookmarkFile = new File(String.join(File.separator, libraryPath, "bookmarks.ini"));
		
		if (!bookmarkFile.isFile()) {
			bookmarkFile.createNewFile();
		}
		
		update();
	}
	
	public void clear() {
		documents.clear();
	}
	
	public void update() {
		clear();
		Set<File> brokenDocuments = new HashSet<>();
		
		for (File file : libraryFile.listFiles()) {
			for (String documentProvider : DocumentFactory.getDocumentProviders()) {
				Document document = null;
				try {
					document = DocumentFactory.getInstance(documentProvider, file.getPath(), bookmarkFile);
					if (brokenDocuments.contains(file)) {
						brokenDocuments.remove(file);
					}
				} catch (Exception e) {
					brokenDocuments.add(file);
					continue;
				}
				
				documents.add(document);
				logger.info(String.format("Document %s with id %s was added to the library", document.getName(), 
										  document.getId()));
			}
		}
		
		if (!brokenDocuments.isEmpty()) {
			for (File file : brokenDocuments) {
				logger.warning(String.format("No document provider was able to initialize %s, deleting", file.getPath()));
				file.delete();
			}
		}
	}
	
	public void delete() {
		for (Document document : documents) {
			document.delete();
		}
	}

	public Document getDocumentById(String id) throws IOException {
		for (Document document : documents) {
			if (document.getId().equals(id)) {
				return document;
			}
		}
		
		String message = String.format("No document with id %s found in the library", id);
		logger.severe(message);
		
		throw new IOException(message);
	}
	
	public Map<String, String> getDocumentMap() {
		Map<String, String> map = new HashMap<>();
		
		documents.forEach(new Consumer<Document>() {
			@Override
			public void accept(Document document) {
				map.put(document.getId(), document.getName());
			}
		});
		
		return map;
	}
	
	public void deleteDocumentById(String documentId) {
		Document documentToRemove = null;
		for (Document document : documents) {
			if (document.getId().equals(documentId)) {
				documentToRemove = document;
				document.delete();
				break;
			}
		}
		
		if (documentToRemove != null) {
			documents.remove(documentToRemove);
		}
	}
}
