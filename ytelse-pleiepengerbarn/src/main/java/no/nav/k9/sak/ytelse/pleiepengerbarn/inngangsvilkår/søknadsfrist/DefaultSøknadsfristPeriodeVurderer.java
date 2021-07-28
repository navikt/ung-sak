package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist;

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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

public class DefaultSøknadsfristPeriodeVurderer {

    private final Period frist = Period.ofMonths(3);

    public LocalDateTimeline<VurdertSøktPeriode<Søknadsperiode>> vurderPeriode(KravDokument søknadsDokument,
                                                                               LocalDateTimeline<SøktPeriode<Søknadsperiode>> søktePeriode,
                                                                               Optional<AvklartKravDokument> avklartKravDokument) {
        var vurderingsdato = søknadsDokument.getInnsendingsTidspunkt().toLocalDate();
        var cutOffDato = utledCutOffDato(vurderingsdato, avklartKravDokument);

        LocalDateTimeline<SøktPeriode<Søknadsperiode>> perioderUtenforSøknadsfrist = søktePeriode.intersection(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, cutOffDato));
        LocalDateTimeline<SøktPeriode<Søknadsperiode>> perioderInnenforSøknadsfrist = søktePeriode.disjoint(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, cutOffDato));

        var godkjentTidslinje = new LocalDateTimeline<>(perioderInnenforSøknadsfrist.stream().map(segment -> vurderEnkeltPeriode(segment, Utfall.OPPFYLT)).collect(Collectors.toList()));
        var avslåtteTidslinje = new LocalDateTimeline<>(perioderUtenforSøknadsfrist.stream().map(segment -> vurderEnkeltPeriode(segment, utledStatus(avklartKravDokument))).collect(Collectors.toList()));

        return godkjentTidslinje.combine(avslåtteTidslinje, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    private Utfall utledStatus(Optional<AvklartKravDokument> avklartKravDokument) {
        if (avklartKravDokument.isPresent()) {
            return Utfall.IKKE_OPPFYLT;
        }
        return Utfall.IKKE_VURDERT;
    }

    private LocalDate utledCutOffDato(LocalDate vurderingsdato, Optional<AvklartKravDokument> avklartKravDokument) {
        if (avklartKravDokument.isPresent()) {
            var avklarteOpplysninger = avklartKravDokument.get();
            if (avklarteOpplysninger.getErGodkjent()) {
                return avklarteOpplysninger.getFraDato();
            }
            // Vurderingen opprettholdes
            // TODO: Mulig det må gjøres noe triks for å kunne overstyre fra OPPFYLT -> IKKE_OPPFYLT
        }
        return vurderingsdato.minus(frist).withDayOfMonth(1).minusDays(1);
    }


    private LocalDateSegment<VurdertSøktPeriode<Søknadsperiode>> vurderEnkeltPeriode(LocalDateSegment<SøktPeriode<Søknadsperiode>> segment, Utfall utfall) {
        SøktPeriode<Søknadsperiode> value = segment.getValue();
        DatoIntervallEntitet justertPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom());

        return new LocalDateSegment<>(segment.getLocalDateInterval(), new VurdertSøktPeriode<>(justertPeriode, value.getType(), value.getArbeidsgiver(), value.getArbeidsforholdRef(), utfall, value.getRaw()));
    }
}
