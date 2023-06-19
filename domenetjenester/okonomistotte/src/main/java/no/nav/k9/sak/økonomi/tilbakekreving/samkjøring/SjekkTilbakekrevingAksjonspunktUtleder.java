package no.nav.k9.sak.økonomi.tilbakekreving.samkjøring;

import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.økonomi.tilbakekreving.dto.BehandlingStatusOgFeilutbetalinger;
import no.nav.k9.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;


@Dependent
public class SjekkTilbakekrevingAksjonspunktUtleder {

    private static final Logger logger = LoggerFactory.getLogger(SjekkTilbakekrevingAksjonspunktUtleder.class);

    private boolean lansert;
    private SjekkEndringUtbetalingTilBrukerTjeneste sjekkEndringUtbetalingTilBrukerTjeneste;
    private K9TilbakeRestKlient k9TilbakeRestKlient;

    @Inject
    public SjekkTilbakekrevingAksjonspunktUtleder(SjekkEndringUtbetalingTilBrukerTjeneste sjekkEndringUtbetalingTilBrukerTjeneste,
                                                  K9TilbakeRestKlient k9TilbakeRestKlient,
                                                  @KonfigVerdi(value = "ENABLE_SJEKK_TILBAKEKREVING", defaultVerdi = "true") boolean lansert) {
        this.sjekkEndringUtbetalingTilBrukerTjeneste = sjekkEndringUtbetalingTilBrukerTjeneste;
        this.k9TilbakeRestKlient = k9TilbakeRestKlient;
        this.lansert = lansert;
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
        return harÅpenIkkeoverlappendeTilbakekreving(aktuellBehandling)
            ? List.of(AksjonspunktDefinisjon.SJEKK_TILBAKEKREVING)
            : List.of();
    }

    boolean harÅpenIkkeoverlappendeTilbakekreving(Behandling aktuellBehandling) {
        LocalDateTimeline<Boolean> endringUtbetalingTilBruker = sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(aktuellBehandling);
        if (endringUtbetalingTilBruker.isEmpty()) {
            return false;
        }

        Fagsak fagsak = aktuellBehandling.getFagsak();
        Optional<BehandlingStatusOgFeilutbetalinger> feilutbetaling = k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(fagsak.getSaksnummer());
        boolean harÅpenTilbakekrevingsbehandling = feilutbetaling.isPresent() && feilutbetaling.get().getAvsluttetDato() == null;
        return harÅpenTilbakekrevingsbehandling && !overlapperTilbakekrevingsbehandlingen(endringUtbetalingTilBruker, feilutbetaling.get());
    }

    private static boolean overlapperTilbakekrevingsbehandlingen(LocalDateTimeline<Boolean> endringUtbetalingTilBruker, BehandlingStatusOgFeilutbetalinger feilutbetaling) {
        //endringen i ytelsesbehandlingen treffer samme perioder (i hele måneder) som i tilbakekrevingen.
        //da skal ytelsesbehandlingen gjennomføres, slik at tilbakekrevingsbehandlingen kan få oppdaterte tall - selv om den sperres
        LocalDateTimeline<Boolean> tilbakekrevingensPerioder = lagTidslinjeHvorFeilutbetalt(feilutbetaling);
        LocalDateTimeline<Boolean> tilbakekrevingenHeleMåneder = utvidTilHeleMåneder(tilbakekrevingensPerioder);
        if (Environment.current().isDev()) {
            logger.info("Tilbakekrevingens perioder {}", tilbakekrevingensPerioder);
            logger.info("Tilbakekrevingens perioder i hele måneder {}", tilbakekrevingenHeleMåneder);
            logger.info("Endring i utbetaling til bruker {}", endringUtbetalingTilBruker);
        }
        return tilbakekrevingenHeleMåneder.intersects(endringUtbetalingTilBruker);
    }

    private static LocalDateTimeline<Boolean> utvidTilHeleMåneder(LocalDateTimeline<Boolean> tidslinje) {
        List<LocalDateSegment<Boolean>> segmenter = tidslinje.stream()
            .map(segment -> new LocalDateSegment<>(segment.getFom().with(TemporalAdjusters.firstDayOfMonth()), segment.getTom().with(TemporalAdjusters.lastDayOfMonth()), true))
            .toList();
        return new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch);

    }

    private static LocalDateTimeline<Boolean> lagTidslinjeHvorFeilutbetalt(BehandlingStatusOgFeilutbetalinger feilutbetaling) {
        return new LocalDateTimeline<>(feilutbetaling.getFeilutbetaltePerioder().stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true))
            .toList());
    }

}
