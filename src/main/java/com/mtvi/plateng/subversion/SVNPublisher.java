package com.mtvi.plateng.subversion;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * 	The hudson plugin wrapper is based off of (and on occasion copied verbatim from) the twitter plugin
 * by justinedelson and cactusman.
 * 
 * @author bsmith
 * 
 */
public class SVNPublisher extends Publisher {

        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
        private static final Logger LOGGER = Logger.getLogger(SVNPublisher.class.getName());

        private String svnUrl;
        private String pomPath;
        private String target;
        private String items;
        private String user;
        private String password;
        private String majorPath;
        private String minorPath;
        private String patchPath;
     
     /**
     * {@stapler-constructor}
     */
    @DataBoundConstructor
    public SVNPublisher(String svnUrl, String pomPath, String target, String items, String user, String password, String majorPath, String minorPath, String patchPath){
        this.svnUrl = svnUrl;
        this.pomPath = pomPath;
        this.target = target;
        this.items = items;
        this.user = user;
        this.password = password;
        this.majorPath = majorPath;
        this.minorPath = minorPath;
        this.patchPath = patchPath;
    }

    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    public String getSvnUrl() {
        return svnUrl;
    }
    
    public String getPomPath() {
        return pomPath;
    }
    
    public String getTarget() {
        return target;
    }
    
    public String getItems() {
        return items;
    }
    
    public String getUser() {
        return user;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getMajorPath() {
        return majorPath;
    }
    
    public String getMinorPath() {
        return minorPath;
    }
    
    public String getPatchPath() {
        return patchPath;
    }

    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    	return _perform(build, launcher, listener);
    }

    protected <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> boolean _perform(B build,
            Launcher launcher, BuildListener listener) {
        if (build.getResult() == Result.SUCCESS){

            try {
                DESCRIPTOR.svnImport(svnUrl, target, items, user, password, pomPath, majorPath, minorPath, patchPath);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unable to import to svn.", e);
            }
        }
        return true;

    }

        public static final class DescriptorImpl extends Descriptor<Publisher> {
            private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());

            private static final List<String> VALUES_REPLACED_WITH_NULL = Arrays.asList("",
                    "(Default)", "(System Default)");
            
            public String svnUrl;
            public String pomPath;
            public String target;
            public String items;
            public String user;
            public String password;
            public String majorPath;
            public String minorPath;
            public String patchPath;
                        
            protected DescriptorImpl() {
                super(SVNPublisher.class);
                load();
            }
           
            
        /**
         * Clean up the formData object by removing blanks and (Default) values.
         * 
         * @param formData
         *            the incoming form data
         * @return a new cleaned JSONObject object
         */
        protected static JSONObject cleanJSON(JSONObject formData) {
            JSONObject cleaned = new JSONObject();
            for (Object key : formData.keySet()) {
                Object o = formData.get(key);
                if (o instanceof String) {
                    if (!VALUES_REPLACED_WITH_NULL.contains(o)) {
                        cleaned.put(key, o);
                    }
                } else {
                    cleaned.put(key, o);
                }
            }
            return cleaned;
        }
        
        @Override
        public boolean configure(StaplerRequest req) throws FormException {

            req.bindParameters(this, "svnpublish.");
            save();
            return super.configure(req);
        }
        
        @Override
        public String getDisplayName() {
            return "SVN";
        }

        public String getSvnUrl() {
            return svnUrl;
        }
        
        public String getTarget() {
            return target;
        }
        
        public String getPomPath() {
            return pomPath;
        }
        
        public String getItems() {
            return items;
        }
        
        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        
        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            JSONObject cleanedFormData = cleanJSON(formData);
            
            Publisher publisher = req.bindJSON(clazz, cleanedFormData);
            return publisher;
        }

        public void svnImport(String svnUrl, String target, String items, String user, String password, String pomPath,  String majorPath, String minorPath, String patchPath) throws Exception {
            
            LOGGER.info("Attempting to import to SVN: " + svnUrl);
            
            // Need to create some repeating jelly input method for the import items
            
            SVNForceImport.forceImport(svnUrl, user, password, target,  parseItems(items), pomPath, majorPath, minorPath, patchPath);

        }
        
        // temp function to allow a list of comma seperated import items to be fed into a simple text field.
        private ArrayList<ImportItem> parseItems(String items){
        	
        	ArrayList<ImportItem> itemsArray = new ArrayList<ImportItem>();
        	
        	String[] itemSplit = items.split(",");
        	
        	for (int i = 0; i < itemSplit.length; i +=3 ){
        		String pattern = itemSplit[i];
        		String svnPath = itemSplit[i+1];
        		String name = itemSplit[i+2];
        		
        		ImportItem item = new ImportItem( pattern, svnPath, name);
        		itemsArray.add(item);
        	}
        	
        	return itemsArray;        	
        }
    }
}