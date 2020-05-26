package no.nav.k9.sak.web.app.tjenester.behandling.tilbakekreving;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.VurderFeilutbetalingOgInntrekkDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;

public class VurderFeilutbetalingOgInntrekkOppdatererTest {

    private TilbakekrevingRepository repository = Mockito.mock(TilbakekrevingRepository.class);
    private HistorikkTjenesteAdapter historikkTjenesteAdapter = Mockito.mock(HistorikkTjenesteAdapter.class);
    private TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger = new TilbakekrevingvalgHistorikkinnslagBygger(historikkTjenesteAdapter);
    private VurderFeilutbetalingOgInntrekkOppdaterer oppdaterer = new VurderFeilutbetalingOgInntrekkOppdaterer(repository, historikkInnslagBygger);

    private ArgumentCaptor<TilbakekrevingValg> captor = ArgumentCaptor.forClass(TilbakekrevingValg.class);

    private Behandling behandling;

    @Before
    public void setup() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        this.behandling = scenario.lagMocked();
    }

    @Test
    public void skal_lagre_at_videre_behandling_er_med_inntrekk_når_riktige_felter_er_valgt() {
        var dto = new VurderFeilutbetalingOgInntrekkDto("lorem ipsum", true, false, null);
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        Mockito.verify(repository).lagre(Mockito.eq(behandling), captor.capture());

        TilbakekrevingValg tilbakekrevingValg = captor.getValue();
        assertThat(tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt()).isTrue();
        assertThat(tilbakekrevingValg.getGrunnerTilReduksjon()).isFalse();
        assertThat(tilbakekrevingValg.getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.INNTREKK);
    }

    @Test
    public void skal_lagre_at_videre_behandling_er_behandle_i_infotrygd_når_det_er_valgt() {
        var dto = new VurderFeilutbetalingOgInntrekkDto("lorem ipsum", true, true, TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING);
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        Mockito.verify(repository).lagre(Mockito.eq(behandling), captor.capture());

        TilbakekrevingValg tilbakekrevingValg = captor.getValue();
        assertThat(tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt()).isTrue();
        assertThat(tilbakekrevingValg.getGrunnerTilReduksjon()).isTrue();
        assertThat(tilbakekrevingValg.getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING);
    }

    @Test
    public void skal_feile_når_boolske_variable_indikerer_inntrekk_men_noe_annet_er_valgt() {
        var dto = new VurderFeilutbetalingOgInntrekkDto("lorem ipsum", true, false, TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING);

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        });
    }

}
