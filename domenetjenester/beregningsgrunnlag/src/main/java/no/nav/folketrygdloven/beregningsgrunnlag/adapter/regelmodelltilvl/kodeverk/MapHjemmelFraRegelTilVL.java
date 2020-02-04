package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.kodeverk;

import java.util.EnumMap;
import java.util.Map;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagHjemmel;
import no.nav.k9.kodeverk.beregningsgrunnlag.Hjemmel;

class MapHjemmelFraRegelTilVL {

    private static final Map<BeregningsgrunnlagHjemmel, Hjemmel> hjemmelMap =
        new EnumMap<>(BeregningsgrunnlagHjemmel.class);
    static {
        hjemmelMap.put(BeregningsgrunnlagHjemmel.F_14_7, Hjemmel.F_14_7);
        hjemmelMap.put(BeregningsgrunnlagHjemmel.F_14_7_8_30, Hjemmel.F_14_7_8_30);
        hjemmelMap.put(BeregningsgrunnlagHjemmel.F_14_7_8_35, Hjemmel.F_14_7_8_35);
        hjemmelMap.put(BeregningsgrunnlagHjemmel.F_14_7_8_38, Hjemmel.F_14_7_8_38);
        hjemmelMap.put(BeregningsgrunnlagHjemmel.F_14_7_8_40, Hjemmel.F_14_7_8_40);
        hjemmelMap.put(BeregningsgrunnlagHjemmel.F_14_7_8_41, Hjemmel.F_14_7_8_41);
        hjemmelMap.put(BeregningsgrunnlagHjemmel.F_14_7_8_42, Hjemmel.F_14_7_8_42);
        hjemmelMap.put(BeregningsgrunnlagHjemmel.F_14_7_8_43, Hjemmel.F_14_7_8_43);
        hjemmelMap.put(BeregningsgrunnlagHjemmel.F_14_7_8_47, Hjemmel.F_14_7_8_47);
        hjemmelMap.put(BeregningsgrunnlagHjemmel.F_14_7_8_49, Hjemmel.F_14_7_8_49);
    }

    private MapHjemmelFraRegelTilVL() {
        // skjul public constructor
    }

    static Hjemmel map(BeregningsgrunnlagHjemmel hjemmel) {
        if (hjemmel == null) {
            return Hjemmel.UDEFINERT;
        }
        return hjemmelMap.get(hjemmel);
    }
}
