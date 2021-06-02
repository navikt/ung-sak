package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
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

    AleneOmOmsorgVilkårsVurderingTjeneste() {
        // for proxy
    }

    @Inject
    public AleneOmOmsorgVilkårsVurderingTjeneste(BehandlingRepository behandlingRepository,
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
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var periode = utledPeriode(behandling);
        return Map.of(
            VilkårType.UTVIDETRETT, new TreeSet<>(Set.of(periode)),
            VilkårType.OMSORGEN_FOR, new TreeSet<>(Set.of(periode)));
    }

    private DatoIntervallEntitet utledPeriode(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        var søknad = søknadRepository.hentSøknad(behandling);
        var maksdato = getMaksDato(fagsak);
        var søknadFom = søknad.getMottattDato();
        var mindato = getMinDato(søknadFom);
        if (maksdato.isAfter(mindato)) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(mindato, maksdato);
        } else {
            log.warn("maksdato [{}] er før mindato[{}], søknadsdato[{}], har ingen periode å vurdere", maksdato, mindato, søknadFom);
            return DatoIntervallEntitet.fraOgMedTilOgMed(mindato, søknadFom);
        }
    }

    private LocalDate getMinDato(LocalDate søknadFom) {
        return søknadFom.minusMonths(3).withDayOfMonth(1); // tar fra start av måned 3 mnd før for sikkerhetsskyld
    }

    private LocalDate getMaksDato(@SuppressWarnings("unused") Fagsak fagsak) {
        var maksdato = Tid.TIDENES_ENDE;
        return maksdato;
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return Integer.MAX_VALUE;
    }
}
