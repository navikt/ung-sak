package no.nav.ung.sak.mottak.inntektsmelding;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

import no.nav.inntektsmelding.xml.kodeliste._20210216.YtelseKodeliste;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

public class MapYtelseTypeFraInntektsmelding {

    private static final Map<String, FagsakYtelseType> MAP = Map.of(
        lc(YtelseKodeliste.FORELDREPENGER), FagsakYtelseType.FORELDREPENGER,
        lc(YtelseKodeliste.OMSORGSPENGER), FagsakYtelseType.OMSORGSPENGER,
        lc(YtelseKodeliste.OPPLAERINGSPENGER), FagsakYtelseType.OPPLÆRINGSPENGER,
        lc(YtelseKodeliste.PLEIEPENGER_BARN), FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
        lc(YtelseKodeliste.PLEIEPENGER_NAERSTAAENDE), FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE,
        lc(YtelseKodeliste.SVANGERSKAPSPENGER), FagsakYtelseType.SVANGERSKAPSPENGER,
        lc(YtelseKodeliste.SYKEPENGER), FagsakYtelseType.SYKEPENGER);

    public static FagsakYtelseType mapYtelseType(String ytelseTypeFraInntektsmelding) {
        if("Pleiepenger".equalsIgnoreCase(ytelseTypeFraInntektsmelding)) {
            // Unnskyld, men Stian sa det var greit.
            return FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
        }
        var ytelse = MAP.get(ytelseTypeFraInntektsmelding == null ? null : lc(ytelseTypeFraInntektsmelding));
        if (ytelse == null) {
            throw new IllegalArgumentException("Ukjent ytelsetype fra inntektsmelding : " + ytelseTypeFraInntektsmelding + "; Sjekk verdier og mapping fra: " + EnumSet.allOf(YtelseKodeliste.class));
        }
        return ytelse;
    }

    private static String lc(YtelseKodeliste ytelseType) {
        return lc(ytelseType.value());
    }

    private static String lc(String str) {
        return str.toLowerCase(Locale.getDefault());
    }
}
