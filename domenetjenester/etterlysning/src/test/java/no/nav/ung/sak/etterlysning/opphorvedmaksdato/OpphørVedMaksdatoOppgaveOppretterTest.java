package no.nav.ung.sak.etterlysning.opphorvedmaksdato;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpphørVedMaksdatoOppgaveOppretterTest {

    @Mock
    private UngBrukerdialogOppgaveKlient oppgaveKlient;
    @Mock
    private Behandling behandling;
    @Mock
    private Fagsak fagsak;

    private OpphørVedMaksdatoOppgaveOppretter oppretter;

    @BeforeEach
    void setUp() {
        oppretter = new OpphørVedMaksdatoOppgaveOppretter(oppgaveKlient);
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(fagsak.getYtelseType()).thenReturn(FagsakYtelseType.UNGDOMSYTELSE);
    }

    @Test
    void skal_opprette_oppgave_for_etterlysning() {
        var maksdato = LocalDate.now().plusWeeks(2);
        var etterlysning = opprettEtterlysning(maksdato);

        oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123"));

        verify(oppgaveKlient).opprettOppgave(any());
    }

    @Test
    void skal_opprette_oppgave_for_alle_etterlysninger() {
        var etterlysning1 = opprettEtterlysning(LocalDate.now().plusWeeks(2));
        var etterlysning2 = opprettEtterlysning(LocalDate.now().plusWeeks(1));

        oppretter.opprettOppgave(behandling, List.of(etterlysning1, etterlysning2), new AktørId("1234567890123"));

        verify(oppgaveKlient, org.mockito.Mockito.times(2)).opprettOppgave(any());
    }

    private Etterlysning opprettEtterlysning(LocalDate maksdato) {
        var fom = maksdato.minusDays(1);
        var etterlysning = Etterlysning.opprettForType(
            1L,
            UUID.randomUUID(),
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, maksdato),
            EtterlysningType.UTTALELSE_OPPHOR_VED_MAKSDATO
        );
        etterlysning.vent(LocalDateTime.now().plusDays(1));
        return etterlysning;
    }
}


