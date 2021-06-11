package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.util.List;
import java.util.Map;

import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

public class InputForKompletthetsvurdering {

    private boolean skipVurderingMotArbeid;
    private UttaksPerioderGrunnlag uttakGrunnlag;
    private Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder;

    public InputForKompletthetsvurdering(boolean skipVurderingMotArbeid, UttaksPerioderGrunnlag uttakGrunnlag,
                                         Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder) {
        this.skipVurderingMotArbeid = skipVurderingMotArbeid;
        this.uttakGrunnlag = uttakGrunnlag;
        this.vurderteSøknadsperioder = vurderteSøknadsperioder;
    }

    public boolean kanVurderesMotArbeidstid() {
        return !skipVurderingMotArbeid && uttakGrunnlag != null;
    }

    public boolean getSkalHoppeOverVurderingMotArbeid() {
        return skipVurderingMotArbeid;
    }

    public UttaksPerioderGrunnlag getUttakGrunnlag() {
        return uttakGrunnlag;
    }

    public Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> getVurderteSøknadsperioder() {
        return vurderteSøknadsperioder;
    }
}
