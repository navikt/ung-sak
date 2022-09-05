package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
public class VurdertOpplæringRepository {

    private EntityManager entityManager;

    public VurdertOpplæringRepository() {
    }

    @Inject
    public VurdertOpplæringRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<VurdertOpplæringGrunnlag> hentAktivtGrunnlagForBehandling(Long behandlingId) {
        return getAktivtGrunnlag(behandlingId);
    }

    public void lagreOgFlush(Long behandlingId, VurdertOpplæringGrunnlag nyttGrunnlag) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(nyttGrunnlag, "nyttGrunnlag");

        final Optional<VurdertOpplæringGrunnlag> aktivtGrunnlag = getAktivtGrunnlag(behandlingId);

        aktivtGrunnlag.ifPresent(grunnlag -> {
            grunnlag.setAktiv(false);
            entityManager.persist(grunnlag);
            entityManager.flush();

            LocalDateTimeline<VurdertOpplæring> vurdertOpplæringTidslinje = utledKombinertTidslinje(
                aktivtGrunnlag.get().getVurdertOpplæring(),
                nyttGrunnlag.getVurdertOpplæring());

            nyttGrunnlag.setVurdertOpplæring(vurdertOpplæringTidslinje
                .stream()
                .map(datoSegment -> new VurdertOpplæring(datoSegment.getValue()).medGrunnlag(nyttGrunnlag).medPeriode(datoSegment.getFom(), datoSegment.getTom()))
                .collect(Collectors.toList()));
        });

        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private Optional<VurdertOpplæringGrunnlag> getAktivtGrunnlag(Long behandlingId) {
        TypedQuery<VurdertOpplæringGrunnlag> query = entityManager.createQuery(
                "SELECT vog FROM VurdertOpplæringGrunnlag vog WHERE vog.behandlingId = :behandling_id AND vog.aktiv = true",
                VurdertOpplæringGrunnlag.class)
            .setParameter("behandling_id", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private LocalDateTimeline<VurdertOpplæring> utledKombinertTidslinje(List<VurdertOpplæring> eksisterende,
                                                                        List<VurdertOpplæring> ny) {
        LocalDateTimeline<VurdertOpplæring> eksisterendeTidslinje = toTidslinje(eksisterende);
        LocalDateTimeline<VurdertOpplæring> nyTidslinje = toTidslinje(ny);

        return eksisterendeTidslinje.combine(nyTidslinje, (datoInterval, datoSegment, datoSegment2) -> {
            VurdertOpplæring value = datoSegment2 == null ? datoSegment.getValue() : datoSegment2.getValue();
            return new LocalDateSegment<>(datoInterval, value);
            },
                LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .compress();
    }

    private LocalDateTimeline<VurdertOpplæring> toTidslinje(List<VurdertOpplæring> perioder) {
        final var segments = perioder
            .stream()
            .map(vurdertOpplæring -> new LocalDateSegment<>(vurdertOpplæring.getPeriode().getFomDato(), vurdertOpplæring.getPeriode().getTomDato(), vurdertOpplæring))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segments);
    }
}
