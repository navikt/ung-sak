package no.nav.k9.sak.behandlingslager.aktør;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class BrukerTjeneste {

    private NavBrukerRepository brukerRepository;

    public BrukerTjeneste() {
    }

    @Inject
    public BrukerTjeneste(NavBrukerRepository brukerRepository) {
        this.brukerRepository = brukerRepository;
    }

    public NavBruker hentEllerOpprettFraAktorId(Personinfo personinfo) {
        Optional<NavBruker> hent = brukerRepository.hent(personinfo.getAktørId());
        return hent.orElse(NavBruker.opprettNy(personinfo));
    }

}
