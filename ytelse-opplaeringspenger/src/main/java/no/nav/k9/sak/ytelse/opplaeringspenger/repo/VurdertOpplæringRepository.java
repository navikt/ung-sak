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

    public void kopierGrunnlagFraEksisterendeBehandling(Long originalBehandlingId, Long nyBehandlingId) {
        Optional<VurdertOpplæringGrunnlag> aktivtGrunnlagFraForrigeBehandling = getAktivtGrunnlag(originalBehandlingId);
        aktivtGrunnlagFraForrigeBehandling.ifPresent(grunnlag -> lagreOgFlush(nyBehandlingId, new VurdertOpplæringGrunnlag(nyBehandlingId, grunnlag)));
    }

    public void lagreOgFlush(Long behandlingId, VurdertOpplæringGrunnlag nyttGrunnlag) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(nyttGrunnlag, "nyttGrunnlag");
        Objects.requireNonNull(nyttGrunnlag.getVurdertOpplæringHolder(), "nyttGrunnlag.vurdertOpplæringHolder");
        Objects.requireNonNull(nyttGrunnlag.getVurdertInstitusjonHolder(), "nyttGrunnlag.vurdertInstitusjonHolder");

        final VurdertOpplæringGrunnlag aktivtGrunnlag = getAktivtGrunnlag(behandlingId).orElse(null);

        if (aktivtGrunnlag != null) {
            aktivtGrunnlag.setAktiv(false);
            entityManager.persist(aktivtGrunnlag);
            entityManager.flush();

            VurdertInstitusjonHolder vurdertInstitusjonHolder = hentVurdertInstitusjonHolderTilNyttGrunnlag(aktivtGrunnlag, nyttGrunnlag);
            VurdertOpplæringHolder vurdertOpplæringHolder = hentVurdertOpplæringHolderTilNyttGrunnlag(aktivtGrunnlag, nyttGrunnlag);
            nyttGrunnlag = new VurdertOpplæringGrunnlag(nyttGrunnlag, vurdertInstitusjonHolder, vurdertOpplæringHolder);
        }

        entityManager.persist(nyttGrunnlag.getVurdertInstitusjonHolder());
        entityManager.persist(nyttGrunnlag.getVurdertOpplæringHolder());
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private VurdertInstitusjonHolder hentVurdertInstitusjonHolderTilNyttGrunnlag(VurdertOpplæringGrunnlag aktivtGrunnlag, VurdertOpplæringGrunnlag nyttGrunnlag) {
        List<VurdertInstitusjon> nyVurdertInstitusjon = nyttGrunnlag.getVurdertInstitusjonHolder().getVurdertInstitusjon();
        if (nyVurdertInstitusjon.size() > 1) {
            throw new IllegalArgumentException("Utviklerfeil: Skal ikke ha flere enn én ny institusjon om gangen.");
        }
        if (!nyVurdertInstitusjon.isEmpty() && trengerNyVurdertInstitusjonHolder(aktivtGrunnlag, nyVurdertInstitusjon.get(0))) {
            List<VurdertInstitusjon> nyVurdertInstitusjonList = aktivtGrunnlag.getVurdertInstitusjonHolder().getVurdertInstitusjon().stream()
                .filter(eksisterendeVurdertInstitusjon -> !eksisterendeVurdertInstitusjon.getInstitusjon().equals(nyVurdertInstitusjon.get(0).getInstitusjon()))
                .map(VurdertInstitusjon::new)
                .collect(Collectors.toList());

            nyVurdertInstitusjonList.add(nyVurdertInstitusjon.get(0));

            return new VurdertInstitusjonHolder(nyVurdertInstitusjonList);
        }
        return aktivtGrunnlag.getVurdertInstitusjonHolder();
    }

    private VurdertOpplæringHolder hentVurdertOpplæringHolderTilNyttGrunnlag(VurdertOpplæringGrunnlag aktivtGrunnlag, VurdertOpplæringGrunnlag nyttGrunnlag) {
        List<VurdertOpplæring> nyVurdertOpplæring = nyttGrunnlag.getVurdertOpplæringHolder().getVurdertOpplæring();
        if (trengerNyVurdertOpplæringHolder(aktivtGrunnlag, nyVurdertOpplæring)) {
            LocalDateTimeline<VurdertOpplæring> vurdertOpplæringTidslinje = utledKombinertTidslinje(
                aktivtGrunnlag.getVurdertOpplæringHolder().getVurdertOpplæring(),
                nyVurdertOpplæring);

            return new VurdertOpplæringHolder(vurdertOpplæringTidslinje
                .stream()
                .map(datoSegment -> new VurdertOpplæring(datoSegment.getValue()).medPeriode(datoSegment.getFom(), datoSegment.getTom()))
                .collect(Collectors.toList()));
        } else {
            return aktivtGrunnlag.getVurdertOpplæringHolder();
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
