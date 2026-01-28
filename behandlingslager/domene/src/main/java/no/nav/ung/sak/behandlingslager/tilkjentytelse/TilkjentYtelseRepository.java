package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.sporing.RegelData;
import no.nav.ung.sak.diff.TraverseEntityGraphFactory;
import no.nav.ung.sak.felles.diff.DiffEntity;
import no.nav.ung.sak.felles.diff.TraverseGraph;
import no.nav.ung.sak.felles.tid.DatoIntervallEntitet;
import no.nav.ung.sak.felles.tid.Virkedager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
        final var eksisterende = hentKontrollertInntektGrunnlag(behandlingId);
        final var differ = differ();
        if (eksisterende.isPresent() && !differ.areDifferent(eksisterende.get().getKontrollertInntektPerioder().getPerioder(), perioder)) {
            log.info("Fant ingen diff mellom eksisterende og nye perioder, lagrer ikke.");
            return;
        }

        eksisterende.ifPresent(this::deaktiver);

        if (perioder.isEmpty()) {
            return;
        }

        if (perioder.stream().anyMatch(it -> it.getId() != null)) {
            throw new IllegalStateException("Perioder som lagres  på nytt grunnlag kan ikke ha id, det betyr at de allerede er lagret.");
        }

        final var ny = KontrollertInntektPerioder.ny(behandlingId)
            .medPerioder(perioder)
            .medRegelInput(input)
            .medRegelSporing(sporing)
            .build();

        entityManager.persist(ny);
        entityManager.flush();

        lagreNyttGrunnlag(behandlingId, ny);

    }

    private void lagreNyttGrunnlag(long behandlingId, KontrollertInntektPerioder ny) {
        KontrollertInntektGrunnlag kontrollertInntektGrunnlag = new KontrollertInntektGrunnlag(behandlingId, ny);
        Optional<KontrollertInntektGrunnlag> eksisterendeGrunnlag = hentKontrollertInntektGrunnlag(behandlingId);
        if (eksisterendeGrunnlag.isPresent()) {
            deaktiver(eksisterendeGrunnlag.get());
            entityManager.persist(eksisterendeGrunnlag.get());
            entityManager.flush();
        }
        entityManager.persist(kontrollertInntektGrunnlag);
        entityManager.flush();
    }


    private DiffEntity differ() {
        TraverseGraph traverser = TraverseEntityGraphFactory.build();
        return new DiffEntity(traverser);
    }


    private void deaktiver(KontrollertInntektGrunnlag eksisterende) {
        eksisterende.setAktiv(false);
        entityManager.persist(eksisterende);
        entityManager.flush();
    }

    public void kopierKontrollPerioder(long originalBehandlingId, long nyBehandlingId) {
        final var eksisterende = hentKontrollertInntektGrunnlag(originalBehandlingId);
        eksisterende.ifPresent(kontrollertInntektGrunnlag -> lagreNyttGrunnlag(nyBehandlingId, kontrollertInntektGrunnlag.getKontrollertInntektPerioder()));
    }

    public void gjenopprettTilOriginal(Long originalBehandlingId, Long behandlingId) {
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
        return hentKontrollertInntektGrunnlag(behandlingId).map(KontrollertInntektGrunnlag::getKontrollertInntektPerioder);
    }

    public Optional<KontrollertInntektGrunnlag> hentKontrollertInntektGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery("SELECT gr FROM KontrollertInntektGrunnlag gr WHERE gr.behandlingId=:id AND gr.aktiv = true", KontrollertInntektGrunnlag.class)
            .setParameter("id", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }


    public Map<Behandling, LocalDateTimeline<TilkjentYtelseVerdi>> hentTidslinjerForFagsak(Long fagsakId) {
        var tilkjentYtelseMap = hentTilkjentYtelseForFagsak(fagsakId);
        return tilkjentYtelseMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> mapTilTidslinje(entry.getValue().getPerioder())
            ));
    }


    private Map<Behandling, TilkjentYtelse> hentTilkjentYtelseForFagsak(Long fagsakId) {
        var query = entityManager.createQuery("SELECT b,t FROM TilkjentYtelse t inner join Behandling b on b.id = t.behandlingId " +
                "WHERE b.fagsak.id=:fagsakId AND t.aktiv = true", Tuple.class)
            .setParameter("fagsakId", fagsakId);

        return query.getResultList().stream()
            .collect(Collectors.toMap(
                it -> it.get(0, Behandling.class),
                it -> it.get(1, TilkjentYtelse.class)
            ));

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
        return mapTilTidslinje(perioder);
    }

    private static LocalDateTimeline<TilkjentYtelseVerdi> mapTilTidslinje(List<TilkjentYtelsePeriode> perioder) {
        List<LocalDateSegment<TilkjentYtelseVerdi>> segments = perioder.stream()
            .map(p -> {
                var antallVirkedager = BigDecimal.valueOf(Virkedager
                    .beregnAntallVirkedager(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()));
                return new LocalDateSegment<>(
                    p.getPeriode().getFomDato(),
                    p.getPeriode().getTomDato(),
                    new TilkjentYtelseVerdi(
                        p.getUredusertBeløp(),
                        p.getReduksjon(),
                        p.getRedusertBeløp(),
                        p.getDagsats(),
                        p.getUtbetalingsgrad(),
                        p.getDagsats().multiply(antallVirkedager)));
            })
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
