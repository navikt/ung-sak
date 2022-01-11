package no.nav.k9.sak.ytelse.frisinn.vilkår;

import java.time.LocalDate;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;

class BeregningPeriode implements VilkårsPeriodiseringsFunksjon {

    private final LocalDate FØRSTE_STP_FRISINN = LocalDate.of(2020, 3, 1);
    // ENDRE DENNE TIL Å VÆRE DATO FOR NYE REGLER
    private final LocalDate ENDRINGSDATO = LocalDate.of(2022, 1,1 );
    private final boolean nyttStpToggle;
    private final UttakRepository uttakRepository;

    BeregningPeriode(UttakRepository uttakRepository, boolean nyttStpToggle) {
        this.uttakRepository = uttakRepository;
        this.nyttStpToggle = nyttStpToggle;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId);
        if (søknadsperioder.isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            var maksPeriode = søknadsperioder.get().getMaksPeriode();
            if (nyttStpToggle) {
                var førsteSøknadsdato = maksPeriode.getFomDato();
                if (skalBrukeGamleReglerForSkjæringstidspunkt(førsteSøknadsdato)) {
                    // Bruker gamle regler med stp lik 1.3.2020
                    return periodeMedStatiskSkjæringstidspunkt(maksPeriode);
                } else {
                    // Nye regler med skjæringstidspunkt lik første søknadsdag
                    return periodeMedSkjæringstidspunktLikFørsteSøknadsdag(maksPeriode, førsteSøknadsdato);
                }
            } else {
                // Bruker gamle regler
                return periodeMedStatiskSkjæringstidspunkt(maksPeriode);
            }
        }
    }

    private boolean skalBrukeGamleReglerForSkjæringstidspunkt(LocalDate førsteSøknadsdato) {
        return førsteSøknadsdato.isBefore(ENDRINGSDATO);
    }

    private NavigableSet<DatoIntervallEntitet> periodeMedSkjæringstidspunktLikFørsteSøknadsdag(DatoIntervallEntitet maksPeriode, LocalDate førsteSøknadsdato) {
        return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(førsteSøknadsdato, maksPeriode.getTomDato()))));
    }

    private NavigableSet<DatoIntervallEntitet> periodeMedStatiskSkjæringstidspunkt(DatoIntervallEntitet maksPeriode) {
        return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(FØRSTE_STP_FRISINN, maksPeriode.getTomDato()))));
    }
}

