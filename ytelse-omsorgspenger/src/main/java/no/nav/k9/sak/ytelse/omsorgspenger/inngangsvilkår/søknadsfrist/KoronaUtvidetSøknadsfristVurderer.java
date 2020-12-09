package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.perioder.Søknad;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;

import java.time.LocalDate;
import java.time.Period;
import java.util.stream.Collectors;

public class KoronaUtvidetSøknadsfristVurderer implements SøknadsfristPeriodeVurderer {
    private final LocalDateInterval unntaksperiode = new LocalDateInterval(LocalDate.of(2020, 3, 12), LocalDate.of(2020, 12, 31));
    private final Period frist = Period.ofMonths(9);

    @Override
    public LocalDateTimeline<VurdertSøktPeriode> vurderPeriode(Søknad søknadsDokument, LocalDateTimeline<SøktPeriode> søktePeriode) {
        LocalDateTimeline<SøktPeriode> søktePerioderInnenforUnntaksperiode = søktePeriode.intersection(unntaksperiode);
        var vurderingsdato = søknadsDokument.getInnsendingsTidspunkt().withDayOfMonth(1).toLocalDate();
        var cutOffDato = vurderingsdato.minus(frist);
        LocalDateTimeline<SøktPeriode> perioderUtenforSøknadsfrist = søktePerioderInnenforUnntaksperiode.intersection(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, cutOffDato));
        LocalDateTimeline<SøktPeriode> perioderInnenforSøknadsfrist = søktePerioderInnenforUnntaksperiode.disjoint(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, cutOffDato));
        var godkjentTidslinje = new LocalDateTimeline<>(perioderInnenforSøknadsfrist.stream().map(segment -> vurderEnkeltPeriode(segment, Utfall.OPPFYLT)).collect(Collectors.toList()));
        var avslåtteTidslinje = new LocalDateTimeline<>(perioderUtenforSøknadsfrist.stream().map(segment -> vurderEnkeltPeriode(segment, Utfall.IKKE_OPPFYLT)).collect(Collectors.toList()));
        return godkjentTidslinje.combine(avslåtteTidslinje, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    @Override
    public LocalDateInterval periodeSomVurderes() {
        return unntaksperiode;
    }

    private LocalDateSegment<VurdertSøktPeriode> vurderEnkeltPeriode(LocalDateSegment<SøktPeriode> segment, Utfall utfall) {
        SøktPeriode value = segment.getValue();
        value.justerPeriode(segment);

        return new LocalDateSegment<>(segment.getLocalDateInterval(), new VurdertSøktPeriode(value.getPeriode(), value.getType(), value.getArbeidsgiver(), value.getArbeidsforholdRef(), utfall));
    }
}
