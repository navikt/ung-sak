package no.nav.k9.sak.mottak.kompletthetssjekk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef
@BehandlingTypeRef
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

    public boolean erSøknadMottatt(BehandlingReferanse ref) {
        final Optional<SøknadEntitet> søknad = søknadRepository.hentSøknadHvisEksisterer(ref.getBehandlingId());
        return søknad.isPresent();
    }
}
