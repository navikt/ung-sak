package no.nav.ung.sak.økonomi.simulering.tjeneste;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.ung.sak.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;

import java.util.Objects;
import java.util.Optional;

@Dependent
public class SimuleringIntegrasjonTjenesteImpl implements SimuleringIntegrasjonTjeneste {

    private TilkjentYtelseTjeneste tilkjentYtelseTjeneste;
    private K9OppdragRestKlient restKlient;

    public SimuleringIntegrasjonTjenesteImpl() {
        // CDI
    }

    @Inject
    public SimuleringIntegrasjonTjenesteImpl(TilkjentYtelseTjeneste tilkjentYtelseTjeneste, K9OppdragRestKlient restKlient) {
        this.tilkjentYtelseTjeneste = tilkjentYtelseTjeneste;
        this.restKlient = restKlient;
    }

    @Override
    public void startSimulering(Behandling behandling) {
        TilkjentYtelseOppdrag input = tilkjentYtelseTjeneste.hentTilkjentYtelseOppdrag(behandling);
        restKlient.startSimulering(input);
    }

    @Override
    public void kansellerSimulering(Behandling behandling) {
        restKlient.kansellerSimulering(behandling.getUuid());
    }

    @Override
    public Optional<SimuleringResultatDto> hentResultat(Behandling behandling) {
        Objects.requireNonNull(behandling, "Utviklerfeil: behandling kan ikke være null");
        return restKlient.hentSimuleringResultat(behandling.getUuid());
    }

}
