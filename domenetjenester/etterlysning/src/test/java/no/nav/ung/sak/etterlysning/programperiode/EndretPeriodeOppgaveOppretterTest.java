package no.nav.ung.sak.etterlysning.programperiode;

import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretperiode.EndretPeriodeDataDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretperiode.PeriodeEndringType;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretsluttdato.EndretSluttdatoDataDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretstartdato.EndretStartdatoDataDto;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

@ExtendWith(MockitoExtension.class)
class EndretPeriodeOppgaveOppretterTest {

    @Mock
    private UngBrukerdialogOppgaveKlient oppgaveKlient;
    @Mock
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Mock
    private EtterlysningRepository etterlysningRepository;
    @Mock
    private UngdomsytelseStartdatoRepository startdatoRepository;

    @Mock
    private Behandling behandling;
    @Mock
    private Fagsak fagsak;

    private EndretPeriodeOppgaveOppretter oppretter;

    @BeforeEach
    void setUp() {
        oppretter = new EndretPeriodeOppgaveOppretter(
            oppgaveKlient,
            ungdomsprogramPeriodeRepository,
            etterlysningRepository,
            startdatoRepository
        );

        when(behandling.getId()).thenReturn(100L);
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(fagsak.getYtelseType()).thenReturn(FagsakYtelseType.UNGDOMSYTELSE);

    }

    @Test
    void fjernet_periode_gir_endret_periode_oppgave_med_fjernet_periode_endringstype() {
        var initielt = grunnlagMedPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        var gjeldende = grunnlagUtenPeriode();
        var aktivEtterlysning = etterlysning(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        when(ungdomsprogramPeriodeRepository.hentInitiell(behandling.getId())).thenReturn(Optional.of(initielt));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(aktivEtterlysning.getGrunnlagsreferanse())).thenReturn(gjeldende);

        oppretter.opprettOppgave(behandling, List.of(aktivEtterlysning), AktørId.dummy());

        var dto = capturerOppgave();
        var oppgaveData = hentOppgaveData(dto);
        assertThat(oppgaveData).isInstanceOf(EndretPeriodeDataDto.class);
        assertThat(hentEndringstyper(oppgaveData)).containsExactly(PeriodeEndringType.FJERNET_PERIODE);
    }

