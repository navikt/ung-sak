package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.perioder.Søknad;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;

import javax.enterprise.context.Dependent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
@FagsakYtelseTypeRef("OMP")
@BehandlingTypeRef
public class OMPVurderSøknadsfristTjeneste implements VurderSøknadsfristTjeneste {

    private Map<LocalDateInterval, SøknadsfristPeriodeVurderer> avviksVurderere;
    private SøknadsfristPeriodeVurderer defaultVurderer = new DefaultSøknadsfristPeriodeVurderer();

    OMPVurderSøknadsfristTjeneste() {
        KoronaUtvidetSøknadsfristVurderer vurderer = new KoronaUtvidetSøknadsfristVurderer();
        avviksVurderere = Map.of(vurderer.periodeSomVurderes(), vurderer);
    }

    @Override
    public Map<Søknad, Set<VurdertSøktPeriode>> vurderSøknadsfrist(Map<Søknad, Set<SøktPeriode>> søknaderMedPerioder) {

        var result = new HashMap<Søknad, Set<VurdertSøktPeriode>>();

        for (Map.Entry<Søknad, Set<SøktPeriode>> entry : søknaderMedPerioder.entrySet()) {
            LocalDateTimeline<SøktPeriode> timeline = new LocalDateTimeline<>(entry.getValue().stream().map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), it)).collect(Collectors.toList()));
            LocalDateTimeline<VurdertSøktPeriode> vurdertTimeline = defaultVurderer.vurderPeriode(entry.getKey(), timeline);

            List<LocalDateInterval> avviksIntervallerTilVurdering = avviksVurderere.keySet().stream().filter(it -> !timeline.intersection(it).isEmpty()).collect(Collectors.toList());
            if (avviksVurderere.keySet().stream().anyMatch(it -> !timeline.intersection(it).isEmpty())) {
                for (LocalDateInterval localDateInterval : avviksIntervallerTilVurdering) {
                    SøknadsfristPeriodeVurderer vurderer = avviksVurderere.get(localDateInterval);
                    LocalDateTimeline<VurdertSøktPeriode> unntaksvurdertTimeline = vurderer.vurderPeriode(entry.getKey(), timeline);

                    vurdertTimeline = vurdertTimeline.combine(unntaksvurdertTimeline, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }
            }
            result.put(entry.getKey(), vurdertTimeline.stream().map(LocalDateSegment::getValue).collect(Collectors.toSet()));
        }

        return result;
    }
}
