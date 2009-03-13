
package com.mtvi.plateng.subversion;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * SVNForceImport can be used to import a maven project into an svn repository. It has the ability to import numerous
 * different files/folders based on matching a regular expression pattern. Each matched item can be renamed and placed
 * in differing folders. 
 * 
 * SVNForceImport can also read a projects pom file and extract Major Minor and Patch version numbers.
 * 
 * @author bsmith
 * @version 0.1
 */
public class SVNForceImport {
	
	/**
	 * Main method, used by hudson until a plugin wrapper can be written.
	 * 
	 * @param args Arguments consist of switches and values as follows:<p>
	 * <ul>
	 *             
	 * <li> -r <code>repository_url</code><br>  
	 *      REQUIRED: The url of the repository to be used, should include the path to the project.<br><br>
	 *             
	 * <li> -t <code>target_path</code><br> 	
	 *      REQUIRED: The path to the local target directory.<br><br>
	 *             
	 * <li> -pom <code>pom.xml_path</code><br>	
	 *     	REQUIRED: The path to the project's pom file.<br><br>
	 *             
	 * <li> -i <code>file_pattern</code> <code>remote_path</code> <code>remote_name</code><br>	
	 *      REQUIRED, MULTIPLE: The item(s) (file/folder) to be imported.<br>
	 *      All arguments may use _MAJOR_,_MINOR_, and _PATCH_ to replace with pom values.<br>
	 *      <ul>
	 *      <li><code>file_pattern</code>: Only items fitting this pattern will be imported.<br>
	 *      <li><code>remote_path</code>:	Path from remote project to desired location, ends in "/". _ROOT_ places item in project root.<br>
	 *      <li><code>remote_name</code>:	Final name for imported item, multiple items matching the same pattern are prepended with numbers.<br>
	 *      </ul><br><br>
	 *      
	 *             
	 * <li> -u <code>svn_username</code><br>		
	 *     	OPTIONAL: svn username<br><br>
	 *             
	 * <li> -p <code>svn_password</code><br>		
	 *     	OPTIONAL: svn password<br>
	 * </ul>
	 */
	public static void main(String[] args) {
		
		ArrayList <ImportItem> importItems = new ArrayList <ImportItem>();
		String svnURL = "";
		String pomPath = "";
		String target = "";
		String user = "";
		String password = "";

		// read through given options
		// I thought about creating an enumeration for these, but I'd rather just write the wrapper.
		try{
			for (int i = 0; i < args.length;){

				if(args[i].equalsIgnoreCase("-r")){
					// set repositoryURL
					svnURL = args[i+1];
					i+= 2;

				}else if(args[i].equalsIgnoreCase("-i")){
					// add item
					importItems.add(new ImportItem(args[i+1], args[i+2], args[i+3]));    //args[i+1]);
					i+= 4;
					
				}else if(args[i].equalsIgnoreCase("-u")){
					// set username
					System.out.println("User: " + args[i+1]);
					user = args[i+1];
					i+=2;

				}else if(args[i].equalsIgnoreCase("-p")){
					// set password
					System.out.println("Password: " + args[i+1]);
					password = args[i+1];
					i+=2;

				}else if(args[i].equalsIgnoreCase("-pom")){
					// set pomPath
					pomPath = args[i+1];
					i+=2;
				}
				else if(args[i].equalsIgnoreCase("-t")){
					// set pomPath
					target = args[i+1];
					i+=2;
				}
			}
			if (svnURL.length()==0){
				System.err.println("SVNForceImport Error: Missing repository URL\n");
				System.exit(1);
			}
		} catch(Exception e){
			System.err.println("SVNForceImport Error: Error while parsing options\n");
			System.exit(1);
		}
		forceImport(svnURL, user, password, target, importItems, pomPath , null, null, null);
		
		System.exit(0);
	}
	
