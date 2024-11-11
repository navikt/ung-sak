package no.nav.ung.sak.behandling.hendelse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagDel;

public class BehandlingskontrollAksjonspunktTypeAutopunktEventObserverTest {

    private HistorikkInnslagForAksjonspunktEventObserver observer; // objectet vi tester

    private BehandlingskontrollKontekst behandlingskontrollKontekst;
    private AksjonspunktDefinisjon autopunktDefinisjon;
    private AksjonspunktDefinisjon manuellpunktDefinisjon;
    private Aksjonspunkt autopunkt;
    private Aksjonspunkt manuellpunkt;
    private HistorikkRepository historikkRepository;
    private Long behandlingId = 1L;
    private String PERIODE = "P2W";
    private LocalDate localDate = LocalDate.now().plus(Period.parse(PERIODE));

    @BeforeEach
    public void setup() {
        autopunktDefinisjon = AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
        manuellpunktDefinisjon = AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE;

        manuellpunkt = Mockito.mock(Aksjonspunkt.class);
        when(manuellpunkt.getAksjonspunktDefinisjon()).thenReturn(manuellpunktDefinisjon);

        autopunkt = Mockito.mock(Aksjonspunkt.class);
        when(autopunkt.erOpprettet()).thenReturn(true);
        when(autopunkt.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjon);
        when(autopunkt.getFristTid()).thenReturn(LocalDateTime.of(localDate, LocalDateTime.now().toLocalTime()));

        behandlingskontrollKontekst = mock(BehandlingskontrollKontekst.class);
        when(behandlingskontrollKontekst.getBehandlingId()).thenReturn(behandlingId);

        historikkRepository = mock(HistorikkRepository.class);
        observer = new HistorikkInnslagForAksjonspunktEventObserver(historikkRepository, "srvung-sak", "ung-sak");
    }

    @Test
    public void skal_opprette_historikk_for_behandling_på_vent() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunkt), null);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkRepository).lagre(any(Historikkinnslag.class));
    }

    @Test
    public void skalIkkeOppretteHistorikkForManuellPunkt() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(manuellpunkt), null);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkRepository, never()).lagre(any());
    }

    @Test
    public void skalOppretteEnHistorikkForAutoPunktOgSjekkPåResultat() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(manuellpunkt, autopunkt), null);

        ArgumentCaptor<Historikkinnslag> captor = ArgumentCaptor.forClass(Historikkinnslag.class);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkRepository).lagre(captor.capture());
        Historikkinnslag historikkinnslag = captor.getValue();
        HistorikkinnslagDel historikkinnslagDel = historikkinnslag.getHistorikkinnslagDeler().get(0);

        assertThat(historikkinnslag.getBehandlingId()).isEqualTo(behandlingId);
        assertThat(historikkinnslagDel.getHendelse()).hasValueSatisfying(hendelse -> {
            assertThat(hendelse.getNavn()).as("navn").isEqualTo(HistorikkinnslagType.BEH_VENT.getKode());
            assertThat(hendelse.getTilVerdi()).as("tilVerdi").isEqualTo(localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        });
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.BEH_VENT);
    }

    @Test
    public void skalOppretteToHistorikkForAutoPunkt() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunkt, autopunkt), null);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkRepository, times(2)).lagre(any());
    }

}
