package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt.AvklarFortsattMedlemskapDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.AksjonspunktGodkjenningDto;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.vedtak.exception.FunksjonellException;

public class AksjonspunktRestTjenesteTest {

    private static final long behandlingId = 1L;
    private static final Long behandlingVersjon = 2L;
    private static final String begrunnelse = "skal_håndtere_overlappende_perioder";
    private AksjonspunktRestTjeneste aksjonspunktRestTjeneste;
    private AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjenesteMock = mock(AksjonspunktApplikasjonTjeneste.class);
    private BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjenesteMock = mock(BehandlingsutredningApplikasjonTjeneste.class);
    private BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private Behandling behandling = mock(Behandling.class);
    private TotrinnTjeneste totrinnTjeneste = mock(TotrinnTjeneste.class);

    @Before
    public void setUp() {
        when(behandling.getUuid()).thenReturn(UUID.randomUUID());
        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(behandling);
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(behandling);
        when(behandling.getStatus()).thenReturn(no.nav.k9.kodeverk.behandling.BehandlingStatus.OPPRETTET);
        doNothing().when(behandlingsutredningApplikasjonTjenesteMock).kanEndreBehandling(anyLong(), anyLong());
        aksjonspunktRestTjeneste = new AksjonspunktRestTjeneste(aksjonspunktApplikasjonTjenesteMock, behandlingRepository,
            behandlingsutredningApplikasjonTjenesteMock, totrinnTjeneste);

    }

    @Test
    public void skal_bekrefte_fatte_vedtak_med_aksjonspunkt_godkjent() throws URISyntaxException {
        when(behandling.getStatus()).thenReturn(BehandlingStatus.FATTER_VEDTAK);
        Collection<BekreftetAksjonspunktDto> aksjonspunkt = new ArrayList<>();
        Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos = new ArrayList<>();
        AksjonspunktGodkjenningDto godkjentAksjonspunkt = opprettetGodkjentAksjonspunkt(true);
        aksjonspunktGodkjenningDtos.add(godkjentAksjonspunkt);
        aksjonspunkt.add(
            new FatterVedtakAksjonspunktDto(
                begrunnelse,
                aksjonspunktGodkjenningDtos));

        aksjonspunktRestTjeneste.bekreft(BekreftedeAksjonspunkterDto.lagDto(behandlingId, behandlingVersjon, aksjonspunkt));

        verify(aksjonspunktApplikasjonTjenesteMock).bekreftAksjonspunkter(ArgumentMatchers.anyCollection(), anyLong());
    }

    @Test(expected = FunksjonellException.class)
    public void skal_ikke_kunne_bekrefte_andre_aksjonspunkt_ved_status_fatter_vedtak() throws URISyntaxException {
        when(behandling.getStatus()).thenReturn(BehandlingStatus.FATTER_VEDTAK);
        Collection<BekreftetAksjonspunktDto> aksjonspunkt = new ArrayList<>();
        aksjonspunkt.add(new AvklarFortsattMedlemskapDto(begrunnelse, new ArrayList<>()));
        aksjonspunktRestTjeneste.bekreft(BekreftedeAksjonspunkterDto.lagDto(behandlingId, behandlingVersjon, aksjonspunkt));
    }

    private AksjonspunktGodkjenningDto opprettetGodkjentAksjonspunkt(boolean godkjent) {
        AksjonspunktGodkjenningDto endretDto = new AksjonspunktGodkjenningDto();
        endretDto.setAksjonspunktKode(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
        endretDto.setGodkjent(godkjent);
        return endretDto;
    }

}
