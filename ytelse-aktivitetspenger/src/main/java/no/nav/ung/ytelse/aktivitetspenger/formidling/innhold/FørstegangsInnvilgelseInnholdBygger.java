package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.satsendring.SatsEndringHendelseDto;
import no.nav.ung.sak.formidling.vedtak.satsendring.SatsEndringUtleder;
import no.nav.ung.sak.formidling.vedtak.satsendring.SatsEndringUtlederInput;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.AktivitetspengerSatsType;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerSatser;
import no.nav.ung.ytelse.aktivitetspenger.beregning.MonthUtils;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.InnvilgelseDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.SatsOgBeregningDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.UtbetalingDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning.BarnetilleggDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning.BeregningDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning.SatsgrunnlagDto;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import static no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger.tilHeltall;
import static no.nav.ung.ytelse.aktivitetspenger.beregning.GrunnsatsType.BEREGNINGSGRUNNLAG;
import static no.nav.ung.ytelse.aktivitetspenger.beregning.GrunnsatsType.MINSTEYTELSE;

@Dependent
public class FørstegangsInnvilgelseInnholdBygger implements VedtaksbrevInnholdBygger {

    private final AktivitetspengerGrunnlagRepository beregningsgrunnlagRepository;
    private final TilkjentYtelseRepository tilkjentYtelseRepository;
    private final LocalDate overrideDagensDatoForTest;

    @Inject
    public FørstegangsInnvilgelseInnholdBygger(
        AktivitetspengerGrunnlagRepository beregningsgrunnlagRepository,
        TilkjentYtelseRepository tilkjentYtelseRepository,
        @KonfigVerdi(value = "BREV_DAGENS_DATO_TEST", required = false) LocalDate overrideDagensDatoForTest) {

        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.overrideDagensDatoForTest = overrideDagensDatoForTest;
    }


    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        LocalDateTimeline<DetaljertResultat> periode = DetaljertResultat.filtererTidslinje(detaljertResultatTidslinje, DetaljertResultatType.INNVILGELSE_KUN_VILKÅR);

        LocalDate ytelseFom = periode.getMinLocalDate();
        LocalDate ytelseTom = null;

        var aktivitetspengerGrunnlag = beregningsgrunnlagRepository.hentGrunnlag(behandling.getId()).orElseThrow(
            () -> new IllegalStateException("Finner ikke beregningsgrunnlag for behandling " + behandling.getId())
        );

        var satsTidslinje = aktivitetspengerGrunnlag.hentAktivitetspengerSatsTidslinje().intersection(detaljertResultatTidslinje);
        var førsteSegment = satsTidslinje.toSegments().first();
        var førsteSatser = førsteSegment.getValue();
        var dagsatsFom = Satsberegner.beregnDagsatsInklBarnetillegg(førsteSatser);

        var utbetalingDto = opprettUtbetalingDto(behandling, detaljertResultatTidslinje);

