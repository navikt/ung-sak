package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell;

import java.util.List;

public class PleiesHjemmeVilkårResultat {

    public static final String PERIODER_PLEIES_HJEMME = "resultat.perioder.pleies.hjemme";

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
