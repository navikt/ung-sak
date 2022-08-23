package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aldersvilkår.regelmodell;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;

public class AldersvilkårBarnVilkårGrunnlag implements VilkårGrunnlag {

    private final List<LocalDate> fødselsdatoBarn;
    private final FagsakYtelseType fagsakYtelseType;
    private DatoIntervallEntitet vilkårsperiode;

    public AldersvilkårBarnVilkårGrunnlag(List<LocalDate> fødselsdatoBarn, FagsakYtelseType fagsakYtelseType, DatoIntervallEntitet vilkårsperiode) {
        Objects.requireNonNull(fødselsdatoBarn);
        Objects.requireNonNull(fagsakYtelseType);
        Objects.requireNonNull(vilkårsperiode);
        if (fødselsdatoBarn.isEmpty()){
            throw new IllegalArgumentException("Trenger fødselsdato for minst ett barn");
        }
        this.fødselsdatoBarn = fødselsdatoBarn;
        this.fagsakYtelseType = fagsakYtelseType;
        this.vilkårsperiode = vilkårsperiode;

    }
    public List<LocalDate> getFødselsdatoBarn() {
        return fødselsdatoBarn;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    public DatoIntervallEntitet getVilkårsperiode() {
        return vilkårsperiode;
    }

    @Override
    public String toString() {
        return "AldersvilkårBarnVilkårGrunnlag{" +
            "fødselsdatoBarn=" + fødselsdatoBarn +
            ", fagsakYtelseType=" + fagsakYtelseType +
            ", vilkårsperiode=" + vilkårsperiode +
            '}';
    }
}
