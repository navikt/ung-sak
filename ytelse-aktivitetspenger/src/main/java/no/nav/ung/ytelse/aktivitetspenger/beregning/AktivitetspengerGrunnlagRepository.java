package no.nav.ung.ytelse.aktivitetspenger.beregning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPeriode;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPerioder;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

@Dependent
public class AktivitetspengerGrunnlagRepository {

    private static final Logger log = LoggerFactory.getLogger(AktivitetspengerGrunnlagRepository.class);
    private EntityManager entityManager;

    @Inject
    public AktivitetspengerGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public void lagreBeregningsgrunnlag(Long behandlingId, Beregningsgrunnlag beregningsgrunnlag) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        var aktivtGrunnlag = grunnlagOptional.orElse(new AktivitetspengerGrunnlag());

        boolean finnesFraFør = aktivtGrunnlag.getBeregningsgrunnlag().contains(beregningsgrunnlag);

        if (finnesFraFør) {
            log.info("[behandlingId={}] Beregningsgrunnlag for skjæringstidspunkt {} finnes allerede, gjenbruker eksisterende.",
                behandlingId, beregningsgrunnlag.getSkjæringstidspunkt());
            return;
        }

        var builder = new AktivitetspengerGrunnlagBuilder(aktivtGrunnlag);
        builder.leggTilBeregningsgrunnlag(beregningsgrunnlag);

        grunnlagOptional.ifPresent(this::deaktiverEksisterende);
        lagre(builder, behandlingId);
    }

    public void deaktiverGrunnlag(Long behandlingId) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        if (grunnlagOptional.isPresent()) {
            log.info("Deaktiverer eksisterende aktivitetspenger grunnlag for behandlingId={}", behandlingId);
            deaktiverEksisterende(grunnlagOptional.get());
        }
    }

    public void lagre(Long behandlingId, AktivitetspengerSatsResultat satsResultat) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        var aktivtGrunnlag = grunnlagOptional.orElse(new AktivitetspengerGrunnlag());

        var perioder = satsResultat.resultatTidslinje().toSegments().stream()
            .map(s -> new AktivitetspengerSatsPeriode(s.getLocalDateInterval(), s.getValue()))
            .toList();
        var grunnsatser = new AktivitetspengerSatsPerioder(perioder, satsResultat.regelInput(), satsResultat.regelSporing());

        var builder = new AktivitetspengerGrunnlagBuilder(aktivtGrunnlag);
        builder.medGrunnsatser(grunnsatser);

        grunnlagOptional.ifPresent(this::deaktiverEksisterende);
        lagre(builder, behandlingId);
    }

    public Optional<AktivitetspengerGrunnlag> hentGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
                "SELECT bg FROM AktivitetspengerGrunnlag bg WHERE bg.behandlingId=:id AND bg.aktiv = true",
                AktivitetspengerGrunnlag.class)
            .setParameter("id", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public LocalDateTimeline<Beregningsgrunnlag> hentBesteBeregningSomTidslinje(long behandlingId) {
        var grunnlag = hentGrunnlag(behandlingId).orElseThrow(
            () -> new IllegalStateException("Fant ikke aktivitetspenger grunnlag for behandlingId=" + behandlingId));
        return grunnlag.hentBeregningsgrunnlagTidslinje();
    }

    private void deaktiverEksisterende(AktivitetspengerGrunnlag grunnlag) {
        grunnlag.setIkkeAktivt();
        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    private void lagre(AktivitetspengerGrunnlagBuilder builder, Long behandlingId) {
        var oppdatertGrunnlag = builder.build();
        oppdatertGrunnlag.setBehandlingId(behandlingId);

        for (Beregningsgrunnlag bg : oppdatertGrunnlag.getBeregningsgrunnlag()) {
            if (!entityManager.contains(bg)) {
                entityManager.persist(bg);
            }
        }
        if (oppdatertGrunnlag.getSatsperioder() != null) {
            entityManager.persist(oppdatertGrunnlag.getSatsperioder());
        }
        entityManager.persist(oppdatertGrunnlag);
        entityManager.flush();
    }
}

