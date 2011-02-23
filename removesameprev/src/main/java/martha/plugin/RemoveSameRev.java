package martha.plugin;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.scm.SameRevisionAction;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link HelloWorldBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)} method
 * will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class RemoveSameRev extends Builder {

    private final String name;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public RemoveSameRev(String name) {
        this.name = name;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        Hudson hudson = Hudson.getInstance();
        List<AbstractProject> projects = hudson.getAllItems(AbstractProject.class);
        for (AbstractProject project : projects) {
            if (project.getLastFailedBuild() != null) {
                List<Action> actions = project.getLastFailedBuild().getActions();
                Iterator it = actions.iterator();
                while (it.hasNext()) {
                    if (it.next() instanceof SameRevisionAction) {
                        try {
                            if (project.getLastBuild().getResult().equals(Result.FAILURE)) {
                                int nextnumber = project.getLastBuild().getNumber();
                                project.getLastBuild().delete();
                                project.updateNextBuildNumber(nextnumber);
                                return true;
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(RemoveSameRev.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        return true;
    }

    // overrided for better type safety.
    // if your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link HelloWorldBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>views/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types 
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

            return super.configure(req,formData);
        }
        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Remove Same Revision";
        }
    }
}

