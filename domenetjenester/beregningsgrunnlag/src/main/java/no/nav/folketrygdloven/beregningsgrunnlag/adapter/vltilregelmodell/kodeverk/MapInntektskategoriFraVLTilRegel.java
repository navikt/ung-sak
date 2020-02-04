package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Inntektskategori;

public class MapInntektskategoriFraVLTilRegel {

    private static final Map<no.nav.k9.kodeverk.iay.Inntektskategori, Inntektskategori> MAP_INNTEKTSKATEGORI;

    static {
        Map<no.nav.k9.kodeverk.iay.Inntektskategori, Inntektskategori> mapInntektskategori = new LinkedHashMap<>();
        mapInntektskategori.put(no.nav.k9.kodeverk.iay.Inntektskategori.ARBEIDSAVKLARINGSPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER);
        mapInntektskategori.put(no.nav.k9.kodeverk.iay.Inntektskategori.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        mapInntektskategori.put(no.nav.k9.kodeverk.iay.Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER);
        mapInntektskategori.put(no.nav.k9.kodeverk.iay.Inntektskategori.DAGMAMMA, Inntektskategori.DAGMAMMA);
        mapInntektskategori.put(no.nav.k9.kodeverk.iay.Inntektskategori.DAGPENGER, Inntektskategori.DAGPENGER);
        mapInntektskategori.put(no.nav.k9.kodeverk.iay.Inntektskategori.FISKER, Inntektskategori.FISKER);
        mapInntektskategori.put(no.nav.k9.kodeverk.iay.Inntektskategori.FRILANSER, Inntektskategori.FRILANSER);
        mapInntektskategori.put(no.nav.k9.kodeverk.iay.Inntektskategori.JORDBRUKER, Inntektskategori.JORDBRUKER);
        mapInntektskategori.put(no.nav.k9.kodeverk.iay.Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        mapInntektskategori.put(no.nav.k9.kodeverk.iay.Inntektskategori.SJØMANN, Inntektskategori.SJØMANN);
        mapInntektskategori.put(no.nav.k9.kodeverk.iay.Inntektskategori.UDEFINERT, Inntektskategori.UDEFINERT);

        MAP_INNTEKTSKATEGORI = Collections.unmodifiableMap(mapInntektskategori);
    }

    private MapInntektskategoriFraVLTilRegel() {
        // skjul public constructor
    }

    public static Inntektskategori map(no.nav.k9.kodeverk.iay.Inntektskategori inntektskategori) {
        if (MAP_INNTEKTSKATEGORI.containsKey(inntektskategori)) {
            return MAP_INNTEKTSKATEGORI.get(inntektskategori);
        }
        throw new IllegalStateException("Inntektskategori (" + inntektskategori + ") finnes ikke i mappingen.");
    }
}
