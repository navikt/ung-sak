package no.nav.ung.sak.etterlysning.opphorvedmaksdato;

import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.opphorvedmaksdato.BekreftOpphorVedMaksdatoOppgavetypeDataDto;
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
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpphørVedMaksdatoOppgaveOppretterTest {

    @Mock
    private UngBrukerdialogOppgaveKlient oppgaveKlient;
    @Mock
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Mock
    private Behandling behandling;
    @Mock
    private Fagsak fagsak;

    private OpphørVedMaksdatoOppgaveOppretter oppretter;

    @BeforeEach
    void setUp() {
        oppretter = new OpphørVedMaksdatoOppgaveOppretter(oppgaveKlient, ungdomsprogramPeriodeRepository);
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(fagsak.getYtelseType()).thenReturn(FagsakYtelseType.UNGDOMSYTELSE);
        when(fagsak.getSaksnummer()).thenReturn(new Saksnummer("123"));
    }

    @Test
    void skal_opprette_oppgave_for_etterlysning() {
        var maksdato = LocalDate.now().plusWeeks(2);
        var etterlysning = opprettEtterlysning(maksdato);
        stubGrunnlag(etterlysning, TIDENES_ENDE, maksdato);

        oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123"));

        var dto = capturerOppgave();
        assertThat(dto.journalføring()).isNotNull();
        assertThat(dto.journalføring().saksnummer().getVerdi()).isEqualTo("123");
    }

    @Test
    void skal_bruke_maksdato_som_sluttdato_når_programperioden_er_åpen() {
        var maksdato = LocalDate.of(2026, 10, 30);
        var etterlysning = opprettEtterlysning(maksdato);
        stubGrunnlag(etterlysning, TIDENES_ENDE, maksdato);

        oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123"));

        var data = (BekreftOpphorVedMaksdatoOppgavetypeDataDto) capturerOppgave().oppgavetypeData();
        assertThat(data.sluttdato()).isEqualTo(maksdato);
        assertThat(data.maxDato()).isEqualTo(maksdato);
    }

    @Test
    void skal_bruke_maksdato_som_sluttdato_når_programperioden_slutter_etter_maksdato() {
        var maksdato = LocalDate.of(2026, 10, 30);
        var etterlysning = opprettEtterlysning(maksdato);
        stubGrunnlag(etterlysning, LocalDate.of(2026, 12, 31), maksdato);

        oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123"));

        var data = (BekreftOpphorVedMaksdatoOppgavetypeDataDto) capturerOppgave().oppgavetypeData();
        assertThat(data.sluttdato()).isEqualTo(maksdato);
        assertThat(data.maxDato()).isEqualTo(maksdato);
    }

    @Test
    void skal_bruke_maksdato_som_sluttdato_når_programperioden_slutter_på_maksdato() {
        var maksdato = LocalDate.of(2026, 10, 30);
        var etterlysning = opprettEtterlysning(maksdato);
        stubGrunnlag(etterlysning, maksdato, maksdato);

        oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123"));

        var data = (BekreftOpphorVedMaksdatoOppgavetypeDataDto) capturerOppgave().oppgavetypeData();
        assertThat(data.sluttdato()).isEqualTo(maksdato);
        assertThat(data.maxDato()).isEqualTo(maksdato);
    }

    @Test
    void skal_opprette_oppgave_for_alle_etterlysninger() {
        var etterlysning1 = opprettEtterlysning(LocalDate.now().plusWeeks(2));
        var etterlysning2 = opprettEtterlysning(LocalDate.now().plusWeeks(1));
        stubGrunnlag(etterlysning1, TIDENES_ENDE, LocalDate.now().plusWeeks(2));
        stubGrunnlag(etterlysning2, TIDENES_ENDE, LocalDate.now().plusWeeks(1));

        oppretter.opprettOppgave(behandling, List.of(etterlysning1, etterlysning2), new AktørId("1234567890123"));

        var captor = ArgumentCaptor.forClass(OpprettOppgaveDto.class);
        verify(oppgaveKlient, times(2)).opprettOppgave(captor.capture());
        assertThat(captor.getAllValues())
            .allSatisfy(dto -> assertThat(dto.journalføring().saksnummer().getVerdi()).isEqualTo("123"));
    }

    private OpprettOppgaveDto capturerOppgave() {
        var captor = ArgumentCaptor.forClass(OpprettOppgaveDto.class);
        verify(oppgaveKlient).opprettOppgave(captor.capture());
        return captor.getValue();
    }

    private void stubGrunnlag(Etterlysning etterlysning, LocalDate programperiodeTom, LocalDate maksdato) {
        var fom = maksdato.minusWeeks(52);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, programperiodeTom);
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag.hentForEksaktEnPeriode()).thenReturn(periode);
        when(grunnlag.getPeriodeMaksDato()).thenReturn(Optional.of(maksdato));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse()))
            .thenReturn(grunnlag);
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


