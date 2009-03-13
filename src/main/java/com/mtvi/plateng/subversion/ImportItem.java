package com.mtvi.plateng.subversion;

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
	private String _pattern = "";
	/**
	 * The path within the repository's project root where the items are to be placed. 
	 */
	private String _svnPath = "";
	/**
	 * The name to be used when placing an item in the repository.
	 */
	private String _name = "";
	
	/**
	 * @param pattern		The pattern to be used to find matching files/folders.
	 * @param svnPath		The path within the project repository where matched items are to be placed.
	 * @param name			The name given to items when they are placed in the repository.
	 */
	public ImportItem(String pattern, String svnPath, String name){
		_pattern = pattern;
		_svnPath = svnPath;
		_name = name;
	}

	/**
	 * Return the pattern used to find files/folders covered by this item.
	 * 
	 * @return the pattern used to find files/folders covered by this item.
	 */
	public String get_pattern() {
		return _pattern;
	}

	/**
	 * Return the path within the repository's project root where the items are to be placed. 
	 * 
	 * @return the path within the repository's project root where the items are to be placed. 
	 */
	public String get_svnPath() {
		return _svnPath;
	}

	/**
	 * Return the name to be used when placing an item in the repository.
	 * 
	 * @return the name to be used when placing an item in the repository.
	 */
	public String get_name() {
		return _name;
	}

	/**
	 * Set the pattern used to find files/folders covered by this item.
	 * 
	 * @param _pattern the pattern used to find files/folders covered by this item.
	 */
	public void set_pattern(String _pattern) {
		this._pattern = _pattern;
	}

	/**
	 * Set the path within the repository's project root where the items are to be placed. 
	 * 
	 * @param path the path within the repository's project root where the items are to be placed. 
	 */
	public void set_svnPath(String path) {
		_svnPath = path;
	}

	/**
	 * Set the name to be used when placing an item in the repository.
	 * 
	 * @param _name the name to be used when placing an item in the repository.
	 */
	public void set_name(String _name) {
		this._name = _name;
	}
}
