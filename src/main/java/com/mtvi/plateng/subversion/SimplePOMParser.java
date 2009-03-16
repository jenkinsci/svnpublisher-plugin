package com.mtvi.plateng.subversion;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A Simple class to parse out the key values in a pom.xml file and make them easily available.
 * 
 * @author bsmith
 *
 */
public class SimplePOMParser {

	private String majorPath;
	
	private String minorPath;
	
	private String patchPath;
	
	/**
	 * The major from the pom.xml
	 */
	private int major;
	
	/**
	 * The minor from the pom.xml
	 */
	private int minor;
	
	/**
	 * The patch from the pom.xml
	 */
	private int patch;
	
	/**
	 * The Document used in parsing the pom.xml
	 */
	private Document doc;
	
	
	public SimplePOMParser(){
		
	}
	
	
	/**
	 * Read the pom if it fits the scheme retrieven the values.
	 * 
	 * @param pom		The pom.xml file to parse.
	 */
	public void parse(File pom){
		
		try{
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(pom);
			Element root = doc.getDocumentElement();
			
			if (null != majorPath){
				major = getValue(root, majorPath);
			}
			if (null != minorPath){
				minor = getValue(root, minorPath);
			}
			if (null != patchPath){
				patch = getValue(root, patchPath);
			}
			
						
		}catch (Exception e){
			System.err.println("Exception encountered while parsing file: " + e.getMessage());
		}
		
	}
	
	private int getValue(Element root, String path){
		try{
			String[] pathElements = path.split("\\.");
			String nodeName;
			int valueSplit = -1;
			int value;
			Element node = root;
			
			for (int i = 0; i < pathElements.length -1; i++){
				
				NodeList nl = node.getChildNodes();
				for (int j = 0; j < nl.getLength(); j++){
					if (nl.item(j).getNodeName().equalsIgnoreCase(pathElements[i])){
						node = (Element) nl.item(j);
						j = nl.getLength();
					}
				}
				
			}
			
			nodeName = pathElements[pathElements.length-1];
			if (nodeName.contains("[")){
				
				// this could be prettied up a lot
				String[] nodeSplit = nodeName.split("[\\[|\\]]");
				valueSplit = Integer.parseInt(nodeSplit[1]);
				nodeName = nodeSplit[0];
				
				NodeList nl = node.getChildNodes();
				for (int i = 0; i < nl.getLength(); i++){
					if (nl.item(i).getNodeName().equalsIgnoreCase(nodeName)){
						node = (Element) nl.item(i);
						i = nl.getLength();
					}
				}
				
				value = Integer.parseInt(node.getFirstChild().getNodeValue().split("\\.")[valueSplit]);
			}else{
				NodeList nl = node.getChildNodes();
				for (int i = 0; i < nl.getLength(); i++){
					if (nl.item(i).getNodeName().equalsIgnoreCase(nodeName)){
						node = (Element) nl.item(i);
						i = nl.getLength();
					}
				}
				value = Integer.parseInt(node.getFirstChild().getNodeValue());
			}

			return value;
		}catch (Exception e){
			return -1;	
		}
	}
	
		
	/**
	 * @return the majorPath
	 */
	public String getMajorPath() {
		return majorPath;
	}


	/**
	 * @param majorPath the majorPath to set
	 */
	public void setMajorPath(String majorPath) {
		this.majorPath = majorPath;
	}




	/**
	 * @return the minorPath
	 */
	public String getMinorPath() {
		return minorPath;
	}




	/**
	 * @param minorPath the minorPath to set
	 */
	public void setMinorPath(String minorPath) {
		this.minorPath = minorPath;
	}




	/**
	 * @return the patchPath
	 */
	public String getPatchPath() {
		return patchPath;
	}




	/**
	 * @param patchPath the patchPath to set
	 */
	public void setPatchPath(String patchPath) {
		this.patchPath = patchPath;
	}




	/**
	 * Return the major version number from the pom.xml
	 * 
	 * @return the major version number from the pom.xml
	 */
	public int getMajor() {
		return major;
	}


	/**
	 * Return the minor version number from the pom.xml
	 * 
	 * @return the minor version number from the pom.xml
	 */
	public int getMinor() {
		return minor;
	}


	/**
	 * Return the patch version number from the pom.xml
	 * 
	 * @return the patch version number from the pom.xml
	 */
	public int getPatch() {
		return patch;
	}
	
}
