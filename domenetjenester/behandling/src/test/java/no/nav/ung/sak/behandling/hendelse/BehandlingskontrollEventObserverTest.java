package no.nav.ung.sak.behandling.hendelse;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.hendelse.EventHendelse;
import no.nav.ung.sak.behandling.hendelse.produksjonsstyring.BehandlingProsessHendelseMapper;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStegOvergangEvent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.behandling.BehandlingProsessHendelse;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BehandlingskontrollEventObserverTest {

    @Test
    void publisererAvslutningNårNavKontorErFerdig() {
        verifiserOvergang(mock(BehandlingStegOvergangEvent.class),
            BehandlingStegType.NAV_KONTOR_BESLUTTER_VILKÅR, BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR,
            EventHendelse.AVSLUTTE_NAVKONTOR);
    }

    @Test
    void publisererGjenopptakVedTilbakeføringTilNavKontor() {
        verifiserOvergang(mock(BehandlingStegOvergangEvent.BehandlingStegTilbakeføringEvent.class),
            BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR, BehandlingStegType.NAV_KONTOR_BESLUTTER_VILKÅR,
            EventHendelse.GJENOPPTA_NAVKONTOR);
    }

    @Test
    void publisererIkkeAvslutningVedVanligInngangTilNavKontor() {
        var event = mock(BehandlingStegOvergangEvent.class);
        var behandling = mock(Behandling.class);
        var repository = mock(BehandlingRepository.class);
        var mapper = mock(BehandlingProsessHendelseMapper.class);
        var taskTjeneste = mock(ProsessTaskTjeneste.class);
        when(event.getBehandlingId()).thenReturn(1L);
        when(event.getFraStegType()).thenReturn(BehandlingStegType.VURDER_SØKNADSFRIST);
        when(event.getTilStegType()).thenReturn(BehandlingStegType.ALDERSVILKÅRET);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.AKTIVITETSPENGER);
        when(repository.hentBehandling(1L)).thenReturn(behandling);
        var observer = new BehandlingskontrollEventObserver(taskTjeneste, repository, mapper, false);

        observer.observerOvergangDel1Del2(event);

        verify(taskTjeneste, never()).lagre(any(ProsessTaskData.class));
        verify(mapper, never()).getProduksjonstyringEventDto(any(), any());
    }

    private void verifiserOvergang(BehandlingStegOvergangEvent event, BehandlingStegType fraSteg, BehandlingStegType tilSteg,
                                  EventHendelse hendelse) {
        var behandling = mock(Behandling.class);
        var repository = mock(BehandlingRepository.class);
        var mapper = mock(BehandlingProsessHendelseMapper.class);
        var taskTjeneste = mock(ProsessTaskTjeneste.class);
        when(event.getBehandlingId()).thenReturn(1L);
        when(event.getFraStegType()).thenReturn(fraSteg);
        when(event.getTilStegType()).thenReturn(tilSteg);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.AKTIVITETSPENGER);
        when(repository.hentBehandling(1L)).thenReturn(behandling);
        when(repository.hentBehandlingHvisFinnes(1L)).thenReturn(Optional.of(behandling));
        when(mapper.getProduksjonstyringEventDto(eq(hendelse), eq(behandling)))
            .thenReturn(BehandlingProsessHendelse.builder().medAksjonspunktTilstander(List.of()).build());
        var observer = new BehandlingskontrollEventObserver(taskTjeneste, repository, mapper, false);

        observer.observerOvergangDel1Del2(event);

        verify(mapper).getProduksjonstyringEventDto(eq(hendelse), eq(behandling));
        verify(taskTjeneste).lagre(any(ProsessTaskData.class));
    }
}
