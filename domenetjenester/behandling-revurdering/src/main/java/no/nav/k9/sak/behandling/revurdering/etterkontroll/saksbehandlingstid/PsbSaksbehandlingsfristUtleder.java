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
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;

@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@ApplicationScoped
public class PsbSaksbehandlingsfristUtleder implements SaksbehandlingsfristUtleder {

    private SøknadRepository søknadRepository;
    private Period fristPeriode;

    @Inject
    public PsbSaksbehandlingsfristUtleder(
        SøknadRepository søknadRepository,
        @KonfigVerdi(value = "DEFAULT_SAKSBEHANDLINGSFRIST_PERIODE", defaultVerdi = "P7W") String fristPeriode
        //Brukes kun for test med frist mindre enn 1 dag.
    ) {
        this.søknadRepository = søknadRepository;
        this.fristPeriode = Period.parse(fristPeriode);
    }

    PsbSaksbehandlingsfristUtleder() {
    }

    @Override
    public Optional<LocalDateTime> utledFrist(Behandling behandling) {
        return søknadRepository.hentSøknadHvisEksisterer(behandling.getId())
            .map(it -> it.getSøknadsdato().plus(fristPeriode).atStartOfDay());
    }
}
