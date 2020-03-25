package no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

import no.nav.inntektsmelding.xml.kodeliste._20180702.YtelseKodeliste;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

public class MapYtelseTypeFraInntektsmelding {

    private static final Map<String, FagsakYtelseType> MAP = Map.of(
        lc(YtelseKodeliste.FORELDREPENGER), FagsakYtelseType.FORELDREPENGER,
        lc(YtelseKodeliste.OMSORGSPENGER), FagsakYtelseType.OMSORGSPENGER,
        lc(YtelseKodeliste.OPPLAERINGSPENGER), FagsakYtelseType.OPPLÆRINGSPENGER,
        lc(YtelseKodeliste.PLEIEPENGER_BARN), FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
        lc(YtelseKodeliste.PLEIEPENGER_PAAROERENDE), FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE,
        lc(YtelseKodeliste.SVANGERSKAPSPENGER), FagsakYtelseType.SVANGERSKAPSPENGER,
        lc(YtelseKodeliste.SYKEPENGER), FagsakYtelseType.SYKEPENGER);

    public static FagsakYtelseType mapYtelseType(String ytelseTypeFraInntektsmelding) {
        var ytelse = MAP.get(ytelseTypeFraInntektsmelding == null ? null : lc(ytelseTypeFraInntektsmelding));
        if(ytelse==null) {
            throw new IllegalArgumentException("Ukjent ytelsetype fra inntektsmelding : " + ytelseTypeFraInntektsmelding +"; Sjekk verdier og mapping fra: " + EnumSet.allOf(YtelseKodeliste.class));
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
