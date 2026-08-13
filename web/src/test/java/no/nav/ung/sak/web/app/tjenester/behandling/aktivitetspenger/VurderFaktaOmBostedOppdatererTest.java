package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.bosatt.*;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedFaktaavklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedVurderingIkkeOppfyltDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderFaktaOmBostedDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderFaktaOmBostedOppdatererTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);
    private static final String SAKSBEHANDLER = "saksbehandler1";
    private static final String BEGRUNNELSE_IKKE_VARSEL = "Begrunnelse for hvorfor det ikke varsles";

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    @Inject
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;

    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private VurderFaktaOmBostedOppdaterer oppdaterer;
    private Behandling behandling;

    @BeforeAll
    static void beforeAll() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(SAKSBEHANDLER);
    }

    @AfterAll
    static void afterAll() {
        SubjectHandlerUtils.reset();
    }

    @BeforeEach
    void setUp() {
        var behandlingRepository = new BehandlingRepository(entityManager);
        var historikkinnslagRepository = new HistorikkinnslagRepository(entityManager);
        bostedsGrunnlagRepository = new BostedsGrunnlagRepository(entityManager);
        etterlysningRepository = new EtterlysningRepository(entityManager);
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);

        oppdaterer = new VurderFaktaOmBostedOppdaterer(
            behandlingRepository,
            historikkinnslagRepository,
            bostedsGrunnlagRepository,
            etterlysningRepository,
            prosessTaskTjeneste,
            vilkårsPerioderTilVurderingTjenester,
            inngangsvilkårVurderingTjeneste
        );

        behandling = opprettBehandlingMedVilkårOgPeriode();
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
    }

    @Test
    void skal_opprette_etterlysning_og_task_nar_soknad_avklaring_endres() {
        var dto = dtoMedEnAvklaring(BostedsvilkårIkkeOppfyltÅrsak.ANNET, true);

        oppdater(dto);

        var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger).hasSize(1);
        assertThat(etterlysninger.getFirst().getType()).isEqualTo(EtterlysningType.UTTALELSE_BOSTED);
        assertThat(etterlysninger.getFirst().getStatus()).isEqualTo(EtterlysningStatus.OPPRETTET);
        assertThat(etterlysninger.getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        assertThat(etterlysninger.getFirst().getGrunnlagsreferanse())
            .isEqualTo(hentSorterteAvklaringer().getFirst().getReferanse());

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste, times(1)).lagre(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTaskType()).isEqualTo(OpprettEtterlysningTask.TASKTYPE);
        assertThat(taskCaptor.getValue().getPropertyValue(OpprettEtterlysningTask.ETTERLYSNING_TYPE))
            .isEqualTo(EtterlysningType.UTTALELSE_BOSTED.getKode());
    }

    @Test
    void skal_avbryte_eksisterende_og_opprette_ny_nar_avklaring_endres() {
        var eksisterende = etterlysningRepository.lagre(Etterlysning.opprettForType(
            behandling.getId(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            EtterlysningType.UTTALELSE_BOSTED
        ));

        var dto = dtoMedEnAvklaring(BostedsvilkårIkkeOppfyltÅrsak.ANNET, true);

        oppdater(dto);

        var skalAvbrytes = etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId());
        assertThat(skalAvbrytes).extracting(Etterlysning::getEksternReferanse)
            .containsExactly(eksisterende.getEksternReferanse());

        var nye = etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BOSTED);
        assertThat(nye).hasSize(1);
        assertThat(nye.getFirst().getEksternReferanse()).isNotEqualTo(eksisterende.getEksternReferanse());

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste, times(2)).lagre(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues())
            .extracting(ProsessTaskData::getTaskType)
            .containsExactlyInAnyOrder(AvbrytEtterlysningTask.TASKTYPE, OpprettEtterlysningTask.TASKTYPE);
    }

    @Test
    void skal_ikke_opprette_eller_avbryte_nar_avklaring_er_uendret() {
        lagreTidligereAvklaring(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM);

        var dto = dtoMedEnAvklaring(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true);

        oppdater(dto);

        assertThat(etterlysningRepository.hentEtterlysninger(behandling.getId())).isEmpty();
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_fjerne_vilkårsvurdering_og_vilkårresultat_for_periode_ved_lagring_av_forslag() {
        var dto = dtoMedEnAvklaring(BostedsvilkårIkkeOppfyltÅrsak.ANNET, false);
        var param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        oppdaterer.oppdater(dto, param);

        var bostedsvilkår = param.getVilkårResultatBuilder().build().getVilkår(VilkårType.BOSTEDSVILKÅR).orElseThrow();
        assertThat(bostedsvilkår.getPerioder()).hasSize(1);
        assertThat(bostedsvilkår.getPerioder().getFirst().getPeriode())
            .isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        assertThat(bostedsvilkår.getPerioder().getFirst().getUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_ikke_opprette_eller_avbryte_nar_skal_ikke_sende_varsel_selv_om_endret() {
        var dto = dtoMedEnAvklaring(BostedsvilkårIkkeOppfyltÅrsak.ANNET, false);

        oppdater(dto);

        assertThat(etterlysningRepository.hentEtterlysninger(behandling.getId())).isEmpty();
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_lagre_avklaring_uten_varsel_og_lukke_apen_periode_ved_ny_oppdatering() {
        oppdater(dtoUtenVarsel(new ÅpenPeriode(FOM, TOM), BostedsvilkårIkkeOppfyltÅrsak.ANNET));

        var førsteLagring = hentSorterteAvklaringer();
        assertThat(førsteLagring).hasSize(1);
        assertThat(førsteLagring.getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        assertThat(førsteLagring.getFirst().skalSendeVarsel()).isFalse();
        assertThat(førsteLagring.getFirst().getBegrunnelseIkkeVarsel()).isEqualTo(BEGRUNNELSE_IKKE_VARSEL);
        assertThat(førsteLagring.getFirst().getVurdertAv()).isEqualTo(SAKSBEHANDLER);

        // Åpen periode skal lukkes med siste dato blant periodene til vurdering
        oppdater(dtoUtenVarsel(new ÅpenPeriode(FOM.plusDays(1), null), BostedsvilkårIkkeOppfyltÅrsak.ANNET));

        var andreLagring = hentSorterteAvklaringer();
        assertThat(andreLagring).hasSize(2);
        assertThat(andreLagring.getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, FOM));
        assertThat(andreLagring.getLast().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM.plusDays(1), TOM));
        assertThat(andreLagring.getLast().skalSendeVarsel()).isFalse();
        assertThat(andreLagring.getLast().getBegrunnelseIkkeVarsel()).isEqualTo(BEGRUNNELSE_IKKE_VARSEL);

        assertThat(etterlysningRepository.hentEtterlysninger(behandling.getId())).isEmpty();
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    private void oppdater(VurderFaktaOmBostedDto dto) {
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode() {
        var behandling = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.AKTIVITETSPENGER)
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var søktStartdato = new SøktStartdato(FOM, new JournalpostId("jp-søknad-1"));
        var startdatoRepository = new StartdatoRepository(entityManager);
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato)));

        new ProsessTriggereRepository(entityManager).leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));
        return behandling;
    }

    private void lagreTidligereAvklaring(BostedsvilkårIkkeOppfyltÅrsak årsak) {
        bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(new BostedsPeriodeAvklaring(
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            årsak,
            "Begrunnelse for hvilke relevante fakta som er lagt til grunn",
            true,
            BostedsvilkårIkkeOppfyltÅrsak.ANNET.equals(årsak) ? "Fritekstbegrunnelse til bruker" : null,
            null,
            SAKSBEHANDLER,
            LocalDateTime.now(),
            Avklaringtype.OPPHØR
        )));
    }

    private List<BostedsPeriodeAvklaring> hentSorterteAvklaringer() {
        return bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId())
            .orElseThrow()
            .getForeslått()
            .getPeriodeAvklaringer()
            .stream()
            .sorted(Comparator.comparing(a -> a.getPeriode().getFomDato()))
            .toList();
    }

    private static VurderFaktaOmBostedDto dtoUtenVarsel(ÅpenPeriode periode, BostedsvilkårIkkeOppfyltÅrsak årsak) {
        var vurdering = new BostedVurderingIkkeOppfyltDto(
            årsak,
            "begrunnelse",
            (årsak == BostedsvilkårIkkeOppfyltÅrsak.ANNET) ? "Fritekstbegrunnelse til bruker" : null,
            BEGRUNNELSE_IKKE_VARSEL
        );
        var avklaring = new BostedFaktaavklaringPeriodeDto(periode, vurdering, true);
        return new VurderFaktaOmBostedDto(List.of(avklaring), "begrunnelse");
    }

    private static VurderFaktaOmBostedDto dtoMedEnAvklaring(BostedsvilkårIkkeOppfyltÅrsak årsak, boolean skalSendeVarsel) {
        var vurdering = new BostedVurderingIkkeOppfyltDto(
            årsak,
            "begrunnelse",
            (årsak == BostedsvilkårIkkeOppfyltÅrsak.ANNET) ? "Fritekstbegrunnelse til bruker" : null,
            skalSendeVarsel ? null : BEGRUNNELSE_IKKE_VARSEL
        );
        var avklaring = new BostedFaktaavklaringPeriodeDto(new ÅpenPeriode(FOM, TOM), vurdering, !skalSendeVarsel);
        return new VurderFaktaOmBostedDto(List.of(avklaring), "begrunnelse");
    }
}
