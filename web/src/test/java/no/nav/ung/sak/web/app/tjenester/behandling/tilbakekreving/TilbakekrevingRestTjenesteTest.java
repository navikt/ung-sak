package no.nav.ung.sak.web.app.tjenester.behandling.tilbakekreving;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.økonomi.tilbakekreving.TilbakekrevingValgDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;

public class TilbakekrevingRestTjenesteTest {

    private BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private TilbakekrevingRepository tilbakekrevingRepository = mock(TilbakekrevingRepository.class);
    private TilbakekrevingRestTjeneste tilbakekrevingRestTjeneste = new TilbakekrevingRestTjeneste(behandlingRepository, tilbakekrevingRepository);

    @BeforeEach
    public void setup() {
        when(behandlingRepository.hentBehandling((Long) Mockito.any())).thenAnswer(invocation -> lagBehandling());
        when(behandlingRepository.hentBehandling((UUID) Mockito.any())).thenAnswer(invocation -> lagBehandling());
    }

    @Test
    public void skal_hentTilbakekrevingValg_når_tilbakekrevingsvalg_finnes() {
        when(tilbakekrevingRepository.hent(Mockito.any())).thenReturn(Optional.of(TilbakekrevingValg.medMulighetForInntrekk(true, true, TilbakekrevingVidereBehandling.INNTREKK)));
        TilbakekrevingValgDto tilbakekrevingValgDto = tilbakekrevingRestTjeneste.hentTilbakekrevingValg(new BehandlingUuidDto("1098c6f4-4ae2-4794-8a23-9224675a1f99"));
        assertThat(tilbakekrevingValgDto).isNotNull();
        assertThat(tilbakekrevingValgDto.erTilbakekrevingVilkårOppfylt()).isTrue();
    }

    @Test
    public void skal_feil_hentTilbakekrevingValg_når_tilbakekrevingsvalg_ikke_finnes() {
        when(tilbakekrevingRepository.hent(Mockito.any())).thenReturn(Optional.empty());
        TilbakekrevingValgDto tilbakekrevingValgDto = tilbakekrevingRestTjeneste.hentTilbakekrevingValg(new BehandlingUuidDto("1098c6f4-4ae2-4794-8a23-9224675a1f99"));
        assertThat(tilbakekrevingValgDto).isNull();
    }

    @Test
    public void skal_hente_varseltekst_ved_henting_av_tilbakekrevingsvalg() {
        String forventetVarselTekst = "varseltekst her";
        when(tilbakekrevingRepository.hent(Mockito.any())).thenReturn(Optional.of(TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.INNTREKK, forventetVarselTekst)));
        TilbakekrevingValgDto tilbakekrevingValgDto = tilbakekrevingRestTjeneste.hentTilbakekrevingValg(new BehandlingUuidDto("1098c6f4-4ae2-4794-8a23-9224675a1f99"));
        assertThat(tilbakekrevingValgDto.getVarseltekst()).isEqualTo(forventetVarselTekst);

    }

    private Behandling lagBehandling() {

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, AktørId.dummy(), new Saksnummer("123456"));
        return Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
    }

}
