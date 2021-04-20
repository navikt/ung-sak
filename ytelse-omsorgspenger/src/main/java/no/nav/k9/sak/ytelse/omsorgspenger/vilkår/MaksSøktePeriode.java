package no.nav.k9.sak.ytelse.omsorgspenger.vilkår;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;

class MaksSøktePeriode implements VilkårsPeriodiseringsFunksjon {

    private OmsorgspengerGrunnlagRepository uttakRepository;

    MaksSøktePeriode(OmsorgspengerGrunnlagRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var maksPeriode = uttakRepository.hentMaksPeriode(behandlingId);
        if (maksPeriode.isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(maksPeriode.get())));
        }
    }
}
