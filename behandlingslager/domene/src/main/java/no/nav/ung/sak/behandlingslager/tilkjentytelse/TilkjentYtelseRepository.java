package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.behandlingslager.behandling.sporing.RegelData;
import no.nav.ung.sak.behandlingslager.diff.DiffEntity;
import no.nav.ung.sak.behandlingslager.diff.TraverseEntityGraphFactory;
import no.nav.ung.sak.behandlingslager.diff.TraverseGraph;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class TilkjentYtelseRepository {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TilkjentYtelseRepository.class);

    private final EntityManager entityManager;

    @Inject
    public TilkjentYtelseRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public void lagre(long behandlingId, List<KontrollertInntektPeriode> perioder) {
        final var eksisterende = hentKontrollertInntektPerioder(behandlingId);
        lagreKontrollertePerioder(behandlingId, perioder,
            eksisterende.map(KontrollertInntektPerioder::getRegelInput).map(RegelData::asJsonString).orElse(null),
            eksisterende.map(KontrollertInntektPerioder::getRegelSporing).map(RegelData::asJsonString).orElse(null));
    }


    public void lagreKontrollertePerioder(long behandlingId, List<KontrollertInntektPeriode> perioder, String input, String sporing) {
        final var eksisterende = hentKontrollertInntektPerioder(behandlingId);
        final var eksisterendePerioder = eksisterende.stream()
            .flatMap(it -> it.getPerioder().stream())
            .toList();
        final var differ = differ();
        if (!differ.areDifferent(eksisterendePerioder, perioder)) {
            log.info("Fant ingen diff mellom eksisterende og nye perioder, lagrer ikke.");
            return;
        }

        eksisterende.ifPresent(this::deaktiver);

        if (perioder.isEmpty()) {
            return;
        }

        final var ny = KontrollertInntektPerioder.ny(behandlingId)
            .medPerioder(perioder)
            .medRegelInput(input)
            .medRegelSporing(sporing)
            .build();
        entityManager.persist(ny);
        entityManager.flush();
    }


    private DiffEntity differ() {
        TraverseGraph traverser = TraverseEntityGraphFactory.build();
        return new DiffEntity(traverser);
    }



    private void deaktiver(KontrollertInntektPerioder eksisterende) {
        eksisterende.setIkkeAktiv();
        entityManager.persist(eksisterende);
        entityManager.flush();
    }

    public void kopierKontrollPerioder(long originalBehandlingId, long nyBehandlingId) {
        final var eksisterende = hentKontrollertInntektPerioder(originalBehandlingId);
        if (eksisterende.isPresent()) {
            final var ny = KontrollertInntektPerioder.kopi(nyBehandlingId, eksisterende.get()).build();
            entityManager.persist(ny);
            entityManager.flush();
        }
    }

    public void gjenopprettTilOriginal(Long originalBehandlingId, Long behandlingId) {
        final var eksisterendeOptional = hentKontrollertInntektPerioder(behandlingId);
        if (eksisterendeOptional.isPresent()) {
            final var eksisterende = eksisterendeOptional.get();
            deaktiver(eksisterende);
        }
        kopierKontrollPerioder(originalBehandlingId, behandlingId);
    }


        public void lagre(long behandlingId, List<TilkjentYtelsePeriode> perioder, String input, String sporing) {
        final var eksisterende = hentTilkjentYtelse(behandlingId);
        if (eksisterende.isPresent()) {
            eksisterende.get().setIkkeAktiv();
            entityManager.persist(eksisterende.get());
            entityManager.flush();
        }
        final var ny = TilkjentYtelse.ny(behandlingId)
            .medPerioder(perioder)
            .medInput(input)
            .medSporing(sporing)
            .build();
        entityManager.persist(ny);
        entityManager.flush();
    }

    public Optional<TilkjentYtelse> hentTilkjentYtelse(Long behandlingId) {
        var query = entityManager.createQuery("SELECT t FROM TilkjentYtelse t WHERE t.behandlingId=:id AND t.aktiv = true", TilkjentYtelse.class)
            .setParameter("id", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Optional<KontrollertInntektPerioder> hentKontrollertInntektPerioder(Long behandlingId) {
        var query = entityManager.createQuery("SELECT t FROM KontrollertInntektPerioder t WHERE t.behandlingId=:id AND t.aktiv = true", KontrollertInntektPerioder.class)
            .setParameter("id", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public LocalDateTimeline<BigDecimal> hentKontrollerInntektTidslinje(Long behandlingId) {
        return hentKontrollertInntektPerioder(behandlingId)
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .map(p -> new LocalDateTimeline<>(
                p.getPeriode().getFomDato(),
                p.getPeriode().getTomDato(),
                p.getInntekt())).reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }


    public LocalDateTimeline<TilkjentYtelseVerdi> hentTidslinje(Long behandlingId) {
        var query = entityManager.createQuery(
                "SELECT p FROM TilkjentYtelse t JOIN t.perioder p " +
                    "WHERE t.behandlingId = :id AND t.aktiv = true", TilkjentYtelsePeriode.class)
            .setParameter("id", behandlingId);

        List<TilkjentYtelsePeriode> perioder = query.getResultList();
        List<LocalDateSegment<TilkjentYtelseVerdi>> segments = perioder.stream()
            .map(p -> new LocalDateSegment<>(
                p.getPeriode().getFomDato(),
                p.getPeriode().getTomDato(),
                new TilkjentYtelseVerdi(
                    p.getUredusertBeløp(),
                    p.getReduksjon(),
                    p.getRedusertBeløp(),
                    p.getDagsats(),
                    p.getUtbetalingsgrad())))
            .collect(Collectors.toList());

        return new LocalDateTimeline<>(segments);
    }


    public void lagre(Long behandlingId, LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelseTidslinje, String input, String sporing) {
        final var tilkjentYtelsePerioder = tilkjentYtelseTidslinje.toSegments().stream()
            .map(it -> TilkjentYtelsePeriode.ny()
                .medUtbetalingsgrad(it.getValue().utbetalingsgrad())
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()))
                .medDagsats(it.getValue().dagsats())
                .medReduksjon(it.getValue().reduksjon())
                .medRedusertBeløp(it.getValue().redusertBeløp())
                .medUredusertBeløp(it.getValue().uredusertBeløp())
                .build()).toList();

        lagre(behandlingId, tilkjentYtelsePerioder, input, sporing);
    }
}
