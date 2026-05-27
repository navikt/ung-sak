package no.nav.ung.sak.etterlysning.automatiskopphor;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomatiskOpphørOppgaveOppretterTest {

    @Mock
    private UngBrukerdialogOppgaveKlient oppgaveKlient;
    @Mock
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Mock
    private Behandling behandling;
    @Mock
    private Fagsak fagsak;
    @Mock
    private UngdomsprogramPeriodeGrunnlag grunnlag;

    private AutomatiskOpphørOppgaveOppretter oppretter;

    @BeforeEach
    void setUp() {
        oppretter = new AutomatiskOpphørOppgaveOppretter(oppgaveKlient, ungdomsprogramPeriodeRepository);
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(fagsak.getYtelseType()).thenReturn(FagsakYtelseType.UNGDOMSYTELSE);
    }

    @Test
    void skal_opprette_oppgave_nar_maksdato_er_innenfor_varselvindu() {
        var maksdato = LocalDate.now().plusWeeks(2);
        var etterlysning = opprettEtterlysning(maksdato);

        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse()))
            .thenReturn(grunnlag);
        when(grunnlag.getPeriodeMaksDato()).thenReturn(Optional.of(maksdato));

        oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123"));

        verify(oppgaveKlient).opprettOppgave(any());
    }

    @Test
    void skal_ikke_opprette_oppgave_nar_maksdato_er_for_gammel() {
        var maksdato = LocalDate.now().minusDays(4);
        var etterlysning = opprettEtterlysning(maksdato);

        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse()))
            .thenReturn(grunnlag);
        when(grunnlag.getPeriodeMaksDato()).thenReturn(Optional.of(maksdato));

        oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123"));

        verify(oppgaveKlient, never()).opprettOppgave(any());
    }

    @Test
    void skal_ikke_opprette_oppgave_nar_maksdato_er_for_langt_frem_i_tid() {
        var maksdato = LocalDate.now().plusWeeks(5);
        var etterlysning = opprettEtterlysning(maksdato);

        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse()))
            .thenReturn(grunnlag);
        when(grunnlag.getPeriodeMaksDato()).thenReturn(Optional.of(maksdato));

        oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123"));

        verify(oppgaveKlient, never()).opprettOppgave(any());
    }

    private Etterlysning opprettEtterlysning(LocalDate maksdato) {
        var fom = maksdato.minusDays(1);
        var etterlysning = Etterlysning.opprettForType(
            1L,
            UUID.randomUUID(),
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, maksdato),
            EtterlysningType.UTTALELSE_AUTOMATISK_OPPHOR
        );
        etterlysning.vent(LocalDateTime.now().plusDays(1));
        return etterlysning;
    }
}


