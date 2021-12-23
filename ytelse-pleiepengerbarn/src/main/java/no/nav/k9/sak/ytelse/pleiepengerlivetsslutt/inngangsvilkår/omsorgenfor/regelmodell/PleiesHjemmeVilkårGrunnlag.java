package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.omsorgenfor.regelmodell;

import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;

public class PleiesHjemmeVilkårGrunnlag implements VilkårGrunnlag {

    private final Boolean pleiesHjemme;

    public PleiesHjemmeVilkårGrunnlag(Boolean pleiesHjemme) {
        this.pleiesHjemme = pleiesHjemme;
    }

    public Boolean getPleiesHjemme() {
        return pleiesHjemme;
    }

    @Override
    public String toString() {
        return "PleiesHjemmeVilkårGrunnlag{" +
            "pleiesHjemme=" + pleiesHjemme +
            '}';
    }

}