	/**
	 * The core SVNForceImport method, used to import files into a repository.
	 * 
	 * @param svnURL		The url of the repository including path to project root.
	 * @param user			The username to use for repository access.
	 * @param password		The password to use for repository access.
	 * @param target		The path to the local target directory, where items are found.
	 * @param items			The ImportItems to be imported.
	 * @param pomPath		The path to the project's pom.xml file.
	 * @param majorPath		The xml path to the major version in the pom file.
	 * @param minorPath		The xml path to the minor version in the pom file.
	 * @param patchPath		The xml path to the patch version in the pom file.
	 */
	public static void forceImport(String svnURL, String user, String password, String target, ArrayList<ImportItem> items, String pomPath, String majorPath, String minorPath, String patchPath){
		
		
		
		// target directory is required
		File targetDir = new File(target);
		if (!targetDir.canRead()) {
			System.err.println("SVNForceImport Error: target File/Directory not accessable: " + target);
		}
		System.out.println("SVNForceImport: target: " + target);
		
		// pom is not required, but without it MAJOR/MINOR/PATCH variables won't be available

		SimplePOMParser spp = new SimplePOMParser();
		if (null != pomPath){
			File pom  = new File(pomPath);
			if (!pom.canRead()){
				System.err.println("SVNForceImport Error: pom File not accessable: " + pomPath);
			}
			spp.setMajorPath(majorPath);
			spp.setMinorPath(minorPath);
			spp.setPatchPath(patchPath);
			spp.parse(pom);
			System.out.println("SVNForceImport: pomPath: " + pomPath);
		}
		
		SVNRepository repository = null;
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();

		// create the repo and authManager
		try {
			setupProtocols();
			repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(svnURL));
			if (null != user){
				authManager = SVNWCUtil.createDefaultAuthenticationManager(user, password);
			}
			repository.setAuthenticationManager(authManager);

		
			// create the commit client that will do the work
			SVNCommitClient commitClient = new SVNCommitClient(authManager, null);
			
			// import each item
			for (ImportItem item: items){
				// if the pom and major/minor/patch paths have been included attempt to do some simple replacement
				item.set_name(variableReplace(spp, item.get_name()));
				item.set_pattern(variableReplace(spp, item.get_pattern()));
				item.set_svnPath(variableReplace(spp, item.get_svnPath()));
				
				ArrayList<File> files = matchFiles(item.get_pattern(), targetDir);
				String prefix = "";
				for (int i = 0; i < files.size(); i++){
					
					ensurePath(repository, commitClient, svnURL, item.get_svnPath());
					
					if (!files.get(i).canRead()) {
						System.err.println("SVNForceImport Error: File/Directory not accessable: " + files.get(i).getAbsolutePath());
					}
	
					SVNNodeKind nodeKind = repository.checkPath(item.get_svnPath() + prefix + item.get_name(), -1);
					if (nodeKind == SVNNodeKind.NONE){
						insertItem(commitClient, svnURL + "/" + item.get_svnPath(), files.get(i), prefix + item.get_name());
					} else {
						deleteItem(commitClient, svnURL + "/" + item.get_svnPath() + prefix + item.get_name());
						insertItem(commitClient, svnURL + "/" + item.get_svnPath(), files.get(i), prefix + item.get_name());
					}
					
					prefix = Integer.toString(i + 1);
					
				}
			
			}
		}catch (SVNException svne) {

			System.err.println("*SVNForceImport Error: " + svne.getMessage());
		}
	}
	
	/**
	 * Insert a given file/folder into the given repository location with a given name
	 * 
	 * @param client			The SVNCommitClient to be used to preform the commit action.
	 * @param fullURL			The full URL pointing to where the importItem will be placed.
	 * @param importItem		The file or folder to be imported in the repository.
	 * @param name				The file/folder name to be used in the repository.
	 * @return 					The results of the commit action.
	 * @throws SVNException
	 */
	private static SVNCommitInfo insertItem(SVNCommitClient client, String fullURL, File importItem, String name)
	throws SVNException {
		String logMessage = "SVNForceImport importing: " + importItem.getAbsolutePath();
		System.out.println(logMessage);
		return client.doImport(
				importItem, 							// File/Directory to be imported
				SVNURL.parseURIEncoded(fullURL + name), // location within svn 
				logMessage, 							// svn comment
				new SVNProperties(), 					// svn properties
				true, 									// use global ignores
				false, 									// ignore unknown node types
				SVNDepth.INFINITY); 					// fully recursive

	}

	/**
	 * Delete a given file/folder from the repository.
	 * 
	 * @param client			The SVNCommitClient to be used to preform the commit action.
	 * @param fullURL			The full URL pointing to the item to be deleted.
	 * @return					The result of the commit action.
	 * @throws SVNException
	 */
	private static SVNCommitInfo deleteItem(SVNCommitClient client, String fullURL)
	throws SVNException {

		String logMessage = "SVNForceImport removing: " + fullURL;
		System.out.println(logMessage);
		SVNURL[] urls = {SVNURL.parseURIEncoded(fullURL)};
		return client.doDelete(urls, logMessage);

	}

	/**
	 * Create a given directory in the repository.
	 * 
	 * @param client			The SVNCommitClient to be used to preform the mkdir action.
	 * @param fullPath			The full URL pointing to where the Directory should be created (including the directory to be created).
	 * @return					The result of the commit action.
	 * @throws SVNException
	 */
	private static SVNCommitInfo createDir(SVNCommitClient client, String fullPath)
	throws SVNException {

		String logMessage = "SVNForceImport creating Directory : " + fullPath;
		System.out.println(logMessage);
		SVNURL[] urls = {SVNURL.parseURIEncoded(fullPath)};
		return client.doMkDir(urls, logMessage);

	}

	/**
	 * Set up the different repository protocol factories so that http,https,svn,and file protocols can all be used.
	 */
	private static void setupProtocols() {

		// http and https
		DAVRepositoryFactory.setup();
		// svn
		SVNRepositoryFactoryImpl.setup();
		// file
		FSRepositoryFactory.setup();
	}

	/**
	 * Search through a given directory and return an ArrayList of any files/folders who's names match the given pattern.
	 * 
	 * @param patternString		The regular expression pattern to use in matching applicable file/folder names
	 * @param parent			The folder to search for matches in.
	 * @return					All files/folders matching the given pattern.
	 */
	private static ArrayList<File> matchFiles(String patternString, File parent){
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher;
		ArrayList<File> files = new ArrayList <File>();
		for (File file : parent.listFiles()){ 
			matcher = pattern.matcher(file.getName());
			if (matcher.matches()){
				System.out.println("SVNForceImport - File Matched: " + file.getName());
				files.add(file);
			}
		}
		return files;

	}
		
	/**
	 * Replace variable names with their values.
	 * 
	 * @param spp				The SimplePOMParser containing the required variables.
	 * @param value				The String to be filtered.
	 * @return					The new String with variable names replaced with values
	 */
	private static String variableReplace(SimplePOMParser spp, String value){
		
		value = value.replace("_ROOT_", "");
		value = value.replace("_MAJOR_", Integer.toString(spp.getMajor()));
		value = value.replace("_MINOR_", Integer.toString(spp.getMinor()));
		value = value.replace("_PATCH_", Integer.toString(spp.getPatch()));
		return value;
		
	}
	
	/**
	 * Validate the the required path exists in the project on the repository. If it doesn't then create it.
	 * 
	 * @param repository		The repository to be checked.
	 * @param commitClient		The SVNCommitClient to be used to preform any commit actions.
	 * @param svnURL			The URL of the project in the repository.
	 * @param path				The path within the project to be checked/created.
	 */
	private static void ensurePath(SVNRepository repository, SVNCommitClient commitClient, String svnURL, String path){
		String[] dirs = path.split("/");
		String constructedPath = "";
		
		if (dirs.length > 0){
			
			for (int i = 0; i < dirs.length; i++){
				try{
					SVNNodeKind nodeKind = repository.checkPath(constructedPath + dirs[i], -1);
					if (nodeKind == SVNNodeKind.NONE){
						createDir(commitClient, svnURL + "/" +  constructedPath + dirs[i]);
						System.out.println("SVNForceImport Creating directory: " + svnURL + "/" +  constructedPath + dirs[i]);
					}
					constructedPath += dirs[i] + "/";
					
				}catch (SVNException svne) {
					System.err.println("SVNForceImport Error: " + svne.getMessage());
				}
			}
		}
	}
}
