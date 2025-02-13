package no.nav.ung.sak.domene.behandling.steg.beregning;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.ytelse.sats.*;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.Barnetillegg;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.FødselOgDødInfo;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.LagBarnetilleggTidslinje;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;
import no.nav.ung.sak.typer.Periode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Dependent
public class UngdomsytelseBeregnDagsats {

    private final LagBarnetilleggTidslinje lagBarnetilleggTidslinje;

    @Inject
    public UngdomsytelseBeregnDagsats(LagBarnetilleggTidslinje lagBarnetilleggTidslinje) {
        this.lagBarnetilleggTidslinje = lagBarnetilleggTidslinje;
    }


    public UngdomsytelseSatsResultat beregnDagsats(BehandlingReferanse behandlingRef, LocalDateTimeline<Boolean> perioder, LocalDate fødselsdato, LocalDate beregningsdato, boolean harTriggerBeregnHøySats) {
        var grunnbeløpTidslinje = GrunnbeløpTidslinje.hentTidslinje();
        var satstypeTidslinje = LagSatsTidslinje.lagSatsTidslinje(fødselsdato, beregningsdato, harTriggerBeregnHøySats);
        LocalDateTimeline<SatsOgGrunnbeløpfaktor> satsOgGrunnbeløpfaktorTidslinje = GrunnbeløpfaktorTidslinje.hentGrunnbeløpfaktorTidslinjeFor(satstypeTidslinje);
        var barnetilleggResultat = lagBarnetilleggTidslinje.lagTidslinje(behandlingRef, perioder);

        var satsTidslinje = perioder
            .intersection(satsOgGrunnbeløpfaktorTidslinje, StandardCombinators::rightOnly)
            .mapValue(UngdomsytelseBeregnDagsats::leggTilSatsTypeOgGrunnbeløpFaktor)
            .intersection(grunnbeløpTidslinje, leggTilGrunnbeløp())
            .combine(barnetilleggResultat.barnetilleggTidslinje(), leggTilBarnetillegg(), LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .mapValue(UngdomsytelseSatser.Builder::build);

        return new UngdomsytelseSatsResultat(
            satsTidslinje,
            lagRegelSporing(grunnbeløpTidslinje, satsOgGrunnbeløpfaktorTidslinje, barnetilleggResultat.barnetilleggTidslinje()),
            lagRegelInput(perioder, fødselsdato, harTriggerBeregnHøySats, beregningsdato, barnetilleggResultat.relevanteBarnPersoninformasjon())
        );
    }

    private static UngdomsytelseSatser.Builder leggTilSatsTypeOgGrunnbeløpFaktor(SatsOgGrunnbeløpfaktor sats) {
        return UngdomsytelseSatser.builder().medGrunnbeløpFaktor(sats.grunnbeløpFaktor()).medSatstype(sats.satstype());
    }

    private static LocalDateSegmentCombinator<UngdomsytelseSatser.Builder, BigDecimal, UngdomsytelseSatser.Builder> leggTilGrunnbeløp() {
        return (di, lhs, rhs) -> {
            var builder = lhs.getValue().kopi();
            return new LocalDateSegment<>(di, builder.medGrunnbeløp(rhs.getValue()));
        };
    }

    private static LocalDateSegmentCombinator<UngdomsytelseSatser.Builder, Barnetillegg, UngdomsytelseSatser.Builder> leggTilBarnetillegg() {
        return (di, lhs, rhs) -> {
            var builder = lhs.getValue().kopi();
            return new LocalDateSegment<>(di,
                rhs == null ?
                    builder.medAntallBarn(0).medBarnetilleggDagsats(0) :
                    builder.medAntallBarn(rhs.getValue().antallBarn()).medBarnetilleggDagsats(rhs.getValue().dagsats()));
        };
    }


    private static String lagRegelSporing(LocalDateTimeline<BigDecimal> grunnbeløpTidslinje, LocalDateTimeline<SatsOgGrunnbeløpfaktor> satsOgGrunnbeløpFaktorTidslinje, LocalDateTimeline<Barnetillegg> barnetilleggTidslinje) {
        var regelSporing = new RegelSporing(mapTilPerioderMedVerdi(grunnbeløpTidslinje), mapTilPerioderMedVerdi(satsOgGrunnbeløpFaktorTidslinje), mapTilPerioderMedVerdi(barnetilleggTidslinje));
        return JsonObjectMapper.toJson(regelSporing, JsonMappingFeil.FACTORY::jsonMappingFeil);
    }

    private static String lagRegelInput(LocalDateTimeline<Boolean> perioder, LocalDate fødselsdato, boolean harTriggerBeregnHøySats, LocalDate beregningsdato, List<FødselOgDødInfo> barnPersoninformasjon) {
        var regelInput = new RegelInput(
            TidslinjeUtil.tilPerioder(perioder),
            fødselsdato,
            harTriggerBeregnHøySats,
            beregningsdato,
            barnPersoninformasjon
        );
        return JsonObjectMapper.toJson(regelInput, JsonMappingFeil.FACTORY::jsonMappingFeil);
    }


    private record RegelSporing(List<PeriodeMedVerdi<BigDecimal>> grunnbeløpPerioder,
                                List<PeriodeMedVerdi<SatsOgGrunnbeløpfaktor>> satsOgGrunnbeløpfaktorPerioder,
                                List<PeriodeMedVerdi<Barnetillegg>> barnetilleggPerioder) {
    }

    private static <T> List<PeriodeMedVerdi<T>> mapTilPerioderMedVerdi(LocalDateTimeline<T> tidslinje) {
        return tidslinje.toSegments().stream().map(it -> new PeriodeMedVerdi<>(new Periode(it.getFom(), it.getTom()), it.getValue())).toList();
    }

    private record PeriodeMedVerdi<T>(Periode periode, T verdi) {
    }

    ;


    private record RegelInput(List<Periode> perioder, LocalDate fødselsdato, boolean harTriggerBeregnHøySats,
                              LocalDate beregningsdato, List<FødselOgDødInfo> barnFødselOgDød) {
    }

    interface JsonMappingFeil extends DeklarerteFeil {

        JsonMappingFeil FACTORY = FeilFactory.create(JsonMappingFeil.class);

        @TekniskFeil(feilkode = "UNG-34523", feilmelding = "JSON-mapping feil: %s", logLevel = LogLevel.WARN)
        Feil jsonMappingFeil(JsonProcessingException e);
    }

}
