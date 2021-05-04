package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

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

@FagsakYtelseTypeRef("OMP_KS")
@BehandlingTypeRef
@RequestScoped
public class KroniskSykVilkårsVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private static final Logger log = LoggerFactory.getLogger(KroniskSykVilkårsVurderingTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private SøknadRepository søknadRepository;
    private PersoninfoAdapter personinfoAdapter;

    KroniskSykVilkårsVurderingTjeneste() {
        // for proxy
    }

    @Inject
    public KroniskSykVilkårsVurderingTjeneste(BehandlingRepository behandlingRepository,
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
        var periode = new TreeSet<>(Set.of(utledPeriode(behandling)));
        return Map.of(
            VilkårType.UTVIDETRETT, periode,
            VilkårType.OMSORGEN_FOR, periode);
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
