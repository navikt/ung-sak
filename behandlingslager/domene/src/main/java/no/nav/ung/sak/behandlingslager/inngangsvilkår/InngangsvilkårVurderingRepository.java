package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Dependent
public class InngangsvilkårVurderingRepository {

    private final EntityManager entityManager;

    @Inject
    public InngangsvilkårVurderingRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public Optional<InngangsvilkårVurderingGrunnlag> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagreBistandsVurderinger(Long behandlingId, List<BistandsvilkårVurderingPeriode> nyeVurderinger) {
        var eksisterende = hentEksisterendeGrunnlag(behandlingId);
        var eksisterendeVurderinger = eksisterende
            .flatMap(InngangsvilkårVurderingGrunnlag::getBistandsvilkårVurderingHolder)
            .map(BistandsvilkårVurderingHolder::getVurderinger)
            .orElse(List.of());

        var kombinerte = kombinerBistand(eksisterendeVurderinger, nyeVurderinger);
        var nyHolder = new BistandsvilkårVurderingHolder(kombinerte);
        var livsoppholdHolder = eksisterende.flatMap(InngangsvilkårVurderingGrunnlag::getAndreLivsoppholdsytelserVurderingHolder).orElse(null);
        persister(eksisterende, new InngangsvilkårVurderingGrunnlag(behandlingId, nyHolder, livsoppholdHolder));
    }

    public void lagreLivsoppholdsVurderinger(Long behandlingId, List<AndreLivsoppholdsytelserVurderingPeriode> nyeVurderinger) {
        var eksisterende = hentEksisterendeGrunnlag(behandlingId);
        var eksisterendeVurderinger = eksisterende
            .flatMap(InngangsvilkårVurderingGrunnlag::getAndreLivsoppholdsytelserVurderingHolder)
            .map(AndreLivsoppholdsytelserVurderingHolder::getVurderinger)
            .orElse(List.of());

        var kombinerte = kombinerLivsopphold(eksisterendeVurderinger, nyeVurderinger);
        var bistandHolder = eksisterende.flatMap(InngangsvilkårVurderingGrunnlag::getBistandsvilkårVurderingHolder).orElse(null);
        var nyHolder = new AndreLivsoppholdsytelserVurderingHolder(kombinerte);
        persister(eksisterende, new InngangsvilkårVurderingGrunnlag(behandlingId, bistandHolder, nyHolder));
    }

    public void kopier(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        hentEksisterendeGrunnlag(eksisterendeBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = new InngangsvilkårVurderingGrunnlag(
                nyBehandlingId,
                eksisterende.getBistandsvilkårVurderingHolder().orElse(null),
                eksisterende.getAndreLivsoppholdsytelserVurderingHolder().orElse(null)
            );
            persister(Optional.empty(), nyttGrunnlag);
        });
    }

    private List<BistandsvilkårVurderingPeriode> kombinerBistand(
            List<BistandsvilkårVurderingPeriode> eksisterende,
            List<BistandsvilkårVurderingPeriode> nye) {
        var eksisterendeTidslinje = tilBistandTidslinje(eksisterende);
        var nyTidslinje = tilBistandTidslinje(nye);
        // Behold eksisterende der nye ikke overlapper, nye tar alltid presedens
        return eksisterendeTidslinje.disjoint(nyTidslinje).crossJoin(nyTidslinje).stream()
            .map(seg -> {
                var periode = DatoIntervallEntitet.fraOgMedTilOgMed(seg.getFom(), seg.getTom());
                var v = seg.getValue();
                return v.getPeriode().equals(periode) ? v : new BistandsvilkårVurderingPeriode(periode, v);
            })
            .toList();
    }

    private List<AndreLivsoppholdsytelserVurderingPeriode> kombinerLivsopphold(
            List<AndreLivsoppholdsytelserVurderingPeriode> eksisterende,
            List<AndreLivsoppholdsytelserVurderingPeriode> nye) {
        var eksisterendeTidslinje = tilLivsoppholdTidslinje(eksisterende);
        var nyTidslinje = tilLivsoppholdTidslinje(nye);
        return eksisterendeTidslinje.disjoint(nyTidslinje).crossJoin(nyTidslinje).stream()
            .map(seg -> {
                var periode = DatoIntervallEntitet.fraOgMedTilOgMed(seg.getFom(), seg.getTom());
                var v = seg.getValue();
                return v.getPeriode().equals(periode) ? v : new AndreLivsoppholdsytelserVurderingPeriode(periode, v);
            })
            .toList();
    }

    private static LocalDateTimeline<BistandsvilkårVurderingPeriode> tilBistandTidslinje(List<BistandsvilkårVurderingPeriode> vurderinger) {
        return new LocalDateTimeline<>(vurderinger.stream()
            .map(v -> new LocalDateSegment<>(v.getPeriode().getFomDato(), v.getPeriode().getTomDato(), v))
            .toList());
    }

    private static LocalDateTimeline<AndreLivsoppholdsytelserVurderingPeriode> tilLivsoppholdTidslinje(List<AndreLivsoppholdsytelserVurderingPeriode> vurderinger) {
        return new LocalDateTimeline<>(vurderinger.stream()
            .map(v -> new LocalDateSegment<>(v.getPeriode().getFomDato(), v.getPeriode().getTomDato(), v))
            .toList());
    }

    private void persister(Optional<InngangsvilkårVurderingGrunnlag> eksisterendeGrunnlag, InngangsvilkårVurderingGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(InngangsvilkårVurderingGrunnlag grunnlag) {
        grunnlag.deaktiver();
        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    private Optional<InngangsvilkårVurderingGrunnlag> hentEksisterendeGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT g FROM InngangsvilkårVurderingGrunnlag g " +
                "WHERE g.behandlingId = :behandlingId " +
                "AND g.aktiv = true",
            InngangsvilkårVurderingGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }
}
