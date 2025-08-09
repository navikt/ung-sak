package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.kodeverk.ytelse.KorrigertYtelseÅrsak;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KorrigertYtelseVerdi;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelsePeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsResultat;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.NavigableSet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class BeregnYtelseStegTest {
    public static final LocalDate FOM = LocalDate.of(2025, 6, 24);
    @Inject
    private EntityManager entityManager;
    private BeregnYtelseSteg beregnYtelseSteg;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        ungdomsytelseGrunnlagRepository = new UngdomsytelseGrunnlagRepository(entityManager);
        tilkjentYtelseRepository = new TilkjentYtelseRepository(entityManager);
        inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        final var månedsvisTidslinjeUtleder = new MånedsvisTidslinjeUtleder(
            new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository, new UngdomsytelseStartdatoRepository(entityManager)),
            behandlingRepository);
        beregnYtelseSteg = new BeregnYtelseSteg(ungdomsytelseGrunnlagRepository,
            tilkjentYtelseRepository,
            månedsvisTidslinjeUtleder
        );

        fagsakRepository = new FagsakRepository(entityManager);

        lagFagsakOgBehandling(FOM);
        lagUngdomsprogramperioder(FOM);
        lagSatser(FOM);
        lagUttakPerioder(FOM);


        lagreIAYUtenRapportertInntekt();
    }

    private void lagreIAYUtenRapportertInntekt() {
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(behandling.getId(), OppgittOpptjeningBuilder.ny());
    }

    @Test
    void skal_opprette_tilkjent_ytelse_med_sporing_og_input() {
        final var behandleStegResultat = beregnYtelseSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling.getId())));
        final var tilkjentYtelse = tilkjentYtelseRepository.hentTilkjentYtelse(behandling.getId());
        assertThat(tilkjentYtelse.get().getInput()).isNotNull();
        assertThat(tilkjentYtelse.get().getSporing()).isNotNull();
    }

    @Test
    void skal_få_tilkjent_ytelse_i_periode_uten_inntekt_med_gjenomført_kontroll() {
        final var kontrollertInntektPeriode = lagKontrollertPeriode(BigDecimal.ZERO, FOM.plusMonths(1).withDayOfMonth(1));
        tilkjentYtelseRepository.lagre(behandling.getId(), List.of(kontrollertInntektPeriode));
        final var behandleStegResultat = beregnYtelseSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling.getId())));
        final var tilkjentYtelse = tilkjentYtelseRepository.hentTilkjentYtelse(behandling.getId());
        assertThat(tilkjentYtelse.get().getInput()).isNotNull();
        assertThat(tilkjentYtelse.get().getSporing()).isNotNull();

        final var perioder = tilkjentYtelse.get().getPerioder();
        assertThat(perioder.size()).isEqualTo(2);
        assertThat(perioder.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, FOM.with(TemporalAdjusters.lastDayOfMonth())));
        assertThat(perioder.get(1).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM.plusMonths(1).withDayOfMonth(1), FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())));
    }

    @Test
    void skal_få_tilkjent_ytelse_i_periode_uten_inntekt_med_gjenomført_kontroll_med_reduksjon_og_positivt_avvik_grunnet_avrunding() {
        final var kontrollertInntektPeriode1 = lagKontrollertPeriode(BigDecimal.valueOf(500), FOM.plusMonths(1).withDayOfMonth(1));
        final var kontrollertInntektPeriode2 = lagKontrollertPeriode(BigDecimal.valueOf(500), FOM.plusMonths(2).withDayOfMonth(1));
        final var kontrollertInntektPeriode3 = lagKontrollertPeriode(BigDecimal.valueOf(500), FOM.plusMonths(3).withDayOfMonth(1));
        final var kontrollertInntektPeriode4 = lagKontrollertPeriode(BigDecimal.valueOf(500), FOM.plusMonths(4).withDayOfMonth(1));
        final var kontrollertInntektPeriode5 = lagKontrollertPeriode(BigDecimal.valueOf(500), FOM.plusMonths(5).withDayOfMonth(1));
        final var kontrollertInntektPeriode6 = lagKontrollertPeriode(BigDecimal.valueOf(500), FOM.plusMonths(6).withDayOfMonth(1));
        final var kontrollertInntektPeriode7 = lagKontrollertPeriode(BigDecimal.valueOf(500), FOM.plusMonths(7).withDayOfMonth(1));
        final var kontrollertInntektPeriode8 = lagKontrollertPeriode(BigDecimal.valueOf(500), FOM.plusMonths(8).withDayOfMonth(1));
        final var kontrollertInntektPeriode9 = lagKontrollertPeriode(BigDecimal.valueOf(500), FOM.plusMonths(9).withDayOfMonth(1));
        final var kontrollertInntektPeriode10 = lagKontrollertPeriode(BigDecimal.valueOf(500), FOM.plusMonths(10).withDayOfMonth(1));
        final var kontrollertInntektPeriode11 = lagKontrollertPeriode(BigDecimal.valueOf(500), FOM.plusMonths(11).withDayOfMonth(1));

        tilkjentYtelseRepository.lagre(behandling.getId(), List.of(
            kontrollertInntektPeriode1,
            kontrollertInntektPeriode2,
            kontrollertInntektPeriode3,
            kontrollertInntektPeriode4,
            kontrollertInntektPeriode5,
            kontrollertInntektPeriode6,
            kontrollertInntektPeriode7,
            kontrollertInntektPeriode8,
            kontrollertInntektPeriode9,
            kontrollertInntektPeriode10,
            kontrollertInntektPeriode11));
        final var behandleStegResultat = beregnYtelseSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling.getId())));
        final var tilkjentYtelse = tilkjentYtelseRepository.hentTilkjentYtelse(behandling.getId());
        assertThat(tilkjentYtelse.get().getInput()).isNotNull();
        assertThat(tilkjentYtelse.get().getSporing()).isNotNull();

        final var perioder = tilkjentYtelse.get().getPerioder();
        assertThat(perioder.size()).isEqualTo(13);
        assertThat(perioder.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, FOM.with(TemporalAdjusters.lastDayOfMonth())));
        var redusertPeriode = perioder.get(1);
        assertThat(redusertPeriode.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM.plusMonths(1).withDayOfMonth(1), FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())));
        assertThat(redusertPeriode.getUredusertBeløp().compareTo(BigDecimal.valueOf(23000))).isEqualTo(0);
        assertThat(redusertPeriode.getReduksjon().compareTo(BigDecimal.valueOf(330))).isEqualTo(0);
        assertThat(redusertPeriode.getRedusertBeløp().compareTo(BigDecimal.valueOf(22670))).isEqualTo(0);
        assertThat(redusertPeriode.getDagsats().compareTo(BigDecimal.valueOf(986))).isEqualTo(0);
        assertThat(redusertPeriode.getAvvikGrunnetAvrunding()).isEqualTo(8.0);

        final var korrigertTidslinje = tilkjentYtelseRepository.hentKorrigertTidslinje(behandling.getId());
        var forventet = new LocalDateTimeline<>(FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()), FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()), new KorrigertYtelseVerdi(BigDecimal.valueOf(994), KorrigertYtelseÅrsak.KORRIGERING_AV_AVRUNDINGSFEIL));
        assertThat(korrigertTidslinje).isEqualTo(LocalDateTimeline.empty());
    }

    @Test
    void skal_få_tilkjent_ytelse_i_periode_uten_inntekt_med_gjenomført_kontroll_med_reduksjon_og_negativt_avvik_grunnet_avrunding() {
        final var kontrollertInntektPeriode1 = lagKontrollertPeriode(BigDecimal.valueOf(506), FOM.plusMonths(1).withDayOfMonth(1));
        final var kontrollertInntektPeriode2 = lagKontrollertPeriode(BigDecimal.valueOf(506), FOM.plusMonths(2).withDayOfMonth(1));
        final var kontrollertInntektPeriode3 = lagKontrollertPeriode(BigDecimal.valueOf(506), FOM.plusMonths(3).withDayOfMonth(1));
        final var kontrollertInntektPeriode4 = lagKontrollertPeriode(BigDecimal.valueOf(506), FOM.plusMonths(4).withDayOfMonth(1));
        final var kontrollertInntektPeriode5 = lagKontrollertPeriode(BigDecimal.valueOf(506), FOM.plusMonths(5).withDayOfMonth(1));
        final var kontrollertInntektPeriode6 = lagKontrollertPeriode(BigDecimal.valueOf(506), FOM.plusMonths(6).withDayOfMonth(1));
        final var kontrollertInntektPeriode7 = lagKontrollertPeriode(BigDecimal.valueOf(506), FOM.plusMonths(7).withDayOfMonth(1));
        final var kontrollertInntektPeriode8 = lagKontrollertPeriode(BigDecimal.valueOf(506), FOM.plusMonths(8).withDayOfMonth(1));
        final var kontrollertInntektPeriode9 = lagKontrollertPeriode(BigDecimal.valueOf(506), FOM.plusMonths(9).withDayOfMonth(1));
        final var kontrollertInntektPeriode10 = lagKontrollertPeriode(BigDecimal.valueOf(506), FOM.plusMonths(10).withDayOfMonth(1));
        final var kontrollertInntektPeriode11 = lagKontrollertPeriode(BigDecimal.valueOf(506), FOM.plusMonths(11).withDayOfMonth(1));

        tilkjentYtelseRepository.lagre(behandling.getId(), List.of(
            kontrollertInntektPeriode1,
            kontrollertInntektPeriode2,
            kontrollertInntektPeriode3,
            kontrollertInntektPeriode4,
            kontrollertInntektPeriode5,
            kontrollertInntektPeriode6,
            kontrollertInntektPeriode7,
            kontrollertInntektPeriode8,
            kontrollertInntektPeriode9,
            kontrollertInntektPeriode10,
            kontrollertInntektPeriode11));
        final var behandleStegResultat = beregnYtelseSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling.getId())));
        final var tilkjentYtelse = tilkjentYtelseRepository.hentTilkjentYtelse(behandling.getId());
        assertThat(tilkjentYtelse.get().getInput()).isNotNull();
        assertThat(tilkjentYtelse.get().getSporing()).isNotNull();

        final var perioder = tilkjentYtelse.get().getPerioder();
        assertThat(perioder.size()).isEqualTo(13);
        assertThat(perioder.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, FOM.with(TemporalAdjusters.lastDayOfMonth())));
        var redusertPeriode = perioder.get(1);
        assertThat(redusertPeriode.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM.plusMonths(1).withDayOfMonth(1), FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())));
        assertThat(redusertPeriode.getUredusertBeløp().compareTo(BigDecimal.valueOf(23000))).isEqualTo(0);
        assertThat(redusertPeriode.getReduksjon().compareTo(BigDecimal.valueOf(333.96))).isEqualTo(0);
        assertThat(redusertPeriode.getRedusertBeløp().compareTo(BigDecimal.valueOf(22666.04))).isEqualTo(0);
        assertThat(redusertPeriode.getDagsats().compareTo(BigDecimal.valueOf(985))).isEqualTo(0);
        assertThat(redusertPeriode.getAvvikGrunnetAvrunding()).isEqualTo(-11.04);


        var sistePeriode = perioder.get(12);
        var dagsatsSistePeriode = sistePeriode.getDagsats();


        var avvik = perioder.stream().map(TilkjentYtelsePeriode::getAvvikGrunnetAvrunding).reduce(Double::sum).orElse(0d);
        final var korrigertTidslinje = tilkjentYtelseRepository.hentKorrigertTidslinje(behandling.getId());
        var forventetKorrigertDagsats = dagsatsSistePeriode.subtract(BigDecimal.valueOf(avvik)).setScale(0, RoundingMode.HALF_UP);

        var segmenter = korrigertTidslinje.toSegments();
        assertThat(segmenter.size()).isEqualTo(1);
        assertThat(segmenter.getFirst().getFom()).isEqualTo(sistePeriode.getPeriode().getTomDato());
        assertThat(segmenter.getFirst().getTom()).isEqualTo(sistePeriode.getPeriode().getTomDato());
        assertThat(segmenter.getFirst().getValue().dagsats().compareTo(forventetKorrigertDagsats)).isEqualTo(0);
        assertThat(segmenter.getFirst().getValue().årsak()).isEqualTo(KorrigertYtelseÅrsak.KORRIGERING_AV_AVRUNDINGSFEIL);
    }

    private static KontrollertInntektPeriode lagKontrollertPeriode(BigDecimal arbeidsinntekt, LocalDate fom) {
        return KontrollertInntektPeriode.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.with(TemporalAdjusters.lastDayOfMonth())))
            .medInntekt(arbeidsinntekt)
            .medKilde(KontrollertInntektKilde.BRUKER)
            .build();
    }


    private void lagUngdomsprogramperioder(LocalDate fom) {
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMed(fom))));
    }

    private void lagSatser(LocalDate fom) {
        final var satser = new UngdomsytelseSatser(BigDecimal.valueOf(1000),
            BigDecimal.TEN,
            BigDecimal.ONE,
            UngdomsytelseSatsType.LAV, 0, 0);
        final var ungdomsytelseSatsResultat = new UngdomsytelseSatsResultat(
            new LocalDateTimeline<>(fom, fom.plusWeeks(52), satser),
            "test", "test"
        );
        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), ungdomsytelseSatsResultat);
    }

    private void lagUttakPerioder(LocalDate fom) {
        final var uttakperioder = new UngdomsytelseUttakPerioder(List.of(new UngdomsytelseUttakPeriode(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusWeeks(52))
        )));
        uttakperioder.setRegelInput("test");
        uttakperioder.setRegelSporing("test");
        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), uttakperioder);
    }


    private Long lagFagsakOgBehandling(LocalDate fom) {
        final var fagsak = new Fagsak(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, fom.plusWeeks(52));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling.getId();
    }



}
