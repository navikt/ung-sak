package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;


import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import javax.enterprise.inject.Instance;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class InfotrygdFeedServiceTest {

    @Mock
    Instance<InfotrygdFeedPeriodeberegner> periodeBeregnere;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;

    private InfotrygdFeedService service;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        service = new InfotrygdFeedService(prosessTaskRepository, periodeBeregnere);
    }

    @Test
    public void mockHelper_lager_sendbar_melding() {
        Behandling behandling = mockHelper().hentBehandling();
        service.publiserHendelse(behandling);
        verify(prosessTaskRepository).lagre(any(ProsessTaskData.class));
    }

    @Test
    public void publiserHendelse_med_all_tilgjengelig_info() {
        LocalDate vilkårligDato = LocalDate.of(2020, 1, 1);
        LocalDate førsteStønadsdag = vilkårligDato.minusMonths(1);
        LocalDate sisteStønadsdag = vilkårligDato.plusMonths(1);

        Behandling behandling = mockHelper()
            .medSaksnummer("saksnummer")
            .medAktørId("123")
            .medAktørIdPleietrengende("321")
            .medFagsakYtelsesType(FagsakYtelseType.OMSORGSPENGER)
            .medOmsorgspengerPeriode(førsteStønadsdag, sisteStønadsdag)
            .medPleiepengerPeriode(vilkårligDato, sisteStønadsdag)
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

        assertThat(pd.getPayload()).isNotNull();
        InfotrygdFeedMessage message = InfotrygdFeedMessage.fromJson(pd.getPayloadAsString());
        assertThat(message.getUuid()).hasSameSizeAs(UUID.randomUUID().toString());
        assertThat(message.getYtelse()).isEqualTo("OM");
        assertThat(message.getSaksnummer()).isEqualTo("saksnummer");
        assertThat(message.getAktoerId()).isEqualTo("123");
        assertThat(message.getAktoerIdPleietrengende()).isEqualTo("321");
        assertThat(message.getFoersteStoenadsdag()).isEqualTo(førsteStønadsdag);
        assertThat(message.getSisteStoenadsdag()).isEqualTo(sisteStønadsdag);
    }

    @Test
    public void publiserHendelse_uten_aktørId_pleietrengende() {
        String aktørId = "123";
        Behandling behandling = mockHelper()
            .medAktørId(aktørId)
            .medAktørIdPleietrengende(null)
            .hentBehandling();

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        when(prosessTaskRepository.lagre(captor.capture())).thenReturn("urelevant");

        service.publiserHendelse(behandling);

        ProsessTaskData pd = captor.getValue();
        InfotrygdFeedMessage message = InfotrygdFeedMessage.fromJson(pd.getPayloadAsString());
        assertThat(message.getAktoerId()).isEqualTo(aktørId);
        assertThat(message.getAktoerIdPleietrengende()).isNull();
    }

    @Test
    public void publiserHendelse_uten_treff_i_tjeneste() {
        Behandling behandling = mockHelper()
            .medFagsakYtelsesType(FagsakYtelseType.OMSORGSPENGER)
            .utenOmsorgspenger()
            .hentBehandling();

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        when(prosessTaskRepository.lagre(captor.capture())).thenReturn("urelevant");

        service.publiserHendelse(behandling);

        ProsessTaskData pd = captor.getValue();
        InfotrygdFeedMessage message = InfotrygdFeedMessage.fromJson(pd.getPayloadAsString());
        assertThat(message.getFoersteStoenadsdag()).isNull();
        assertThat(message.getSisteStoenadsdag()).isNull();
    }

    @Test
    public void publiserHendelse_med_min_max_dato() {
        Behandling behandling = mockHelper()
            .medFagsakYtelsesType(FagsakYtelseType.OMSORGSPENGER)
            .medOmsorgspengerPeriode(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)
            .hentBehandling();

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        when(prosessTaskRepository.lagre(captor.capture())).thenReturn("urelevant");

        service.publiserHendelse(behandling);

        ProsessTaskData pd = captor.getValue();
        InfotrygdFeedMessage message = InfotrygdFeedMessage.fromJson(pd.getPayloadAsString());
        assertThat(message.getFoersteStoenadsdag()).isNull();
        assertThat(message.getSisteStoenadsdag()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void publiserHendelse_feil_ytelse() {
        Behandling behandling = mockHelper()
            .medFagsakYtelsesType(FagsakYtelseType.ARBEIDSAVKLARINGSPENGER)
            .hentBehandling();
        service.publiserHendelse(behandling);
    }

    // ==== ProsessTask-felt ====

    @Test(expected = InfotrygdFeedService.ManglendeVerdiException.class)
    public void publiserHendelse_uten_saksnummer() {
        Behandling behandling = mockHelper()
            .medSaksnummer(null)
            .hentBehandling();
        service.publiserHendelse(behandling);
    }

    @Test(expected = InfotrygdFeedService.ManglendeVerdiException.class)
    public void publiserHendelse_uten_behandlingsversjon() {
        Behandling behandling = mockHelper()
            .medVersjonBehandling(null)
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

class FeedServiceMockHelper {
    // Mocks
    Instance<InfotrygdFeedPeriodeberegner> periodeBeregnere;

    // Builder-parametere
    private Saksnummer saksnummer = new Saksnummer("x123");
    private AktørId aktørId = new AktørId(123L);
    private AktørId aktørIdPleietrengende; // = new AktørId(543L);
    private FagsakYtelseType fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
    private Long versjonFagsak = 1L;
    private Long versjonBehandling = 2L;

    private InfotrygdFeedPeriodeberegner omsorgspengerPeriodeberegner;
    private InfotrygdFeedPeriodeberegner pleiepengerPeriodeberegner;

    // Annen tilstand
    private long sisteBehandlingsId = 0L;

    FeedServiceMockHelper(Instance<InfotrygdFeedPeriodeberegner> periodeBeregnere) {
        this.periodeBeregnere = periodeBeregnere;

        utenOmsorgspenger();
        utenPleiepenger();
    }

    FeedServiceMockHelper medSaksnummer(String saksnummer) {
        if(saksnummer == null) {
            this.saksnummer = null;
        } else {
            this.saksnummer = new Saksnummer(saksnummer);
        }
        return this;
    }

    FeedServiceMockHelper medAktørId(String aktørId) {
        if(aktørId == null) {
            this.aktørId = null;
        } else {
            this.aktørId = new AktørId(aktørId);
        }
        return this;
    }

    FeedServiceMockHelper medAktørIdPleietrengende(String aktørIdPleietrengende) {
        if(aktørIdPleietrengende == null) {
            this.aktørIdPleietrengende = null;
        } else {
            this.aktørIdPleietrengende = new AktørId(aktørIdPleietrengende);
        }
        return this;
    }

    FeedServiceMockHelper medOmsorgspengerPeriode(LocalDate fom, LocalDate tom) {
        omsorgspengerPeriodeberegner = new StubPeriodeberegner(fom, tom, FagsakYtelseType.OMSORGSPENGER, "OM");
        return this;
    }

    FeedServiceMockHelper medPleiepengerPeriode(LocalDate fom, LocalDate tom) {
        pleiepengerPeriodeberegner = new StubPeriodeberegner(fom, tom, FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "PN");
        return this;
    }

    FeedServiceMockHelper utenOmsorgspenger() {
        medOmsorgspengerPeriode(null, null);
        return this;
    }

    FeedServiceMockHelper utenPleiepenger() {
        medPleiepengerPeriode(null, null);
        return this;
    }

    FeedServiceMockHelper medFagsakYtelsesType(FagsakYtelseType fagsakYtelseType) {
        this.fagsakYtelseType = fagsakYtelseType;
        return this;
    }

    FeedServiceMockHelper medVersjonFagsak(Long versjonFagsak) {
        this.versjonFagsak = versjonFagsak;
        return this;
    }

    FeedServiceMockHelper medVersjonBehandling(Long versjonBehandling) {
        this.versjonBehandling = versjonBehandling;
        return this;
    }

    Behandling hentBehandling() {
        long behandlingsId = nesteBehandlingsId();

        when(periodeBeregnere.iterator())
            .thenReturn(List.of(
                omsorgspengerPeriodeberegner,
                pleiepengerPeriodeberegner
            ).iterator());

        return mockBehandling(behandlingsId);
    }

    // ==== Behandling ====

    private Behandling mockBehandling(Long behandlingId) {
        Behandling behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(behandlingId);
        when(behandling.getVersjon()).thenReturn(versjonBehandling);

        Fagsak fagsak = mockFagsak();
        when(behandling.getFagsak()).thenReturn(fagsak);


        return behandling;
    }

    private Fagsak mockFagsak() {
        Fagsak fagsak = mock(Fagsak.class);
        when(fagsak.getYtelseType()).thenReturn(fagsakYtelseType);
        when(fagsak.getSaksnummer()).thenReturn(saksnummer);
        when(fagsak.getAktørId()).thenReturn(aktørId);
        when(fagsak.getPleietrengendeAktørId()).thenReturn(aktørIdPleietrengende);
        when(fagsak.getVersjon()).thenReturn(versjonFagsak);
        return fagsak;
    }

    private long nesteBehandlingsId() {
        return sisteBehandlingsId++;
    }

    private static final class StubPeriodeberegner implements InfotrygdFeedPeriodeberegner {

        private final LocalDate fom;
        private final LocalDate tom;
        private final FagsakYtelseType fagsakYtelseType;
        private final String infotrygdYtelseKode;

        private StubPeriodeberegner(LocalDate fom, LocalDate tom, FagsakYtelseType fagsakYtelseType, String infotrygdYtelseKode) {
            this.fagsakYtelseType = fagsakYtelseType;
            this.infotrygdYtelseKode = infotrygdYtelseKode;
            this.fom = fom;
            this.tom = tom;
        }

        @Override
        public FagsakYtelseType getFagsakYtelseType() {
            return fagsakYtelseType;
        }

        @Override
        public String getInfotrygdYtelseKode() {
            return infotrygdYtelseKode;
        }

        @Override
        public InfotrygdFeedPeriode finnInnvilgetPeriode(Saksnummer saksnummer) {
            return new InfotrygdFeedPeriode(fom, tom);
        }
    }
}
