// CHECKSTYLE:OFF Code from Cucumber code base
package org.fuin.esc.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import cucumber.api.CucumberOptions;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.junit.Assertions;
import cucumber.runtime.junit.FeatureRunner;
import cucumber.runtime.junit.JUnitReporter;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.snippets.SummaryPrinter;

/**
 * Slightly changed version of {@link cucumber.api.junit.Cucumber} that can
 * handle parameters (See {@link EscCucumberArgs}).<br>
 * <br>
 * Classes annotated with {@code @RunWith(EscCucumber.class)} will run a
 * Cucumber Feature. The class should be empty without any fields or methods.
 * <p/>
 * Cucumber will look for a {@code .feature} file on the classpath, using the
 * same resource path as the annotated class ({@code .class} substituted by
 * {@code .feature}).
 * <p/>
 * Additional hints can be given to Cucumber by annotating the class with
 * {@link CucumberOptions}.
 */
public class EscCucumber extends ParentRunner<FeatureRunner> {
    
    public static final String SYSTEM_PROPERTY = EscCucumber.class.getSimpleName() + "Arg";
    
    private final JUnitReporter jUnitReporter;
    private final List<FeatureRunner> children = new ArrayList<FeatureRunner>();
    private final Runtime runtime;

    /**
     * Constructor called by JUnit.
     *
     * @param clazz
     *            the class with the @RunWith annotation.
     * @throws java.io.IOException
     *             if there is a problem
     * @throws org.junit.runners.model.InitializationError
     *             if there is another problem
     */
    public EscCucumber(Class<?> clazz) throws InitializationError, IOException {
        super(clazz);
        ClassLoader classLoader = clazz.getClassLoader();
        Assertions.assertNoCucumberAnnotatedMethods(clazz);

        final List<String> argList = new ArrayList<String>();
        final EscCucumberArgs args = clazz.getAnnotation(EscCucumberArgs.class);
        if (args == null) {
            argList.add("NONE");
        } else {
            argList.addAll(Arrays.asList(args.value()));
        }
        
        RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(
                clazz, new Class[] { CucumberOptions.class });
        RuntimeOptions runtimeOptions = runtimeOptionsFactory.create();
        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader,
                classLoader);
        runtime = new Runtime(resourceLoader, classFinder, classLoader,
                runtimeOptions);
        jUnitReporter = new JUnitReporter(runtimeOptions.reporter(classLoader),
                runtimeOptions.formatter(classLoader),
                runtimeOptions.isStrict());
        
        for (final String arg : argList) {
            addChildren(runtimeOptions.cucumberFeatures(resourceLoader), arg);
        
        }
        
    }

    @Override
    public List<FeatureRunner> getChildren() {
        return children;
    }

    @Override
    protected Description describeChild(FeatureRunner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(FeatureRunner child, RunNotifier notifier) {
        child.run(notifier);
    }

    @Override
    public void run(RunNotifier notifier) {
        super.run(notifier);
        jUnitReporter.done();
        jUnitReporter.close();
        new SummaryPrinter(System.out).print(runtime);
    }

    private void addChildren(final List<CucumberFeature> cucumberFeatures, final String arg) throws InitializationError {
        for (final CucumberFeature cucumberFeature : cucumberFeatures) {
            children.add(new FeatureRunner(cucumberFeature, runtime, jUnitReporter) {
                @Override
                public String getName() {
                    return "[" + arg + "] " + super.getName();
                }
                @Override
                public void run(RunNotifier notifier) {
                    System.setProperty(SYSTEM_PROPERTY, arg);
                    super.run(notifier);
                }
            });
        }
    }
    
}
// CHECKTYLE:ON
