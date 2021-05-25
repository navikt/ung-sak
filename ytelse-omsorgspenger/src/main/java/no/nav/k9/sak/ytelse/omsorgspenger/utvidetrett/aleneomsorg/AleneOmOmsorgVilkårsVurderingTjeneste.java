package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg;

import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@FagsakYtelseTypeRef("OMP_AO")
@BehandlingTypeRef
@RequestScoped
public class AleneOmOmsorgVilkårsVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {
    private static final Logger log = LoggerFactory.getLogger(AleneOmOmsorgVilkårsVurderingTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private SøknadRepository søknadRepository;
    private PersoninfoAdapter personinfoAdapter;

    AleneOmOmsorgVilkårsVurderingTjeneste() {
        // for proxy
    }

    @Inject
    public AleneOmOmsorgVilkårsVurderingTjeneste(BehandlingRepository behandlingRepository,
                                                 PersoninfoAdapter personinfoAdapter,
                                                 SøknadRepository søknadRepository) {
        this.behandlingRepository = behandlingRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.søknadRepository = søknadRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var periode = utledPeriode(behandling);
        return new TreeSet<>(Set.of(periode));
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var søknadsperiode = utledPeriode(behandling);
        var omsorgperiode = DatoIntervallEntitet.fraOgMed(søknadsperiode.getFomDato()); // lar denne perioden stå åpen for omsorg
        return Map.of(
            VilkårType.UTVIDETRETT, new TreeSet<>(Set.of(søknadsperiode)),
            VilkårType.OMSORGEN_FOR, new TreeSet<>(Set.of(omsorgperiode)));
    }

    private DatoIntervallEntitet utledPeriode(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        var søknad = søknadRepository.hentSøknad(behandling);
        var personinfo = personinfoAdapter.hentBrukerBasisForAktør(fagsak.getPleietrengendeAktørId()).orElseThrow(() -> new IllegalStateException("Mangler personinfo for pleietrengende aktørId"));

        var maksdato = personinfo.getFødselsdato().plusYears(18).withMonth(12).withDayOfMonth(31); // siste dag året fyller 18
        var søknadFom = søknad.getMottattDato();
        if (maksdato.isAfter(søknadFom)) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(søknadFom, maksdato);
        } else {
            log.warn("maksdato [{}] er før søknadsdato[{}], har ingen periode å vurdere", maksdato, søknadFom);
            return DatoIntervallEntitet.fraOgMedTilOgMed(søknadFom, søknadFom);
        }
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return Integer.MAX_VALUE;
    }
}
