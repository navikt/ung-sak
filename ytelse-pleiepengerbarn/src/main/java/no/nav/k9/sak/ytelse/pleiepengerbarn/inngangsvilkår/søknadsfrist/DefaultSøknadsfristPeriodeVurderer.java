package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist;

import java.time.Period;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.TimelineMerger;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

public class DefaultSøknadsfristPeriodeVurderer {

    private final Period frist = Period.ofMonths(3);

    public LocalDateTimeline<VurdertSøktPeriode<Søknadsperiode>> vurderPeriode(KravDokument søknadsDokument, LocalDateTimeline<SøktPeriode<Søknadsperiode>> søktePeriode) {
        var vurderingsdato = søknadsDokument.getInnsendingsTidspunkt().toLocalDate();
        var cutOffDato = vurderingsdato.minus(frist).withDayOfMonth(1).minusDays(1);

        LocalDateTimeline<SøktPeriode<Søknadsperiode>> perioderUtenforSøknadsfrist = søktePeriode.intersection(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, cutOffDato));
        LocalDateTimeline<SøktPeriode<Søknadsperiode>> perioderInnenforSøknadsfrist = søktePeriode.disjoint(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, cutOffDato));

        var godkjentTidslinje = new LocalDateTimeline<>(perioderInnenforSøknadsfrist.stream().map(segment -> vurderEnkeltPeriode(segment, Utfall.OPPFYLT)).collect(Collectors.toList()));
        var avslåtteTidslinje = new LocalDateTimeline<>(perioderUtenforSøknadsfrist.stream().map(segment -> vurderEnkeltPeriode(segment, Utfall.IKKE_VURDERT)).collect(Collectors.toList()));

        return godkjentTidslinje.combine(avslåtteTidslinje, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }


    private LocalDateSegment<VurdertSøktPeriode<Søknadsperiode>> vurderEnkeltPeriode(LocalDateSegment<SøktPeriode<Søknadsperiode>> segment, Utfall utfall) {
        SøktPeriode<Søknadsperiode> value = segment.getValue();
        value.justerPeriode(segment);

        return new LocalDateSegment<>(segment.getLocalDateInterval(), new VurdertSøktPeriode<>(value.getPeriode(), value.getType(), value.getArbeidsgiver(), value.getArbeidsforholdRef(), utfall, value.getRaw()));
    }
}
