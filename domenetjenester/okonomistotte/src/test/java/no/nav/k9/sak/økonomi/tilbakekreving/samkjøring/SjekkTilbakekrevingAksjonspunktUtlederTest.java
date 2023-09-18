package no.nav.k9.sak.økonomi.tilbakekreving.samkjøring;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.økonomi.simulering.tjeneste.SimuleringIntegrasjonTjeneste;
import no.nav.k9.sak.økonomi.tilbakekreving.dto.BehandlingStatusOgFeilutbetalinger;
import no.nav.k9.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;

class SjekkTilbakekrevingAksjonspunktUtlederTest {

    private SjekkEndringUtbetalingTilBrukerTjeneste sjekkEndringUtbetalingTilBrukerTjeneste;
    private K9TilbakeRestKlient k9TilbakeRestKlient;
    private SjekkTilbakekrevingAksjonspunktUtleder utleder;
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private LocalDate _2022_jan_01 = LocalDate.of(2022, 1, 1);
    private LocalDate _2022_jan_12 = LocalDate.of(2022, 1, 12);
    private LocalDate _2023_jan_01 = LocalDate.of(2023, 1, 1);

    private LocalDate _2023_feb_15 = LocalDate.of(2023, 2, 15);
    private Behandling behandling = TestScenarioBuilder.builderUtenSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN).lagMocked();

    public SjekkTilbakekrevingAksjonspunktUtlederTest() {
        sjekkEndringUtbetalingTilBrukerTjeneste = Mockito.mock(SjekkEndringUtbetalingTilBrukerTjeneste.class);
        k9TilbakeRestKlient = Mockito.mock(K9TilbakeRestKlient.class);
        simuleringIntegrasjonTjeneste = Mockito.mock(SimuleringIntegrasjonTjeneste.class);
        utleder = new SjekkTilbakekrevingAksjonspunktUtleder(sjekkEndringUtbetalingTilBrukerTjeneste, k9TilbakeRestKlient, simuleringIntegrasjonTjeneste, true);
    }

    @Test
    void skal_ha_aksjonspunkt_når_det_er_endring_til_bruker_og_åpen_tilbakekreving_som_overlapper_med_endringene() {
        Mockito.when(k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(behandling.getFagsak().getSaksnummer())).thenReturn(
            Optional.of(new BehandlingStatusOgFeilutbetalinger(null, List.of(new Periode(_2023_jan_01, _2023_jan_01))))
        );
        Mockito.when(sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(behandling)).thenReturn(
            new LocalDateTimeline<>(_2023_jan_01, _2023_jan_01, true)
        );
        List<AksjonspunktDefinisjon> aksjonspunkt = utleder.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling);
        Assertions.assertThat(aksjonspunkt).containsOnly(AksjonspunktDefinisjon.SJEKK_TILBAKEKREVING);
    }

    @Test
    void skal_ha_aksjonspunkt_når_det_er_endring_til_bruker_og_åpen_tilbakekreving_i_intilliggende_måned() {
        Mockito.when(k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(behandling.getFagsak().getSaksnummer())).thenReturn(
            Optional.of(new BehandlingStatusOgFeilutbetalinger(null, List.of(new Periode(_2023_feb_15, _2023_feb_15))))
        );
        Mockito.when(sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(behandling)).thenReturn(
            new LocalDateTimeline<>(_2023_jan_01, _2023_jan_01, true)
        );
        List<AksjonspunktDefinisjon> aksjonspunkt = utleder.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling);
        Assertions.assertThat(aksjonspunkt).containsOnly(AksjonspunktDefinisjon.SJEKK_TILBAKEKREVING);
    }

    @Test
    void skal_ikke_ha_aksjonspunkt_når_det_er_endring_til_bruker_og_åpen_tilbakekreving_ikke_overlapper_med_endringene() {

        Mockito.when(k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(behandling.getFagsak().getSaksnummer())).thenReturn(
            Optional.of(new BehandlingStatusOgFeilutbetalinger(null, List.of(new Periode(_2022_jan_01, _2022_jan_12))))
        );
        Mockito.when(sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(behandling)).thenReturn(
            new LocalDateTimeline<>(_2023_jan_01, _2023_jan_01, true)
        );
        List<AksjonspunktDefinisjon> aksjonspunkt = utleder.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling);
        Assertions.assertThat(aksjonspunkt).isEmpty();
    }

    @Test
    void skal_ikke_ha_aksjonspunkt_når_det_er_endring_til_bruker_og_lukket_tilbakekreving() {
        Mockito.when(k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(behandling.getFagsak().getSaksnummer())).thenReturn(
            Optional.of(new BehandlingStatusOgFeilutbetalinger(LocalDate.now(), List.of(new Periode(_2022_jan_01, _2022_jan_12))))
        );
        Mockito.when(sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(behandling)).thenReturn(
            new LocalDateTimeline<>(_2022_jan_01, _2022_jan_12, true)
        );
        List<AksjonspunktDefinisjon> aksjonspunkt = utleder.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling);
        Assertions.assertThat(aksjonspunkt).isEmpty();
    }

    @Test
    void skal_ikke_ha_aksjonspunkt_når_det_ikke_er_endring_til_bruker() {
        Mockito.when(k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(behandling.getFagsak().getSaksnummer())).thenReturn(
            Optional.of(new BehandlingStatusOgFeilutbetalinger(LocalDate.now(), List.of(new Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 12)))))
        );
        Mockito.when(sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(behandling)).thenReturn(
            LocalDateTimeline.empty()
        );
        List<AksjonspunktDefinisjon> aksjonspunkt = utleder.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling);
        Assertions.assertThat(aksjonspunkt).isEmpty();
    }

    @Test
    void skal_ha_aksjonspunkt_når_det_er_tilbakekrevingsbehandling_og_økning_av_feilutbetalt_beløp() {
        Mockito.when(k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(behandling.getFagsak().getSaksnummer())).thenReturn(
            Optional.of(new BehandlingStatusOgFeilutbetalinger(null, List.of(new Periode(_2023_feb_15, _2023_feb_15))))
        );
        boolean slåttAvInntrekk = false;
        Long feilutbetaling = 1000L;
        Long inntrekk = 0L;
        Mockito.when(simuleringIntegrasjonTjeneste.hentResultat(behandling)).thenReturn(
            Optional.of(new SimuleringResultatDto(feilutbetaling, inntrekk, slåttAvInntrekk)));
        Mockito.when(sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(behandling)).thenReturn(
            new LocalDateTimeline<>(_2022_jan_01, _2022_jan_12, true)
        );

        List<AksjonspunktDefinisjon> aksjonspunkt = utleder.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling);
        Assertions.assertThat(aksjonspunkt).containsOnly(AksjonspunktDefinisjon.SJEKK_TILBAKEKREVING);
    }

    @Test
    void skal_ha_aksjonspunkt_når_det_er_tilbakekrevingsbehandling_og_inntrekk() {
        Mockito.when(k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(behandling.getFagsak().getSaksnummer())).thenReturn(
            Optional.of(new BehandlingStatusOgFeilutbetalinger(null, List.of(new Periode(_2023_feb_15, _2023_feb_15))))
        );
        boolean slåttAvInntrekk = false;
        Long feilutbetaling = 0L;
        Long inntrekk = 1310L;
        Mockito.when(simuleringIntegrasjonTjeneste.hentResultat(behandling)).thenReturn(
            Optional.of(new SimuleringResultatDto(feilutbetaling, inntrekk, slåttAvInntrekk)));
        Mockito.when(sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(behandling)).thenReturn(
            new LocalDateTimeline<>(_2022_jan_01, _2022_jan_12, true)
        );

        List<AksjonspunktDefinisjon> aksjonspunkt = utleder.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling);
        Assertions.assertThat(aksjonspunkt).containsOnly(AksjonspunktDefinisjon.SJEKK_TILBAKEKREVING);
    }

    @Test
    void skal_ikke_ha_aksjonspunkt_når_det_ikke_er_tilbakekrevingsbehandling() {
        Mockito.when(k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(behandling.getFagsak().getSaksnummer())).thenReturn(
            Optional.empty()
        );
        Mockito.when(sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(behandling)).thenReturn(
            new LocalDateTimeline<>(LocalDate.now(), LocalDate.now(), true)
        );
        List<AksjonspunktDefinisjon> aksjonspunkt = utleder.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling);
        Assertions.assertThat(aksjonspunkt).isEmpty();
    }
}
