package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell;

import java.util.List;

import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;
import no.nav.k9.sak.typer.Periode;

public class PleiesHjemmeVilkårGrunnlag implements VilkårGrunnlag {

    private List<PleiePeriode> innleggelsesPerioder;
    private Periode vilkårsperiode;

    public PleiesHjemmeVilkårGrunnlag(Periode vilkårsperiode, List<PleiePeriode> innleggelsesPerioder) {
        this.vilkårsperiode = vilkårsperiode;
        this.innleggelsesPerioder = innleggelsesPerioder;
    }

    public List<PleiePeriode> getInnleggelsesPerioder() {
        return innleggelsesPerioder;
    }

    public Periode getVilkårsperiode() {
        return vilkårsperiode;
    }

    @Override
    public String toString() {
        return "PleiesHjemmeVilkårGrunnlag{" +
            "vilkårsperiode=" + vilkårsperiode +
            "innleggelsesPerioder=" + innleggelsesPerioder +
            '}';
    }
}
