package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Pleietrengende;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiebehov.PleiebehovResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiebehov.PleiebehovResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiebehov.Pleieperiode;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiebehov.Pleieperioder;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class InfotrygdFeedServiceTest {

    @Mock
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;

    @Mock
    private PleiebehovResultatRepository pleiebehovResultatRepository;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;

    private InfotrygdFeedService service;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        service = new InfotrygdFeedService(medisinskGrunnlagRepository, pleiebehovResultatRepository, prosessTaskRepository);
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
            .medPeriode(førsteStønadsdag, vilkårligDato)
            .medPeriode(vilkårligDato, sisteStønadsdag)
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
    public void publiserHendelse_uten_resultat_i_medisinskGrunnlagRepository() {
        Behandling behandling = mockHelper()
            .utenMedisinskGrunnlag()
            .hentBehandling();

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        when(prosessTaskRepository.lagre(captor.capture())).thenReturn("urelevant");

        service.publiserHendelse(behandling);

        ProsessTaskData pd = captor.getValue();
        InfotrygdFeedMessage message = InfotrygdFeedMessage.fromJson(pd.getPayloadAsString());
        assertThat(message.getAktoerIdPleietrengende()).isNull();
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

    // todo: uten resultat i pleiebehovResultatRepository

    // todo: tester for manglende treff i repo og nullverdier
    // todo: hvordan sjekker jeg at en periode er annulert ???????

    private FeedServiceMockHelper mockHelper() {
        return new FeedServiceMockHelper(medisinskGrunnlagRepository, pleiebehovResultatRepository, prosessTaskRepository);
    }
}

class FeedServiceMockHelper {
    // Mocks
    private final MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private final PleiebehovResultatRepository pleiebehovResultatRepository;
    private final ProsessTaskRepository prosessTaskRepository;

    // Builder-parametere
    private Saksnummer saksnummer = new Saksnummer("x123");
    private AktørId aktørId = new AktørId(123L);
    private AktørId aktørIdPleietrengende = new AktørId(543L);
    private FagsakYtelseType fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
    private Long versjonFagsak = 1L;
    private Long versjonBehandling = 2L;
    private List<FomTom> perioder = new ArrayList<>();
    private boolean harMedisinskGrunnlag = true;
    private boolean harPleiebehovResultat = true;

    // Annen tilstand
    private long sisteBehandlingsId = 0L;

    FeedServiceMockHelper(MedisinskGrunnlagRepository medisinskGrunnlagRepository, PleiebehovResultatRepository pleiebehovResultatRepository, ProsessTaskRepository prosessTaskRepository) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
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
        this.aktørIdPleietrengende = new AktørId(aktørIdPleietrengende);
        return this;
    }

    FeedServiceMockHelper medPeriode(LocalDate fom, LocalDate tom) {
        perioder.add(new FomTom(fom, tom));
        return this;
    }

    FeedServiceMockHelper utenMedisinskGrunnlag() {
        harMedisinskGrunnlag = false;
        return this;
    }

    FeedServiceMockHelper utenPleiebehovResultat() {
        harPleiebehovResultat = false;
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

        if(harPleiebehovResultat) {
            mockPleiebehov(behandlingsId);
        }

        if(harMedisinskGrunnlag) {
            mockMedisinskGrunnlag(behandlingsId);
        }

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
        when(fagsak.getVersjon()).thenReturn(versjonFagsak);
        return fagsak;
    }

    private long nesteBehandlingsId() {
        return sisteBehandlingsId++;
    }

    // ==== MedisinskGrunnlag ====

    private void mockMedisinskGrunnlag(long behandlingsId) {
        MedisinskGrunnlag medisinskGrunnlag = mock(MedisinskGrunnlag.class);
        when(medisinskGrunnlagRepository.hent(behandlingsId)).thenReturn(medisinskGrunnlag);

        Pleietrengende pleietrengende = mockPleietrengende();
        when(medisinskGrunnlag.getPleietrengende()).thenReturn(pleietrengende);
    }

    private Pleietrengende mockPleietrengende() {
        Pleietrengende pleietrengende = mock(Pleietrengende.class);
        when(pleietrengende.getAktørId()).thenReturn(aktørIdPleietrengende);
        return pleietrengende;
    }

    // ==== Pleiebehov ====
    private void mockPleiebehov(long behandlingsId) {
        PleiebehovResultat pleiebehovResultat = mock(PleiebehovResultat.class);
        when(pleiebehovResultatRepository.hent(behandlingsId)).thenReturn(pleiebehovResultat);

        Pleieperioder pleieperioder = mock(Pleieperioder.class);
        when(pleiebehovResultat.getPleieperioder()).thenReturn(pleieperioder);

        List<Pleieperiode> perioder = this.perioder.stream()
            .map(this::mockPleieperiode)
            .collect(Collectors.toList());
        when(pleieperioder.getPerioder()).thenReturn(perioder);
    }

    private Pleieperiode mockPleieperiode(FomTom fomTom) {
        Pleieperiode pleieperiode = mock(Pleieperiode.class);
        when(pleieperiode.getPeriode()).thenReturn(DatoIntervallEntitet.fraOgMedTilOgMed(fomTom.fom, fomTom.tom));
        return pleieperiode;
    }


    private static class FomTom {
        private final LocalDate fom;
        private final LocalDate tom;

        private FomTom(LocalDate fom, LocalDate tom) {
            this.fom = fom;
            this.tom = tom;
        }
    }
}
