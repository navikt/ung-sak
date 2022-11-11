package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
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
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
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
class PleiepengerBeregningForlengelseperiodeUtlederTest {

    public static final String ORGANISASJONSNUMMER = "123456789";
    public static final String ORGANISASJONSNUMMER2 = "123456788";

    public static final Arbeidsforhold ARBEIDSFORHOLD_1 = new Arbeidsforhold(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD.getKode(),
        ORGANISASJONSNUMMER, null, null);
    public static final Arbeidsforhold ARBEIDSFORHOLD_2 = new Arbeidsforhold(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD.getKode(),
        ORGANISASJONSNUMMER2, null, null);
    public static final Duration SJU_OG_EN_HALV_TIME = Duration.ofHours(7).plusMinutes(30);
    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private Behandling behandling;

    private Behandling originalBehandling;


    private UttakTjeneste uttakTjeneste = mock(UttakTjeneste.class);
    private PleiepengerBeregningForlengelseperiodeUtleder utleder;

    @BeforeEach
    void setUp() {
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        utleder = new PleiepengerBeregningForlengelseperiodeUtleder(uttakTjeneste, behandlingRepository);
        originalBehandling = opprettBehandling(SKJÆRINGSTIDSPUNKT);
        behandling = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING).build();
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

        var forlengelseperioder = utleder.utledForlengelseperioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

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

        var forlengelseperioder = utleder.utledForlengelseperioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

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
        leggTilPeriode(perioder2, fom, utbetalingsgraderPeriode1, antallDagerTilEndring-1);
        List<Utbetalingsgrader> utbetalingsgraderPeriode2 = List.of(delvisUtbetaling(ARBEIDSFORHOLD_1, BigDecimal.valueOf(50)));
        leggTilPeriode(perioder2, fom.plusDays(antallDagerTilEndring), utbetalingsgraderPeriode2, antallDager);
        Uttaksplan uttaksplanForlengelse = new Uttaksplan(perioder2, List.of());

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplanForlengelse);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplanOriginalBehandling);

        var forlengelseperioder = utleder.utledForlengelseperioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

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
        leggTilPeriode(perioder2, fom, utbetalingsgraderPeriode1, antallDagerTilEndring-1);
        List<Utbetalingsgrader> utbetalingsgraderPeriode2 = List.of(fullUtbetaling(ARBEIDSFORHOLD_1));
        leggTilPeriode(perioder2, fom.plusDays(antallDagerTilEndring), utbetalingsgraderPeriode2, antallDager);
        Uttaksplan uttaksplanForlengelse = new Uttaksplan(perioder2, List.of());

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplanForlengelse);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplanOriginalBehandling);

        var forlengelseperioder = utleder.utledForlengelseperioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

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
        leggTilPeriode(perioder2, fom, utbetalingsgraderPeriode1, antallDagerTilEndring-1);
        List<Utbetalingsgrader> utbetalingsgraderPeriode2 = List.of(fullUtbetaling(ARBEIDSFORHOLD_1), fullUtbetaling(ARBEIDSFORHOLD_2));
        leggTilPeriode(perioder2, fom.plusDays(antallDagerTilEndring), utbetalingsgraderPeriode2, antallDager);
        Uttaksplan uttaksplanForlengelse = new Uttaksplan(perioder2, List.of());

        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true))
            .thenReturn(uttaksplanForlengelse);
        when(uttakTjeneste.hentUttaksplan(originalBehandling.getUuid(), true))
            .thenReturn(uttaksplanOriginalBehandling);

        var forlengelseperioder = utleder.utledForlengelseperioder(BehandlingReferanse.fra(behandling), DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(antallDager)));

        assertThat(forlengelseperioder.size()).isEqualTo(1);
        var periode = forlengelseperioder.iterator().next();
        assertThat(periode.getFomDato()).isEqualTo(fom.plusDays(antallDagerTilEndring));
        assertThat(periode.getTomDato()).isEqualTo(fom.plusDays(antallDager));
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


}
