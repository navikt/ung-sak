package no.nav.ung.sak.behandling.hendelse;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BehandlingskontrollAksjonspunktTypeAutopunktEventObserverTest {

    private HistorikkInnslagForAksjonspunktEventObserver observer; // objectet vi tester

    private BehandlingskontrollKontekst behandlingskontrollKontekst;
    private AksjonspunktDefinisjon autopunktDefinisjon;
    private AksjonspunktDefinisjon manuellpunktDefinisjon;
    private Aksjonspunkt autopunkt;
    private Aksjonspunkt manuellpunkt;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private Long behandlingId = 1L;
    private String PERIODE = "P2W";
    private LocalDate localDate = LocalDate.now().plus(Period.parse(PERIODE));

    @BeforeEach
    public void setup() {
        autopunktDefinisjon = AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
        manuellpunktDefinisjon = AksjonspunktDefinisjon.OVERSTYRING_AV_INNTEKT;

        manuellpunkt = Mockito.mock(Aksjonspunkt.class);
        when(manuellpunkt.getAksjonspunktDefinisjon()).thenReturn(manuellpunktDefinisjon);

        autopunkt = Mockito.mock(Aksjonspunkt.class);
        when(autopunkt.erOpprettet()).thenReturn(true);
        when(autopunkt.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjon);
        when(autopunkt.getFristTid()).thenReturn(LocalDateTime.of(localDate, LocalDateTime.now().toLocalTime()));

        behandlingskontrollKontekst = mock(BehandlingskontrollKontekst.class);
        when(behandlingskontrollKontekst.getBehandlingId()).thenReturn(behandlingId);

        historikkinnslagRepository = mock(HistorikkinnslagRepository.class);
        observer = new HistorikkInnslagForAksjonspunktEventObserver(historikkinnslagRepository, "srvung-sak", "ung-sak");
    }

    @Test
    public void skal_opprette_historikk_for_behandling_på_vent() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunkt), null);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkinnslagRepository).lagre(any(Historikkinnslag.class));
    }

    @Test
    public void skalIkkeOppretteHistorikkForManuellPunkt() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(manuellpunkt), null);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkinnslagRepository, never()).lagre(any());
    }

    @Test
    public void skalOppretteEnHistorikkForAutoPunktOgSjekkPåResultat() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(manuellpunkt, autopunkt), null);

        ArgumentCaptor<Historikkinnslag> captor = ArgumentCaptor.forClass(Historikkinnslag.class);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkinnslagRepository).lagre(captor.capture());
        Historikkinnslag historikkinnslag = captor.getValue();
        var linje = historikkinnslag.getLinjer().get(0);

        assertThat(historikkinnslag.getBehandlingId()).isEqualTo(behandlingId);
        assertThat(linje.getTekst()).isEqualTo("");
    }

    @Test
    public void skalOppretteToHistorikkForAutoPunkt() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunkt, autopunkt), null);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkinnslagRepository, times(2)).lagre(any());
    }

}
