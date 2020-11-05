package no.nav.k9.sak.ytelse.unntaksbehandling.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class ManuellVilkårsvurderingGrunnlagRepositoryTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private final ManuellVilkårsvurderingGrunnlagRepository manuellVilkårsvurderingGrunnlagRepository = new ManuellVilkårsvurderingGrunnlagRepository(repoRule.getEntityManager());
    private Behandling behandling;

    private final BasicBehandlingBuilder behandlingBuilder = new BasicBehandlingBuilder(repoRule.getEntityManager());

    @Before
    public void setup() {
        behandling = opprettBehandling();
    }

    private Behandling opprettBehandling() {
        return behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    public void lagre_fritekst() {
        // Arrange
        VilkårsvurderingFritekst fritekstEntitet = new VilkårsvurderingFritekst("dette er friteksten");

        // Act
        manuellVilkårsvurderingGrunnlagRepository.lagreOgFlushFritekst(behandling.getId(), fritekstEntitet);

        // Assert
        Optional<ManuellVilkårsvurderingGrunnlag> grunnlag = manuellVilkårsvurderingGrunnlagRepository.hentGrunnlag(behandling.getId());
        assertThat(grunnlag)
            .hasValueSatisfying(gr ->
                assertThat(gr.getFritekstEntitet().getFritekst()).isEqualTo("dette er friteksten")
            );
    }

    @Test
    public void oppdatere_fritekst() {
        // Arrange
        VilkårsvurderingFritekst fritekst1 = new VilkårsvurderingFritekst("dette er friteksten");
        manuellVilkårsvurderingGrunnlagRepository.lagreOgFlushFritekst(behandling.getId(), fritekst1);
        VilkårsvurderingFritekst fritekst2 = new VilkårsvurderingFritekst("dette er den nye friteksten");

        // Act
        manuellVilkårsvurderingGrunnlagRepository.lagreOgFlushFritekst(behandling.getId(), fritekst2);

        // Assert
        Optional<ManuellVilkårsvurderingGrunnlag> grunnlag = manuellVilkårsvurderingGrunnlagRepository.hentGrunnlag(behandling.getId());
        assertThat(grunnlag).describedAs("ho ho")
            .hasValueSatisfying(gr ->
                assertThat(gr.getFritekstEntitet().getFritekst()).isEqualTo("dette er den nye friteksten")
            );


    }

}
