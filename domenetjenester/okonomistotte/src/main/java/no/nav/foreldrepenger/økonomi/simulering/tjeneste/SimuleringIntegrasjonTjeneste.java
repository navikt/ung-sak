package no.nav.foreldrepenger.økonomi.simulering.tjeneste;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.foreldrepenger.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.SimulerOppdragDto;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.SimuleringResultatDto;

@ApplicationScoped
public class SimuleringIntegrasjonTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(SimuleringIntegrasjonTjeneste.class);

    private TilkjentYtelseTjeneste tilkjentYtelseTjeneste;
    private K9OppdragRestKlient restKlient;

    public SimuleringIntegrasjonTjeneste() {
        // CDI
    }

    @Inject
    public SimuleringIntegrasjonTjeneste(TilkjentYtelseTjeneste tilkjentYtelseTjeneste, K9OppdragRestKlient restKlient) {
        this.tilkjentYtelseTjeneste = tilkjentYtelseTjeneste;
        this.restKlient = restKlient;
    }

    public void startSimulering(Behandling behandling) {
        TilkjentYtelseOppdrag input = tilkjentYtelseTjeneste.hentTilkjentYtelseOppdrag(behandling);
        restKlient.startSimulering(input);
    }

    public void kansellerSimulering(Behandling behandling) {
        restKlient.kansellerSimulering(behandling.getUuid());
    }

    public Optional<SimuleringResultatDto> hentResultat(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "Utviklerfeil: behandlingId kan ikke være null");
        // FIXME K9 koble på simulering
        return Optional.empty();
        // return restKlient.hentResultat(behandlingId);
    }

    private SimulerOppdragDto map(Long behandlingId, List<String> oppdragListe) {
        return SimulerOppdragDto.lagDto(behandlingId, oppdragListe);
    }

}
