package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.kafka.InfotrygdFeedMeldingProducer;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class PubliserInfotrygdFeedElementTaskTest {

    @Mock
    private InfotrygdFeedMeldingProducer meldingProducer;
    @Mock
    private BehandlingRepository behandlingRepository;

    private PubliserInfotrygdFeedElementTask task;
    private InfotrygdFeedPeriodeberegner periodeberegner;

    @BeforeEach
    public void setUp() throws Exception {
        initMocks(this);
        var behandling = Behandling.nyBehandlingFor(Fagsak.opprettNy(FagsakYtelseType.OMSORGSPENGER, AktørId.dummy(), new Saksnummer("1234")), BehandlingType.FØRSTEGANGSSØKNAD).build();
        when(behandlingRepository.hentBehandling(any(String.class))).thenReturn(behandling);
        initServices(LocalDate.now(), LocalDate.now().plusDays(1));
    }

    private void initServices(LocalDate fom, LocalDate tom) {
        periodeberegner = mock(InfotrygdFeedPeriodeberegner.class);
        when(periodeberegner.finnInnvilgetPeriode(any())).thenReturn(new InfotrygdFeedPeriode(fom, tom));
        task = new PubliserInfotrygdFeedElementTask(behandlingRepository, meldingProducer, periodeberegner);
    }

    @Test
    public void skal_sende_melding() {
        ProsessTaskData pd = new ProsessTaskData(PubliserInfotrygdFeedElementTask.TASKTYPE);
        pd.setBehandling("", "", "");
        pd.setProperty(PubliserInfotrygdFeedElementTask.KAFKA_KEY_PROPERTY, "kafka-key");
        pd.setPayload("payload");

        task.doTask(pd);

        verify(meldingProducer).send(any(String.class), any(String.class));
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

        InfotrygdFeedMessage message = task.getInfotrygdFeedMessage(behandling);
        assertThat(message.getUuid()).hasSameSizeAs(UUID.randomUUID().toString());
        assertThat(message.getYtelse()).isEqualTo(FagsakYtelseType.OMSORGSPENGER.getInfotrygdBehandlingstema());
        assertThat(message.getSaksnummer()).isEqualTo("saksnummer");
        assertThat(message.getAktoerId()).isEqualTo("123");
        assertThat(message.getAktoerIdPleietrengende()).isEqualTo("321");
        assertThat(message.getFoersteStoenadsdag()).isEqualTo(LocalDate.now());
        assertThat(message.getSisteStoenadsdag()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    public void publiserHendelse_uten_aktørId_pleietrengende() {
        String aktørId = "123";
        Behandling behandling = mockHelper()
            .medAktørId(aktørId)
            .medAktørIdPleietrengende(null)
            .hentBehandling();

        InfotrygdFeedMessage message = task.getInfotrygdFeedMessage(behandling);
        assertThat(message.getAktoerId()).isEqualTo(aktørId);
        assertThat(message.getAktoerIdPleietrengende()).isNull();
    }

    @Test
    public void publiserHendelse_uten_treff_i_tjeneste() {
        initServices(null, null);
        Behandling behandling = mockHelper()
            .medFagsakYtelsesType(FagsakYtelseType.OMSORGSPENGER)
            .hentBehandling();

        InfotrygdFeedMessage message = task.getInfotrygdFeedMessage(behandling);
        assertThat(message.getFoersteStoenadsdag()).isNull();
        assertThat(message.getSisteStoenadsdag()).isNull();
    }

    @Test
    public void publiserHendelse_med_min_max_dato() {
        initServices(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);
        Behandling behandling = mockHelper()
            .medFagsakYtelsesType(FagsakYtelseType.OMSORGSPENGER)
            .hentBehandling();

        InfotrygdFeedMessage message = task.getInfotrygdFeedMessage(behandling);
        assertThat(message.getFoersteStoenadsdag()).isNull();
        assertThat(message.getSisteStoenadsdag()).isNull();
    }

    private FeedServiceMockHelper mockHelper() {
        return new FeedServiceMockHelper(periodeberegner);
    }
}

class FeedServiceMockHelper {
    // Mocks
    InfotrygdFeedPeriodeberegner periodeBeregnere;

    // Builder-parametere
    private Saksnummer saksnummer = new Saksnummer("x123");
    private AktørId aktørId = new AktørId(123L);
    private AktørId aktørIdPleietrengende; // = new AktørId(543L);
    private FagsakYtelseType fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
    private Long versjonFagsak = 1L;
    private Long versjonBehandling = 2L;

    // Annen tilstand
    private long sisteBehandlingsId = 0L;

    FeedServiceMockHelper(InfotrygdFeedPeriodeberegner periodeBeregnere) {
        this.periodeBeregnere = periodeBeregnere;

    }

    FeedServiceMockHelper medSaksnummer(String saksnummer) {
        if (saksnummer == null) {
            this.saksnummer = null;
        } else {
            this.saksnummer = new Saksnummer(saksnummer);
        }
        return this;
    }

    FeedServiceMockHelper medAktørId(String aktørId) {
        if (aktørId == null) {
            this.aktørId = null;
        } else {
            this.aktørId = new AktørId(aktørId);
        }
        return this;
    }

    FeedServiceMockHelper medAktørIdPleietrengende(String aktørIdPleietrengende) {
        if (aktørIdPleietrengende == null) {
            this.aktørIdPleietrengende = null;
        } else {
            this.aktørIdPleietrengende = new AktørId(aktørIdPleietrengende);
        }
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

        return mockBehandling(behandlingsId);
    }

    // ==== Behandling ====

    private Behandling mockBehandling(Long behandlingId) {
        var fagsak1 = Fagsak.opprettNy(fagsakYtelseType, aktørId, saksnummer);
        fagsak1.setPleietrengende(aktørIdPleietrengende);

        Behandling behandling = Behandling.nyBehandlingFor(fagsak1, BehandlingType.FØRSTEGANGSSØKNAD).build();
        try {
            getField(fagsak1.getClass(), "id").set(fagsak1, behandlingId);
            getField(fagsak1.getClass(), "versjon").set(fagsak1, versjonFagsak);
            getField(behandling.getClass(), "id").set(behandling, behandlingId);
            if (versjonBehandling != null) {
                getField(behandling.getClass(), "versjon").set(behandling, versjonBehandling);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        return behandling;
    }

    @NotNull
    private Field getField(Class<?> classToBeManipuliated, String fieltName) throws NoSuchFieldException {
        var field = classToBeManipuliated.getDeclaredField(fieltName);
        field.setAccessible(true);
        return field;
    }

    private long nesteBehandlingsId() {
        return sisteBehandlingsId++;
    }

}
