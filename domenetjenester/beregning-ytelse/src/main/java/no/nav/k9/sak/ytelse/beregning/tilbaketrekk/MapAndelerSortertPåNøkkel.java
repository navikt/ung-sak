package no.nav.k9.sak.ytelse.beregning.tilbaketrekk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAktivitetsnøkkelV2;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;

public class MapAndelerSortertPåNøkkel {
    public static List<BRNøkkelMedAndeler> map(List<BeregningsresultatAndel> resultatandeler) {
        Map<BeregningsresultatAktivitetsnøkkelV2, List<BeregningsresultatAndel>> nøkkelMap = lagMapSorertPåNøkkel(resultatandeler);
        return lagListeMedSammenligningsandeler(nøkkelMap);
    }

    private static List<BRNøkkelMedAndeler> lagListeMedSammenligningsandeler(Map<BeregningsresultatAktivitetsnøkkelV2, List<BeregningsresultatAndel>> nøkkelMap) {
        List<BRNøkkelMedAndeler> listeSortertPåNøkkel = new ArrayList<>();
        nøkkelMap.forEach((key, value) -> {
            BRNøkkelMedAndeler sammenligningAndel = new BRNøkkelMedAndeler(key);
            value.forEach(sammenligningAndel::leggTilAndel);
            listeSortertPåNøkkel.add(sammenligningAndel);
        });
        return listeSortertPåNøkkel;
    }

    private static Map<BeregningsresultatAktivitetsnøkkelV2, List<BeregningsresultatAndel>> lagMapSorertPåNøkkel(List<BeregningsresultatAndel> resultatandeler) {
        Map<BeregningsresultatAktivitetsnøkkelV2, List<BeregningsresultatAndel>> nøkkelMap = new HashMap<>();
        resultatandeler.forEach(andel -> {
            BeregningsresultatAktivitetsnøkkelV2 nøkkel = andel.getAktivitetsnøkkelV2();
            List<BeregningsresultatAndel> andelsliste = nøkkelMap.getOrDefault(nøkkel, new ArrayList<>());
            andelsliste.add(andel);
            nøkkelMap.put(nøkkel, andelsliste);
        });
        return nøkkelMap;
    }
}
