package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import java.time.Year;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.sak.ytelse.beregning.regelmodell.MottakerType;

public class FeriepengeOppsummering {

    public static FeriepengeOppsummering tom() {
        return new FeriepengeOppsummering(Map.of());
    }

    private Map<MottakerOgOpptjeningsår, Long> tilkjentPrMottakerOgÅr;

    private FeriepengeOppsummering(Map<MottakerOgOpptjeningsår, Long> tilkjentPrMottakerOgÅr) {
        this.tilkjentPrMottakerOgÅr = tilkjentPrMottakerOgÅr;
    }

    public static class Builder {

        private Map<MottakerOgOpptjeningsår, Long> tilkjentPrMottakerOgÅr = new HashMap<>();

        public void leggTil(Year opptjeningsår, MottakerType mottakerType, String mottakerId, long beløp) {
            MottakerOgOpptjeningsår nøkkel = new MottakerOgOpptjeningsår(opptjeningsår, mottakerType, mottakerId);
            Long eksisterende = tilkjentPrMottakerOgÅr.getOrDefault(nøkkel, 0L);
            tilkjentPrMottakerOgÅr.put(nøkkel, eksisterende + beløp);
        }

        public FeriepengeOppsummering build() {
            return new FeriepengeOppsummering(tilkjentPrMottakerOgÅr);
        }

    }

    public static Set<Year> utledOpptjeningsårSomHarDifferanse(FeriepengeOppsummering a, FeriepengeOppsummering b) {
        return utledOpptjeningsårSomHarDifferanseOver(a, b, 0L);
    }

    public static Set<Year> utledOpptjeningsårSomHarDifferanseOver(FeriepengeOppsummering a, FeriepengeOppsummering b, long akseptabelDifferranseUtenRevurdering) {
        Set<MottakerOgOpptjeningsår> nøkler = new HashSet<>();
        nøkler.addAll(a.tilkjentPrMottakerOgÅr.keySet());
        nøkler.addAll(b.tilkjentPrMottakerOgÅr.keySet());
        Map<Year, Long> absoluttDifferansePrÅr = new HashMap<>();
        for (MottakerOgOpptjeningsår nøkkel : nøkler) {
            Year år = nøkkel.opptjeningsår;
            long aVerdi = a.tilkjentPrMottakerOgÅr.getOrDefault(nøkkel, 0L);
            long bVerdi = b.tilkjentPrMottakerOgÅr.getOrDefault(nøkkel, 0L);
            long absoluttDifferanse = Math.abs(aVerdi - bVerdi);
            absoluttDifferansePrÅr.put(år, absoluttDifferansePrÅr.getOrDefault(år, 0L) + absoluttDifferanse);
        }
        return absoluttDifferansePrÅr.entrySet().stream()
            .filter(e -> e.getValue() > akseptabelDifferranseUtenRevurdering)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    record MottakerOgOpptjeningsår(
        Year opptjeningsår,
        MottakerType mottakerType,
        String mottkerId) {

        public MottakerOgOpptjeningsår {
            Objects.requireNonNull(opptjeningsår);
            Objects.requireNonNull(mottakerType);
            if (mottakerType == MottakerType.BRUKER && mottkerId != null) {
                throw new IllegalArgumentException("ikke sett mottakerId for BRUKER");
            }
            if (mottakerType == MottakerType.ARBEIDSGIVER && mottkerId == null) {
                throw new IllegalArgumentException("mangler mottakerId for ARBEIDSGIVER");
            }
        }
    }
}
