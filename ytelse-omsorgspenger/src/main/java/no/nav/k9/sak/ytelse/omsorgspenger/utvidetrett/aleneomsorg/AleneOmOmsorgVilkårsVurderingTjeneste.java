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
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;

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
        var periode = utledPeriode(behandling);
        return Map.of(
            VilkårType.UTVIDETRETT, new TreeSet<>(Set.of(periode)),
            VilkårType.OMSORGEN_FOR, new TreeSet<>(Set.of(periode)));
    }

    private DatoIntervallEntitet utledPeriode(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        var søknad = søknadRepository.hentSøknad(behandling);
        AktørId barnAktørId = fagsak.getPleietrengendeAktørId();
        return utledPeriode(søknad.getSøknadsperiode(), barnAktørId);
    }

    DatoIntervallEntitet utledPeriode(DatoIntervallEntitet periode, AktørId barnAktørId) {
        var søknadFom = periode.getFomDato();
        var barninfo = personinfoAdapter.hentKjerneinformasjon(barnAktørId);

        // ikke åpne fagsaken før barnets fødselsdato
        var fødselsdato = barninfo.getFødselsdato();
        // 1. jan minst 3 år før søknad sendt inn (spesielle særtilfeller tillater at et går an å sette tilbake it itid
        var fristFørSøknadsdato = søknadFom.minusYears(3).withMonth(1).withDayOfMonth(1);

        var mindato = Set.of(fødselsdato, fristFørSøknadsdato).stream().max(LocalDate::compareTo).get();
        var maksdato = Tid.TIDENES_ENDE;
        if (maksdato.isAfter(mindato)) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(mindato, maksdato);
        } else {
            log.warn("maksdato [{}] er før mindato[{}], søknadsperiode fom[{}], har ingen periode å vurdere", maksdato, mindato, søknadFom);
            return DatoIntervallEntitet.fraOgMedTilOgMed(mindato, søknadFom);
        }
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return Integer.MAX_VALUE;
    }
}
