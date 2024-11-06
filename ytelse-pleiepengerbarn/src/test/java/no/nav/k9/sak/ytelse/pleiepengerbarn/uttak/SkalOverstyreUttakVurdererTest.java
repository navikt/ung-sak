package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrUttakRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@CdiDbAwareTest
class SkalOverstyreUttakVurdererTest {

    public static final LocalDate FAGSAKPERIODE_FOM = LocalDate.now();
    public static final LocalDate FAGSAKPERIODE_TOM = LocalDate.now().plusDays(10);
    @Inject
    private EntityManager entityManager;

    private SkalOverstyreUttakVurderer skalOverstyreUttakVurderer;

    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = mock(VilkårsPerioderTilVurderingTjeneste.class);
    private Behandling behandling;
    private OverstyrUttakRepository overstyrUttakRepository;
    @Inject
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker("saksbehandler1");
        behandlingRepository = new BehandlingRepository(entityManager);
        overstyrUttakRepository = new OverstyrUttakRepository(entityManager);
        skalOverstyreUttakVurderer = new SkalOverstyreUttakVurderer(
            overstyrUttakRepository,
            vilkårsPerioderTilVurderingTjeneste,
            behandlingRepository
        );

        var fagsakRepository = new FagsakRepository(entityManager);
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, new AktørId(123L), new Saksnummer("987"), FAGSAKPERIODE_FOM, FAGSAKPERIODE_TOM);
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));

    }


    @Test
    void skal_ikke_overstyre_uttak_dersom_det_ikke_finnes_overstyring() {
        when(vilkårsPerioderTilVurderingTjeneste.utledFraDefinerendeVilkår(ArgumentMatchers.anyLong()))
            .thenReturn(new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(FAGSAKPERIODE_FOM, FAGSAKPERIODE_TOM))));

        var skalOverstyreUttak = skalOverstyreUttakVurderer.skalOverstyreUttak(BehandlingReferanse.fra(behandling));

        assertThat(skalOverstyreUttak).isFalse();
    }

    @Test
    void skal_ikke_overstyre_uttak_dersom_ingen_perioder_er_til_vurdering() {
        when(vilkårsPerioderTilVurderingTjeneste.utledFraDefinerendeVilkår(ArgumentMatchers.anyLong()))
            .thenReturn(new TreeSet<>());


        overstyrUttakRepository.oppdaterOverstyringAvUttak(behandling.getId(), List.of(),
            new LocalDateTimeline<>(FAGSAKPERIODE_FOM, FAGSAKPERIODE_TOM, new OverstyrtUttakPeriode(null, BigDecimal.TEN, Set.of(), "en begrunnelse")));

        var skalOverstyreUttak = skalOverstyreUttakVurderer.skalOverstyreUttak(BehandlingReferanse.fra(behandling));

        assertThat(skalOverstyreUttak).isFalse();
    }

    @Test
    void skal_overstyre_dersom_overlappende_periode_til_vurdering_og_overstyring() {
        when(vilkårsPerioderTilVurderingTjeneste.utledFraDefinerendeVilkår(ArgumentMatchers.anyLong()))
            .thenReturn(new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(FAGSAKPERIODE_FOM, FAGSAKPERIODE_TOM))));


        overstyrUttakRepository.oppdaterOverstyringAvUttak(behandling.getId(), List.of(),
            new LocalDateTimeline<>(FAGSAKPERIODE_FOM, FAGSAKPERIODE_TOM, new OverstyrtUttakPeriode(null, BigDecimal.TEN, Set.of(), "en begrunnelse")));

        var skalOverstyreUttak = skalOverstyreUttakVurderer.skalOverstyreUttak(BehandlingReferanse.fra(behandling));

        assertThat(skalOverstyreUttak).isTrue();
    }

    @Test
    void skal_ikke_overstyre_dersom_ikke_overlappende_periode_til_vurdering_og_overstyring() {
        when(vilkårsPerioderTilVurderingTjeneste.utledFraDefinerendeVilkår(ArgumentMatchers.anyLong()))
            .thenReturn(new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(FAGSAKPERIODE_FOM, FAGSAKPERIODE_FOM.plusDays(1)))));


        overstyrUttakRepository.oppdaterOverstyringAvUttak(behandling.getId(), List.of(),
            new LocalDateTimeline<>(FAGSAKPERIODE_FOM.plusDays(2), FAGSAKPERIODE_TOM, new OverstyrtUttakPeriode(null, BigDecimal.TEN, Set.of(), "en begrunnelse")));

        var skalOverstyreUttak = skalOverstyreUttakVurderer.skalOverstyreUttak(BehandlingReferanse.fra(behandling));

        assertThat(skalOverstyreUttak).isFalse();
    }


    @Test
    void skal_ikke_overstyre_dersom_overlappende_periode_til_vurdering_og_overstyring_og_aksjonspunkt_for_overlappende_saker() {
        when(vilkårsPerioderTilVurderingTjeneste.utledFraDefinerendeVilkår(ArgumentMatchers.anyLong()))
            .thenReturn(new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(FAGSAKPERIODE_FOM, FAGSAKPERIODE_TOM))));

        aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_OVERLAPPENDE_SØSKENSAKER);

        overstyrUttakRepository.oppdaterOverstyringAvUttak(behandling.getId(), List.of(),
            new LocalDateTimeline<>(FAGSAKPERIODE_FOM, FAGSAKPERIODE_TOM, new OverstyrtUttakPeriode(null, BigDecimal.TEN, Set.of(), "en begrunnelse")));

        var skalOverstyreUttak = skalOverstyreUttakVurderer.skalOverstyreUttak(BehandlingReferanse.fra(behandling));

        assertThat(skalOverstyreUttak).isFalse();
    }



}
