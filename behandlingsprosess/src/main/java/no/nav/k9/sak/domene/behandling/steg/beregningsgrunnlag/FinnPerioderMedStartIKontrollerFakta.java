package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;

@Dependent
public class FinnPerioderMedStartIKontrollerFakta {

    private static final Logger log = LoggerFactory.getLogger(FinnPerioderMedStartIKontrollerFakta.class);


    private final VilkårResultatRepository vilkårResultatRepository;
    private final VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;

    private final boolean isEnabled;

    private final Set<Long> BEHANDLING_ID_MED_FREMOVERHOPP = Set.of(1685776L);


    @Inject
    public FinnPerioderMedStartIKontrollerFakta(VilkårResultatRepository vilkårResultatRepository,
                                                VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider,
                                                @KonfigVerdi(value = "PSB_START_I_KOFAKBER_VED_FORLENGELSE_OPPTJENING", defaultVerdi = "false") boolean isEnabled) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
        this.isEnabled = isEnabled;
    }

    /**
     * Finner perioder skal kopiere resultatet fra fastsett skjæringstidspunkt fra forrige behandling/kobling og starte prosessering av nytt beregningsgrunnlag
     * i steget kontroller fakta beregning
     *
     * @param ref                          Behandlingreferanse
     * @param allePerioder                 Alle perioder (vilkårsperioder)
     * @param forlengelseperioderBeregning Perioder med forlengelse i beregning
     * @return Perioder med start i kontroller fakta beregning
     */
    public NavigableSet<PeriodeTilVurdering> finnPerioder(BehandlingReferanse ref,
                                                          NavigableSet<PeriodeTilVurdering> allePerioder,
                                                          Set<PeriodeTilVurdering> forlengelseperioderBeregning) {
        if (!isEnabled && !BEHANDLING_ID_MED_FREMOVERHOPP.contains(ref.getBehandlingId())) {
            return new TreeSet<>();
        }
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        periodeFilter.ignorerAvslåttePerioder();
        var oppfylteBeregningsperioderForrigeBehandling = finnOppfylteVilkårsperioderForrigeBehandling(ref);
        var perioder = allePerioder.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet());
        var forlengelserIOpptjening = periodeFilter.filtrerPerioder(perioder, VilkårType.OPPTJENINGSVILKÅRET).stream()
            .filter(PeriodeTilVurdering::erForlengelse)
            .collect(Collectors.toSet());

        log.info("Perioder med forlengelse i opptjening: " + forlengelserIOpptjening);
        log.info("Perioder med oppfylte perioder forrige behandling: " + oppfylteBeregningsperioderForrigeBehandling);

        // Filtrerer ut perioder som er forlengelse i opptjening, men ikkje beregning
        return allePerioder.stream()
            .filter(forlengelserIOpptjening::contains)
            .filter(periode -> !forlengelseperioderBeregning.contains(periode))
            .filter(periode -> oppfylteBeregningsperioderForrigeBehandling.contains(periode.getPeriode()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<DatoIntervallEntitet> finnOppfylteVilkårsperioderForrigeBehandling(BehandlingReferanse ref) {
        return vilkårResultatRepository.hentHvisEksisterer(ref.getOriginalBehandlingId().orElseThrow()).orElseThrow()
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream()
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> p.getGjeldendeUtfall().equals(Utfall.OPPFYLT))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toSet());
    }


}
