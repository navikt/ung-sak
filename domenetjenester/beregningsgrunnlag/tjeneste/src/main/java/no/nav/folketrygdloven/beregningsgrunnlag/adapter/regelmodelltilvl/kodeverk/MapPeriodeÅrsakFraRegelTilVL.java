package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.kodeverk;

import java.util.EnumMap;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.PeriodeÅrsak;

public class MapPeriodeÅrsakFraRegelTilVL {

    private static final EnumMap<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak, PeriodeÅrsak> PERIODE_ÅRSAK_MAP =
        new EnumMap<>(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.class);

    static {
        PERIODE_ÅRSAK_MAP.put(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.NATURALYTELSE_BORTFALT, PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        PERIODE_ÅRSAK_MAP.put(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.ARBEIDSFORHOLD_AVSLUTTET, PeriodeÅrsak.ARBEIDSFORHOLD_AVSLUTTET);
        PERIODE_ÅRSAK_MAP.put(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.NATURALYTELSE_TILKOMMER, PeriodeÅrsak.NATURALYTELSE_TILKOMMER);
        PERIODE_ÅRSAK_MAP.put(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV, PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV);
        PERIODE_ÅRSAK_MAP.put(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.REFUSJON_OPPHØRER, PeriodeÅrsak.REFUSJON_OPPHØRER);
        PERIODE_ÅRSAK_MAP.put(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.GRADERING, PeriodeÅrsak.GRADERING);
        PERIODE_ÅRSAK_MAP.put(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.GRADERING_OPPHØRER, PeriodeÅrsak.GRADERING_OPPHØRER);
        PERIODE_ÅRSAK_MAP.put(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak.ENDRING_I_AKTIVITETER_SØKT_FOR, PeriodeÅrsak.ENDRING_I_AKTIVITETER_SØKT_FOR);
    }

    private MapPeriodeÅrsakFraRegelTilVL() {
        // skjul public constructor
    }

    public static PeriodeÅrsak map(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeÅrsak periodeÅrsak) {
        return PERIODE_ÅRSAK_MAP.get(periodeÅrsak);
    }
}
