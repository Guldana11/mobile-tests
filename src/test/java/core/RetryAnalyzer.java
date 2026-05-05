package core;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries failed tests once. Compensates for flaky setUp on a slow Android emulator
 * where the splash sometimes takes longer than expected. Don't bump above 1 retry —
 * that hides real bugs instead of just smoothing infrastructure noise.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRY_COUNT = 1;
    private int currentRetry = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (currentRetry < MAX_RETRY_COUNT) {
            currentRetry++;
            System.out.println("[retry] " + result.getName()
                    + " — attempt " + (currentRetry + 1) + " of " + (MAX_RETRY_COUNT + 1));
            return true;
        }
        return false;
    }
}
