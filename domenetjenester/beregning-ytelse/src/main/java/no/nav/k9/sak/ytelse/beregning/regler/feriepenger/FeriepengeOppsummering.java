package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import java.time.Year;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import no.nav.k9.sak.ytelse.beregning.regelmodell.MottakerType;

public class FeriepengeOppsummering {

    public static FeriepengeOppsummering tom(){
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

    public static long utledAbsoluttverdiDifferanse(FeriepengeOppsummering a, FeriepengeOppsummering b) {
        Set<MottakerOgOpptjeningsår> nøkler = new HashSet<>();
        nøkler.addAll(a.tilkjentPrMottakerOgÅr.keySet());
        nøkler.addAll(b.tilkjentPrMottakerOgÅr.keySet());
        long absoluttDifferanse = 0;
        for (MottakerOgOpptjeningsår nøkkel : nøkler) {
            long aVerdi = a.tilkjentPrMottakerOgÅr.getOrDefault(nøkkel, 0L);
            long bVerdi = b.tilkjentPrMottakerOgÅr.getOrDefault(nøkkel, 0L);
            absoluttDifferanse = Math.abs(aVerdi - bVerdi);
        }
        return absoluttDifferanse;
    }

    static record MottakerOgOpptjeningsår(
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
