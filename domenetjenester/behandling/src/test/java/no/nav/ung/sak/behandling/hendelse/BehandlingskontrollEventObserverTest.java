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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BehandlingskontrollEventObserverTest {

    @Test
    void senderEksplisittStegVedAvslutningAvNavKontor() {
        verifiserOvergang(mock(BehandlingStegOvergangEvent.class),
            BehandlingStegType.NAV_KONTOR_BESLUTTER_VILKÅR, BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR,
            EventHendelse.AVSLUTTE_NAVKONTOR, BehandlingStegType.NAV_KONTOR_BESLUTTER_VILKÅR);
    }

    @Test
    void overstyrerIkkeStegVedGjenopptakAvNavKontor() {
        verifiserOvergang(mock(BehandlingStegOvergangEvent.BehandlingStegTilbakeføringEvent.class),
            BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR, BehandlingStegType.NAV_KONTOR_BESLUTTER_VILKÅR,
            EventHendelse.GJENOPPTA_NAVKONTOR, null);
    }

    private void verifiserOvergang(BehandlingStegOvergangEvent event, BehandlingStegType fraSteg, BehandlingStegType tilSteg,
                                  EventHendelse hendelse, BehandlingStegType overstyrtSteg) {
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
        when(mapper.getProduksjonstyringEventDto(any(), eq(hendelse), eq(behandling), isNull(), eq(overstyrtSteg)))
            .thenReturn(BehandlingProsessHendelse.builder().medAksjonspunktTilstander(List.of()).build());
        var observer = new BehandlingskontrollEventObserver(taskTjeneste, repository, mapper, false);

        observer.observerOvergangDel1Del2(event);

        verify(mapper).getProduksjonstyringEventDto(any(), eq(hendelse), eq(behandling), isNull(), eq(overstyrtSteg));
        verify(taskTjeneste).lagre(any(ProsessTaskData.class));
    }
}
