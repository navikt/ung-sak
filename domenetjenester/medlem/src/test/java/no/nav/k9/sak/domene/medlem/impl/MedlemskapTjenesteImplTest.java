package no.nav.k9.sak.domene.medlem.impl;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.medlem.MedlemTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class MedlemskapTjenesteImplTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider provider ;
    private FagsakRepository fagsakRepository ;
    private BehandlingRepository behandlingRepository ;
    private VilkårResultatRepository vilkårResultatRepository ;

    @Inject
    private MedlemTjeneste tjeneste;

    @BeforeEach
    public void setup()
    {
        provider = new BehandlingRepositoryProvider(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = provider.getBehandlingRepository();
        vilkårResultatRepository = provider.getVilkårResultatRepository();
    }

    @Test
    public void skal_returnere_empty_når_vilkåret_er_overstyrt_til_godkjent() {
        // Arrange
        var behandling = lagBehandling();
        LocalDate now = LocalDate.now();

        VilkårResultatBuilder vilkår = Vilkårene.builder();
        final var vilkårBuilder = vilkår.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(now, Tid.TIDENES_ENDE).medUtfall(Utfall.IKKE_OPPFYLT));

        Vilkårene vilkårene = vilkår.build();
        vilkårResultatRepository.lagre(behandling.getId(), vilkårene);

        // Act
        Optional<LocalDate> localDate = tjeneste.hentOpphørsdatoHvisEksisterer(behandling.getId());

        // Assert
        assertThat(localDate).isEmpty();
    }

    private Behandling lagBehandling() {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, AktørId.dummy());
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        return behandling;
    }
}
