package no.nav.k9.sak.ytelse.beregning.grunnlag;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.diff.DiffEntity;
import no.nav.k9.sak.behandlingslager.diff.TraverseEntityGraphFactory;
import no.nav.k9.sak.behandlingslager.diff.TraverseGraph;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@Dependent
public class BeregningPerioderGrunnlagRepository {

    private static final Logger log = LoggerFactory.getLogger(BeregningPerioderGrunnlagRepository.class);
    private EntityManager entityManager;
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public BeregningPerioderGrunnlagRepository(EntityManager entityManager, VilkårResultatRepository vilkårResultatRepository) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public void lagre(Long behandlingId, BeregningsgrunnlagPeriode periode) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        var aktivtGrunnlag = grunnlagOptional.orElse(new BeregningsgrunnlagPerioderGrunnlag());

        var builder = new BeregningsgrunnlagPerioderGrunnlagBuilder(aktivtGrunnlag);
        builder.leggTil(periode);

        var differ = differ();

        if (builder.erForskjellig(aktivtGrunnlag, differ)) {
            grunnlagOptional.ifPresent(this::deaktiverEksisterende);

            lagre(builder, behandlingId);
        } else {
            log.info("[behandlingId={}] Forkaster lagring nytt resultat da dette er identisk med eksisterende resultat.", behandlingId);
        }
    }

    private DiffEntity differ() {
        TraverseGraph traverser = TraverseEntityGraphFactory.build();
        return new DiffEntity(traverser);
    }

    private void deaktiverEksisterende(BeregningsgrunnlagPerioderGrunnlag grunnlag) {
        grunnlag.setIkkeAktivt();
        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    public void deaktiver(Long behandlingId, LocalDate skjæringstidspunkt) {
        var aktivtGrunnlag = hentGrunnlag(behandlingId);
        if (aktivtGrunnlag.isPresent()) {
            var grunnlag = aktivtGrunnlag.get();

            var builder = new BeregningsgrunnlagPerioderGrunnlagBuilder(grunnlag);
            builder.deaktiver(skjæringstidspunkt);

            deaktiverEksisterende(grunnlag);

            lagre(builder, behandlingId);
        }
    }

    public Optional<BeregningsgrunnlagPerioderGrunnlag> hentGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT bg " +
                "FROM BeregningsgrunnlagPerioderGrunnlag bg " +
                "WHERE bg.behandlingId=:id " +
                "AND bg.aktiv = true",
            BeregningsgrunnlagPerioderGrunnlag.class)
            .setParameter("id", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void lagre(BeregningsgrunnlagPerioderGrunnlagBuilder builder, Long behandlingId) {
        validerMotVilkårsPerioder(behandlingId, builder);

        var oppdatertGrunnlag = builder.build();
        oppdatertGrunnlag.setBehandlingId(behandlingId);

        entityManager.persist(oppdatertGrunnlag.getHolder());
        entityManager.persist(oppdatertGrunnlag);
        entityManager.flush();
    }

    public void kopier(Long fraBehandlingId, Long tilBehandlingId) {
        var aktivtGrunnlag = hentGrunnlag(fraBehandlingId);
        if (aktivtGrunnlag.isPresent()) {
            var grunnlag = aktivtGrunnlag.get();
            var builder = new BeregningsgrunnlagPerioderGrunnlagBuilder(grunnlag);

            lagre(builder, tilBehandlingId);
        }
    }

    private void validerMotVilkårsPerioder(Long behandlingId, BeregningsgrunnlagPerioderGrunnlagBuilder builder) {
        var vilkår = vilkårResultatRepository.hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR));
        vilkår.ifPresent(builder::validerMotVilkår);
    }
}
