package no.nav.k9.sak.ytelse.omsorgspenger.vilkår;

import java.util.Set;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;

class MaksSøktePeriode implements VilkårsPeriodiseringsFunksjon {

    private OmsorgspengerGrunnlagRepository uttakRepository;

    MaksSøktePeriode(OmsorgspengerGrunnlagRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @Override
    public Set<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentOppgittFraværHvisEksisterer(behandlingId);
        if (søknadsperioder.isEmpty()) {
            return Set.of();
        } else {
            return Set.of(søknadsperioder.get().getMaksPeriode());
        }
    }
}
