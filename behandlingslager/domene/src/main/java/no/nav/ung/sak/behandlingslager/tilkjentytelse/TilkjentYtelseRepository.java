package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class TilkjentYtelseRepository {

    private final EntityManager entityManager;

    @Inject
    public TilkjentYtelseRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public void lagre(long behandlingId, List<TilkjentYtelsePeriode> perioder, String input, String sporing) {
        final var eksisterende = hentTilkjentYtelse(behandlingId);
        if (eksisterende.isPresent()) {
            eksisterende.get().setIkkeAktiv();
            entityManager.persist(eksisterende.get());
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
