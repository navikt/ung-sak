package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell;

import java.util.Objects;

public class MedisinskMellomregningData {

    private final MedisinskVilkårGrunnlag grunnlag;

    MedisinskMellomregningData(MedisinskVilkårGrunnlag grunnlag) {
        Objects.requireNonNull(grunnlag);
        this.grunnlag = grunnlag;
    }

    public MedisinskVilkårGrunnlag getGrunnlag() {
        return grunnlag;
    }

    public void oppdaterResultat(MedisinskVilkårResultat resultatStruktur) {

    }
}
