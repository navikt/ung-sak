package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.AktivitetspengerSatsType;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerSatser;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsGrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.InnvilgelseDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.SatsOgBeregningDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning.BarnetilleggDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning.BeregningDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning.SatsgrunnlagDto;
import org.slf4j.Logger;

import java.math.RoundingMode;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import static no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger.tilHeltall;
import static no.nav.ung.ytelse.aktivitetspenger.beregning.GrunnsatsType.BEREGNINGSGRUNNLAG;
import static no.nav.ung.ytelse.aktivitetspenger.beregning.GrunnsatsType.MINSTEYTELSE;

@Dependent
public class FørstegangsInnvilgelseInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FørstegangsInnvilgelseInnholdBygger.class);

    private final AktivitetspengerGrunnlagRepository beregningsgrunnlagRepository;

    @Inject
    public FørstegangsInnvilgelseInnholdBygger(AktivitetspengerGrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }


    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {

        LocalDateTimeline<DetaljertResultat> periode = DetaljertResultat.filtererTidslinje(detaljertResultatTidslinje, DetaljertResultatType.INNVILGELSE_KUN_VILKÅR);

        var ytelseFom = periode.getMinLocalDate();
        var ytelseTom = periode.getMaxLocalDate();

        var aktivitetspengerGrunnlag = beregningsgrunnlagRepository.hentGrunnlag(behandling.getId()).orElseThrow(
            () -> new IllegalStateException("Finner ikke beregningsgrunnlag for behandling " + behandling.getId())
        );

        var satsTidslinje = aktivitetspengerGrunnlag.hentAktivitetspengerSatsTidslinje();

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_INNVILGELSE,
            new InnvilgelseDto(
                ytelseFom,
                ytelseTom,
                byggSatsOgBeregning(satsTidslinje.toSegments())
            ));
    }

    private static SatsOgBeregningDto byggSatsOgBeregning(NavigableSet<LocalDateSegment<AktivitetspengerSatser>> beregningOgSatsSegmenter) {
        var satsTyper = beregningOgSatsSegmenter.stream()
            .map(it -> it.getValue().hentSatsType())
            .collect(Collectors.toSet());

        if (satsTyper.size() > 2) {
            throw new IllegalStateException("Brevet støtter ikke beregninger med besteberegning, lav og høy sats samtdig.");
        }
        var harLavSatstype = satsTyper.contains(AktivitetspengerSatsType.LAV);

        var tidligsteSegment = beregningOgSatsSegmenter.first().getValue();
        var grunnsatsType = tidligsteSegment.utledGrunnsatsBenyttet();

        var beregningsgrunnlag = BEREGNINGSGRUNNLAG.equals(grunnsatsType) ?
            lagBeregningsgrunnlagDto(tidligsteSegment.beregningsgrunnlag()) :
            null;

        var minsteYtelsegrunnlag = MINSTEYTELSE.equals(grunnsatsType) ?
            mapTilSatsgrunnlagDto(tidligsteSegment.satsGrunnlag()) :
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
            Satsberegner.beregnDagsatsInklBarnetillegg(senesteSats))
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
        return mapTilSatsgrunnlagDto(senesteSats.satsGrunnlag());
    }

    private static SatsgrunnlagDto mapTilSatsgrunnlagDto(AktivitetspengerSatsGrunnlag sats) {
        return new SatsgrunnlagDto(
            sats.grunnbeløpFaktor().setScale(3, RoundingMode.HALF_UP),
            tilHeltall(sats.minsteytelse()),
            tilHeltall(sats.dagsats())
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
}
