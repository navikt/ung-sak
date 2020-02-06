package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt.TilbakekrevingvalgHistorikkinnslagBygger;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt.VurderFeilutbetalingOppdaterer;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.sak.kontrakt.økonomi.VurderFeilutbetalingDto;

public class VurderFeilutbetalingOppdatererTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private TilbakekrevingRepository repository = mock(TilbakekrevingRepository.class);
    private HistorikkTjenesteAdapter historikkTjenesteAdapter = mock(HistorikkTjenesteAdapter.class);
    private TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger = new TilbakekrevingvalgHistorikkinnslagBygger(historikkTjenesteAdapter);
    private VurderFeilutbetalingOppdaterer oppdaterer = new VurderFeilutbetalingOppdaterer(repository, historikkInnslagBygger);

    private ArgumentCaptor<TilbakekrevingValg> captor = ArgumentCaptor.forClass(TilbakekrevingValg.class);

    private Behandling behandling;

    @Before
    public void setup() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        this.behandling = scenario.lagMocked();
    }

    @Test
    public void skal_lagre_at_videre_behandling_behandle_i_Infotrygd_når_det_er_valgt() {
        String varseltekst = "varsel";
        VurderFeilutbetalingDto dto = new VurderFeilutbetalingDto("lorem ipsum", TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD, varseltekst);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        verify(repository).lagre(eq(behandling), captor.capture());

        TilbakekrevingValg tilbakekrevingValg = captor.getValue();
        assertThat(tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt()).isNull();
        assertThat(tilbakekrevingValg.getGrunnerTilReduksjon()).isNull();
        assertThat(tilbakekrevingValg.getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD);
        assertThat(tilbakekrevingValg.getVarseltekst()).isEqualTo(varseltekst);
    }

    @Test
    public void skal_feile_når_Inntrekk_er_forsøkt_valgt() {
        VurderFeilutbetalingDto dto = new VurderFeilutbetalingDto("lorem ipsum", TilbakekrevingVidereBehandling.INNTREKK, null);

        expectedException.expect(IllegalArgumentException.class);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

    }

}
