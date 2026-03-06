package no.nav.ung.ytelse.aktivitetspenger.beregning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.diff.DiffEntity;
import no.nav.ung.sak.diff.TraverseEntityGraphFactory;
import no.nav.ung.sak.diff.TraverseGraph;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Dependent
public class AktivitetspengerBeregningsgrunnlagRepository {

    private static final Logger log = LoggerFactory.getLogger(AktivitetspengerBeregningsgrunnlagRepository.class);
    private EntityManager entityManager;

    @Inject
    public AktivitetspengerBeregningsgrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }


    public void lagreBeregningsgrunnlag(Long behandlingId, Beregningsgrunnlag beregningsgrunnlag) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        var aktivtGrunnlag = grunnlagOptional.orElse(new AktivitetspengerBeregningsgrunnlag());

        var builder = new AktivitetspengerBeregningsgrunnlagBuilder(aktivtGrunnlag);
        builder.medBeregningsgrunnlag(beregningsgrunnlag);

        var differ = differ();

        if (builder.erForskjellig(aktivtGrunnlag, differ)) {
            grunnlagOptional.ifPresent(this::deaktiverEksisterende);
            lagre(builder, behandlingId);
        } else {
            log.info("[behandlingId={}] Forkaster lagring nytt beregningsgrunnlag da dette er identisk med eksisterende resultat.", behandlingId);
        }
    }

    public void deaktiverGrunnlag(Long behandlingId) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        if (grunnlagOptional.isPresent()) {
            log.info("Deaktiverer eksisterende aktivitetspenger beregningsgrunnlag for behandlingId={}", behandlingId);
            deaktiverEksisterende(grunnlagOptional.get());
        }
    }

    public Optional<AktivitetspengerBeregningsgrunnlag> hentGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
                "SELECT bg FROM AktivitetspengerBeregningsgrunnlag bg WHERE bg.behandlingId=:id AND bg.aktiv = true",
                AktivitetspengerBeregningsgrunnlag.class)
            .setParameter("id", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public LocalDateTimeline<Beregningsgrunnlag> hentBesteBeregningSomTidslinje(long behandlingId) {
        var grunnlag = hentGrunnlag(behandlingId).orElseThrow(
            () -> new IllegalStateException("Fant ikke aktivitetspenger beregningsgrunnlag for behandlingId=" + behandlingId));
        var besteberegning = grunnlag.getBeregningsgrunnlag();
        if (besteberegning == null) {
            throw new IllegalStateException("Fant ikke besteberegning på aktivitetspenger beregningsgrunnlag for behandlingId=" + behandlingId);
        }
        return new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(besteberegning.getVirkningsdato(), null, besteberegning)));
    }

    private DiffEntity differ() {
        TraverseGraph traverser = TraverseEntityGraphFactory.build();
        return new DiffEntity(traverser);
    }

    private void deaktiverEksisterende(AktivitetspengerBeregningsgrunnlag grunnlag) {
        grunnlag.setIkkeAktivt();
        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    private void lagre(AktivitetspengerBeregningsgrunnlagBuilder builder, Long behandlingId) {
        var oppdatertGrunnlag = builder.build();
        oppdatertGrunnlag.setBehandlingId(behandlingId);

        if (oppdatertGrunnlag.getBeregningsgrunnlag() != null) {
            entityManager.persist(oppdatertGrunnlag.getBeregningsgrunnlag());
        }
        entityManager.persist(oppdatertGrunnlag);
        entityManager.flush();
    }
}


