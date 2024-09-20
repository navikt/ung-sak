package no.nav.k9.sak.ytelse.ung.beregning;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.behandlingslager.diff.DiffEntity;
import no.nav.k9.sak.behandlingslager.diff.TraverseEntityGraphFactory;
import no.nav.k9.sak.behandlingslager.diff.TraverseGraph;

@Dependent
public class UngdomsytelseGrunnlagRepository {

    private static final Logger log = LoggerFactory.getLogger(UngdomsytelseGrunnlagRepository.class);
    private EntityManager entityManager;

    @Inject
    public UngdomsytelseGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public void lagre(Long behandlingId, UngdomsytelseSatsPerioder perioder) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        var aktivtGrunnlag = grunnlagOptional.orElse(new UngdomsytelseGrunnlag());

        var builder = new UngdomsytelseGrunnlagBuilder(aktivtGrunnlag);
        builder.leggTilPerioder(perioder);

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

    private void deaktiverEksisterende(UngdomsytelseGrunnlag grunnlag) {
        grunnlag.setIkkeAktivt();
        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    public Optional<UngdomsytelseGrunnlag> hentGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
                "SELECT bg " +
                    "FROM UngdomsytelseGrunnlag bg " +
                    "WHERE bg.behandlingId=:id " +
                    "AND bg.aktiv = true",
                UngdomsytelseGrunnlag.class)
            .setParameter("id", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void lagre(UngdomsytelseGrunnlagBuilder builder, Long behandlingId) {
        var oppdatertGrunnlag = builder.build();
        oppdatertGrunnlag.setBehandlingId(behandlingId);

        if (oppdatertGrunnlag.getSatsPerioder() != null) {
            entityManager.persist(oppdatertGrunnlag.getSatsPerioder());
        }

        entityManager.persist(oppdatertGrunnlag);
        entityManager.flush();
    }

}
