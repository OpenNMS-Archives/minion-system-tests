package org.opennms.minion.stests.junit;

import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Inspired by org.junit.rules.ExternalResource with the following additions:
 *  - A boolean is passed to after() indicating if any exceptions were thrown
 *    (did any tests fail?), allow it to alter the way it tears down resources.
 *  - after() is always called, even when before() fails
 * 
 * @author jwhite
 */
public abstract class ExternalResourceRule implements TestRule {
    public Statement apply(Statement base, Description description) {
        return statement(base);
    }

    ExternalResource a;
    
    private Statement statement(final Statement base) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                boolean didFail = false;
                try {
                    before();
                    base.evaluate();
                } catch (Throwable t) {
                    didFail = true;
                    throw t;
                } finally {
                    after(didFail);
                }
            }
        };
    }

    /**
     * Override to set up your specific external resource.
     *
     * @throws Throwable if setup fails (which will disable {@code after}
     */
    protected void before() throws Throwable {
        // do nothing
    }

    /**
     * Override to tear down your specific external resource.
     */
    protected void after(boolean didFail) {
        // do nothing
    }
}
