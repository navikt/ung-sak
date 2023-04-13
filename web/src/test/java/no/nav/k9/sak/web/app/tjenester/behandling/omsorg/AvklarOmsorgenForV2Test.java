package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenFor;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForGrunnlag;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForPeriode;
import no.nav.k9.sak.kontrakt.omsorg.AvklarOmsorgenForDto;
import no.nav.k9.sak.kontrakt.omsorg.OmsorgenForOppdateringDto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.FosterbarnRepository;

@CdiDbAwareTest
class AvklarOmsorgenForV2Test {

    private AvklarOmsorgenForV2 avklarOmsorgenForV2;
    @Inject
    private HistorikkTjenesteAdapter historikkAdapter;
    @Inject
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;
    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenesteInstanceBean;
    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjenesteBean;
    @Inject
    private FosterbarnRepository fosterbarnRepository;
    @Inject
    private PersoninfoAdapter personinfoAdapter;
    @Inject
    private EntityManager entityManager;

    private Behandling behandling;
    private final Periode søknadsperiode = new Periode(LocalDate.now().minusWeeks(2), LocalDate.now());

    @BeforeEach
    void setup() {
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad().medSøknadDato(LocalDate.now());
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR_V2, BehandlingStegType.VURDER_OMSORG_FOR);
        behandling = scenario.lagre(entityManager);

        var perioderTilVurderingTjenesteMock = spy(perioderTilVurderingTjenesteBean);
        var vilkårsPerioderTilVurderingTjenesteInstanceMock = spy(vilkårsPerioderTilVurderingTjenesteInstanceBean);
        when(vilkårsPerioderTilVurderingTjenesteInstanceMock.select(eq(VilkårsPerioderTilVurderingTjeneste.class), any(Annotation[].class))).thenReturn(vilkårsPerioderTilVurderingTjenesteInstanceMock);
        when(vilkårsPerioderTilVurderingTjenesteInstanceMock.get()).thenReturn(perioderTilVurderingTjenesteMock);
        when(perioderTilVurderingTjenesteMock.utled(behandling.getId(), VilkårType.OMSORGEN_FOR)).thenReturn(new TreeSet<>(Set.of(DatoIntervallEntitet.fra(søknadsperiode))));

        avklarOmsorgenForV2 = new AvklarOmsorgenForV2(historikkAdapter, omsorgenForGrunnlagRepository, vilkårsPerioderTilVurderingTjenesteInstanceMock, fosterbarnRepository, personinfoAdapter);
    }

    @Test
    void skalLagreNyttGrunnlagOgArveFraEksisterende() {
        Periode periode1 = new Periode(søknadsperiode.getFom(), søknadsperiode.getFom().plusWeeks(1));
        Periode periode2 = new Periode(søknadsperiode.getFom().plusWeeks(1).plusDays(1), søknadsperiode.getTom());

        setBruker("enball");
        AvklarOmsorgenForDto dto1 = new AvklarOmsorgenForDto("", List.of(new OmsorgenForOppdateringDto(periode1, "fordi", Resultat.OPPFYLT)), List.of());
        oppdater(dto1);

        setBruker("toball");
        AvklarOmsorgenForDto dto2 = new AvklarOmsorgenForDto("", List.of(new OmsorgenForOppdateringDto(periode2, "derfor", Resultat.OPPFYLT)), List.of());
        oppdater(dto2);

        OmsorgenForGrunnlag grunnlag = omsorgenForGrunnlagRepository.hent(behandling.getId());
        OmsorgenFor omsorgenFor = grunnlag.getOmsorgenFor();
        assertThat(omsorgenFor).isNotNull();
        assertThat(omsorgenFor.getPerioder()).hasSize(2);
        OmsorgenForPeriode omsorgenForPeriode1 = omsorgenFor.getPerioder().stream().filter(omsorgenForPeriode -> omsorgenForPeriode.getPeriode().tilPeriode().equals(periode1)).findFirst().orElseThrow();
        assertThat(omsorgenForPeriode1.getBegrunnelse()).isEqualTo("fordi");
        assertThat(omsorgenForPeriode1.getVurdertAv()).isEqualTo("enball");
        assertThat(omsorgenForPeriode1.getResultat()).isEqualTo(Resultat.OPPFYLT);
        OmsorgenForPeriode omsorgenForPeriode2 = omsorgenFor.getPerioder().stream().filter(omsorgenForPeriode -> omsorgenForPeriode.getPeriode().tilPeriode().equals(periode2)).findFirst().orElseThrow();
        assertThat(omsorgenForPeriode2.getBegrunnelse()).isEqualTo("derfor");
        assertThat(omsorgenForPeriode2.getVurdertAv()).isEqualTo("toball");
        assertThat(omsorgenForPeriode2.getResultat()).isEqualTo(Resultat.OPPFYLT);
        assertThat(omsorgenForPeriode2.getVurdertTidspunkt()).isAfter(omsorgenForPeriode1.getVurdertTidspunkt());
    }

    private void oppdater(AvklarOmsorgenForDto dto) {
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        OppdateringResultat resultat = avklarOmsorgenForV2.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        assertThat(resultat).isNotNull();
    }

    private static void setBruker(String brukerId) {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(brukerId);
    }
}
