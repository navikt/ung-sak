package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell;

import java.util.List;

public class MedisinskVilkårResultat {

    public static final String DOKUMENTASJON_LANGVARIG_SYKDOM_PERIODER = "resultat.dokumentasjon.langvarigsykdom";

    private List<LangvarigSykdomPeriode> langvarigSykdomPerioder;

    public List<LangvarigSykdomPeriode> getLangvarigSykdomPerioder() {
        return langvarigSykdomPerioder;
    }

    public void setLangvarigSykdomPerioder(List<LangvarigSykdomPeriode> langvarigSykdomPerioder) {
        this.langvarigSykdomPerioder = langvarigSykdomPerioder;
    }

}
