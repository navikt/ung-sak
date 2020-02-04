package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Aktivitet;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class MapOpptjeningAktivitetTypeFraVLTilRegel {
    private static final Map<OpptjeningAktivitetType, Aktivitet> MAP_OPPTJENINGAKTIVITETTYPE_TIL_AKTIVITET;

    static {
        Map<OpptjeningAktivitetType, Aktivitet> map = new LinkedHashMap<>();
        map.put(OpptjeningAktivitetType.ARBEIDSAVKLARING, Aktivitet.AAP_MOTTAKER);
        map.put(OpptjeningAktivitetType.ARBEID, Aktivitet.ARBEIDSTAKERINNTEKT);
        map.put(OpptjeningAktivitetType.DAGPENGER, Aktivitet.DAGPENGEMOTTAKER);
        map.put(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE, Aktivitet.ETTERLØNN_SLUTTPAKKE);
        map.put(OpptjeningAktivitetType.FORELDREPENGER, Aktivitet.FORELDREPENGER_MOTTAKER);
        map.put(OpptjeningAktivitetType.FRILANS, Aktivitet.FRILANSINNTEKT);
        map.put(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE, Aktivitet.MILITÆR_ELLER_SIVILTJENESTE);
        map.put(OpptjeningAktivitetType.NÆRING, Aktivitet.NÆRINGSINNTEKT);
        map.put(OpptjeningAktivitetType.OMSORGSPENGER, Aktivitet.OMSORGSPENGER);
        map.put(OpptjeningAktivitetType.OPPLÆRINGSPENGER, Aktivitet.OPPLÆRINGSPENGER);
        map.put(OpptjeningAktivitetType.PLEIEPENGER, Aktivitet.PLEIEPENGER_MOTTAKER);
        map.put(OpptjeningAktivitetType.SVANGERSKAPSPENGER, Aktivitet.SVANGERSKAPSPENGER_MOTTAKER);
        map.put(OpptjeningAktivitetType.SYKEPENGER, Aktivitet.SYKEPENGER_MOTTAKER);
        map.put(OpptjeningAktivitetType.VENTELØNN_VARTPENGER, Aktivitet.VENTELØNN_VARTPENGER);
        map.put(OpptjeningAktivitetType.VIDERE_ETTERUTDANNING, Aktivitet.VIDERE_ETTERUTDANNING);

        map.put(OpptjeningAktivitetType.UDEFINERT, Aktivitet.UDEFINERT);
      
        /** @deprecated Ikke i bruk */
        map.put(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD, Aktivitet.UDEFINERT);
      
        /** @deprecated Fjerenet i forbindelse med TFP-1644. */
        map.put(OpptjeningAktivitetType.UTDANNINGSPERMISJON, Aktivitet.UDEFINERT);
        
        MAP_OPPTJENINGAKTIVITETTYPE_TIL_AKTIVITET = Collections.unmodifiableMap(map);
    }

    private MapOpptjeningAktivitetTypeFraVLTilRegel() {
        // skjul public constructor
    }

    public static Aktivitet map(OpptjeningAktivitetType aktivitetType) {
        if (MAP_OPPTJENINGAKTIVITETTYPE_TIL_AKTIVITET.containsKey(aktivitetType)) {
            return MAP_OPPTJENINGAKTIVITETTYPE_TIL_AKTIVITET.get(aktivitetType);
        }
        throw new IllegalStateException(aktivitetType + " finnes ikke i mappingen.");
    }
    
}
