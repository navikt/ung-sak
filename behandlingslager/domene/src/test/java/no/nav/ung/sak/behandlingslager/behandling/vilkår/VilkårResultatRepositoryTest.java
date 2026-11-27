package no.nav.ung.sak.behandlingslager.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.diff.DiffResult;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VilkårResultatRepositoryTest {

    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);
    private static final LocalDate TOM = LocalDate.of(2025, 8, 31);

    @Inject
    private EntityManager entityManager;
    private VilkårResultatRepository repository;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        repository = new VilkårResultatRepository(entityManager);
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAK1"), LocalDate.now(), null);
        new FagsakRepository(entityManager).opprettNy(fagsak);
        behandling = new BasicBehandlingBuilder(entityManager).opprettNyBehandling(fagsak, BehandlingType.REVURDERING, BehandlingStatus.UTREDES);
    }

    @Test
    void diff_på_utfall_ser_bort_fra_begrunnelse() {
        var oppfylt = lagre(Utfall.OPPFYLT, "første vurdering");
        var nyBegrunnelse = lagre(Utfall.OPPFYLT, "andre vurdering");
        var ikkeOppfylt = lagre(Utfall.IKKE_OPPFYLT, "andre vurdering");

        assertThat(diffUtfall(oppfylt, nyBegrunnelse).isEmpty()).isTrue();
        assertThat(diffUtfall(nyBegrunnelse, ikkeOppfylt).isEmpty()).isFalse();
    }

    private Long lagre(Utfall utfall, String begrunnelse) {
        var builder = Vilkårene.builder();
        var vilkår = builder.hentBuilderFor(VilkårType.UNGDOMSPROGRAMVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder().medPeriode(FOM, TOM).medUtfall(utfall).medBegrunnelse(begrunnelse));
        repository.lagre(behandling.getId(), builder.leggTil(vilkår).build());
        return (Long) repository.finnAktivGrunnlagId(behandling.getId()).getGrunnlagRef();
    }

    private DiffResult diffUtfall(Long id1, Long id2) {
        assertThat(id1).isNotEqualTo(id2);
        return repository.diffUtfall(EndringsresultatDiff.medDiff(VilkårsResultat.class, id1, id2));
    }
}
