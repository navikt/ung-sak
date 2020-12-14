package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.perioder.Søknad;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;

import java.time.Period;
import java.util.stream.Collectors;

public class DefaultSøknadsfristPeriodeVurderer implements SøknadsfristPeriodeVurderer {

    private final Period frist = Period.ofMonths(3);

    @Override
    public LocalDateTimeline<VurdertSøktPeriode> vurderPeriode(Søknad søknadsDokument, LocalDateTimeline<SøktPeriode> søktePeriode) {
        var vurderingsdato = søknadsDokument.getInnsendingsTidspunkt().toLocalDate();
        var cutOffDato = vurderingsdato.minus(frist).withDayOfMonth(1).minusDays(1);

        LocalDateTimeline<SøktPeriode> perioderUtenforSøknadsfrist = søktePeriode.intersection(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, cutOffDato));
        LocalDateTimeline<SøktPeriode> perioderInnenforSøknadsfrist = søktePeriode.disjoint(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, cutOffDato));

        var godkjentTidslinje = new LocalDateTimeline<>(perioderInnenforSøknadsfrist.stream().map(segment -> vurderEnkeltPeriode(segment, Utfall.OPPFYLT)).collect(Collectors.toList()));
        var avslåtteTidslinje = new LocalDateTimeline<>(perioderUtenforSøknadsfrist.stream().map(segment -> vurderEnkeltPeriode(segment, Utfall.IKKE_OPPFYLT)).collect(Collectors.toList()));

        return godkjentTidslinje.combine(avslåtteTidslinje, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }


    private LocalDateSegment<VurdertSøktPeriode> vurderEnkeltPeriode(LocalDateSegment<SøktPeriode> segment, Utfall utfall) {
        SøktPeriode value = segment.getValue();
        value.justerPeriode(segment);

        return new LocalDateSegment<>(segment.getLocalDateInterval(), new VurdertSøktPeriode(value.getPeriode(), value.getType(), value.getArbeidsgiver(), value.getArbeidsforholdRef(), utfall));
    }
}
