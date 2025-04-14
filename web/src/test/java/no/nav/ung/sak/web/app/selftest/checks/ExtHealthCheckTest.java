package no.nav.ung.sak.web.app.selftest.checks;

import com.codahale.metrics.health.HealthCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtHealthCheckTest {

    private MyExtHealthCheck check; // objektet vi tester

    private ExtHealthCheck.InternalResult internalResult;

    @BeforeEach
    public void setup() {
        check = new MyExtHealthCheck();
        internalResult = null;
    }

    @Test
    public void test_check_healthy() {
        internalResult = new ExtHealthCheck.InternalResult();
        internalResult.setOk(true);

        HealthCheck.Result result = check.check();

        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getError()).isNull();
        assertThat(result.getMessage()).isNull();
    }

    @Test
    public void test_check_unhealthyWithMessage() {
        internalResult = new ExtHealthCheck.InternalResult();
        internalResult.setOk(false);
        internalResult.setMessage("alltid min feil");
        //internalResult.setException(new RuntimeException(("au")));

        HealthCheck.Result result = check.check();

        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getError()).isNull();
        assertThat(result.getMessage()).isEqualTo("alltid min feil");
    }

    @Test
    public void test_check_unhealthyWithException() {
        internalResult = new ExtHealthCheck.InternalResult();
        internalResult.setOk(false);
        internalResult.setException(new RuntimeException(("auda")));

        HealthCheck.Result result = check.check();

        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getError()).isNotNull();
        assertThat(result.getMessage()).isNotNull();
    }

    //-------

    private class MyExtHealthCheck extends ExtHealthCheck {

        @Override
        protected String getDescription() {
            return "my test";
        }

        @Override
        protected String getEndpoint() {
            return "http://my.test";
        }

        @Override
        protected InternalResult performCheck() {
            return internalResult;
        }
    }
}
