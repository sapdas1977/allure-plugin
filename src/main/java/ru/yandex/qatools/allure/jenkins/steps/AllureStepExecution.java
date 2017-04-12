package ru.yandex.qatools.allure.jenkins.steps;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import ru.yandex.qatools.allure.jenkins.AllureReportPublisherDescriptor;
import ru.yandex.qatools.allure.jenkins.tools.AllureCommandlineInstallation;
import ru.yandex.qatools.allure.jenkins.utils.BuildUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;

import static ru.yandex.qatools.allure.jenkins.utils.BuildUtils.getAllureInstallations;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class AllureStepExecution extends StepExecution implements Serializable {

    private final transient WithAllureStep step;
    private final transient TaskListener listener;
    private final transient Launcher launcher;
    private final transient EnvVars env;

    private transient PrintStream console;

    public AllureStepExecution(StepContext context, WithAllureStep step) throws Exception {
        super(context);
        this.step = step;
        listener = context.get(TaskListener.class);
        launcher = context.get(Launcher.class);
        env = context.get(EnvVars.class);
    }

    @Override
    public boolean start() throws Exception {
        console = listener.getLogger();
        setupAllure();

        try {
            getContext().newBodyInvoker().withContexts(env).start();
            getContext().onSuccess(null);
        } catch (Exception e) {
            getContext().onFailure(e);
        }

        return false;
    }

    @Override
    public void stop(@Nonnull Throwable cause) {
        console.print("[withAllure] Allure report generation failed with error " + cause.getMessage());
    }

    private void setupAllure() throws IOException, InterruptedException {
        String allureInstallationName = step.getCommandline();
        if (allureInstallationName == null) {
            throw new AbortException("[withAllure] Define an Allure Commandline installation to use");
        }

        AllureCommandlineInstallation installation = new AllureReportPublisherDescriptor()
                .findCommandlineByName(allureInstallationName);
        if (installation == null) {
            throw new AbortException("[withAllure] Could not find Allure Commandline installation with name '"
                    + allureInstallationName + "'.");
        }

        BuildUtils.setUpTool(installation, launcher, listener, env);
    }
}
