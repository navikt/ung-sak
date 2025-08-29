package no.nav.ung.sak.domene.behandling.steg.beregning;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.behandlingslager.behandling.sporing.LagRegelSporing;
import no.nav.ung.sak.behandlingslager.ytelse.sats.GrunnbeløpfaktorTidslinje;
import no.nav.ung.sak.behandlingslager.ytelse.sats.SatsOgGrunnbeløpfaktor;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsResultat;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.Barnetillegg;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.BeregnDagsatsInput;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.FødselOgDødInfo;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.LagBarnetilleggTidslinje;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class UngdomsytelseBeregnDagsats {

    public static UngdomsytelseSatsResultat beregnDagsats(BeregnDagsatsInput input) {
        var grunnbeløpTidslinje = GrunnbeløpTidslinje.hentTidslinje();
        var satstypeTidslinje = LagSatsTidslinje.lagSatsTidslinje(mapTilSatsInput(input));
        var satsOgGrunnbeløpfaktorTidslinje = GrunnbeløpfaktorTidslinje.hentGrunnbeløpfaktorTidslinjeFor(satstypeTidslinje);
        var barnetilleggResultat = LagBarnetilleggTidslinje.lagTidslinje(input.barnsFødselOgDødInformasjon());

        var satsTidslinje = input.perioder()
            .intersection(satsOgGrunnbeløpfaktorTidslinje, StandardCombinators::rightOnly)
            .mapValue(UngdomsytelseBeregnDagsats::leggTilSatsTypeOgGrunnbeløpFaktor)
            .intersection(grunnbeløpTidslinje, leggTilGrunnbeløp())
            .combine(barnetilleggResultat.barnetilleggTidslinje(), leggTilBarnetillegg(), LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .mapValue(UngdomsytelseSatser.Builder::build);

        return new UngdomsytelseSatsResultat(
            satsTidslinje,
            LagRegelSporing.lagRegelSporingFraTidslinjer(Map.of(
                "grunnbeløptidslinje", grunnbeløpTidslinje,
                "satsOgGrunnbeløpfaktorTidslinje", satsOgGrunnbeløpfaktorTidslinje,
                "barnetilleggTidslinje", barnetilleggResultat.barnetilleggTidslinje()
            )),
            lagRegelInput(input.perioder(), input.fødselsdato(), input.harTriggerBeregnHøySats(), input.harBeregnetHøySatsTidligere(), input.barnsFødselOgDødInformasjon())
        );
    }

    private static UtledSatsInput mapTilSatsInput(BeregnDagsatsInput input) {
        return new UtledSatsInput(input.fødselsdato(), input.harTriggerBeregnHøySats(), input.harBeregnetHøySatsTidligere(), input.perioder().getMinLocalDate());
    }

    private static UngdomsytelseSatser.Builder leggTilSatsTypeOgGrunnbeløpFaktor(SatsOgGrunnbeløpfaktor sats) {
        return UngdomsytelseSatser.builder().medGrunnbeløpFaktor(sats.grunnbeløpFaktor()).medSatstype(sats.satstype());
    }

    private static LocalDateSegmentCombinator<UngdomsytelseSatser.Builder, Grunnbeløp, UngdomsytelseSatser.Builder> leggTilGrunnbeløp() {
        return (di, lhs, rhs) -> {
            var builder = lhs.getValue().kopi();
            return new LocalDateSegment<>(di, builder.medGrunnbeløp(rhs.getValue().verdi()));
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

    private static String lagRegelInput(
        LocalDateTimeline<Boolean> perioder,
        LocalDate fødselsdato,
        boolean harTriggerBeregnHøySats,
        boolean harBeregnetHøySatsTidligere,
        List<FødselOgDødInfo> barnPersoninformasjon) {
        var regelInput = new RegelInput(
            TidslinjeUtil.tilPerioder(perioder),
            fødselsdato,
            harTriggerBeregnHøySats,
            harBeregnetHøySatsTidligere,
            barnPersoninformasjon
        );
        return JsonObjectMapper.toJson(regelInput, LagRegelSporing.JsonMappingFeil.FACTORY::jsonMappingFeil);
    }


    public record RegelInput(List<Periode> perioder,
                             LocalDate fødselsdato,
                             boolean harTriggerBeregnHøySats,
                             boolean harBeregnetHøySatsTidligere,
                             List<FødselOgDødInfo> barnFødselOgDød) {
    }


}
