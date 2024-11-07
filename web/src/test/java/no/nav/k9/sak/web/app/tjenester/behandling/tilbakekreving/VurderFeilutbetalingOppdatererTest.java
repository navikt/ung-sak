package no.nav.k9.sak.web.app.tjenester.behandling.tilbakekreving;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;

import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.VurderFeilutbetalingDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;

public class VurderFeilutbetalingOppdatererTest {

    private TilbakekrevingRepository repository = mock(TilbakekrevingRepository.class);
    private HistorikkTjenesteAdapter historikkTjenesteAdapter = mock(HistorikkTjenesteAdapter.class);
    private TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger = new TilbakekrevingvalgHistorikkinnslagBygger(historikkTjenesteAdapter);
    private VurderFeilutbetalingOppdaterer oppdaterer = new VurderFeilutbetalingOppdaterer(repository, historikkInnslagBygger);

    private ArgumentCaptor<TilbakekrevingValg> captor = ArgumentCaptor.forClass(TilbakekrevingValg.class);

    private Behandling behandling;

    @BeforeEach
    public void setup() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        this.behandling = scenario.lagMocked();
    }

    @Test
    public void skal_lagre_at_videre_behandling_behandle_i_Infotrygd_når_det_er_valgt() {
        String varseltekst = "varsel";
        VurderFeilutbetalingDto dto = new VurderFeilutbetalingDto("lorem ipsum", TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, varseltekst);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        verify(repository).lagre(eq(behandling), captor.capture());

        TilbakekrevingValg tilbakekrevingValg = captor.getValue();
        assertThat(tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt()).isNull();
        assertThat(tilbakekrevingValg.getGrunnerTilReduksjon()).isNull();
        assertThat(tilbakekrevingValg.getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING);
        assertThat(tilbakekrevingValg.getVarseltekst()).isEqualTo(varseltekst);
    }

    @Test
    public void skal_feile_når_Inntrekk_er_forsøkt_valgt() {

        // Arrange
        VurderFeilutbetalingDto dto = new VurderFeilutbetalingDto("lorem ipsum", TilbakekrevingVidereBehandling.INNTREKK, null);

        // Assert
        Assertions.assertThrows(IllegalArgumentException.class, () -> {

            // Act
            oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        });

    }

}
