package org.jenkinsci.plugins.visibleadministrativemonitors;

import hudson.Extension;
import hudson.Functions;
import hudson.model.AdministrativeMonitor;
import hudson.model.PageDecorator;
import hudson.security.GlobalSecurityConfiguration;
import hudson.util.HudsonIsLoading;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

@Extension
public class Decorator extends PageDecorator {
    private static final Logger LOGGER = Logger.getLogger(Decorator.class.getName());

    private final Collection<String> ignoredJenkinsRestOfUrls = new ArrayList<String>();

    public Decorator() {
        // redundant
        ignoredJenkinsRestOfUrls.add("manage");

        // otherwise this would be added to every internal context menu building request
        ignoredJenkinsRestOfUrls.add("contextMenu");
    }

    @Override
    public String getDisplayName() {
        return "Visible Administrative Monitors"; // TODO i18n
    }

    public boolean isAnyAdministrativeMonitorActive() {
        return getActiveAdministrativeMonitors().size() > 0;
    }

    public Collection<AdministrativeMonitor> getActiveAdministrativeMonitors() {
        Collection<AdministrativeMonitor> active = new ArrayList<AdministrativeMonitor>();
        Collection<AdministrativeMonitor> ams = new ArrayList<AdministrativeMonitor>(Jenkins.getInstance().administrativeMonitors);
        for (AdministrativeMonitor am : ams) {
            if (am.isEnabled() && am.isActivated()) {
                active.add(am);
            }
        }
        return active;
    }

    public int getActiveAdministrativeMonitorsCount() {
        return getActiveAdministrativeMonitors().size();
    }

    public boolean shouldDisplay() throws IOException, ServletException {
        if (!Functions.hasPermission(Jenkins.ADMINISTER)) {
            return false;
        }

        if (!isAnyAdministrativeMonitorActive()) {
            return false;
        }

        StaplerRequest req = Stapler.getCurrentRequest();

        if (req == null) {
            return false;
        }
        List<Ancestor> ancestors = req.getAncestors();

        if (ancestors == null || ancestors.size() == 0) {
            // ???
            return false;
        }

        Ancestor a = ancestors.get(ancestors.size() - 1);
        Object o = a.getObject();

        // don't show while Jenkins is loading
        if (o instanceof HudsonIsLoading) {
            return false;
        }

        if (o instanceof GlobalSecurityConfiguration) {
            return false;
        }

        // don't show for some URLs served directly by Jenkins
        if (o instanceof Jenkins) {
            String url = a.getRestOfUrl();

            if (url.startsWith(req.getContextPath())) {
                // https://github.com/stapler/stapler/issues/34
                // only relevant for Jenkins 1.604 or older
                url = url.substring(req.getContextPath().length());

                if (url.startsWith("/")) {
                    url = url.substring(1);
                }
            }

            // hide on /manage page and major config pages
            if (ignoredJenkinsRestOfUrls.contains(url)) {
                return false;
            }
        }

        return true;
    }
}
