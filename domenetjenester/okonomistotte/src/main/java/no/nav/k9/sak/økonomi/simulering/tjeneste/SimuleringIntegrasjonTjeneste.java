package no.nav.k9.sak.økonomi.simulering.tjeneste;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.k9.sak.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;

@Dependent
public class SimuleringIntegrasjonTjeneste {

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

    public Optional<SimuleringResultatDto> hentResultat(Behandling behandling) {
        Objects.requireNonNull(behandling, "Utviklerfeil: behandling kan ikke være null");
        return restKlient.hentSimuleringResultat(behandling.getUuid());
    }

}
