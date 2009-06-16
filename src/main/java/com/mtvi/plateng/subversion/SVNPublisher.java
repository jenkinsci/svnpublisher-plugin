package com.mtvi.plateng.subversion;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.Publisher;

import java.io.PrintStream;
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
        private ArrayList<ImportItem> items;
        private String user;
        private String password;
        private String majorPath;
        private String minorPath;
        private String patchPath;
        private String workspace = "NA";
     
     /**
     * {@stapler-constructor}
     */
    @DataBoundConstructor
    public SVNPublisher(String svnUrl, String pomPath, String target, ArrayList<ImportItem> items, String user, String password, String majorPath, String minorPath, String patchPath){
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
    
    public ArrayList<ImportItem> getItems() {
        return items;
    }
    
    public void setItems(ArrayList <ImportItem> items){
    	this.items = items;
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
        	try{
        		workspace = build.getProject().getWorkspace().toURI().getPath();
        		listener.getLogger().println("workspace: " + workspace);
        	}catch (Exception e){
        		
        	}
        
        	listener.getLogger().println("Attempting to import to SVN: " + svnUrl);
            try {
                DESCRIPTOR.svnImport(svnUrl, target, items, user, password, pomPath, majorPath, minorPath, patchPath, workspace, listener.getLogger());
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
            public ArrayList <ImportItem> items;
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
        
        
        public List<ImportItem> getItems(SVNPublisher instance) {
            if (instance == null) {
                return new ArrayList<ImportItem> ();
            }
            return instance.getItems(); 
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
            SVNPublisher instance = req.bindJSON(SVNPublisher.class, cleanedFormData);
            ArrayList<ImportItem> itemsList = new ArrayList<ImportItem>(req.bindJSONToList(ImportItem.class, cleanedFormData.get("itm")));
            instance.setItems(itemsList);

            return instance;
            
        }

        public void svnImport(String svnUrl, String target, ArrayList<ImportItem> items, String user, String password, String pomPath,  String majorPath, String minorPath, String patchPath, String workspace, PrintStream stream) throws Exception {
                        
             SVNForceImport.forceImport(svnUrl, user, password, target,  items, pomPath, majorPath, minorPath, patchPath, workspace, stream);
	
        }
    }
}