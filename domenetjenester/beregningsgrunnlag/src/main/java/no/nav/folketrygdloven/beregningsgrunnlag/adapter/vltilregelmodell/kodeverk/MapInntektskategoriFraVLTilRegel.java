package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Inntektskategori;

public class MapInntektskategoriFraVLTilRegel {

    private static final Map<no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori, Inntektskategori> MAP_INNTEKTSKATEGORI;

    static {
        Map<no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori, Inntektskategori> mapInntektskategori = new LinkedHashMap<>();
        mapInntektskategori.put(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori.ARBEIDSAVKLARINGSPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER);
        mapInntektskategori.put(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        mapInntektskategori.put(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER);
        mapInntektskategori.put(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori.DAGMAMMA, Inntektskategori.DAGMAMMA);
        mapInntektskategori.put(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori.DAGPENGER, Inntektskategori.DAGPENGER);
        mapInntektskategori.put(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori.FISKER, Inntektskategori.FISKER);
        mapInntektskategori.put(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori.FRILANSER, Inntektskategori.FRILANSER);
        mapInntektskategori.put(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori.JORDBRUKER, Inntektskategori.JORDBRUKER);
        mapInntektskategori.put(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        mapInntektskategori.put(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori.SJØMANN, Inntektskategori.SJØMANN);
        mapInntektskategori.put(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori.UDEFINERT, Inntektskategori.UDEFINERT);

        MAP_INNTEKTSKATEGORI = Collections.unmodifiableMap(mapInntektskategori);
    }

    private MapInntektskategoriFraVLTilRegel() {
        // skjul public constructor
    }

    public static Inntektskategori map(no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori inntektskategori) {
        if (MAP_INNTEKTSKATEGORI.containsKey(inntektskategori)) {
            return MAP_INNTEKTSKATEGORI.get(inntektskategori);
        }
        throw new IllegalStateException("Inntektskategori (" + inntektskategori + ") finnes ikke i mappingen.");
    }
}
