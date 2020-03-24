package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell;

import java.util.List;

public class MedisinskVilkårResultat {

    public static final String PERIODER_UTEN_TILSYN_OG_PLEIE = "resultat.perioder.uten.tilsynogpleie";
    public static final String PERIODER_MED_EN_TILSYNSPERSONER = "resultat.perioder.tilsynspersoner.en";
    public static final String PERIODER_MED_TO_TILSYNSPERSONER = "resultat.perioder.tilsynspersoner.to";

    private List<PleiePeriode> pleieperioder;

    public MedisinskVilkårResultat() {
    }

    public List<PleiePeriode> getPleieperioder() {
        return List.copyOf(pleieperioder);
    }

    public void setPleieperioder(List<PleiePeriode> pleieperioder) {
        this.pleieperioder = pleieperioder;
    }

    @Override
    public String toString() {
        return "MedisinskVilkårResultat{" +
            "pleieperioder=" + pleieperioder +
            '}';
    }
}
