package no.nav.k9.sak.inngangsvilkår.regelmodell.adapter;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.k9.kodeverk.vilkår.Utfall.IKKE_OPPFYLT;
import static no.nav.k9.kodeverk.vilkår.Utfall.IKKE_VURDERT;
import static no.nav.k9.kodeverk.vilkår.Utfall.OPPFYLT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.InngangsvilkårTjeneste;
import no.nav.k9.sak.inngangsvilkår.RegelOrkestrerer;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.exception.TekniskException;

public class RegelOrkestrererImplTest {

    private static final FagsakYtelseType YTELSE_TYPE = TestScenarioBuilder.DEFAULT_TEST_YTELSE;

    private RegelOrkestrerer orkestrerer;

    private InngangsvilkårTjeneste inngangsvilkårTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void oppsett() {
        inngangsvilkårTjeneste = Mockito.mock(InngangsvilkårTjeneste.class);
    }

    @Test
    public void skal_kalle_regeltjeneste_for_medlemskapvilkåret_og_oppdatere_vilkårresultat() {
        // Arrange
        VilkårType vilkårType = VilkårType.MEDLEMSKAPSVILKÅRET;
        DatoIntervallEntitet intervall = DatoIntervallEntitet.fraOgMed(LocalDate.now());
        VilkårData vilkårData = new VilkårData(intervall, vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, YTELSE_TYPE)).thenReturn((b, periode) -> vilkårData);
        Behandling behandling = byggBehandlingMedVilkårresultat(vilkårType);

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), BehandlingReferanse.fra(behandling), List.of(intervall));

        // Assert
        assertThat(regelResultat.getVilkårene().getVilkårene()).hasSize(1);
        assertThat(regelResultat.getVilkårene().getVilkårene().iterator().next().getVilkårType())
            .isEqualTo(vilkårType);
    }

    @Test
    public void skal_ikke_returnere_aksjonspunkter_fra_regelmotor_dersom_allerede_overstyrt() {
        // Arrange
        Behandling behandling = lagBehandling();

        VilkårType vilkårType = VilkårType.MEDLEMSKAPSVILKÅRET;
        DatoIntervallEntitet intervall = DatoIntervallEntitet.fraOgMed(LocalDate.now());
        final var resultatBuilder = Vilkårene.builder();
        final var vilkårBuilder = resultatBuilder.hentBuilderFor(vilkårType);
        resultatBuilder.leggTil(vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(intervall.getFomDato(), intervall.getTomDato()).medUtfallOverstyrt(OPPFYLT)));
        repositoryProvider.getVilkårResultatRepository().lagre(behandling.getId(), resultatBuilder.build());
        VilkårData vilkårData = new VilkårData(intervall, vilkårType, OPPFYLT, List.of(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD));
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, YTELSE_TYPE)).thenReturn((b, periode) -> vilkårData);

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), BehandlingReferanse.fra(behandling), List.of(intervall));

        // Assert
        assertThat(regelResultat.getAksjonspunktDefinisjoner()).isEmpty();
    }

    @Test
    public void skal_returnere_aksjonspunkter_fra_regelmotor_dersom_allerede_manuelt_vurdert() {
        // Arrange
        Behandling behandling = lagBehandling();
        VilkårType vilkårType = VilkårType.OPPTJENINGSVILKÅRET;
        DatoIntervallEntitet intervall = DatoIntervallEntitet.fraOgMed(LocalDate.now());
        final var resultatBuilder = Vilkårene.builder();
        final var vilkårBuilder = resultatBuilder.hentBuilderFor(vilkårType);
        resultatBuilder.leggTil(vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(intervall.getFomDato(), intervall.getTomDato())
            .medUtfallManuell(OPPFYLT)));
        repositoryProvider.getVilkårResultatRepository().lagre(behandling.getId(), resultatBuilder.build());

        VilkårData vilkårData = new VilkårData(intervall, vilkårType, OPPFYLT, List.of(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD));
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, YTELSE_TYPE)).thenReturn((b, periode) -> vilkårData);

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), BehandlingReferanse.fra(behandling), List.of(intervall));

        // Assert
        assertThat(regelResultat.getAksjonspunktDefinisjoner()).containsExactly(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
    }

    @Test
    public void skal_sammenstille_individuelle_vilkårsutfall_til_ett_samlet_vilkårresultat() {
        orkestrerer = new RegelOrkestrerer(inngangsvilkårTjeneste, null);
        // Enkelt vilkårutfall
        assertThat(orkestrerer.utledInngangsvilkårUtfall(Set.of(IKKE_OPPFYLT))).isEqualTo(VilkårResultatType.AVSLÅTT);
        assertThat(orkestrerer.utledInngangsvilkårUtfall(Set.of(IKKE_VURDERT))).isEqualTo(VilkårResultatType.IKKE_FASTSATT);
        assertThat(orkestrerer.utledInngangsvilkårUtfall(Set.of(OPPFYLT))).isEqualTo(VilkårResultatType.INNVILGET);

        // Sammensatt vilkårutfall
        assertThat(orkestrerer.utledInngangsvilkårUtfall(asList(IKKE_OPPFYLT, IKKE_VURDERT))).isEqualTo(VilkårResultatType.AVSLÅTT);
        assertThat(orkestrerer.utledInngangsvilkårUtfall(asList(IKKE_OPPFYLT, OPPFYLT))).isEqualTo(VilkårResultatType.AVSLÅTT);

        assertThat(orkestrerer.utledInngangsvilkårUtfall(asList(IKKE_VURDERT, OPPFYLT))).isEqualTo(VilkårResultatType.IKKE_FASTSATT);
    }

    @Test
    public void skal_kaste_feil_dersom_vilkårsresultat_ikke_kan_utledes() {
        orkestrerer = new RegelOrkestrerer(inngangsvilkårTjeneste, null);
        Assert.assertThrows(TekniskException.class, () -> {
            orkestrerer.utledInngangsvilkårUtfall(emptyList());
        });
    }

    private Behandling byggBehandlingMedVilkårresultat(VilkårType vilkårType) {
        Behandling behandling = lagBehandling();
        final var resultatBuilder = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fraOgMed(LocalDate.now())), vilkårType);
        repositoryProvider.getVilkårResultatRepository().lagre(behandling.getId(), resultatBuilder.build());
        return behandling;
    }

    private Behandling lagBehandling() {
        final var testScenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        repositoryProvider = testScenarioBuilder.mockBehandlingRepositoryProvider();
        orkestrerer = new RegelOrkestrerer(inngangsvilkårTjeneste, repositoryProvider.getVilkårResultatRepository());
        return testScenarioBuilder.lagMocked();
    }

}
