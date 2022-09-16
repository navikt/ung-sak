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
        //TODO flere nullsjekker?

        final Optional<VurdertOpplæringGrunnlag> aktivtGrunnlag = getAktivtGrunnlag(behandlingId);

        aktivtGrunnlag.ifPresent(grunnlag -> {
            grunnlag.setAktiv(false);
            entityManager.persist(grunnlag);
            entityManager.flush();

            leggTilVurdertInstitusjonINyttGrunnlag(grunnlag, nyttGrunnlag);
            leggTilVurdertOpplæringINyttGrunnlag(grunnlag, nyttGrunnlag);
        });

        entityManager.persist(nyttGrunnlag.getVurdertInstitusjonHolder());
        entityManager.persist(nyttGrunnlag.getVurdertOpplæringHolder());
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void leggTilVurdertInstitusjonINyttGrunnlag(VurdertOpplæringGrunnlag aktivtGrunnlag, VurdertOpplæringGrunnlag nyttGrunnlag) {
        VurdertInstitusjon nyVurdertInstitusjon = nyttGrunnlag.getVurdertInstitusjonHolder().getVurdertInstitusjon().get(0);
        if (trengerNyVurdertInstitusjonHolder(aktivtGrunnlag, nyVurdertInstitusjon)) {
            List<VurdertInstitusjon> nyVurdertInstitusjonList = aktivtGrunnlag.getVurdertInstitusjonHolder().getVurdertInstitusjon().stream()
                .filter(eksisterendeVurdertInstitusjon -> !eksisterendeVurdertInstitusjon.getInstitusjon().equals(nyVurdertInstitusjon.getInstitusjon()))
                .map(VurdertInstitusjon::new)
                .collect(Collectors.toList());

            nyVurdertInstitusjonList.add(nyVurdertInstitusjon);

            nyttGrunnlag.medVurdertInstitusjon(nyVurdertInstitusjonList);
        } else {
            nyttGrunnlag.setVurdertInstitusjonHolder(aktivtGrunnlag.getVurdertInstitusjonHolder());
        }
    }

    private void leggTilVurdertOpplæringINyttGrunnlag(VurdertOpplæringGrunnlag aktivtGrunnlag, VurdertOpplæringGrunnlag nyttGrunnlag) {
        List<VurdertOpplæring> nyVurdertOpplæring = nyttGrunnlag.getVurdertOpplæringHolder().getVurdertOpplæring();
        if (trengerNyVurdertOpplæringHolder(aktivtGrunnlag, nyVurdertOpplæring)) {
            LocalDateTimeline<VurdertOpplæring> vurdertOpplæringTidslinje = utledKombinertTidslinje(
                aktivtGrunnlag.getVurdertOpplæringHolder().getVurdertOpplæring(),
                nyVurdertOpplæring);

            nyttGrunnlag.medVurdertOpplæring(vurdertOpplæringTidslinje
                .stream()
                .map(datoSegment -> new VurdertOpplæring(datoSegment.getValue()).medPeriode(datoSegment.getFom(), datoSegment.getTom()))
                .collect(Collectors.toList()));
        } else {
            nyttGrunnlag.setVurdertOpplæringHolder(aktivtGrunnlag.getVurdertOpplæringHolder());
        }
    }

    private boolean trengerNyVurdertOpplæringHolder(VurdertOpplæringGrunnlag aktivtGrunnlag, List<VurdertOpplæring> nyVurdertOpplæring) {
        for (VurdertOpplæring nyVO : nyVurdertOpplæring) {
            if (aktivtGrunnlag.getVurdertOpplæringHolder().getVurdertOpplæring().stream()
                .noneMatch(oldVO -> oldVO.equals(nyVO))) {
                return true;
            }
        }
        return false;
    }

    private boolean trengerNyVurdertInstitusjonHolder(VurdertOpplæringGrunnlag aktivtGrunnlag, VurdertInstitusjon nyVurdertInstitusjon) {
        return aktivtGrunnlag.getVurdertInstitusjonHolder().getVurdertInstitusjon().stream()
            .noneMatch(oldVurdertInstitusjon -> oldVurdertInstitusjon.equals(nyVurdertInstitusjon));
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
