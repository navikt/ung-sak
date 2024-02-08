package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.LagTidslinjeForRefusjon;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.typer.Beløp;

/**
 * Finner summert refusjonskravtidslinje. Skal normalt ikke brukes til å bestemme eksakt refusjonskrav i automatisk saksbehandling,
 * men kan brukes til visning.
 * <p>
 * NB: Logikken brukes for migreringer fra infotrygd, da dette er spesielle saker som krever sammenstilling av data før det sendes til kalkulus.
 */
class SammenstillRefusjonskravForInfotrygdmigrering {

    static LocalDateTimeline<BigDecimal> lagTidslinje(LocalDate stp, LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForAktivitet) {
        return inntektsmeldingerForAktivitet
            .map(ims -> ims.getValue().stream()
                .map(im -> tilRefusjontidslinje(im, stp))
                .reduce((t1, t2) -> t1.crossJoin(t2, StandardCombinators::sum))
                .orElse(LocalDateTimeline.empty())
                .intersection(ims.getLocalDateInterval())
                .stream()
                .toList())
            .compress();
    }

    private static LocalDateTimeline<BigDecimal> tilRefusjontidslinje(Inntektsmelding im, LocalDate stp) {
        return LagTidslinjeForRefusjon.lagRefusjontidslinje(im, stp).mapValue(Beløp::getVerdi);

    }


}
