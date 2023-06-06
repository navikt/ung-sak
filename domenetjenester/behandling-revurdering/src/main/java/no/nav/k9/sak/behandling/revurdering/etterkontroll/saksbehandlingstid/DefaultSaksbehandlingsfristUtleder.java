package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import java.time.LocalDateTime;

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
    private Long fristMinutter;

    @Inject
    public DefaultSaksbehandlingsfristUtleder(
        SøknadRepository søknadRepository,
        //Default 7 uker
        @KonfigVerdi(value = "DEFAULT_SAKSBEHANDLINGSFRIST_MIN", defaultVerdi = "70560") Long fristMinutter
    ) {
        this.søknadRepository = søknadRepository;
        this.fristMinutter = fristMinutter;
    }

    DefaultSaksbehandlingsfristUtleder() {
    }

    @Override
    public LocalDateTime utledFrist(Behandling behandling) {
        SøknadEntitet søknadEntitet = søknadRepository.hentSøknad(behandling.getId());
        return søknadEntitet.getSøknadsdato().plusWeeks(fristMinutter).atStartOfDay();
    }
}
