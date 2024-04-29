package no.nav.k9.sak.ytelse.pleiepengerbarn.saksbehandlingstid;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.saksbehandlingstid.SaksbehandlingsfristUtleder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.person.personopplysning.UtlandVurdererTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@ApplicationScoped
public class PsbSaksbehandlingsfristUtleder implements SaksbehandlingsfristUtleder {

    private static Logger log = LoggerFactory.getLogger(PsbSaksbehandlingsfristUtleder.class);

    private SøknadRepository søknadRepository;
    private Period fristPeriode;
    private UtlandVurdererTjeneste utlandVurdererTjeneste;

    @Inject
    public PsbSaksbehandlingsfristUtleder(
        SøknadRepository søknadRepository,
        @KonfigVerdi(value = "DEFAULT_SAKSBEHANDLINGSFRIST_PERIODE", defaultVerdi = "P7W") Period fristPeriode,
        UtlandVurdererTjeneste utlandVurdererTjeneste
    ) {
        this.søknadRepository = søknadRepository;
        this.fristPeriode = fristPeriode;
        this.utlandVurdererTjeneste = utlandVurdererTjeneste;
    }

    PsbSaksbehandlingsfristUtleder() {
    }


    @Override
    public Optional<LocalDateTime> utledFrist(Behandling behandling) {
        if (behandling.getType() != BehandlingType.FØRSTEGANGSSØKNAD) {
            return Optional.empty();
        }

        if (behandling.erAvsluttet()) {
            log.info("Utleder ikke frist for avsluttet sak");
            return Optional.empty();
        }

        if (utlandVurdererTjeneste.erUtenlandssak(behandling)) {
            log.info("Utleder ikke frist for utenlandssak");
            return Optional.empty();
        }

        return søknadRepository.hentSøknadHvisEksisterer(behandling.getId())
            .map(it -> it.getMottattDato().plus(fristPeriode).atStartOfDay());
    }



}
