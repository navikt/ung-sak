package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

public class InputForKompletthetsvurdering {

    private boolean skipVurderingMotArbeid;
    private Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder;
    private Set<PerioderFraSøknad> perioderFraSøknad;

    public InputForKompletthetsvurdering(boolean skipVurderingMotArbeid,
                                         Set<PerioderFraSøknad> perioderFraSøknad,
                                         Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder) {
        this.skipVurderingMotArbeid = skipVurderingMotArbeid;
        this.perioderFraSøknad = perioderFraSøknad;
        this.vurderteSøknadsperioder = vurderteSøknadsperioder;
    }

    public boolean getSkalHoppeOverVurderingMotArbeid() {
        return skipVurderingMotArbeid;
    }

    public Set<PerioderFraSøknad> getPerioderFraSøknadene() {
        return perioderFraSøknad;
    }

    public Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> getVurderteSøknadsperioder() {
        return vurderteSøknadsperioder;
    }
}
