package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak;


public class MapPeriodeÅrsakFraVlTilRegel {

    private static final Map<no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak, PeriodeÅrsak> PERIODE_ÅRSAK_MAP;

    static {
        Map<no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak, PeriodeÅrsak> mapPeriodeÅrsak = new LinkedHashMap<>();

        mapPeriodeÅrsak.put(no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak.NATURALYTELSE_BORTFALT, PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        mapPeriodeÅrsak.put(no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak.ARBEIDSFORHOLD_AVSLUTTET, PeriodeÅrsak.ARBEIDSFORHOLD_AVSLUTTET);
        mapPeriodeÅrsak.put(no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak.NATURALYTELSE_TILKOMMER, PeriodeÅrsak.NATURALYTELSE_TILKOMMER);
        mapPeriodeÅrsak.put(no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV, PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV);
        mapPeriodeÅrsak.put(no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak.REFUSJON_OPPHØRER, PeriodeÅrsak.REFUSJON_OPPHØRER);
        mapPeriodeÅrsak.put(no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak.GRADERING, PeriodeÅrsak.GRADERING);
        mapPeriodeÅrsak.put(no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak.GRADERING_OPPHØRER, PeriodeÅrsak.GRADERING_OPPHØRER);
        mapPeriodeÅrsak.put(no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak.ENDRING_I_AKTIVITETER_SØKT_FOR, PeriodeÅrsak.ENDRING_I_AKTIVITETER_SØKT_FOR);

        PERIODE_ÅRSAK_MAP = Collections.unmodifiableMap(mapPeriodeÅrsak);

    }

    private MapPeriodeÅrsakFraVlTilRegel() {
        // skjul public constructor
    }

    public static PeriodeÅrsak map(no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak periodeÅrsak) {
        return PERIODE_ÅRSAK_MAP.get(periodeÅrsak);
    }

}
