package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;

@Dependent
public class KompletthetssjekkerSøknad {

    private SøknadRepository søknadRepository;
    private Period ventefristForTidligSøknad;

    KompletthetssjekkerSøknad() {
        // for proxy
    }

    @Inject
    public KompletthetssjekkerSøknad(SøknadRepository søknadRepository,
                                     @KonfigVerdi(value = "fp.ventefrist.tidlig.soeknad", defaultVerdi = "P4W") Period ventefristForTidligSøknad) {
        this.ventefristForTidligSøknad = ventefristForTidligSøknad;
        this.søknadRepository = søknadRepository;
    }

    public Optional<LocalDateTime> erSøknadMottattForTidlig(BehandlingReferanse ref) {
        Optional<LocalDate> permisjonsstart = ref.getSkjæringstidspunkt().getSkjæringstidspunktHvisUtledet();
        if (permisjonsstart.isPresent()) {
            LocalDate ventefrist = permisjonsstart.get().minus(ventefristForTidligSøknad);
            boolean erSøknadMottattForTidlig = ventefrist.isAfter(LocalDate.now());
            if (erSøknadMottattForTidlig) {
                LocalDateTime ventefristTidspunkt = ventefrist.atStartOfDay();
                return Optional.of(ventefristTidspunkt);
            }
        }
        return Optional.empty();
    }

    public Boolean erSøknadMottatt(BehandlingReferanse ref) {
        final Optional<SøknadEntitet> søknad = søknadRepository.hentSøknadHvisEksisterer(ref.getBehandlingId());
        return søknad.isPresent();
    }
}
