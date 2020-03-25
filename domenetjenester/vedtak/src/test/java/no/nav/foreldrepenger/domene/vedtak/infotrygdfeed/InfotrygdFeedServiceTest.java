package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;


import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UtfallType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.domene.uttak.rest.UttakRestKlient;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.InnvilgetUttaksplanperiode;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Periode;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Uttaksplanperiode;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class InfotrygdFeedServiceTest {

    @Mock
    UttakTjeneste uttakTjeneste;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;

    private InfotrygdFeedService service;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        service = new InfotrygdFeedService(uttakTjeneste, prosessTaskRepository);
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
            .medInnvilgetPeriode(førsteStønadsdag, vilkårligDato)
            .medInnvilgetPeriode(vilkårligDato, sisteStønadsdag)
            .medAvslåttPeriode(sisteStønadsdag, sisteStønadsdag.plusMonths(1))
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
    public void publiserHendelse_uten_treff_i_uttak() {
        Behandling behandling = mockHelper()
            .utenUttaksplaner()
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
        return new FeedServiceMockHelper(uttakTjeneste, prosessTaskRepository);
    }
}

class FeedServiceMockHelper {
    // Mocks
    private final UttakTjeneste uttakTjeneste;
    private final ProsessTaskRepository prosessTaskRepository;

    // Builder-parametere
    private Saksnummer saksnummer = new Saksnummer("x123");
    private AktørId aktørId = new AktørId(123L);
    private AktørId aktørIdPleietrengende; // = new AktørId(543L);
    private FagsakYtelseType fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
    private Long versjonFagsak = 1L;
    private Long versjonBehandling = 2L;
    private List<FomTom> perioder = new ArrayList<>();
    private boolean harUttaksplaner = true;

    // Annen tilstand
    private long sisteBehandlingsId = 0L;

    FeedServiceMockHelper(UttakTjeneste uttakTjeneste, ProsessTaskRepository prosessTaskRepository) {
        this.uttakTjeneste = uttakTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
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

    FeedServiceMockHelper medInnvilgetPeriode(LocalDate fom, LocalDate tom) {
        perioder.add(new FomTom(fom, tom, true));
        return this;
    }

    FeedServiceMockHelper medAvslåttPeriode(LocalDate fom, LocalDate tom) {
        perioder.add(new FomTom(fom, tom, false));
        return this;
    }

    FeedServiceMockHelper utenUttaksplaner() {
        harUttaksplaner = false;
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

        if(harUttaksplaner) {
            lagUttaksplaner(saksnummer);
        }

        return mockBehandling(behandlingsId);
    }

    private void lagUttaksplaner(Saksnummer saksnummer) {
        if(saksnummer == null) {
            return;
        }
        Uttaksplan uttaksplan = mockUttaksplan();
        when(uttakTjeneste.hentUttaksplaner(List.of(saksnummer)))
            .thenReturn(Map.of(saksnummer, uttaksplan));
    }

    private Uttaksplan mockUttaksplan() {
        Uttaksplan uttaksplan = mock(Uttaksplan.class);

        NavigableMap<Periode, Uttaksplanperiode> perioder = new TreeMap<>();

        for(FomTom fomTom : this.perioder) {
            Periode periode = new Periode(fomTom.fom, fomTom.tom);
            Uttaksplanperiode uttaksplanperiode = mock(Uttaksplanperiode.class);
            if(fomTom.innvilget) {
                when(uttaksplanperiode.getUtfall()).thenReturn(UtfallType.INNVILGET);
            } else {
                when(uttaksplanperiode.getUtfall()).thenReturn(UtfallType.AVSLÅTT);
            }
            perioder.put(periode, uttaksplanperiode);
        }

        when(uttaksplan.getPerioder()).thenReturn(perioder);

        return uttaksplan;
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



    private static final class FomTom {
        private final LocalDate fom;
        private final LocalDate tom;
        private final boolean innvilget;

        private FomTom(LocalDate fom, LocalDate tom, boolean innvilget) {
            this.fom = fom;
            this.tom = tom;
            this.innvilget = innvilget;
        }
    }
}