    @Test
    void kun_endret_startdato_fra_tidligere_periodegrunnlag_gir_startdatooppgave() {
        var initielt = grunnlagMedPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        var gjeldende = grunnlagMedPeriode(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 12, 31));
        var tidligere = grunnlagMedPeriode(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 12, 31));
        var avbruttEtterlysning = etterlysningMedGrunnlagsreferanse(tidligere.getGrunnlagsreferanse(), EtterlysningStatus.AVBRUTT,
            LocalDate.of(2024, 2, 1), LocalDate.of(2024, 12, 31));
        var aktivEtterlysning = etterlysningMedGrunnlagsreferanse(gjeldende.getGrunnlagsreferanse(), EtterlysningStatus.OPPRETTET,
            LocalDate.of(2024, 2, 1), LocalDate.of(2024, 12, 31));

        when(ungdomsprogramPeriodeRepository.hentInitiell(behandling.getId())).thenReturn(Optional.of(initielt));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(aktivEtterlysning.getGrunnlagsreferanse())).thenReturn(gjeldende);
        when(startdatoRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.empty());
        when(etterlysningRepository.hentEtterlysningerMedSisteFørst(any(), eq(EtterlysningType.UTTALELSE_ENDRET_PERIODE)))
            .thenReturn(List.of(avbruttEtterlysning));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraReferanser(List.of(tidligere.getGrunnlagsreferanse()))).thenReturn(List.of(tidligere));

        oppretter.opprettOppgave(behandling, List.of(aktivEtterlysning), AktørId.dummy());

        var dto = capturerOppgave();
        var oppgaveData = hentOppgaveData(dto);
        assertThat(oppgaveData).isInstanceOf(EndretStartdatoDataDto.class);
        assertThat(hentAlleDatoer(oppgaveData)).contains(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 15));
    }

    @Test
    void kun_endret_startdato_fra_startdatogrunnlag_gir_startdatooppgave() {
        var initielt = grunnlagMedPeriode(LocalDate.of(2024, 2, 1), TIDENES_ENDE);
        var gjeldende = grunnlagMedPeriode(LocalDate.of(2024, 2, 1), TIDENES_ENDE);
        var aktivEtterlysning = etterlysningMedGrunnlagsreferanse(gjeldende.getGrunnlagsreferanse(), EtterlysningStatus.OPPRETTET,
            LocalDate.of(2024, 2, 1), TIDENES_ENDE);

        when(ungdomsprogramPeriodeRepository.hentInitiell(behandling.getId())).thenReturn(Optional.of(initielt));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(aktivEtterlysning.getGrunnlagsreferanse())).thenReturn(gjeldende);
        var startdatoGrunnlag = startdatoGrunnlag(LocalDate.of(2024, 1, 1));
        when(startdatoRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.of(startdatoGrunnlag));
        when(etterlysningRepository.hentEtterlysningerMedSisteFørst(any(), eq(EtterlysningType.UTTALELSE_ENDRET_PERIODE))).thenReturn(List.of());
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraReferanser(List.of())).thenReturn(List.of());

        oppretter.opprettOppgave(behandling, List.of(aktivEtterlysning), AktørId.dummy());

        var dto = capturerOppgave();
        var oppgaveData = hentOppgaveData(dto);
        assertThat(oppgaveData).isInstanceOf(EndretStartdatoDataDto.class);
        assertThat(hentAlleDatoer(oppgaveData)).contains(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 1));
    }

    @Test
    void endret_startdato_fra_startdatogrunnlag_med_flere_uendrede_grunnlag_gir_startdatooppgave() {
        var initielt = grunnlagMedPeriode(LocalDate.of(2024, 2, 1), TIDENES_ENDE);
        var gjeldende = grunnlagMedPeriode(LocalDate.of(2024, 2, 1), TIDENES_ENDE);
        var tidligere1 = grunnlagMedPeriode(LocalDate.of(2024, 2, 1), TIDENES_ENDE);
        var tidligere2 = grunnlagMedPeriode(LocalDate.of(2024, 2, 1), TIDENES_ENDE);
        var aktivEtterlysning = etterlysningMedGrunnlagsreferanse(gjeldende.getGrunnlagsreferanse(), EtterlysningStatus.OPPRETTET,
            LocalDate.of(2024, 2, 1), TIDENES_ENDE);
        var avbrutt1 = etterlysningMedGrunnlagsreferanse(tidligere1.getGrunnlagsreferanse(), EtterlysningStatus.AVBRUTT,
            LocalDate.of(2024, 2, 1), TIDENES_ENDE);
        var avbrutt2 = etterlysningMedGrunnlagsreferanse(tidligere2.getGrunnlagsreferanse(), EtterlysningStatus.AVBRUTT,
            LocalDate.of(2024, 2, 1), TIDENES_ENDE);

        when(ungdomsprogramPeriodeRepository.hentInitiell(behandling.getId())).thenReturn(Optional.of(initielt));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(aktivEtterlysning.getGrunnlagsreferanse())).thenReturn(gjeldende);
        when(etterlysningRepository.hentEtterlysningerMedSisteFørst(any(), eq(EtterlysningType.UTTALELSE_ENDRET_PERIODE)))
            .thenReturn(List.of(avbrutt1, avbrutt2));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraReferanser(List.of(tidligere1.getGrunnlagsreferanse(), tidligere2.getGrunnlagsreferanse())))
            .thenReturn(List.of(tidligere1, tidligere2));
        var startdatoGrunnlag = startdatoGrunnlag(LocalDate.of(2024, 1, 1), LocalDate.of(2023, 11, 1));
        when(startdatoRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.of(startdatoGrunnlag));

        oppretter.opprettOppgave(behandling, List.of(aktivEtterlysning), AktørId.dummy());

        var dto = capturerOppgave();
        var oppgaveData = hentOppgaveData(dto);
        assertThat(oppgaveData).isInstanceOf(EndretStartdatoDataDto.class);
        assertThat(hentAlleDatoer(oppgaveData)).contains(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 1));
    }

    @Test
    void kun_endret_sluttdato_fra_forrige_grunnlag_gir_sluttdatooppgave() {
        var initielt = grunnlagMedPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        var gjeldende = grunnlagMedPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 11, 30));
        var forrige = grunnlagMedPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        var avbruttEtterlysning = etterlysningMedGrunnlagsreferanse(forrige.getGrunnlagsreferanse(), EtterlysningStatus.AVBRUTT,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 11, 30));
        var aktivEtterlysning = etterlysningMedGrunnlagsreferanse(gjeldende.getGrunnlagsreferanse(), EtterlysningStatus.OPPRETTET,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 11, 30));

        when(ungdomsprogramPeriodeRepository.hentInitiell(behandling.getId())).thenReturn(Optional.of(initielt));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(aktivEtterlysning.getGrunnlagsreferanse())).thenReturn(gjeldende);
        when(startdatoRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.empty());
        when(etterlysningRepository.hentEtterlysningerMedSisteFørst(any(), eq(EtterlysningType.UTTALELSE_ENDRET_PERIODE)))
            .thenReturn(List.of(avbruttEtterlysning));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraReferanser(List.of(forrige.getGrunnlagsreferanse()))).thenReturn(List.of(forrige));

        oppretter.opprettOppgave(behandling, List.of(aktivEtterlysning), AktørId.dummy());

        var dto = capturerOppgave();
        var oppgaveData = hentOppgaveData(dto);
        assertThat(oppgaveData).isInstanceOf(EndretSluttdatoDataDto.class);
        assertThat(hentAlleDatoer(oppgaveData)).contains(LocalDate.of(2024, 11, 30), LocalDate.of(2024, 12, 31));
    }

    @Test
    void kun_endret_sluttdato_fra_tidligere_ikke_forrige_grunnlag_gir_sluttdatooppgave() {
        var initielt = grunnlagMedPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        var gjeldende = grunnlagMedPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 11, 30));
        var forrige = grunnlagMedPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 11, 30));
        var tidligere = grunnlagMedPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        var avbruttForrige = etterlysningMedGrunnlagsreferanse(forrige.getGrunnlagsreferanse(), EtterlysningStatus.AVBRUTT,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 11, 30));
        var avbruttTidligere = etterlysningMedGrunnlagsreferanse(tidligere.getGrunnlagsreferanse(), EtterlysningStatus.AVBRUTT,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        var aktivEtterlysning = etterlysningMedGrunnlagsreferanse(gjeldende.getGrunnlagsreferanse(), EtterlysningStatus.OPPRETTET,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 11, 30));

        when(ungdomsprogramPeriodeRepository.hentInitiell(behandling.getId())).thenReturn(Optional.of(initielt));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(aktivEtterlysning.getGrunnlagsreferanse())).thenReturn(gjeldende);
        when(startdatoRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.empty());
        when(etterlysningRepository.hentEtterlysningerMedSisteFørst(any(), eq(EtterlysningType.UTTALELSE_ENDRET_PERIODE)))
            .thenReturn(List.of(avbruttForrige, avbruttTidligere));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraReferanser(List.of(forrige.getGrunnlagsreferanse(), tidligere.getGrunnlagsreferanse())))
            .thenReturn(List.of(forrige, tidligere));

        oppretter.opprettOppgave(behandling, List.of(aktivEtterlysning), AktørId.dummy());

        var dto = capturerOppgave();
        var oppgaveData = hentOppgaveData(dto);
        assertThat(oppgaveData).isInstanceOf(EndretSluttdatoDataDto.class);
        assertThat(hentAlleDatoer(oppgaveData)).contains(LocalDate.of(2024, 11, 30), LocalDate.of(2024, 12, 31));
    }

    @Test
    void endret_startdato_og_sluttdato_i_forrige_grunnlag_gir_endret_periodeoppgave() {
        var initielt = grunnlagMedPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        var gjeldende = grunnlagMedPeriode(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 11, 30));
        var forrige = grunnlagMedPeriode(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 12, 31));
        var avbruttEtterlysning = etterlysningMedGrunnlagsreferanse(forrige.getGrunnlagsreferanse(), EtterlysningStatus.AVBRUTT,
            LocalDate.of(2024, 2, 1), LocalDate.of(2024, 11, 30));
        var aktivEtterlysning = etterlysningMedGrunnlagsreferanse(gjeldende.getGrunnlagsreferanse(), EtterlysningStatus.OPPRETTET,
            LocalDate.of(2024, 2, 1), LocalDate.of(2024, 11, 30));

        when(ungdomsprogramPeriodeRepository.hentInitiell(behandling.getId())).thenReturn(Optional.of(initielt));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(aktivEtterlysning.getGrunnlagsreferanse())).thenReturn(gjeldende);
        when(startdatoRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.empty());
        when(etterlysningRepository.hentEtterlysningerMedSisteFørst(any(), eq(EtterlysningType.UTTALELSE_ENDRET_PERIODE)))
            .thenReturn(List.of(avbruttEtterlysning));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraReferanser(List.of(forrige.getGrunnlagsreferanse()))).thenReturn(List.of(forrige));

        oppretter.opprettOppgave(behandling, List.of(aktivEtterlysning), AktørId.dummy());

        var dto = capturerOppgave();
        var oppgaveData = hentOppgaveData(dto);
        assertThat(oppgaveData).isInstanceOf(EndretPeriodeDataDto.class);
        assertThat(hentEndringstyper(oppgaveData)).containsExactlyInAnyOrder(PeriodeEndringType.ENDRET_STARTDATO, PeriodeEndringType.ENDRET_SLUTTDATO);
    }

    @Test
    void endret_startdato_i_tidligere_grunnlag_og_sluttdato_i_forrige_med_avbrutt_etterlysning_gir_endret_periodeoppgave() {
        var initielt = grunnlagMedPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        var gjeldende = grunnlagMedPeriode(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 11, 30));
        var forrige = grunnlagMedPeriode(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 12, 31));
        var tidligere = grunnlagMedPeriode(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 12, 31));

        var opprettetEtterlysning = etterlysning(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 11, 30));
        var avbruttForrige = etterlysningMedGrunnlagsreferanse(forrige.getGrunnlagsreferanse(), EtterlysningStatus.AVBRUTT,
            LocalDate.of(2024, 2, 1), LocalDate.of(2024, 12, 31));
        var avbruttTidligere = etterlysningMedGrunnlagsreferanse(tidligere.getGrunnlagsreferanse(), EtterlysningStatus.AVBRUTT,
            LocalDate.of(2024, 1, 15), LocalDate.of(2024, 12, 31));

        when(ungdomsprogramPeriodeRepository.hentInitiell(behandling.getId())).thenReturn(Optional.of(initielt));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(opprettetEtterlysning.getGrunnlagsreferanse())).thenReturn(gjeldende);
        when(startdatoRepository.hentGrunnlag(behandling.getId())).thenReturn(Optional.empty());
        when(etterlysningRepository.hentEtterlysningerMedSisteFørst(any(), eq(EtterlysningType.UTTALELSE_ENDRET_PERIODE)))
            .thenReturn(List.of(opprettetEtterlysning, avbruttForrige, avbruttTidligere));
        when(ungdomsprogramPeriodeRepository.hentGrunnlagFraReferanser(List.of(forrige.getGrunnlagsreferanse(), tidligere.getGrunnlagsreferanse())))
            .thenReturn(List.of(forrige, tidligere));

        oppretter.opprettOppgave(behandling, List.of(opprettetEtterlysning), AktørId.dummy());

        var dto = capturerOppgave();
        var oppgaveData = hentOppgaveData(dto);
        assertThat(oppgaveData).isInstanceOf(EndretPeriodeDataDto.class);
        assertThat(hentEndringstyper(oppgaveData)).containsExactlyInAnyOrder(PeriodeEndringType.ENDRET_STARTDATO, PeriodeEndringType.ENDRET_SLUTTDATO);
    }

    private OpprettOppgaveDto capturerOppgave() {
        var captor = ArgumentCaptor.forClass(OpprettOppgaveDto.class);
        verify(oppgaveKlient).opprettOppgave(captor.capture());
        return captor.getValue();
    }

    private static Object hentOppgaveData(OpprettOppgaveDto dto) {
        var kandidater = Arrays.stream(dto.getClass().getMethods())
            .filter(m -> m.getParameterCount() == 0)
            .filter(m -> m.getReturnType().getName().contains("DataDto"))
            .toList();

        if (kandidater.isEmpty()) {
            throw new IllegalStateException("Fant ikke oppgavedata-felt på " + dto.getClass().getName());
        }
        return invoke(dto, kandidater.getFirst());
    }

    @SuppressWarnings("unchecked")
    private static Set<PeriodeEndringType> hentEndringstyper(Object oppgaveData) {
        var method = Arrays.stream(oppgaveData.getClass().getMethods())
            .filter(m -> m.getParameterCount() == 0)
            .filter(m -> Set.class.isAssignableFrom(m.getReturnType()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Fant ikke endringstyper på " + oppgaveData.getClass().getName()));
        return (Set<PeriodeEndringType>) invoke(oppgaveData, method);
    }

    private static List<LocalDate> hentAlleDatoer(Object dto) {
        return Arrays.stream(dto.getClass().getMethods())
            .filter(m -> m.getParameterCount() == 0)
            .filter(m -> m.getReturnType().equals(LocalDate.class))
            .map(m -> (LocalDate) invoke(dto, m))
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    private static Object invoke(Object target, Method method) {
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Klarte ikke lese " + method.getName() + " fra " + target.getClass().getName(), e);
        }
    }

    private static Etterlysning etterlysning(LocalDate fom, LocalDate tom) {
        return etterlysningMedGrunnlagsreferanse(UUID.randomUUID(), EtterlysningStatus.OPPRETTET, fom, tom);
    }

    private static Etterlysning etterlysningMedGrunnlagsreferanse(UUID grunnlagsreferanse, EtterlysningStatus status, LocalDate fom, LocalDate tom) {
        var etterlysning = new Etterlysning(
            100L,
            grunnlagsreferanse,
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            EtterlysningType.UTTALELSE_ENDRET_PERIODE,
            status
        );
        etterlysning.setFrist(LocalDateTime.now().plusDays(7));
        return etterlysning;
    }

    private static UngdomsprogramPeriodeGrunnlag grunnlagMedPeriode(LocalDate fom, LocalDate tom) {
        var grunnlag = org.mockito.Mockito.mock(UngdomsprogramPeriodeGrunnlag.class);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        var perioder = new UngdomsprogramPerioder(Set.of(new UngdomsprogramPeriode(periode)));
        lenient().when(grunnlag.getGrunnlagsreferanse()).thenReturn(UUID.randomUUID());
        lenient().when(grunnlag.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(periode));
        lenient().when(grunnlag.getUngdomsprogramPerioder()).thenReturn(perioder);
        return grunnlag;
    }

    private static UngdomsprogramPeriodeGrunnlag grunnlagUtenPeriode() {
        var grunnlag = org.mockito.Mockito.mock(UngdomsprogramPeriodeGrunnlag.class);
        lenient().when(grunnlag.getGrunnlagsreferanse()).thenReturn(UUID.randomUUID());
        lenient().when(grunnlag.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.empty());
        return grunnlag;
    }

    private static UngdomsytelseStartdatoGrunnlag startdatoGrunnlag(LocalDate... startdatoer) {
        var grunnlag = org.mockito.Mockito.mock(UngdomsytelseStartdatoGrunnlag.class);
        var oppgitteStartdatoer = org.mockito.Mockito.mock(UngdomsytelseStartdatoer.class);
        var søkteStartdatoer = Arrays.stream(startdatoer)
            .map(dato -> new UngdomsytelseSøktStartdato(dato, new JournalpostId("123")))
            .collect(java.util.stream.Collectors.toSet());
        when(grunnlag.getOppgitteStartdatoer()).thenReturn(oppgitteStartdatoer);
        when(oppgitteStartdatoer.getStartdatoer()).thenReturn(søkteStartdatoer);
        return grunnlag;
    }
}




