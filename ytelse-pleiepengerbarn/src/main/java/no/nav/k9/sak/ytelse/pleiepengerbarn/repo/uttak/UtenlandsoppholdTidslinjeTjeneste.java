package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.delt.UtledetUtenlandsopphold;
import no.nav.k9.søknad.felles.Kildesystem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UtenlandsoppholdTidslinjeTjeneste {

    public static LocalDateTimeline<UtledetUtenlandsopphold> byggTidslinje(
            Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravDokumenter,
            Set<PerioderFraSøknad> perioderFraSøknader) {
        Set<KravDokument> kravDokumenterSorted = kravDokumenter.keySet().stream().sorted(KravDokument::compareTo).collect(Collectors.toCollection(LinkedHashSet::new));
        LocalDateTimeline<UtledetUtenlandsopphold> resultatTimeline = new LocalDateTimeline<>(List.of());

        for (KravDokument kravDokument : kravDokumenterSorted) {
            Set<PerioderFraSøknad> perioderFraSøknaderForKravdokument = perioderFraSøknader.stream()
                .filter(it -> it.getJournalpostId().equals(kravDokument.getJournalpostId()))
                .collect(Collectors.toSet());
            if (perioderFraSøknaderForKravdokument.size() != 1) {
                throw new IllegalStateException("Forventet ett sett perioder fra søknad per kravdokument. Fant " + perioderFraSøknaderForKravdokument.size() + " for " + kravDokument.getJournalpostId());
            }
            var perioderFraSøknad = perioderFraSøknaderForKravdokument.iterator().next();
            if (perioderFraSøknad.getUtenlandsopphold().isEmpty() && kravDokument.getKildesystem() == Kildesystem.SØKNADSDIALOG) {
                //TODO Denne løsningen funker bare når hele utenlandsoppholdet skal slettes. Trenger en løsning for å slette deler av perioden (se TODO nedenfor).
                LocalDateTimeline<UtledetUtenlandsopphold> tidslinjeSomSkalSlettes =
                    new LocalDateTimeline<>(perioderFraSøknad.getUttakPerioder().stream()
                        .map(UttakPeriode::getPeriode)
                        .map(periode -> new LocalDateSegment<>(periode.getFomDato(), periode.getTomDato(), (UtledetUtenlandsopphold) null))
                        .toList());
                resultatTimeline = resultatTimeline.disjoint(tidslinjeSomSkalSlettes);
            } else {
                for (UtenlandsoppholdPeriode utenlandsoppholdPeriode : perioderFraSøknad.getUtenlandsopphold()) {
                    LocalDateTimeline<UtledetUtenlandsopphold> timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(
                        utenlandsoppholdPeriode.getPeriode().getFomDato(),
                        utenlandsoppholdPeriode.getPeriode().getTomDato(),
                        new UtledetUtenlandsopphold(utenlandsoppholdPeriode.getLand(), utenlandsoppholdPeriode.getÅrsak())
                    )));
                    if (utenlandsoppholdPeriode.isAktiv()) {
                        resultatTimeline = resultatTimeline.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                    } else {
                        //TODO Det ser ut til at denne koden er død fordi feltet "perioderSomSkalSlettes" under utenlandsopphold i søknaden ikke er i bruk. Bør finne en annen løsning for å slette perioder.
                        resultatTimeline = resultatTimeline.disjoint(timeline);
                    }
                }
            }
        }
        return resultatTimeline;
    }
}
