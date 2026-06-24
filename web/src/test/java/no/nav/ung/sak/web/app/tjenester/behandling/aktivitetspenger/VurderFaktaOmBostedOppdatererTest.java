package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.inject.Instance;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlag;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsfaktaOgAvklaring;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedFaktaavklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedVurderingIkkeOppfyltDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderFaktaOmBostedDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.annotation.Annotation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static no.nav.ung.kodeverk.vilkår.VilkårType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VurderFaktaOmBostedOppdatererTest {

    private static final long BEHANDLING_ID = 123L;
    private static final long FAGSAK_ID = 456L;
    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private VurderFaktaOmBostedOppdaterer oppdaterer;
    private Behandling behandling;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;

    @BeforeEach
    void setUp() {
        behandlingRepository = mock(BehandlingRepository.class);
        historikkinnslagRepository = mock(HistorikkinnslagRepository.class);
        bostedsGrunnlagRepository = mock(BostedsGrunnlagRepository.class);
        etterlysningRepository = mock(EtterlysningRepository.class);
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        vilkårsPerioderTilVurderingTjeneste = mock(Instance.class);
        inngangsvilkårVurderingTjeneste = mock(InngangsvilkårVurderingTjeneste.class);

        mockPerioderTilVurdering(FOM, TOM);

        oppdaterer = new VurderFaktaOmBostedOppdaterer(
            behandlingRepository,
            historikkinnslagRepository,
            bostedsGrunnlagRepository,
            etterlysningRepository,
            prosessTaskTjeneste,
            vilkårsPerioderTilVurderingTjeneste,
            inngangsvilkårVurderingTjeneste
        );

        behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        when(behandling.getFagsakId()).thenReturn(FAGSAK_ID);
        when(behandling.getOriginalBehandlingId()).thenReturn(Optional.empty());
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.AKTIVITETSPENGER);
        when(behandling.getType()).thenReturn(BehandlingType.FØRSTEGANGSSØKNAD);

        var fagsak = mock(Fagsak.class);
        when(fagsak.getSaksnummer()).thenReturn(new Saksnummer("SAK1"));
        when(fagsak.getPeriode()).thenReturn(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        when(behandling.getFagsak()).thenReturn(fagsak);

        when(behandlingRepository.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);

        when(bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(eq(BEHANDLING_ID), any()))
            .thenReturn(Map.of(FOM, UUID.randomUUID()));
        when(etterlysningRepository.hentEtterlysningerSomVenterPåSvar(BEHANDLING_ID)).thenReturn(List.of());
    }

    private void mockPerioderTilVurdering(LocalDate fom, LocalDate tom) {
        var vilkårsTjenesteMock = mock(VilkårsPerioderTilVurderingTjeneste.class);
        when(vilkårsTjenesteMock.utled(any(), any()))
            .thenReturn(new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))));

        @SuppressWarnings("unchecked")
        Instance<VilkårsPerioderTilVurderingTjeneste> behandlingInstans = mock(Instance.class);
        when(behandlingInstans.isResolvable()).thenReturn(true);
        when(behandlingInstans.get()).thenReturn(vilkårsTjenesteMock);

        @SuppressWarnings("unchecked")
        Instance<VilkårsPerioderTilVurderingTjeneste> fagsakInstans = mock(Instance.class);
        when(fagsakInstans.isUnsatisfied()).thenReturn(false);
        when(fagsakInstans.select(any(Class.class), any(Annotation.class))).thenReturn(behandlingInstans);

        when(vilkårsPerioderTilVurderingTjeneste.select(any(Class.class), any(Annotation.class))).thenReturn(fagsakInstans);
    }

    @Test
    void skal_opprette_etterlysning_og_task_nar_soknad_avklaring_endres() {
        var grunnlag = mock(BostedsGrunnlag.class);
        when(grunnlag.hentOppgittOgForeslåttFaktaSomTidslinje())
            .thenReturn(tidslinjeMedTidligereAvklaring(true, null));
        when(bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(BEHANDLING_ID)).thenReturn(Optional.of(grunnlag));

        var dto = dtoMedEnAvklaring(false, false, BostedsvilkårIkkeOppfyltÅrsak.ANNET);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        var etterlysningCaptor = ArgumentCaptor.forClass(Etterlysning.class);
        verify(etterlysningRepository).lagre(etterlysningCaptor.capture());
        assertThat(etterlysningCaptor.getValue().getType()).isEqualTo(EtterlysningType.UTTALELSE_BOSTED);
        assertThat(etterlysningCaptor.getValue().getPeriode().getFomDato()).isEqualTo(FOM);

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTaskType()).isEqualTo(OpprettEtterlysningTask.TASKTYPE);
        assertThat(taskCaptor.getValue().getPropertyValue(OpprettEtterlysningTask.ETTERLYSNING_TYPE))
            .isEqualTo(EtterlysningType.UTTALELSE_BOSTED.getKode());

        verify(prosessTaskTjeneste, times(1)).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_avbryte_eksisterende_og_opprette_ny_nar_avklaring_endres() {
        var grunnlag = mock(BostedsGrunnlag.class);
        when(grunnlag.hentOppgittOgForeslåttFaktaSomTidslinje())
            .thenReturn(tidslinjeMedTidligereAvklaring(true, null));
        when(bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(BEHANDLING_ID)).thenReturn(Optional.of(grunnlag));

        var eksisterende = Etterlysning.opprettForType(
            BEHANDLING_ID,
            UUID.randomUUID(),
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            EtterlysningType.UTTALELSE_BOSTED
        );
        when(etterlysningRepository.hentEtterlysningerSomVenterPåSvar(BEHANDLING_ID)).thenReturn(List.of(eksisterende));

        var dto = dtoMedEnAvklaring(false, false, BostedsvilkårIkkeOppfyltÅrsak.ANNET);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        verify(etterlysningRepository).lagre(anyList());
        assertThat(eksisterende.getStatus()).isEqualTo(EtterlysningStatus.SKAL_AVBRYTES);
        verify(etterlysningRepository).lagre(any(Etterlysning.class));

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste, times(2)).lagre(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues())
            .extracting(ProsessTaskData::getTaskType)
            .containsExactlyInAnyOrder(AvbrytEtterlysningTask.TASKTYPE, OpprettEtterlysningTask.TASKTYPE);
    }

    @Test
    void skal_ikke_opprette_eller_avbryte_nar_avklaring_er_uendret() {
        var grunnlag = mock(BostedsGrunnlag.class);
        when(grunnlag.hentOppgittOgForeslåttFaktaSomTidslinje())
            .thenReturn(tidslinjeMedTidligereAvklaring(false, null));
        when(bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(BEHANDLING_ID)).thenReturn(Optional.of(grunnlag));

        var dto = dtoMedEnAvklaring(false, false, null);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        verify(etterlysningRepository, never()).lagre(any(Etterlysning.class));
        verify(etterlysningRepository, never()).lagre(anyList());
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_fjerne_vilkårsvurdering_og_vilkårresultat_for_periode_ved_lagring_av_forslag() {
        when(bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(BEHANDLING_ID)).thenReturn(Optional.empty());

        var dto = dtoMedEnAvklaring(false, true, BostedsvilkårIkkeOppfyltÅrsak.ANNET);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        var vilkårCaptor = ArgumentCaptor.forClass(VilkårType.class);
        var perioderCaptor = ArgumentCaptor.forClass(List.class);
        var resultatBuiderCaptor = ArgumentCaptor.forClass(VilkårResultatBuilder.class);

        verify(inngangsvilkårVurderingTjeneste).fjernVilkårVurderingOgSettVilkårResultatIkkeVurdertForPeriode(
            eq(BEHANDLING_ID),
            resultatBuiderCaptor.capture(),
            vilkårCaptor.capture(),
            perioderCaptor.capture()
        );
        assertThat(vilkårCaptor.getValue()).isEqualTo(BOSTEDSVILKÅR);
        assertThat(perioderCaptor.getValue()).containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
    }

    @Test
    void skal_ikke_opprette_eller_avbryte_nar_skal_ikke_sende_varsel_selv_om_endret() {
        var grunnlag = mock(BostedsGrunnlag.class);
        when(grunnlag.hentOppgittOgForeslåttFaktaSomTidslinje())
            .thenReturn(tidslinjeMedTidligereAvklaring(true, null));
        when(bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(BEHANDLING_ID)).thenReturn(Optional.of(grunnlag));

        var dto = dtoMedEnAvklaring(false, true, BostedsvilkårIkkeOppfyltÅrsak.ANNET);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        verify(etterlysningRepository, never()).lagre(any(Etterlysning.class));
        verify(etterlysningRepository, never()).lagre(anyList());
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    private static LocalDateTimeline<BostedsfaktaOgAvklaring> tidslinjeMedTidligereAvklaring(boolean bosattITrondheim, BostedsvilkårIkkeOppfyltÅrsak årsak) {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        var avklaring = new BostedsPeriodeAvklaring(
            periode,
            bosattITrondheim,
            årsak,
            "A12345",
            LocalDateTime.now()
        );
        var fakta = new BostedsfaktaOgAvklaring(null, avklaring);
        return new LocalDateTimeline<>(List.of(new LocalDateSegment<>(FOM, TOM, fakta)));
    }

    private static VurderFaktaOmBostedDto dtoMedEnAvklaring(boolean borITrondheimHelePerioden,
                                                             boolean skalIkkeSendeVarsel,
                                                             BostedsvilkårIkkeOppfyltÅrsak årsak) {
        var periode = new Periode(FOM, TOM);
        var vurdering = !borITrondheimHelePerioden ? new BostedVurderingIkkeOppfyltDto(årsak, "begrunnelse") : null;
        var avklaring = new BostedFaktaavklaringPeriodeDto(periode, vurdering, skalIkkeSendeVarsel);
        return new VurderFaktaOmBostedDto(List.of(avklaring), "begrunnelse");
    }
}