        var satsendringer = lagSatsEndringHendelser(satsTidslinje);

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_INNVILGELSE,
            new InnvilgelseDto(
                ytelseFom,
                ytelseTom,
                dagsatsFom,
                utbetalingDto,
                satsendringer,
                byggSatsOgBeregning(satsTidslinje.toSegments())
            ));
    }

    private UtbetalingDto opprettUtbetalingDto(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        var tilkjentYtelseTimeline = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).intersection(detaljertResultatTidslinje);
        if (tilkjentYtelseTimeline.isEmpty()) {
            throw new IllegalStateException("Fant ingen tilkjent ytelse tidslinje for behandling i perioden %s".formatted(detaljertResultatTidslinje.getLocalDateIntervals()));
        }
        var førsteTilkjentMåned = tilkjentYtelseTimeline.getMinLocalDate().withDayOfMonth(1);
        var dagensDato = bestemDagensDato();

        var førstkommendeUtbetalingskjøring = førsteTilkjentMåned.plusMonths(1).plusDays(9);
        var erEtterbetaling = dagensDato.isAfter(førstkommendeUtbetalingskjøring);

        var månedNavn = MonthUtils.getMonthNameInNorwegian(førstkommendeUtbetalingskjøring.getMonth());
        return new UtbetalingDto(månedNavn, erEtterbetaling);
    }


    private List<SatsEndringHendelseDto> lagSatsEndringHendelser(LocalDateTimeline<AktivitetspengerSatser> satsTidslinje) {
        var inputs = satsTidslinje.toSegments().stream()
            .map(FørstegangsInnvilgelseInnholdBygger::tilSatsEndringUtlederInput)
            .toList();
        return new SatsEndringUtleder(inputs).lagSatsEndringHendelser();
        }

    private static SatsEndringUtlederInput tilSatsEndringUtlederInput(LocalDateSegment<AktivitetspengerSatser> segment) {
        var aktivitetspengerSatser = segment.getValue();
        var satserGrunnlag = aktivitetspengerSatser.satsGrunnlag();
        return new SatsEndringUtlederInput(
            satserGrunnlag.antallBarn(),
            satserGrunnlag.satsType() == UngdomsytelseSatsType.HØY,
            Satsberegner.beregnDagsatsInklBarnetillegg(aktivitetspengerSatser),
            Satsberegner.beregnBarnetilleggSats(satserGrunnlag),
            segment.getFom()
        );
    }

    private static SatsOgBeregningDto byggSatsOgBeregning(NavigableSet<LocalDateSegment<AktivitetspengerSatser>> beregningOgSatsSegmenter) {
        var satsTyper = beregningOgSatsSegmenter.stream()
            .map(it -> it.getValue().hentSatsType())
            .collect(Collectors.toSet());

        if (satsTyper.size() > 2) {
            throw new IllegalStateException("Brevet støtter ikke beregninger med besteberegning, lav og høy sats samtdig.");
        }
        var harLavSatstype = satsTyper.contains(AktivitetspengerSatsType.LAV);

        var tidligsteSegment = beregningOgSatsSegmenter.first();
        var tidligsteSatsOgBeregning = tidligsteSegment.getValue();
        var grunnsatsType = tidligsteSatsOgBeregning.utledGrunnsatsBenyttet();

        var beregningsgrunnlag = BEREGNINGSGRUNNLAG.equals(grunnsatsType) ?
            lagBeregningsgrunnlagDto(tidligsteSatsOgBeregning.beregningsgrunnlag()) :
            null;

        var minsteYtelsegrunnlag = MINSTEYTELSE.equals(grunnsatsType) ?
            mapTilSatsgrunnlagDto(tidligsteSegment) :
            null;

        var senesteSegment = beregningOgSatsSegmenter.last();
        var senesteSats = senesteSegment.getValue().satsGrunnlag();
        var minsteYtelsegrunnlagOvergangTilHøySats = satsTyper.size() > 1 ? kontrollerOvergangTilHøySats(senesteSegment) :  null;
        var grunnbeløp = tilHeltall(senesteSats.grunnbeløp());

        var barnetillegg = senesteSats.antallBarn() > 0
            ? new BarnetilleggDto(
            Satsberegner.tallTilNorskHunkjønnTekst(senesteSats.antallBarn()),
            senesteSats.antallBarn() > 1,
            Satsberegner.beregnBarnetilleggSats(senesteSats),
            Satsberegner.beregnDagsatsInklBarnetillegg(senesteSegment.getValue()))
            : null;


        return new SatsOgBeregningDto(
            Sats.HØY.getFomAlder(),
            harLavSatstype,
            beregningsgrunnlag,
            minsteYtelsegrunnlag,   // Vil inneholde LAV (eller HØY hvis kun høy sats)
            minsteYtelsegrunnlagOvergangTilHøySats,
            grunnbeløp,
            barnetillegg
        );
    }

    private static SatsgrunnlagDto kontrollerOvergangTilHøySats(LocalDateSegment<AktivitetspengerSatser> senesteSatsSegment) {
        var senesteSats = senesteSatsSegment.getValue();
        if (senesteSats.hentSatsType() != AktivitetspengerSatsType.HØY) {
            throw new IllegalStateException("Forventet at nyeste sats skulle være høy når det er flere satser, men var %s for periode %s".formatted(senesteSats.hentSatsType(), senesteSatsSegment.getLocalDateInterval()));
        }
        return mapTilSatsgrunnlagDto(senesteSatsSegment);
    }

    private static SatsgrunnlagDto mapTilSatsgrunnlagDto(LocalDateSegment<AktivitetspengerSatser> satser) {
        var satsgrunnlag = satser.getValue().satsGrunnlag();
        return new SatsgrunnlagDto(
            Satsberegner.lagGrunnbeløpFaktorTekst(satser),
            tilHeltall(satsgrunnlag.minsteytelse()),
            tilHeltall(satsgrunnlag.dagsats())
        );
    }

    private static BeregningDto lagBeregningsgrunnlagDto(Beregningsgrunnlag beregningsgrunnlag) {
        return new BeregningDto(
            beregningsgrunnlag.getSisteLignedeÅr().toString(),
            beregningsgrunnlag.utledBesteBeregningResultatType(),
            beregningsgrunnlag.getBeregnetPrAar(),
            tilHeltall(beregningsgrunnlag.getDagsats())
        );
    }

    private LocalDate bestemDagensDato() {
        return Environment.current().isLocal() && overrideDagensDatoForTest != null ? overrideDagensDatoForTest : LocalDate.now();
    }
}
