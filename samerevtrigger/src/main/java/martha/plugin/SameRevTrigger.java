package martha.plugin;
import hudson.Launcher;
import hudson.Extension;
import hudson.tasks.BuildStepMonitor;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.Cause.RemoteCause;
import hudson.model.CauseAction;
import hudson.model.Hudson;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.StringParameterValue;
import hudson.scm.SameRevisionAction;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
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
public class SameRevTrigger extends Recorder {

    private final String name;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public SameRevTrigger(String name) {
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
        if (build.getResult().equals(Result.FAILURE)) {
            CauseAction causeaction = build.getAction(CauseAction.class);
            if (causeaction.getCauses().iterator().next() instanceof RemoteCause) {
                if (build.getAction(SameRevisionAction.class) != null) {
                    Hudson hudson = Hudson.getInstance();
                    AbstractProject p = hudson.getItemByFullName("SameRev", AbstractProject.class);
                    List values = new ArrayList();
                    values.add(new StringParameterValue("FailedProject", build.getProject().getDisplayName()));
                    values.add(new StringParameterValue("FailedBuildNumber", Integer.toString(build.getNumber())));
                    p.scheduleBuild(1, null, new ParametersAction(values));
                    return true;
                }
            }
        }
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
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
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        
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
            return "Same Revision Trigger";
        }
    }
}

