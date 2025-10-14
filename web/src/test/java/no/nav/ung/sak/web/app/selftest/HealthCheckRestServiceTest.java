package no.nav.ung.sak.web.app.selftest;

import jakarta.ws.rs.core.Response;
import no.nav.ung.sak.web.app.tjenester.ApplicationServiceStarter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SuppressWarnings("resource")
public class HealthCheckRestServiceTest {

    private HealthCheckRestService restTjeneste;

    private ApplicationServiceStarter serviceStarterMock = mock(ApplicationServiceStarter.class);
    private SelftestService selftestServiceMock = mock(SelftestService.class);

    @BeforeEach
    public void setup() {
        restTjeneste = new HealthCheckRestService(serviceStarterMock, selftestServiceMock);
    }

    @Test
    public void test_isAlive_skal_returnere_status_200() {
        Response response = restTjeneste.isAlive();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void test_isReady_skal_returnere_service_unavailable_når_kritiske_selftester_feiler() {
        when(selftestServiceMock.kritiskTjenesteFeilet()).thenReturn(true);

        Response response = restTjeneste.isReady();

        assertThat(response.getStatus()).isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
    }

    @Test
    public void test_isReady_skal_returnere_status_ok_når_selftester_er_ok() {
        when(selftestServiceMock.kritiskTjenesteFeilet()).thenReturn(false);

        Response response = restTjeneste.isReady();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void test_preStop_skal_kalle_stopServices_og_returnere_status_ok() {
        Response response = restTjeneste.preStop();

        verify(serviceStarterMock).stopServices();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }
}
