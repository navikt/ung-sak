package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import java.util.List;

public class MedisinskVilkårResultat {

    public static final String PLEIEPERIODER_MED_PLEIELOKASJON = "resultat.pleieperioder";

    private List<PleiePeriode> pleieperioder;

    public List<PleiePeriode> getPleieperioder() {
        return List.copyOf(pleieperioder);
    }

    public void setPleieperioder(List<PleiePeriode> pleieperioder) {
        this.pleieperioder = pleieperioder;
    }

    @Override
    public String toString() {
        return "PleiesHjemmeVilkårResultat{" +
            "pleieperioder=" + pleieperioder +
            '}';
    }
}
