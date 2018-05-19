// CHECKSTYLE:OFF
package org.fuin.esc.test;

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
import cucumber.runtime.junit.JUnitOptions;
import cucumber.runtime.junit.JUnitReporter;
import cucumber.runtime.model.CucumberFeature;

import org.fuin.esc.spi.EscSpiUtils;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Slightly changed version of {@link cucumber.api.junit.Cucumber} that can
 * handle parameters (See {@link EscCucumberArgs}).<br>
 * <br>
 * Classes annotated with {@code @RunWith(EscCucumber.class)} will run a
 * Cucumber Feature. The class should be empty without any fields or methods.
 * <p>
 * Classes annotated with {@code @RunWith(Cucumber.class)} will run a Cucumber Feature.
 * The class should be empty without any fields or methods.
 * </p>
 * <p>
 * Cucumber will look for a {@code .feature} file on the classpath, using the same resource
 * path as the annotated class ({@code .class} substituted by {@code .feature}).
 * </p>
 * Additional hints can be given to Cucumber by annotating the class with {@link CucumberOptions}.
 *
 * @see CucumberOptions
 */
public class EscCucumber extends ParentRunner<FeatureRunner> {

    public static final String SYSTEM_PROPERTY = EscCucumber.class
            .getSimpleName() + "Arg";

    private final JUnitReporter jUnitReporter;
    private final List<FeatureRunner> children = new ArrayList<FeatureRunner>();
    private final Runtime runtime;

    /**
     * Constructor called by JUnit.
     *
     * @param clazz the class with the @RunWith annotation.
     * @throws java.io.IOException                         if there is a problem
     * @throws org.junit.runners.model.InitializationError if there is another problem
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
            argList.addAll(EscSpiUtils.asList(args.value()));
        }
        
        RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(clazz);
        RuntimeOptions runtimeOptions = runtimeOptionsFactory.create();

        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        runtime = createRuntime(resourceLoader, classLoader, runtimeOptions);

        final JUnitOptions junitOptions = new JUnitOptions(runtimeOptions.getJunitOptions());
        final List<CucumberFeature> cucumberFeatures = runtimeOptions.cucumberFeatures(resourceLoader);
        jUnitReporter = new JUnitReporter(runtimeOptions.reporter(classLoader), runtimeOptions.formatter(classLoader), runtimeOptions.isStrict(), junitOptions);
        
        for (final String arg : argList) {
            addChildren(cucumberFeatures, arg);
        }
        
    }

    /**
     * Create the Runtime. Can be overridden to customize the runtime or backend.
     *
     * @param resourceLoader used to load resources
     * @param classLoader    used to load classes
     * @param runtimeOptions configuration
     * @return a new runtime
     * @throws InitializationError if a JUnit error occurred
     * @throws IOException if a class or resource could not be loaded
     */
    protected Runtime createRuntime(ResourceLoader resourceLoader, ClassLoader classLoader,
                                    RuntimeOptions runtimeOptions) throws InitializationError, IOException {
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        return new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
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
        runtime.printSummary();
    }

    private void addChildren(final List<CucumberFeature> cucumberFeatures,
            final String arg) throws InitializationError {
        for (final CucumberFeature cucumberFeature : cucumberFeatures) {
            children.add(new FeatureRunner(cucumberFeature, runtime,
                    jUnitReporter) {
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
// CHECKSTYLE:ON
