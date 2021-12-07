package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.omsorgenfor.regelmodell;

import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;

public class PleiesHjemmeVilkårGrunnlag implements VilkårGrunnlag {

    private final Boolean erPleietIHjemmet;

    public PleiesHjemmeVilkårGrunnlag(Boolean erPleietIHjemmet) {
        this.erPleietIHjemmet = erPleietIHjemmet;
    }

    public Boolean getErPleietIHjemmet() {
        return erPleietIHjemmet;
    }

    @Override
    public String toString() {
        return "OmsorgenForGrunnlag{" +
            "erPleietIHjemmet=" + erPleietIHjemmet +
            '}';
    }

}
