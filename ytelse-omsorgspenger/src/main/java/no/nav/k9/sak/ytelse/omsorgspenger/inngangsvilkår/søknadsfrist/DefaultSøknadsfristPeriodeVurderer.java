package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartKravDokument;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.TimelineMerger;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class DefaultSøknadsfristPeriodeVurderer implements SøknadsfristPeriodeVurderer<OppgittFraværPeriode> {

    private final Period frist = Period.ofMonths(3);

    @Override
    public LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>> vurderPeriode(KravDokument søknadsDokument, LocalDateTimeline<SøktPeriode<OppgittFraværPeriode>> søktePeriode, Optional<AvklartKravDokument> avklartKravDokument) {
        var vurderingsdato = søknadsDokument.getInnsendingsTidspunkt().toLocalDate();
        var cutOffDato = utledCutOffDato(vurderingsdato, avklartKravDokument);

        LocalDateTimeline<SøktPeriode<OppgittFraværPeriode>> perioderUtenforSøknadsfrist = søktePeriode.intersection(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, cutOffDato));
        LocalDateTimeline<SøktPeriode<OppgittFraværPeriode>> perioderInnenforSøknadsfrist = søktePeriode.disjoint(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, cutOffDato));

        var godkjentTidslinje = new LocalDateTimeline<>(perioderInnenforSøknadsfrist.stream().map(segment -> vurderEnkeltPeriode(segment, Utfall.OPPFYLT)).collect(Collectors.toList()));
        var avslåtteTidslinje = new LocalDateTimeline<>(perioderUtenforSøknadsfrist.stream().map(segment -> vurderEnkeltPeriode(segment, Utfall.IKKE_OPPFYLT)).collect(Collectors.toList()));

        return godkjentTidslinje.combine(avslåtteTidslinje, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    private LocalDate utledCutOffDato(LocalDate vurderingsdato, Optional<AvklartKravDokument> avklartKravDokument) {
        if (avklartKravDokument.isPresent() && avklartKravDokument.get().getErGodkjent()) {
            return avklartKravDokument.get().getFraDato();
        }
        return vurderingsdato.minus(frist).withDayOfMonth(1).minusDays(1);
    }


    private LocalDateSegment<VurdertSøktPeriode<OppgittFraværPeriode>> vurderEnkeltPeriode(LocalDateSegment<SøktPeriode<OppgittFraværPeriode>> segment, Utfall utfall) {
        SøktPeriode<OppgittFraværPeriode> value = segment.getValue();
        DatoIntervallEntitet justertPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom());

        return new LocalDateSegment<>(segment.getLocalDateInterval(), new VurdertSøktPeriode<>(justertPeriode, value.getType(), value.getArbeidsgiver(), value.getArbeidsforholdRef(), utfall, value.getRaw()));
    }
}
