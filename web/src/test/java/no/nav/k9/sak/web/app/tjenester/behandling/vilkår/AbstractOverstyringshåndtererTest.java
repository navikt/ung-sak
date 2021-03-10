package no.nav.k9.sak.web.app.tjenester.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.medlem.OverstyringMedlemskapsvilkåretDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktApplikasjonTjeneste;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class AbstractOverstyringshåndtererTest {

    private static final String IKKE_OK = "ikke ok likevel";

    @Inject
    public EntityManager entityManager;

    @Inject
    private AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjeneste;

    private BehandlingRepositoryProvider repositoryProvider ;
    private AksjonspunktTestSupport aksjonspunktRepository ;

    @BeforeEach
    public void setup(){
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        aksjonspunktRepository = new AksjonspunktTestSupport();
    }

    @Test
    public void skal_reaktivere_inaktivt_aksjonspunkt() throws Exception {
        Behandling behandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
        final var vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fraOgMed(LocalDate.now())), VilkårType.MEDLEMSKAPSVILKÅRET)
            .build();
        repositoryProvider.getVilkårResultatRepository().lagre(behandling.getId(), vilkårene);
        Aksjonspunkt ap = aksjonspunktRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET);
        aksjonspunktRepository.setTilUtført(ap, "OK");

        var dto = new OverstyringMedlemskapsvilkåretDto(new Periode(LocalDate.now(), Tid.TIDENES_ENDE),     false, IKKE_OK, Avslagsårsak.MANGLENDE_DOKUMENTASJON.getKode());

        aksjonspunktApplikasjonTjeneste.overstyrAksjonspunkter(Set.of(dto), behandling.getId());

        assertThat(behandling.getAksjonspunktFor(AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET).getBegrunnelse()).isEqualTo(IKKE_OK);
    }

}
