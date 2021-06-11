package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

public class InputForKompletthetsvurdering {

    private boolean skipVurderingMotArbeid;
    private Vilkår vilkår;
    private UttaksPerioderGrunnlag uttakGrunnlag;
    private Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder;

    public InputForKompletthetsvurdering(boolean skipVurderingMotArbeid, Vilkårene vilkår, UttaksPerioderGrunnlag uttakGrunnlag,
                                         Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder) {
        this.skipVurderingMotArbeid = skipVurderingMotArbeid;
        this.vilkår = vilkår != null ? vilkår.getVilkårene().stream().filter(it -> Objects.equals(VilkårType.OPPTJENINGSVILKÅRET, it.getVilkårType())).findAny().orElse(null) : null;// Forventer at opptjening eksisterer;
        this.uttakGrunnlag = uttakGrunnlag;
        this.vurderteSøknadsperioder = vurderteSøknadsperioder;
    }

    public boolean kanVurderesMotArbeidstid() {
        return !skipVurderingMotArbeid && vilkår != null && uttakGrunnlag != null;
    }

    public boolean getSkalHoppeOverVurderingMotArbeid() {
        return skipVurderingMotArbeid;
    }

    public Vilkår getVilkår() {
        return vilkår;
    }

    public UttaksPerioderGrunnlag getUttakGrunnlag() {
        return uttakGrunnlag;
    }

    public Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> getVurderteSøknadsperioder() {
        return vurderteSøknadsperioder;
    }
}
