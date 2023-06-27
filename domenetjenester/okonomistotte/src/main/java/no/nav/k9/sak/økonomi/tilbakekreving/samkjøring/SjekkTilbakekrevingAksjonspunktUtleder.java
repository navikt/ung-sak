package no.nav.k9.sak.økonomi.tilbakekreving.samkjøring;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.økonomi.simulering.tjeneste.SimuleringIntegrasjonTjeneste;
import no.nav.k9.sak.økonomi.tilbakekreving.dto.BehandlingStatusOgFeilutbetalinger;
import no.nav.k9.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;


@Dependent
public class SjekkTilbakekrevingAksjonspunktUtleder {

    private static final Logger logger = LoggerFactory.getLogger(SjekkTilbakekrevingAksjonspunktUtleder.class);

    private boolean lansert;
    private SjekkEndringUtbetalingTilBrukerTjeneste sjekkEndringUtbetalingTilBrukerTjeneste;
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private K9TilbakeRestKlient k9TilbakeRestKlient;

    @Inject
    public SjekkTilbakekrevingAksjonspunktUtleder(SjekkEndringUtbetalingTilBrukerTjeneste sjekkEndringUtbetalingTilBrukerTjeneste,
                                                  K9TilbakeRestKlient k9TilbakeRestKlient,
                                                  SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste,
                                                  @KonfigVerdi(value = "ENABLE_SJEKK_TILBAKEKREVING", defaultVerdi = "true") boolean lansert) {
        this.sjekkEndringUtbetalingTilBrukerTjeneste = sjekkEndringUtbetalingTilBrukerTjeneste;
        this.k9TilbakeRestKlient = k9TilbakeRestKlient;
        this.lansert = lansert;
        this.simuleringIntegrasjonTjeneste = simuleringIntegrasjonTjeneste;
    }

    /**
     * Når k9-sak iverksetter endringer for bruker mot oppdragsystemet og det finnes et kravgrunnlag (pga feilutbetaling til bruker),
     * vil oppdragsystemet sperre kravgrunnlaget i en periode (3 dager). Mens kravgrunnlaget er sperret, er det ikke mulig å
     * gjennomføre tilbakekrevingsbehandlinger. Når bruker sender inn daglige endringer som behandles automatisk, vil det
     * da bli umulig å gjennomføre tilbakekrevingsbehandligner på saken.
     * <p>
     * Her utledes et aksjonspunkt som bryter flyten, slik at det skal være mulig å gjennomføre tilbakekrevingsbehandlingen.
     */
    public List<AksjonspunktDefinisjon> sjekkMotÅpenIkkeoverlappendeTilbakekreving(Behandling aktuellBehandling) {
        if (!lansert) {
            return List.of();
        }
        return påvirkerÅpenTilbakekrevingsbehandling(aktuellBehandling)
            ? List.of(AksjonspunktDefinisjon.SJEKK_TILBAKEKREVING)
            : List.of();
    }

    boolean simuleringViserFeilutbetaling(Behandling behandling) {
        Optional<SimuleringResultatDto> simuleringResultatDto = simuleringIntegrasjonTjeneste.hentResultat(behandling);
        if (simuleringResultatDto.isEmpty()) {
            return false;
        }
        SimuleringResultatDto simuleringsresultat = simuleringResultatDto.get();
        return simuleringsresultat.harFeilutbetaling();

    }

    boolean påvirkerÅpenTilbakekrevingsbehandling(Behandling aktuellBehandling) {
        Fagsak fagsak = aktuellBehandling.getFagsak();
        Optional<BehandlingStatusOgFeilutbetalinger> feilutbetaling = k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(fagsak.getSaksnummer());
        boolean harÅpenTilbakekrevingsbehandling = feilutbetaling.isPresent() && feilutbetaling.get().getAvsluttetDato() == null;
        if (!harÅpenTilbakekrevingsbehandling) {
            logger.info("Har ingen åpen tilbakekrevingsbehandling");
            return false;
        }
        LocalDateTimeline<Boolean> endringUtbetalingTilBruker = sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(aktuellBehandling);
        if (endringUtbetalingTilBruker.isEmpty()) {
            logger.info("ingen endring i utbetaling til bruker");
            return false;
        }
        if (simuleringViserFeilutbetaling(aktuellBehandling)) {
            logger.info("Simulering viser feilutbetaling, så tilbakekrevingsbehandling er påvirket");
            return true;
        }
        boolean overlappendeEndring = overlapperTilbakekrevingsbehandlingen(endringUtbetalingTilBruker, feilutbetaling.get());
        logger.info("Behandlingen overlapper {} tilbakekrevingsbehandlingen", overlappendeEndring ? "" : " ikke ");
        return overlappendeEndring;
    }

    private static boolean overlapperTilbakekrevingsbehandlingen(LocalDateTimeline<Boolean> endringUtbetalingTilBruker, BehandlingStatusOgFeilutbetalinger feilutbetaling) {
        //endringen i ytelsesbehandlingen treffer samme perioder (i hele måneder) som i tilbakekrevingen.
        //da skal ytelsesbehandlingen gjennomføres, slik at tilbakekrevingsbehandlingen kan få oppdaterte tall - selv om den sperres
        LocalDateTimeline<Boolean> tilbakekrevingensPerioder = lagTidslinjeHvorFeilutbetalt(feilutbetaling);
        if (Environment.current().isDev()) {
            logger.info("Tilbakekrevingens perioder {}", tilbakekrevingensPerioder);
            logger.info("Endring i utbetaling til bruker {}", endringUtbetalingTilBruker);
        }
        return tilbakekrevingensPerioder.intersects(endringUtbetalingTilBruker);
    }

    private static LocalDateTimeline<Boolean> lagTidslinjeHvorFeilutbetalt(BehandlingStatusOgFeilutbetalinger feilutbetaling) {
        return new LocalDateTimeline<>(feilutbetaling.getFeilutbetaltePerioder().stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true))
            .toList());
    }

}
