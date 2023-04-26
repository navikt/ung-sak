package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.AnnenPart;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class PleiepengerEndretUtbetalingPeriodeutlederTest {

    public static final String ORGANISASJONSNUMMER = "123456789";
    public static final String ORGANISASJONSNUMMER2 = "123456788";

    public static final Arbeidsforhold ARBEIDSFORHOLD_1 = new Arbeidsforhold(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD.getKode(),
        ORGANISASJONSNUMMER, null, null);
    public static final Arbeidsforhold ARBEIDSFORHOLD_2 = new Arbeidsforhold(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD.getKode(),
        ORGANISASJONSNUMMER2, null, null);
    public static final Duration SJU_OG_EN_HALV_TIME = Duration.ofHours(7).plusMinutes(30);
    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    public static final String JOURNALPOST_ID = "123567324234";
    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private Behandling behandling;

    private Behandling originalBehandling;


    private UttakTjeneste uttakTjeneste = mock(UttakTjeneste.class);

    private SøknadsperiodeRepository søknadsperiodeRepository;

    @Inject
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;

    private PleiepengerEndretUtbetalingPeriodeutleder utleder;
    private MottatteDokumentRepository mottatteDokumentRepository;

    @BeforeEach
    void setUp() {
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        søknadsperiodeRepository = new SøknadsperiodeRepository(entityManager);
        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        utleder = new PleiepengerEndretUtbetalingPeriodeutleder(uttakTjeneste, behandlingRepository, null, søknadsperiodeRepository,
            new ProsessTriggereRepository(entityManager), søknadsperiodeTjeneste, true);
        originalBehandling = opprettBehandling(SKJÆRINGSTIDSPUNKT);
        behandling = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(behandling, new BehandlingLås(null));

        mottatteDokumentRepository.lagre(byggMottattDokument(behandling.getFagsakId()), DokumentStatus.GYLDIG);
    }

    @Test
    void skal_gi_tom_periode_ved_ingen_endring() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var antallDager = 10;

        Uttaksplan uttaksplan = lagUttaksplanEnPeriode(fom, antallDager, List.of(fullUtbetaling(ARBEIDSFORHOLD_1)));

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplan);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplan);

        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

        assertThat(forlengelseperioder.size()).isEqualTo(0);
    }

    @Test
    void skal_returnere_relevante_søknadsperioder() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var dagerEtterSTPSøknadFom = 5;
        var antallDager = 10;

        Uttaksplan uttaksplan = lagUttaksplanEnPeriode(fom, antallDager, List.of(fullUtbetaling(ARBEIDSFORHOLD_1)));

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplan);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplan);


        var søknadsperiode = new Søknadsperiode(SKJÆRINGSTIDSPUNKT.plusDays(dagerEtterSTPSøknadFom), SKJÆRINGSTIDSPUNKT.plusDays(antallDager));
        var søknadsperioder = new Søknadsperioder(new JournalpostId(JOURNALPOST_ID), søknadsperiode);
        søknadsperiodeRepository.lagre(behandling.getId(), søknadsperioder);
        søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandling.getId(), new SøknadsperioderHolder(søknadsperioder));
        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT.plusDays(dagerEtterSTPSøknadFom));
        assertThat(periode.getTomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT.plusDays(antallDager));
    }

    @Test
    void skal_returnere_tom_liste_for_søknadsendringer_utenfor_periode() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var dagerEtterSTPSøknadFom = 5;
        var antallDager = 10;

        Uttaksplan uttaksplan = lagUttaksplanEnPeriode(fom, antallDager, List.of(fullUtbetaling(ARBEIDSFORHOLD_1)));

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplan);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplan);


        var søknadsperiode = new Søknadsperiode(SKJÆRINGSTIDSPUNKT.plusDays(dagerEtterSTPSøknadFom), SKJÆRINGSTIDSPUNKT.plusDays(antallDager));
        var søknadsperioder = new Søknadsperioder(new JournalpostId(JOURNALPOST_ID), søknadsperiode);
        søknadsperiodeRepository.lagre(behandling.getId(), søknadsperioder);
        søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandling.getId(), new SøknadsperioderHolder(søknadsperioder));
        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, fom.plusDays(dagerEtterSTPSøknadFom - 4)));

        assertThat(forlengelseperioder.size()).isEqualTo(0);
    }

    @Test
    void skal_gi_en_periode_ved_endring_i_utbetalingsgrad_i_hele_perioden() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var antallDager = 10;

        Uttaksplan uttaksplan = lagUttaksplanEnPeriode(fom, antallDager, List.of(fullUtbetaling(ARBEIDSFORHOLD_1)));

        Uttaksplan uttaksplan2 = lagUttaksplanEnPeriode(fom, antallDager, List.of(delvisUtbetaling(ARBEIDSFORHOLD_1, BigDecimal.valueOf(50))));

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplan);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplan2);

        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(fom);
        assertThat(periode.getTomDato()).isEqualTo(fom.plusDays(antallDager));
    }

    @Test
    void skal_gi_en_periode_ved_endring_i_utbetalingsgrad_i_deler_av_perioden() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var antallDager = 10;
        var antallDagerTilEndring = 6;

        Uttaksplan uttaksplanOriginalBehandling = lagUttaksplanEnPeriode(fom, antallDager, List.of(fullUtbetaling(ARBEIDSFORHOLD_1)));


        Map<LukketPeriode, UttaksperiodeInfo> perioder2 = new HashMap<>();
        List<Utbetalingsgrader> utbetalingsgraderPeriode1 = List.of(fullUtbetaling(ARBEIDSFORHOLD_1));
        leggTilPeriode(perioder2, fom, utbetalingsgraderPeriode1, antallDagerTilEndring - 1);
        List<Utbetalingsgrader> utbetalingsgraderPeriode2 = List.of(delvisUtbetaling(ARBEIDSFORHOLD_1, BigDecimal.valueOf(50)));
        leggTilPeriode(perioder2, fom.plusDays(antallDagerTilEndring), utbetalingsgraderPeriode2, antallDager - antallDagerTilEndring);
        Uttaksplan uttaksplanForlengelse = new Uttaksplan(perioder2, List.of());

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplanForlengelse);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplanOriginalBehandling);

        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(fom.plusDays(antallDagerTilEndring));
        assertThat(periode.getTomDato()).isEqualTo(fom.plusDays(antallDager));
    }

    @Test
    void skal_gi_en_periode_ved_fjernet_arbeidsforhold_i_deler_av_perioden() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var antallDager = 10;
        var antallDagerTilEndring = 6;

        Uttaksplan uttaksplanOriginalBehandling = lagUttaksplanEnPeriode(fom, antallDager, List.of(fullUtbetaling(ARBEIDSFORHOLD_1), fullUtbetaling(ARBEIDSFORHOLD_2)));


        Map<LukketPeriode, UttaksperiodeInfo> perioder2 = new HashMap<>();
        List<Utbetalingsgrader> utbetalingsgraderPeriode1 = List.of(fullUtbetaling(ARBEIDSFORHOLD_1), fullUtbetaling(ARBEIDSFORHOLD_2));
        leggTilPeriode(perioder2, fom, utbetalingsgraderPeriode1, antallDagerTilEndring - 1);
        List<Utbetalingsgrader> utbetalingsgraderPeriode2 = List.of(fullUtbetaling(ARBEIDSFORHOLD_1));
        leggTilPeriode(perioder2, fom.plusDays(antallDagerTilEndring), utbetalingsgraderPeriode2, antallDager - antallDagerTilEndring);
        Uttaksplan uttaksplanForlengelse = new Uttaksplan(perioder2, List.of());

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplanForlengelse);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplanOriginalBehandling);

        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(fom.plusDays(antallDagerTilEndring));
        assertThat(periode.getTomDato()).isEqualTo(fom.plusDays(antallDager));
    }

    @Test
    void skal_gi_en_periode_ved_nytt_arbeidsforhold_i_deler_av_perioden() {
        var fom = SKJÆRINGSTIDSPUNKT;
        var antallDager = 10;
        var antallDagerTilEndring = 6;

        Uttaksplan uttaksplanOriginalBehandling = lagUttaksplanEnPeriode(fom, antallDager, List.of(fullUtbetaling(ARBEIDSFORHOLD_1)));


        Map<LukketPeriode, UttaksperiodeInfo> perioder2 = new HashMap<>();
        List<Utbetalingsgrader> utbetalingsgraderPeriode1 = List.of(fullUtbetaling(ARBEIDSFORHOLD_1));
        leggTilPeriode(perioder2, fom, utbetalingsgraderPeriode1, antallDagerTilEndring - 1);
        List<Utbetalingsgrader> utbetalingsgraderPeriode2 = List.of(fullUtbetaling(ARBEIDSFORHOLD_1), fullUtbetaling(ARBEIDSFORHOLD_2));
        leggTilPeriode(perioder2, fom.plusDays(antallDagerTilEndring), utbetalingsgraderPeriode2, antallDager - antallDagerTilEndring);
        Uttaksplan uttaksplanForlengelse = new Uttaksplan(perioder2, List.of());

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplanForlengelse);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplanOriginalBehandling);

        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(fom.plusDays(antallDagerTilEndring));
        assertThat(periode.getTomDato()).isEqualTo(fom.plusDays(antallDager));
    }

    @Test
    void skal_gi_en_periode_ved_trukket_periode() {
        var fom = SKJÆRINGSTIDSPUNKT;

        Uttaksplan uttaksplan = lagUttaksplanEnPeriode(fom, 15, List.of(fullUtbetaling(ARBEIDSFORHOLD_1)));

        Uttaksplan uttaksplan2 = lagUttaksplanEnPeriode(fom, 10, List.of(fullUtbetaling(ARBEIDSFORHOLD_1)));

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplan);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplan2);

        var forlengelseperioder = utleder.utledPerioder(
            BehandlingReferanse.fra(behandling),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(10)));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(fom.plusDays(11));
        assertThat(periode.getTomDato()).isEqualTo(fom.plusDays(15));
    }


    @Test
    void skal_gi_en_periode_ved_endring_i_utbetalingsgrad_i_hele_perioden_bortsett_fra_helg() {
        var fom = SKJÆRINGSTIDSPUNKT.with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        Map<LukketPeriode, UttaksperiodeInfo> perioder = new HashMap<>();
        List<Utbetalingsgrader> utbetalingsgraderPeriode1 = List.of(fullUtbetaling(ARBEIDSFORHOLD_1));
        leggTilPeriode(perioder, fom, utbetalingsgraderPeriode1, 4);
        List<Utbetalingsgrader> utbetalingsgraderPeriode2 = List.of(delvisUtbetaling(ARBEIDSFORHOLD_1, BigDecimal.valueOf(0)));
        leggTilPeriode(perioder, fom.plusDays(5), utbetalingsgraderPeriode2, 1);
        leggTilPeriode(perioder, fom.plusDays(7), utbetalingsgraderPeriode1, 4);
        Uttaksplan uttaksplan = new Uttaksplan(perioder, List.of());

        Map<LukketPeriode, UttaksperiodeInfo> perioder_forlengelse = new HashMap<>();
        List<Utbetalingsgrader> utbetalingsgraderPeriode1_forlengelse = List.of(delvisUtbetaling(ARBEIDSFORHOLD_1, BigDecimal.valueOf(50)));
        leggTilPeriode(perioder_forlengelse, fom, utbetalingsgraderPeriode1_forlengelse, 4);
        leggTilPeriode(perioder_forlengelse, fom.plusDays(5), utbetalingsgraderPeriode2, 1);
        leggTilPeriode(perioder_forlengelse, fom.plusDays(7), utbetalingsgraderPeriode1_forlengelse, 4);
        Uttaksplan uttaksplan_forlengelse = new Uttaksplan(perioder_forlengelse, List.of());

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplan_forlengelse);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplan);


        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(11)));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(fom);
        assertThat(periode.getTomDato()).isEqualTo(fom.plusDays(11));
    }


    private Uttaksplan lagUttaksplanEnPeriode(LocalDate fom, int antallDager, List<Utbetalingsgrader> utbetalingsgrader) {
        Map<LukketPeriode, UttaksperiodeInfo> perioder = new HashMap<>();
        leggTilPeriode(perioder, fom, utbetalingsgrader, antallDager);
        return new Uttaksplan(perioder, List.of());
    }

    private Utbetalingsgrader fullUtbetaling(Arbeidsforhold arbeidsforhold) {
        return new Utbetalingsgrader(arbeidsforhold, SJU_OG_EN_HALV_TIME, Duration.ZERO, BigDecimal.valueOf(100));
    }

    private Utbetalingsgrader delvisUtbetaling(Arbeidsforhold arbeidsforhold, BigDecimal utbetalingsgrad) {
        return new Utbetalingsgrader(arbeidsforhold, SJU_OG_EN_HALV_TIME, Duration.ZERO, utbetalingsgrad);
    }

    private void leggTilPeriode(Map<LukketPeriode, UttaksperiodeInfo> perioder, LocalDate fom, List<Utbetalingsgrader> utbetalingsgrader, int antallDager) {
        perioder.put(new LukketPeriode(fom, fom.plusDays(antallDager)),
            new UttaksperiodeInfo(Utfall.OPPFYLT, BigDecimal.valueOf(100), utbetalingsgrader,
                null, null, Set.of(),
                new HashMap<>(), BigDecimal.valueOf(100), null,
                Set.of(), UUID.randomUUID().toString(),
                AnnenPart.ALENE, null, null, null,
                false, null));
    }

    private Behandling opprettBehandling(@SuppressWarnings("unused") LocalDate skjæringstidspunkt) {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            AktørId.dummy(), new Saksnummer("SAK"), skjæringstidspunkt, skjæringstidspunkt.plusDays(3));
        @SuppressWarnings("unused")
        Long fagsakId = fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }


    public static MottattDokument byggMottattDokument(Long fagsakId) {
        MottattDokument.Builder builder = new MottattDokument.Builder();
        builder.medMottattDato(LocalDate.now());
        builder.medType(Brevkode.PLEIEPENGER_BARN_SOKNAD);
        builder.medPayload("payload");
        builder.medFagsakId(fagsakId);
        builder.medJournalPostId(new JournalpostId(JOURNALPOST_ID));
        return builder.build();
    }

}
