package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public class InfotrygdFeedServiceTest {

    private ProsessTaskTjeneste taskTjeneste;

    private InfotrygdFeedService service;

    @BeforeEach
    public void setUp() throws Exception {
        taskTjeneste = mock(ProsessTaskTjeneste.class);
        service = new InfotrygdFeedService(taskTjeneste);
    }

    @Test
    public void mockHelper_lager_sendbar_melding() {
        Behandling behandling = mockHelper().hentBehandling();
        service.publiserHendelse(behandling);
        verify(taskTjeneste).lagre(any(ProsessTaskData.class));
    }

    @Test
    public void publiserHendelse_med_all_tilgjengelig_info() {
        Behandling behandling = mockHelper()
            .medSaksnummer("saksnummer")
            .medAktørId("123")
            .medAktørIdPleietrengende("321")
            .medFagsakYtelsesType(FagsakYtelseType.OMSORGSPENGER)
            .medVersjonFagsak(99L)
            .medVersjonBehandling(88L)
            .hentBehandling();

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        when(taskTjeneste.lagre(captor.capture())).thenReturn("urelevant");

        service.publiserHendelse(behandling);

        ProsessTaskData pd = captor.getValue();
        assertThat(pd.getTaskType()).isEqualTo(PubliserInfotrygdFeedElementTask.TASKTYPE);
        assertThat(pd.getPropertyValue(PubliserInfotrygdFeedElementTask.KAFKA_KEY_PROPERTY)).isEqualTo("saksnummer");
        assertThat(pd.getSekvens()).isEqualTo("00099-00088"); // fra "versjonFagsak-versjonBehandling"
    }

    @Test
    public void publiserHendelse_ikke_publiser_FRISINN() {
        Behandling behandling = mockHelper()
            .medFagsakYtelsesType(FagsakYtelseType.FRISINN)
            .hentBehandling();

        service.publiserHendelse(behandling);

        Mockito.verifyNoInteractions(taskTjeneste);
    }

    @Test
    public void publiserHendelse_uten_saksnummer() {
        Assertions.assertThrows(InfotrygdFeedService.ManglendeVerdiException.class, () -> {
            Behandling behandling = mockHelper()
                .medSaksnummer(null)
                .hentBehandling();
            service.publiserHendelse(behandling);
        });
    }

    // === Obligatoriske meldingsfelt ====

    @Test
    public void publiserHendelse_uten_aktørId() {
        Assertions.assertThrows(InfotrygdFeedService.ManglendeVerdiException.class, () -> {
            Behandling behandling = mockHelper()
                .medAktørId(null)
                .hentBehandling();
            service.publiserHendelse(behandling);
        });
    }

    private FeedServiceMockHelper mockHelper() {
        var periodeberegner = mock(InfotrygdFeedPeriodeberegner.class);
        return new FeedServiceMockHelper(periodeberegner);
    }
}

