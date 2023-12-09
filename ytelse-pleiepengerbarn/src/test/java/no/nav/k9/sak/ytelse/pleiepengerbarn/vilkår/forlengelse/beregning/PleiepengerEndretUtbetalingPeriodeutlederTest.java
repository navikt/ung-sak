package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static java.util.Collections.emptyNavigableSet;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.TreeSet;
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
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
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

    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste = mock(MapInputTilUttakTjeneste.class);


    private SøknadsperiodeRepository søknadsperiodeRepository;

    @Inject
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;

    private PleiepengerEndretUtbetalingPeriodeutleder utleder;
    private MottatteDokumentRepository mottatteDokumentRepository;

    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = mock();

    @BeforeEach
    void setUp() {
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        søknadsperiodeRepository = new SøknadsperiodeRepository(entityManager);
        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        utleder = new PleiepengerEndretUtbetalingPeriodeutleder(uttakTjeneste, behandlingRepository, new UnitTestLookupInstanceImpl<>(vilkårsPerioderTilVurderingTjeneste),
            new ProsessTriggereRepository(entityManager), søknadsperiodeTjeneste, new UttakNyeReglerRepository(entityManager), mapInputTilUttakTjeneste);
        originalBehandling = opprettBehandling(SKJÆRINGSTIDSPUNKT);
        behandling = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(behandling, new BehandlingLås(null));

        mottatteDokumentRepository.lagre(byggMottattDokument(behandling.getFagsakId()), DokumentStatus.GYLDIG);

        when(vilkårsPerioderTilVurderingTjeneste.utled(any(), any())).thenReturn(emptyNavigableSet());
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

    //hull på 10 dager mellom stp1 og stp2 periodene inkl fom og tom
        /*
          beh1    |---|     |----|
          ben2         |---|
          res          |---------|
         */
    @Test
    void skal_inkludere_uttak_uten_endring_hvis_kant_i_kant_med_tidligere_stp() {
        var stp1 = SKJÆRINGSTIDSPUNKT;
        var tom1 = stp1.plusDays(5);

        var fomHull = tom1.plusDays(1);
        var tomHull = fomHull.plusDays(7);

        var stp2 = tomHull.plusDays(1);
        var tom2 = stp2.plusDays(20);

        var utbPeriode = List.of(fullUtbetaling(ARBEIDSFORHOLD_1));

        Map<LukketPeriode, UttaksperiodeInfo> perioder = new HashMap<>();
        leggTilPeriode(perioder, utbPeriode, stp1, tom1);
        leggTilPeriode(perioder, utbPeriode, stp2, tom2);
        Uttaksplan uttaksplanOriginal = new Uttaksplan(perioder, List.of());

        Map<LukketPeriode, UttaksperiodeInfo> perioder_tette_hull = new HashMap<>();
        leggTilPeriode(perioder_tette_hull, utbPeriode, stp1, tom1);
        leggTilPeriode(perioder_tette_hull, utbPeriode, fomHull, tomHull);
        leggTilPeriode(perioder_tette_hull, utbPeriode, stp2, tom2);
        Uttaksplan uttaksplan_tette_hull = new Uttaksplan(perioder_tette_hull, List.of());

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplan_tette_hull);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplanOriginal);

        when(vilkårsPerioderTilVurderingTjeneste.utled(originalBehandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>(List.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(stp1, tom1),
                DatoIntervallEntitet.fraOgMedTilOgMed(stp2, tom2)
            )));


        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(stp1, tom2));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(fomHull);
        assertThat(periode.getTomDato()).isEqualTo(tom2);
    }

    //hull på 10 dager mellom stp1 og stp2 periodene inkl fom og tom
    //men tettes bare delvis
        /*
          beh1        |---|           |----|
          ben2               |---|
          resultat           |---|
         */
    @Test
    void skal_inkludere_uttak_kun_med_endring_hvis_ikke_kant_i_kant_fra_tidligere_stp() {
        var stp1 = SKJÆRINGSTIDSPUNKT;
        var tom1 = stp1.plusDays(5);

        var fomHull = tom1.plusDays(3);
        var tomHull = fomHull.plusDays(4);

        var stp2 = tomHull.plusDays(3);
        var tom2 = stp2.plusDays(20);

        var utbPeriode = List.of(fullUtbetaling(ARBEIDSFORHOLD_1));

        Map<LukketPeriode, UttaksperiodeInfo> perioder = new HashMap<>();
        leggTilPeriode(perioder, utbPeriode, stp1, tom1);
        leggTilPeriode(perioder, utbPeriode, stp2, tom2);
        Uttaksplan uttaksplanOriginal = new Uttaksplan(perioder, List.of());

        Map<LukketPeriode, UttaksperiodeInfo> perioder_tette_hull = new HashMap<>();
        leggTilPeriode(perioder_tette_hull, utbPeriode, stp1, tom1);
        leggTilPeriode(perioder_tette_hull, utbPeriode, fomHull, tomHull);
        leggTilPeriode(perioder_tette_hull, utbPeriode, stp2, tom2);
        Uttaksplan uttaksplan_tette_hull = new Uttaksplan(perioder_tette_hull, List.of());

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplan_tette_hull);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplanOriginal);

        when(vilkårsPerioderTilVurderingTjeneste.utled(originalBehandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>(List.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(stp1, tom1),
                DatoIntervallEntitet.fraOgMedTilOgMed(stp2, tom2)
            )));


        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(stp1, tom2));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(fomHull);
        assertThat(periode.getTomDato()).isEqualTo(tomHull);
    }

    /*
    beh1                                   |------------------|
    beh 2                         |-------|        |---|

    resultat:                     |---------------------------|
     */
    @Test
    void skal_håndtere_kant_i_kant_stp_og_overlapp() {
        var stp1 = SKJÆRINGSTIDSPUNKT;
        var tom1 = stp1.plusDays(5);

        var fomHull = tom1.plusDays(1);
        var tomHull = fomHull.plusDays(4);

        var stp2 = tomHull.plusDays(1);
        var tom2 = stp2.plusDays(20);

        var stp0 = fomHull;
        var tom0 = tom2.plusDays(10);

        var utbPeriode = List.of(fullUtbetaling(ARBEIDSFORHOLD_1));

        Map<LukketPeriode, UttaksperiodeInfo> perioder = new HashMap<>();
        leggTilPeriode(perioder, utbPeriode, stp0, tom0);
        Uttaksplan uttaksplanOriginal = new Uttaksplan(perioder, List.of());

        Map<LukketPeriode, UttaksperiodeInfo> perioder_endring = new HashMap<>();
        leggTilPeriode(perioder_endring, utbPeriode, stp1, tom1);
        leggTilPeriode(perioder_endring, utbPeriode, fomHull, tomHull);
        leggTilPeriode(perioder_endring, List.of(delvisUtbetaling(ARBEIDSFORHOLD_1, BigDecimal.valueOf(60))), stp2, tom2);
        leggTilPeriode(perioder_endring, utbPeriode, tom2.plusDays(1), tom0);
        Uttaksplan uttaksplan_tette_hull = new Uttaksplan(perioder_endring, List.of());

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplan_tette_hull);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplanOriginal);

        when(vilkårsPerioderTilVurderingTjeneste.utled(originalBehandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>(List.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(stp0, tom0)
            )));


        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(stp1, tom2));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(stp1);
        assertThat(periode.getTomDato()).isEqualTo(tom0);
    }

    /*
    beh1                                   |------------------|
    beh 2                                             |----|  |---|
    resultat                                          |----|  |---|
     */
    @Test
    void skal_håndtere_endring_og_forlengelse() {
        var stp1 = SKJÆRINGSTIDSPUNKT;
        var tom1 = stp1.plusDays(20);

        var fomEndring = stp1.plusDays(2);
        var tomEndring = fomEndring.plusDays(4);

        var fomForlengelse = tom1.plusDays(1);
        var tomForlengelse = fomForlengelse.plusDays(20);

        var original = List.of(delvisUtbetaling(ARBEIDSFORHOLD_1, BigDecimal.valueOf(60)));
        var endring = List.of(fullUtbetaling(ARBEIDSFORHOLD_1));

        Map<LukketPeriode, UttaksperiodeInfo> perioder = new HashMap<>();
        leggTilPeriode(perioder, original, stp1, tom1);
        Uttaksplan uttaksplanOriginal = new Uttaksplan(perioder, List.of());

        Map<LukketPeriode, UttaksperiodeInfo> perioder_endring = new HashMap<>();
        leggTilPeriode(perioder_endring, original, stp1, fomEndring.minusDays(1));
        leggTilPeriode(perioder_endring, endring, fomEndring, tomEndring);
        leggTilPeriode(perioder_endring, original, tomEndring.plusDays(1), tom1);
        leggTilPeriode(perioder_endring, endring, fomForlengelse, tomForlengelse);
        Uttaksplan uttaksplan_tette_hull = new Uttaksplan(perioder_endring, List.of());

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplan_tette_hull);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplanOriginal);

        when(vilkårsPerioderTilVurderingTjeneste.utled(originalBehandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>(List.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(stp1, tom1)
            )));


        var forlengelseperioder = utleder.utledPerioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(stp1, tomForlengelse));

        assertThat(forlengelseperioder.size()).isEqualTo(2);
        var iterator = forlengelseperioder.iterator();
        var periode = iterator.next();
        assertThat(periode.getFomDato()).isEqualTo(fomEndring);
        assertThat(periode.getTomDato()).isEqualTo(tomEndring);

        periode = iterator.next();
        assertThat(periode.getFomDato()).isEqualTo(fomForlengelse);
        assertThat(periode.getTomDato()).isEqualTo(tomForlengelse);
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
        final LukketPeriode periode = new LukketPeriode(fom, fom.plusDays(antallDager));
        leggTilPeriode(perioder, utbetalingsgrader, periode.getFom(), periode.getTom());
    }

    private void leggTilPeriode(Map<LukketPeriode, UttaksperiodeInfo> uttaksperioder, List<Utbetalingsgrader> utbetalingsgrader, LocalDate fom, LocalDate tom) {
        uttaksperioder.put(new LukketPeriode(fom, tom),
            new UttaksperiodeInfo(Utfall.OPPFYLT, BigDecimal.valueOf(100), utbetalingsgrader,
                null, null, Set.of(),
                new HashMap<>(), BigDecimal.valueOf(100), null,
                Set.of(), UUID.randomUUID().toString(),
                AnnenPart.ALENE, null, null, null,
                false, null, false));
    }

    private Behandling opprettBehandling(LocalDate skjæringstidspunkt) {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), new Saksnummer("SAK"), skjæringstidspunkt, skjæringstidspunkt.plusDays(3));

        fagsakRepository.opprettNy(fagsak);
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
