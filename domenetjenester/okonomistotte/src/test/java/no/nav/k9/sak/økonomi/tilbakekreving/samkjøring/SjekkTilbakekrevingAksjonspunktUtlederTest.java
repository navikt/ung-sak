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
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.økonomi.tilbakekreving.dto.BehandlingStatusOgFeilutbetalinger;
import no.nav.k9.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;

class SjekkTilbakekrevingAksjonspunktUtlederTest {

    SjekkEndringUtbetalingTilBrukerTjeneste sjekkEndringUtbetalingTilBrukerTjeneste;

    K9TilbakeRestKlient k9TilbakeRestKlient;

    SjekkTilbakekrevingAksjonspunktUtleder utleder;


    LocalDate _2022_jan_01 = LocalDate.of(2022, 1, 1);
    LocalDate _2022_jan_07 = LocalDate.of(2022, 1, 7);
    LocalDate _2022_jan_12 = LocalDate.of(2022, 1, 12);
    LocalDate _2022_jan_31 = LocalDate.of(2022, 1, 31);
    LocalDate _2023_jan_01 = LocalDate.of(2023, 1, 1);

    Behandling behandling = TestScenarioBuilder.builderUtenSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN).lagMocked();

    public SjekkTilbakekrevingAksjonspunktUtlederTest() {
        sjekkEndringUtbetalingTilBrukerTjeneste = Mockito.mock(SjekkEndringUtbetalingTilBrukerTjeneste.class);
        k9TilbakeRestKlient = Mockito.mock(K9TilbakeRestKlient.class);
        utleder = new SjekkTilbakekrevingAksjonspunktUtleder(sjekkEndringUtbetalingTilBrukerTjeneste, k9TilbakeRestKlient, true);
    }

    @Test
    void skal_ha_aksjonspunkt_når_det_er_endring_til_bruker_og_åpen_tilbakekreving_som_ikke_overlapper_med_endringene() {
        Mockito.when(k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(behandling.getFagsak().getSaksnummer())).thenReturn(
            Optional.of(new BehandlingStatusOgFeilutbetalinger(null, List.of(new Periode(_2022_jan_01, _2022_jan_12))))
        );
        Mockito.when(sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(behandling)).thenReturn(
            new LocalDateTimeline<>(_2023_jan_01, _2023_jan_01, true)
        );
        List<AksjonspunktDefinisjon> aksjonspunkt = utleder.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling);
        Assertions.assertThat(aksjonspunkt).containsOnly(AksjonspunktDefinisjon.SJEKK_TILBAKEKREVING);
    }

    @Test
    void skal_ikke_ha_aksjonspunkt_når_det_er_endring_til_bruker_og_åpen_tilbakekreving_overlapper_med_endringene() {

        Mockito.when(k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(behandling.getFagsak().getSaksnummer())).thenReturn(
            Optional.of(new BehandlingStatusOgFeilutbetalinger(null, List.of(new Periode(_2022_jan_01, _2022_jan_12))))
        );
        Mockito.when(sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(behandling)).thenReturn(
            new LocalDateTimeline<>(_2022_jan_07, _2022_jan_07, true)
        );
        List<AksjonspunktDefinisjon> aksjonspunkt = utleder.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling);
        Assertions.assertThat(aksjonspunkt).isEmpty();
    }

    @Test
    void skal_ikke_ha_aksjonspunkt_når_det_er_endring_til_bruker_og_endringene_er_i_samme_måned_som_tilbakekrevingen() {
        Mockito.when(k9TilbakeRestKlient.hentFeilutbetalingerForSisteBehandling(behandling.getFagsak().getSaksnummer())).thenReturn(
            Optional.of(new BehandlingStatusOgFeilutbetalinger(null, List.of(new Periode(_2022_jan_01, _2022_jan_12))))
        );
        Mockito.when(sjekkEndringUtbetalingTilBrukerTjeneste.endringerUtbetalingTilBruker(behandling)).thenReturn(
            new LocalDateTimeline<>(_2022_jan_31, _2022_jan_31, true)
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
            new LocalDateTimeline<>(_2023_jan_01, _2023_jan_01, true)
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
