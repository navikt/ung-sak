package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

public class InngangsvilkårSøkersOpplysningspliktTest {

    InngangsvilkårSøkersOpplysningsplikt testObjekt;
    private KompletthetsjekkerProvider kompletthetssjekkerProvider = mock(KompletthetsjekkerProvider.class);
    private Kompletthetsjekker kompletthetssjekker = mock(Kompletthetsjekker.class);
    private DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMed(LocalDate.now());

    @Before
    public void setup() {
        kompletthetssjekkerProvider = mock(KompletthetsjekkerProvider.class);
        testObjekt = new InngangsvilkårSøkersOpplysningsplikt(kompletthetssjekkerProvider);
    }

    @Test
    public void komplett_søknad_skal_medføre_oppfylt() {
        when(kompletthetssjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetssjekker);
        when(kompletthetssjekker.erForsendelsesgrunnlagKomplett(any()))
            .thenReturn(true);
        Behandling behandling = TestScenarioBuilder.builderMedSøknad().lagMocked();

        VilkårData vilkårData = testObjekt.vurderVilkår(lagRef(behandling), periode);

        assertThat(vilkårData).isNotNull();
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.SØKERSOPPLYSNINGSPLIKT);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.OPPFYLT);
        assertThat(vilkårData.getApDefinisjoner()).isEmpty();
    }

    @Test
    public void ikke_komplett_søknad_skal_medføre_manuell_vurdering() {
        when(kompletthetssjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetssjekker);
        when(kompletthetssjekker.erForsendelsesgrunnlagKomplett(any()))
            .thenReturn(false);
        Behandling behandling = TestScenarioBuilder.builderMedSøknad().lagMocked();

        VilkårData vilkårData = testObjekt.vurderVilkår(lagRef(behandling), periode);

        assertThat(vilkårData).isNotNull();
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.SØKERSOPPLYSNINGSPLIKT);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.IKKE_VURDERT);
        assertThat(vilkårData.getApDefinisjoner()).hasSize(1);
        assertThat(vilkårData.getApDefinisjoner()).contains(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU);
    }

    @Test
    public void revurdering_skal_medføre_at_vilkår_er_oppfylt() {
        when(kompletthetssjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetssjekker);
        when(kompletthetssjekker.erForsendelsesgrunnlagKomplett(any()))
            .thenReturn(false);
        Behandling revurdering = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();

        VilkårData vilkårData = testObjekt.vurderVilkår(lagRef(revurdering), periode);

        assertThat(vilkårData).isNotNull();
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.SØKERSOPPLYSNINGSPLIKT);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.OPPFYLT);
        assertThat(vilkårData.getApDefinisjoner()).isEmpty();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

}
