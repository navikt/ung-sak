package no.nav.ung.sak.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.ytelse.sats.GrunnbeløpfaktorTidslinje;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import static no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger.tilHeltall;

class Satsberegner {


    static long beregnDagsatsInklBarnetillegg(UngdomsytelseSatser satser) {
        return tilHeltall(satser.dagsats().add(BigDecimal.valueOf(satser.dagsatsBarnetillegg())));
    }

    static long beregnBarnetilleggSats(UngdomsytelseSatser satser) {
        if (satser.antallBarn() <= 0) {
            return 0;
        }
         return tilHeltall(BigDecimal.valueOf(satser.dagsatsBarnetillegg())
             .divide(BigDecimal.valueOf(satser.antallBarn()), RoundingMode.HALF_UP));
    }

    static String lagGrunnbeløpFaktorTekst(LocalDateSegment<UngdomsytelseSatser> satssegment) {
        BigDecimal faktor = GrunnbeløpfaktorTidslinje
            .finnStandardGrunnbeløpFaktorFor(satssegment.getLocalDateInterval())
            .setScale(3, RoundingMode.HALF_UP);
        String norskFaktor = NumberFormat.getInstance(Locale.forLanguageTag("no-NO"))
            .format(faktor);

        if (satssegment.getValue().satsType() == UngdomsytelseSatsType.LAV) {
            return "2/3 av " + norskFaktor;
        }
        return norskFaktor;
    }

    public static String tallTilNorskHunkjønnTekst(int antall) {
        return switch (antall) {
            case 0 -> "null";
            case 1 -> "ett";
            case 2 -> "to";
            case 3 -> "tre";
            case 4 -> "fire";
            case 5 -> "fem";
            case 6 -> "seks";
            case 7 -> "sju";
            case 8 -> "åtte";
            case 9 -> "ni";
            case 10 -> "ti";
            case 11 -> "elleve";
            case 12 -> "tolv";
            default -> String.valueOf(antall); // Fallback
        };
    }
}
