package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.diff.DiffEntity;
import no.nav.ung.sak.diff.TraverseEntityGraphFactory;
import no.nav.ung.sak.diff.TraverseGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Dependent
public class BesteBeregningGrunnlagRepository {

    private static final Logger log = LoggerFactory.getLogger(BesteBeregningGrunnlagRepository.class);
    private EntityManager entityManager;

    @Inject
    public BesteBeregningGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public void lagre(Long behandlingId, BesteBeregningResultat resultat) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        var aktivtGrunnlag = grunnlagOptional.orElse(new BesteBeregningGrunnlag());

        var builder = new BesteBeregningGrunnlagBuilder(aktivtGrunnlag);
        builder.medResultat(resultat);

        var differ = differ();

        if (builder.erForskjellig(aktivtGrunnlag, differ)) {
            grunnlagOptional.ifPresent(this::deaktiverEksisterende);
            lagre(builder, behandlingId);
        } else {
            log.info("[behandlingId={}] Forkaster lagring nytt resultat da dette er identisk med eksisterende resultat.", behandlingId);
        }
    }

    public LocalDateTimeline<BesteBeregningGrunnlag> hentBesteBeregningSomTidslinje(long behandlingId) {
        var besteberegningGrunnlag = hentGrunnlag(behandlingId).orElseThrow(
            () -> new IllegalStateException("Fant ikke beste beregning grunnlag for behandlingId=" + behandlingId));

        return new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(besteberegningGrunnlag.getVirkningsdato(), null, besteberegningGrunnlag)));
    }

    /**
     * Deaktiverer grunnlag for en gitt behandlingId hvis det finnes et aktivt grunnlag.
     * Logger informasjon om deaktivering.
     *
     * @param behandlingId Id til behandlingen hvor grunnlaget skal deaktiveres
     */
    public void deaktiverGrunnlag(Long behandlingId) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        if (grunnlagOptional.isPresent()) {
            log.info("Deaktiverer eksisterende beste beregning grunnlag for behandlingId={}", behandlingId);
            deaktiverEksisterende(grunnlagOptional.get());
        }
    }

    public Optional<BesteBeregningGrunnlag> hentGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
                "SELECT gr FROM BesteBeregningGrunnlag gr WHERE gr.behandlingId = :id AND gr.aktiv = true",
                BesteBeregningGrunnlag.class)
            .setParameter("id", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private DiffEntity differ() {
        TraverseGraph traverser = TraverseEntityGraphFactory.build();
        return new DiffEntity(traverser);
    }

    private void deaktiverEksisterende(BesteBeregningGrunnlag grunnlag) {
        grunnlag.setIkkeAktivt();
        entityManager.persist(grunnlag);
        entityManager.flush();
    }


    private void lagre(BesteBeregningGrunnlagBuilder builder, Long behandlingId) {
        var oppdatertGrunnlag = builder.build();
        oppdatertGrunnlag.setBehandlingId(behandlingId);

        entityManager.persist(oppdatertGrunnlag);
        entityManager.flush();
    }

}
