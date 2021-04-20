package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.alene;

import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@FagsakYtelseTypeRef("OMP_MA")
@BehandlingTypeRef
@RequestScoped
public class MidlertidigAleneVilkårsVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private BehandlingRepository behandlingRepository;
    private SøknadRepository søknadRepository;

    MidlertidigAleneVilkårsVurderingTjeneste() {
        // for proxy
    }

    @Inject
    public MidlertidigAleneVilkårsVurderingTjeneste(BehandlingRepository behandlingRepository,
                                                    SøknadRepository søknadRepository) {
        this.behandlingRepository = behandlingRepository;
        this.søknadRepository = søknadRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var periode = utledPeriode(behandling);
        return new TreeSet<>(Set.of(periode));
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utled(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var søknadsperiode = utledPeriode(behandling);
        var omsorgperiode = DatoIntervallEntitet.fraOgMed(søknadsperiode.getFomDato()); // lar denne perioden stå åpen for omsorg
        return Map.of(
            VilkårType.UTVIDETRETT, new TreeSet<>(Set.of(søknadsperiode)),
            VilkårType.OMSORGEN_FOR, new TreeSet<>(Set.of(omsorgperiode)));
    }

    private DatoIntervallEntitet utledPeriode(Behandling behandling) {
        var søknad = søknadRepository.hentSøknad(behandling);
        var søknadsperiode = søknad.getSøknadsperiode();
        return DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFomDato(), søknadsperiode.getTomDato());
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return Integer.MAX_VALUE;
    }
}
