package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;

@ApplicationScoped
public class KalkulusStartpunktUtleder {

    private FinnPerioderMedStartIKontrollerFakta finnPerioderMedStartIKontrollerFakta;
    private VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;
    private BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;

    private static final Logger log = LoggerFactory.getLogger(KalkulusStartpunktUtleder.class);

    @Inject
    public KalkulusStartpunktUtleder(FinnPerioderMedStartIKontrollerFakta finnPerioderMedStartIKontrollerFakta,
                                     VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider,
                                     BeregningsgrunnlagVilkårTjeneste vilkårTjeneste) {
        this.finnPerioderMedStartIKontrollerFakta = finnPerioderMedStartIKontrollerFakta;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    public KalkulusStartpunktUtleder() {
    }

    /**
     * Utleder Map fra BehandlingStegType til en samling PeriodeTilVurdering som sier hvilke perioder som har det gitte steget som startpunkt.
     * <p>
     * Periodene pr behandlingsstegtype vil ha kopiert beregningsgrunnlag fra steget før og kjører beregning for alle steg etter (se BehandlingModell)
     *
     * @param ref Behandlingreferanse
     * @return Map med perioder som starter i gitt behandlingssteg
     */
    public Map<BehandlingStegType, NavigableSet<PeriodeTilVurdering>> utledPerioderPrStartpunkt(BehandlingReferanse ref) {
        var periodeStartStegMap = new HashMap<BehandlingStegType, NavigableSet<PeriodeTilVurdering>>();
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        periodeFilter.ignorerAvslåtteUnntattForLavtBeregningsgrunnlag();
        var utenAvslagFørBeregning = vilkårTjeneste.utledDetaljertPerioderTilVurdering(ref, periodeFilter);

        if (!utenAvslagFørBeregning.isEmpty()) {
            log.info("Perioder som skal vurderes i beregning: " + utenAvslagFørBeregning);
        }

        if (ref.getBehandlingType().equals(BehandlingType.REVURDERING)) {

            var forlengelseperioder = utenAvslagFørBeregning.stream().filter(PeriodeTilVurdering::erForlengelse).collect(Collectors.toCollection(TreeSet::new));
            if (!forlengelseperioder.isEmpty()) {
                log.info("Perioder med start i vurder refusjon: " + forlengelseperioder);
            }
            settStartpunkt(forlengelseperioder, periodeStartStegMap, BehandlingStegType.VURDER_REF_BERGRUNN);
            var startIKontrollerFaktaBeregning = finnPerioderMedStartIKontrollerFakta.finnPerioder(ref, utenAvslagFørBeregning, forlengelseperioder);
            if (!startIKontrollerFaktaBeregning.isEmpty()) {
                log.info("Perioder med start i kontroller fakta beregning: " + startIKontrollerFaktaBeregning);
            }
            settStartpunkt(startIKontrollerFaktaBeregning, periodeStartStegMap, BehandlingStegType.KONTROLLER_FAKTA_BEREGNING);
        }

        var perioderFraStart = finnPerioderFraStart(periodeStartStegMap, utenAvslagFørBeregning);
        if (!perioderFraStart.isEmpty()) {
            log.info("Perioder med start i fastsett skjæringstidspunkt: " + perioderFraStart);
        }
        settStartpunkt(perioderFraStart, periodeStartStegMap, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING);
        return periodeStartStegMap;

    }

    private static NavigableSet<PeriodeTilVurdering> finnPerioderFraStart(HashMap<BehandlingStegType, NavigableSet<PeriodeTilVurdering>> periodeStartStegMap, NavigableSet<PeriodeTilVurdering> utenAvslagFørBeregning) {
        var allePerioderMedHopp = periodeStartStegMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        var perioderFraStart = utenAvslagFørBeregning.stream().filter(it -> !allePerioderMedHopp.contains(it)).collect(Collectors.toCollection(TreeSet::new));
        return perioderFraStart;
    }

    private static void settStartpunkt(NavigableSet<PeriodeTilVurdering> hoppTilVurderRefusjonPerioder, HashMap<BehandlingStegType, NavigableSet<PeriodeTilVurdering>> periodeStartStegMap, BehandlingStegType behandlingStegType) {
        periodeStartStegMap.put(behandlingStegType, hoppTilVurderRefusjonPerioder);
    }


}
