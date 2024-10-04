package no.nav.k9.sak.ytelse.ung.beregnytelse;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.BEREGN_YTELSE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Period;
import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.k9.sak.ytelse.ung.beregning.UngdomsytelseGrunnlag;
import no.nav.k9.sak.ytelse.ung.beregning.UngdomsytelseGrunnlagRepository;
import no.nav.k9.sak.ytelse.ung.beregning.UngdomsytelseSatser;

@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@BehandlingStegRef(value = BEREGN_YTELSE)
@BehandlingTypeRef
@ApplicationScoped
public class UngdomsytelseBeregneYtelseSteg implements BeregneYtelseSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    protected UngdomsytelseBeregneYtelseSteg() {
        // for proxy
    }

    @Inject
    public UngdomsytelseBeregneYtelseSteg(BehandlingRepositoryProvider repositoryProvider,
                                          UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId);
        var satsTidslinje = ungdomsytelseGrunnlag.map(UngdomsytelseGrunnlag::getSatsTidslinje).orElse(LocalDateTimeline.empty());
        var utbetalingsgradTidslinje = ungdomsytelseGrunnlag.map(UngdomsytelseGrunnlag::getUtbetalingsgradTidslinje).orElse(LocalDateTimeline.empty());

        var resultatTidslinje = satsTidslinje.intersection(utbetalingsgradTidslinje, sammenstillSatsOgGradering());


        // stopper periodisering her for å unngå 'evigvarende' ekspansjon -
        // tar første av potensielle maks datoer som berører intersection av de to tidslinjene.
        var minsteMaksDato = Stream.of(satsTidslinje.getMaxLocalDate(), utbetalingsgradTidslinje.getMaxLocalDate()).sorted().findFirst().orElseThrow();
        // Splitter på år, pga chk_br_andel_samme_aar constraint i database
        resultatTidslinje = resultatTidslinje.splitAtRegular(utbetalingsgradTidslinje.getMinLocalDate().withDayOfYear(1), minsteMaksDato, Period.ofYears(1));

        var beregningsresultatEntitet = BeregningsresultatEntitet.builder()
            .medRegelInput(mapTilRegelInput(satsTidslinje, utbetalingsgradTidslinje))
            .medRegelSporing(lagRegelSporing(resultatTidslinje))
            .build();

        resultatTidslinje.toSegments()
            .forEach(p -> {
                var resultatPeriode = BeregningsresultatPeriode.builder()
                    .medBeregningsresultatPeriodeFomOgTom(p.getFom(), p.getTom())
                    .build(beregningsresultatEntitet);
                BeregningsresultatAndel.builder()
                    .medDagsats(p.getValue().dagsats().intValue())
                    .medDagsatsFraBg(p.getValue().dagsats().intValue()) // TODO: Denne er ikkje nødvendig for UNG, men er påkrevd
                    .medInntektskategori(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER)
                    .medUtbetalingsgradOppdrag(p.getValue().utbetalingsgrad())
                    .medUtbetalingsgrad(p.getValue().utbetalingsgrad())
                    .medStillingsprosent(BigDecimal.ZERO) // TODO: Denne var påkrevd i resultatandel, men gir ikkje meining for UNG-ytelsen
                    .medBrukerErMottaker(true)
                    .buildFor(resultatPeriode);
            });

        beregningsresultatRepository.lagre(behandling, beregningsresultatEntitet);


        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static LocalDateSegmentCombinator<UngdomsytelseSatser, BigDecimal, DagsatsOgUtbetalingsgrad> sammenstillSatsOgGradering() {
        return (di, lhs, rhs) ->
            new LocalDateSegment<>(di,
                new DagsatsOgUtbetalingsgrad(lhs.getValue().dagsats().multiply(rhs.getValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP), rhs.getValue()));
    }

    private String mapTilRegelInput(LocalDateTimeline<UngdomsytelseSatser> satsTidslinje, LocalDateTimeline<BigDecimal> utbetalingsgradTidslinje) {

        var satsperioder = satsTidslinje.toSegments().stream().map(s -> """
            { "periode": ":periode", "satser": ":satser" }""".stripLeading()
            .replaceFirst(":periode", s.getLocalDateInterval().toString())
            .replaceFirst(":satser", s.getValue().toString()))
            .reduce("", (s1, s2) -> s1 + ", " + s2);

        var utbetalingsgradperioder = utbetalingsgradTidslinje.toSegments().stream().map(s -> """
                { "periode": ":periode", "utbetalingsgrad": ":grad" }""".stripLeading()
                .replaceFirst(":periode", s.getLocalDateInterval().toString())
                .replaceFirst(":grad", s.getValue().toString()))
            .reduce("", (s1, s2) -> s1 + ", " + s2);

        return """
            { "satsperioder": [ :satsperioder ], "utbetalingsgradperioder": [ :utbetalingsgradperioder ] }""".stripLeading()
            .replaceFirst(":satsperioder", satsperioder)
            .replaceFirst(":utbetalingsgradperioder", utbetalingsgradperioder);

    }


    private static String lagRegelSporing(LocalDateTimeline<DagsatsOgUtbetalingsgrad> DagsatsOgUtbetalingsgrad) {

        var resultatPerioder = DagsatsOgUtbetalingsgrad.toSegments().stream().map(s -> """
            { "periode": ":periode", "dagsats": ":dagsats", "utbetalingsgrad": ":grad" }""".stripLeading()
                .replaceFirst(":periode", s.getLocalDateInterval().toString())
                .replaceFirst(":dagsats", s.getValue().dagsats().toString())
                .replaceFirst(":grad", s.getValue().utbetalingsgrad().toString()))
            .reduce("", (s1, s2) -> s1 + ", " + s2);

        return """
            { "resultatperioder": [ :resultatPerioder ] }""".stripLeading()
            .replaceFirst(":resultatPerioder", resultatPerioder);
    }


    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), kontekst.getSkriveLås());
    }

    private record DagsatsOgUtbetalingsgrad(BigDecimal dagsats, BigDecimal utbetalingsgrad) {
    }

}
