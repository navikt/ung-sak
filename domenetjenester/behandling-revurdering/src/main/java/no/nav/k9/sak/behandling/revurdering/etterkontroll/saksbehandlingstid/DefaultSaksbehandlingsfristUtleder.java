package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;

@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@ApplicationScoped
public class DefaultSaksbehandlingsfristUtleder implements SaksbehandlingsfristUtleder {

    private SøknadRepository søknadRepository;
    private Period fristPeriode;

    @Inject
    public DefaultSaksbehandlingsfristUtleder(
        SøknadRepository søknadRepository,
        @KonfigVerdi(value = "DEFAULT_SAKSBEHANDLINGSFRIST_PERIODE", defaultVerdi = "P7W") String fristPeriode
        //Brukes kun for test med frist mindre enn 1 dag.
    ) {
        this.søknadRepository = søknadRepository;
        this.fristPeriode = Period.parse(fristPeriode);
    }

    DefaultSaksbehandlingsfristUtleder() {
    }

    @Override
    public LocalDateTime utledFrist(Behandling behandling) {
        Optional<SøknadEntitet> s = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        return s.map(it -> it.getSøknadsdato().plus(fristPeriode).atStartOfDay())
            .orElse(null);
    }
}
