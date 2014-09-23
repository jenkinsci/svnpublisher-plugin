package com.mtvi.plateng.subversion;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.google.common.collect.Lists;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.kohsuke.stapler.AncestorInPath;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.tmatesoft.svn.core.SVNException;

/**
 * The jenkins plugin wrapper is based off of (and on occasion copied verbatim
 * from) the twitter plugin by justinedelson and cactusman.
 *
 * @author bsmith
 *
 */
public class SVNPublisher extends Notifier {

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    private static final Logger LOGGER = Logger.getLogger(SVNPublisher.class.getName());

    private String svnUrl;
    private String target;
    private String credentialsId;
    private String commitMessage;
    private List<ImportItem> artifacts = Lists.newArrayList();;

    @DataBoundConstructor
    public SVNPublisher(String svnUrl, String target, String credentialsId, String commitMessage, List<ImportItem> artifacts) {
        this.svnUrl = svnUrl;
        this.target = target;
        this.credentialsId = credentialsId;
        this.artifacts = artifacts;
        this.commitMessage = commitMessage;
    }

    public String getSvnUrl() {
        return svnUrl;
    }

    public String getTarget() {
        return target;
    }

    public List<ImportItem> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(ArrayList<ImportItem> items) {
        this.artifacts = items;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    private List<ImportItem> cloneItems(List<ImportItem> oldArtifacts) {
        List<ImportItem> newArts = Lists.newArrayList();
        for (ImportItem a : oldArtifacts) {
            newArts.add(new ImportItem(a));
        }
        return newArts;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        PrintStream buildLogger = listener.getLogger();
        
        String workspace;
        try {
            EnvVars envVars = build.getEnvironment(listener);
            if (target.trim().equals(""))
                target = envVars.get("WORKSPACE");
           
            workspace = envVars.get("WORKSPACE") + SVNWorker.systemSeparator + "svnpublisher";
            SVNWorker repo = new SVNWorker(SVNWorker.replaceVars(envVars, this.svnUrl),  workspace,  SVNWorker.replaceVars(envVars,this.target), DescriptorImpl.lookupCredentials(this.svnUrl, build.getProject(), this.credentialsId));
            try {                
                List<ImportItem> artifact = SVNWorker.parseAndReplaceEnvVars(envVars, cloneItems(this.artifacts));
                if (repo.createWorkingCopy(artifact).isEmpty()){
                    repo.dispose();
                    return true;
                }
                repo.setCommitMessage(SVNWorker.replaceVars(envVars,commitMessage));
                repo.commit();
         
            } catch (SVNPublisherException ex) {
                buildLogger.println(ex.getMessage());
                return false;
            } finally {
                repo.dispose();                
            }
        } catch (IOException ex) {
            buildLogger.println(ex.getMessage());
             return false;
        } catch (InterruptedException ex) {
            buildLogger.println(ex.getMessage());
            return false;
        }
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());

        protected DescriptorImpl() {
            super(SVNPublisher.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "Publish to Subversion repository";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public <P extends AbstractProject> FormValidation doCheckCredentialsId(@AncestorInPath Item context, @QueryParameter("svnUrl") final String url, @QueryParameter("credentialsId") final String credentialsId) {
            try {
                Credentials cred = DescriptorImpl.lookupCredentials(url, (P) context, credentialsId);
                SVNWorker svn = new SVNWorker(url, cred);
                svn.getSVNRepository().getRepositoryPath("/");

                return FormValidation.ok("Connected to repository");
            } catch (SVNException ex) {
                return FormValidation.error(ex.getErrorMessage().getMessage());
            }
        }
        
        public <P extends AbstractProject> FormValidation doCheckSvnURL(@AncestorInPath Item context, @QueryParameter("svnUrl") final String url, @QueryParameter("credentialsId") final String credentialsId) {
            return doCheckCredentialsId(context,url,credentialsId);
        }
        
        public <P extends AbstractProject> FormValidation doCheckTarget(@AncestorInPath AbstractProject project, @QueryParameter("target") final String target){
             if (target.trim().equals(""))
                 return FormValidation.error("Path is not valid");             
            try {
                File f  = new File(SVNWorker.replaceVars(project.getSomeBuildWithWorkspace().getEnvironment(TaskListener.NULL), target));
                if (!f.exists()) return FormValidation.error("Path does not exists");
            } catch (IOException ex) {
                return FormValidation.error(ex.getMessage());
            } catch (InterruptedException ex) {
                return FormValidation.error(ex.getMessage());
            }
             return FormValidation.ok();
         }    

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String svnUrl) {
            List<DomainRequirement> domainRequirements;
            domainRequirements = URIRequirementBuilder.fromUri(svnUrl.trim()).build();
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withMatching(
                            CredentialsMatchers.anyOf(
                                    CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class),
                                    CredentialsMatchers.instanceOf(StandardCertificateCredentials.class),
                                    CredentialsMatchers.instanceOf(SSHUserPrivateKey.class)
                            ),
                            CredentialsProvider.lookupCredentials(StandardCredentials.class,
                                    context,
                                    ACL.SYSTEM,
                                    domainRequirements)
                    );
        }

        private static <P extends AbstractProject> Credentials lookupCredentials(String repoUrl, P context, String credentialsId) {
            Credentials credentials = credentialsId == null ? null : CredentialsMatchers
                    .firstOrNull(CredentialsProvider.lookupCredentials(StandardCredentials.class, context,
                                    ACL.SYSTEM, URIRequirementBuilder.fromUri(repoUrl).build()),
                            CredentialsMatchers.allOf(CredentialsMatchers.withId(credentialsId),
                                    CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardCredentials.class),
                                            CredentialsMatchers.instanceOf(SSHUserPrivateKey.class))));
            return credentials;
        }
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public void setSvnUrl(String svnUrl) {
        this.svnUrl = svnUrl;
    }

    public void setTarget(String target) {
        this.target = target;
    }

 
    
    
}
