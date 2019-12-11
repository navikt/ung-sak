package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Inntektskategori;

public class MapInntektskategoriFraVLTilRegel {

    private static final Map<no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori, Inntektskategori> MAP_INNTEKTSKATEGORI;

    static {
        Map<no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori, Inntektskategori> mapInntektskategori = new LinkedHashMap<>();
        mapInntektskategori.put(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori.ARBEIDSAVKLARINGSPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER);
        mapInntektskategori.put(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        mapInntektskategori.put(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER);
        mapInntektskategori.put(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori.DAGMAMMA, Inntektskategori.DAGMAMMA);
        mapInntektskategori.put(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori.DAGPENGER, Inntektskategori.DAGPENGER);
        mapInntektskategori.put(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori.FISKER, Inntektskategori.FISKER);
        mapInntektskategori.put(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori.FRILANSER, Inntektskategori.FRILANSER);
        mapInntektskategori.put(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori.JORDBRUKER, Inntektskategori.JORDBRUKER);
        mapInntektskategori.put(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        mapInntektskategori.put(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori.SJØMANN, Inntektskategori.SJØMANN);
        mapInntektskategori.put(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori.UDEFINERT, Inntektskategori.UDEFINERT);

        MAP_INNTEKTSKATEGORI = Collections.unmodifiableMap(mapInntektskategori);
    }

    private MapInntektskategoriFraVLTilRegel() {
        // skjul public constructor
    }

    public static Inntektskategori map(no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori inntektskategori) {
        if (MAP_INNTEKTSKATEGORI.containsKey(inntektskategori)) {
            return MAP_INNTEKTSKATEGORI.get(inntektskategori);
        }
        throw new IllegalStateException("Inntektskategori (" + inntektskategori + ") finnes ikke i mappingen.");
    }
}
