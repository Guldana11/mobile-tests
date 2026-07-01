package core;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Auto-attaches {@link RetryAnalyzer} to every @Test, so we don't have to mark each one.
 * Wired up via TestNG suite XML &lt;listener&gt;.
 */
public class RetryListener implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {
        // DESTRUCTIVE tests (real deposit open, account lockout) must NOT be retried — a retry would
        // open a SECOND deposit / lock the account again. They opt out via the "destructive" group.
        for (String group : annotation.getGroups()) {
            if ("destructive".equals(group)) return;
        }
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
