package org.fuin.esc.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a list of string arguments to the cucumber class. All features will be
 * executed as a cross product of features and parameters. The test can query
 * the current argument using a system parameter named
 * <code>org.fuin.esc.test.EscCucumberArg</code>.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EscCucumberArgs {

    /** Arguments to execute the test with. */
    String[] value();

}
