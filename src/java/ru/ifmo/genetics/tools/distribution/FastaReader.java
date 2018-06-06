package ru.ifmo.genetics.tools.distribution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Reads FASTA format
 * @author fedor.tsarev
 *
 */
public class FastaReader {
	
	private final String COMMENT_CHARACTERS = ";>"; 
	
	private BufferedReader in;
	
	private String currentString;
	
	public FastaReader(File file) throws IOException {
		in = new BufferedReader(new FileReader(file));
		nextLine();
	}
	
	public String nextRead() throws IOException {
		if (currentString == null) {
			return null;
		}
		while (currentString.isEmpty()  || COMMENT_CHARACTERS.indexOf(currentString.charAt(0)) >= 0) {
			nextLine();
		}
		StringBuilder currentRead = new StringBuilder("");
		while (currentString != null && (currentString.isEmpty() || COMMENT_CHARACTERS.indexOf(currentString.charAt(0)) < 0)) {
			currentRead.append(currentString);
			nextLine();
		}
		return currentRead.toString();
	}
	
	//TODO: make good exception handling
	private void nextLine() throws IOException {
		currentString = in.readLine();
	}
	
}
