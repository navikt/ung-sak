package no.nav.ung.sak.behandlingslager.ytelse;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.behandlingslager.diff.DiffEntity;
import no.nav.ung.sak.behandlingslager.diff.TraverseEntityGraphFactory;
import no.nav.ung.sak.behandlingslager.diff.TraverseGraph;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsResultat;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;

@Dependent
public class UngdomsytelseGrunnlagRepository {

    private static final Logger log = LoggerFactory.getLogger(UngdomsytelseGrunnlagRepository.class);
    private EntityManager entityManager;

    @Inject
    public UngdomsytelseGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public void lagre(Long behandlingId, UngdomsytelseSatsResultat perioder) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        var aktivtGrunnlag = grunnlagOptional.orElse(new UngdomsytelseGrunnlag());

        var builder = new UngdomsytelseGrunnlagBuilder(aktivtGrunnlag);
        builder.medSatsPerioder(perioder.resultatTidslinje(), perioder.regelInput(), perioder.regelSporing());

        var differ = differ();

        if (builder.erForskjellig(aktivtGrunnlag, differ)) {
            grunnlagOptional.ifPresent(this::deaktiverEksisterende);
            lagre(builder, behandlingId);
        } else {
            log.info("[behandlingId={}] Forkaster lagring nytt resultat da dette er identisk med eksisterende resultat.", behandlingId);
        }
    }

    public void lagre(Long behandlingId, UngdomsytelseUttakPerioder uttakperioder) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        var aktivtGrunnlag = grunnlagOptional.orElse(new UngdomsytelseGrunnlag());

        var builder = new UngdomsytelseGrunnlagBuilder(aktivtGrunnlag);
        builder.medUttakPerioder(uttakperioder);

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
        var query = entityManager.createQuery("SELECT bg FROM UngdomsytelseGrunnlag bg WHERE bg.behandlingId=:id AND bg.aktiv = true",UngdomsytelseGrunnlag.class)
            .setParameter("id", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void lagre(UngdomsytelseGrunnlagBuilder builder, Long behandlingId) {
        var oppdatertGrunnlag = builder.build();
        oppdatertGrunnlag.setBehandlingId(behandlingId);

        entityManager.persist(oppdatertGrunnlag.getSatsPerioder());
        if (oppdatertGrunnlag.getUttakPerioder() != null) {
            entityManager.persist(oppdatertGrunnlag.getUttakPerioder());
        }
        entityManager.persist(oppdatertGrunnlag);
        entityManager.flush();
    }

}
