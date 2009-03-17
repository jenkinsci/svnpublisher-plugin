package com.mtvi.plateng.subversion;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A class used to hold information as to how a file or folder (or set of files/folder) is to be recognized, 
 * where the matching files/folders are to be placed in the project repository, and what name should be given to the 
 * files/folders.
 * 
 * @author bsmith
 *
 */
public class ImportItem {
	
	/**
	 * The pattern used to find files/folders covered by this item.
	 */
	private String pattern;
	/**
	 * The path within the repository's project root where the items are to be placed. 
	 */
	private String path;
	/**
	 * The name to be used when placing an item in the repository.
	 */
	private String name;
	
	/**
	 * @param pattern		The pattern to be used to find matching files/folders.
	 * @param path		The path within the project repository where matched items are to be placed.
	 * @param name			The name given to items when they are placed in the repository.
	 */
	@DataBoundConstructor
	public ImportItem(String pattern, String path, String name){
		this.pattern = pattern;
		this.path = path;
		this.name = name;
		
	}

	/**
	 * Return the pattern used to find files/folders covered by this item.
	 * 
	 * @return the pattern used to find files/folders covered by this item.
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * Return the path within the repository's project root where the items are to be placed. 
	 * 
	 * @return the path within the repository's project root where the items are to be placed. 
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Return the name to be used when placing an item in the repository.
	 * 
	 * @return the name to be used when placing an item in the repository.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the pattern used to find files/folders covered by this item.
	 * 
	 * @param _pattern the pattern used to find files/folders covered by this item.
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Set the path within the repository's project root where the items are to be placed. 
	 * 
	 * @param path the path within the repository's project root where the items are to be placed. 
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Set the name to be used when placing an item in the repository.
	 * 
	 * @param _name the name to be used when placing an item in the repository.
	 */
	public void setName(String name) {
		this.name = name;
	}
}
