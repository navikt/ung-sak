package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.LocalDate;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class InfotrygdFeedServiceTest {

    private Instance<InfotrygdFeedPeriodeberegner> periodeBeregnere;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;

    private InfotrygdFeedService service;

    @BeforeEach
    public void setUp() throws Exception {
        initMocks(this);
        initServices(LocalDate.now(), LocalDate.now().plusDays(1));
    }

    private void initServices(LocalDate fom, LocalDate tom) {
        InfotrygdFeedPeriodeberegner periodeberegner = new FeedServiceMockHelper.StubPeriodeberegner(fom, tom, "OM");
        periodeBeregnere = new UnitTestLookupInstanceImpl<>(periodeberegner);
        service = new InfotrygdFeedService(prosessTaskRepository);
    }

    @Test
    public void mockHelper_lager_sendbar_melding() {
        Behandling behandling = mockHelper().hentBehandling();
        service.publiserHendelse(behandling);
        verify(prosessTaskRepository).lagre(any(ProsessTaskData.class));
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
        when(prosessTaskRepository.lagre(captor.capture())).thenReturn("urelevant");

        service.publiserHendelse(behandling);

        ProsessTaskData pd = captor.getValue();
        assertThat(pd.getTaskType()).isEqualTo(PubliserInfotrygdFeedElementTask.TASKTYPE);
        assertThat(pd.getPropertyValue(PubliserInfotrygdFeedElementTask.KAFKA_KEY_PROPERTY)).isEqualTo("saksnummer");
        assertThat(pd.getSekvens()).isEqualTo("00099-00088"); // fra "versjonFagsak-versjonBehandling"
        assertThat(pd.getGruppe()).contains(PubliserInfotrygdFeedElementTask.TASKTYPE, "saksnummer");
    }

    @Test
    public void publiserHendelse_ikke_publiser_FRISINN() {
        Behandling behandling = mockHelper()
            .medFagsakYtelsesType(FagsakYtelseType.FRISINN)
            .hentBehandling();

        service.publiserHendelse(behandling);

        Mockito.verifyNoInteractions(prosessTaskRepository);
    }

    @Test(expected = InfotrygdFeedService.ManglendeVerdiException.class)
    public void publiserHendelse_uten_saksnummer() {
        Behandling behandling = mockHelper()
            .medSaksnummer(null)
            .hentBehandling();
        service.publiserHendelse(behandling);
    }

    // === Obligatoriske meldingsfelt ====

    @Test(expected = InfotrygdFeedService.ManglendeVerdiException.class)
    public void publiserHendelse_uten_aktørId() {
        Behandling behandling = mockHelper()
            .medAktørId(null)
            .hentBehandling();
        service.publiserHendelse(behandling);
    }

    private FeedServiceMockHelper mockHelper() {
        return new FeedServiceMockHelper(periodeBeregnere);
    }
}

