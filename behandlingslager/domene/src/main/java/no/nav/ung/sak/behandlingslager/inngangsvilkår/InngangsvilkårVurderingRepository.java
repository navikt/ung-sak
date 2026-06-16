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

    public Optional<AktivitetspengerInngangsvilkårResultatGrunnlag> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagreBistandsVurderinger(Long behandlingId, List<BistandsvilkårResultatPeriode> nyeVurderinger) {
        var eksisterende = hentEksisterendeGrunnlag(behandlingId);
        var eksisterendeVurderinger = eksisterende
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBistandsvilkårResultatHolder)
            .map(BistandsvilkårResultatHolder::getVurderinger)
            .orElse(List.of());

        var kombinerte = kombinerBistand(eksisterendeVurderinger, nyeVurderinger);
        var nyHolder = new BistandsvilkårResultatHolder(kombinerte);
        var livsoppholdHolder = eksisterende.flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getAndreLivsoppholdsytelserResultatHolder).orElse(null);
        var bostedHolder = eksisterende.flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBostedsvilkårResultatHolder).orElse(null);
        persister(eksisterende, new AktivitetspengerInngangsvilkårResultatGrunnlag(behandlingId, nyHolder, livsoppholdHolder, bostedHolder));
    }

    public void lagreYtelseVurderinger(Long behandlingId, List<AndreLivsoppholdsytelserResultatPeriode> nyeVurderinger) {
        var eksisterende = hentEksisterendeGrunnlag(behandlingId);
        var eksisterendeVurderinger = eksisterende
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getAndreLivsoppholdsytelserResultatHolder)
            .map(AndreLivsoppholdsytelserResultatHolder::getVurderinger)
            .orElse(List.of());

        var kombinerte = kombinerYtelser(eksisterendeVurderinger, nyeVurderinger);
        var bistandHolder = eksisterende.flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBistandsvilkårResultatHolder).orElse(null);
        var nyHolder = new AndreLivsoppholdsytelserResultatHolder(kombinerte);
        var bostedHolder = eksisterende.flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBostedsvilkårResultatHolder).orElse(null);
        persister(eksisterende, new AktivitetspengerInngangsvilkårResultatGrunnlag(behandlingId, bistandHolder, nyHolder, bostedHolder));
    }

    public void lagreBostedVurderinger(Long behandlingId, List<BostedsvilkårResultatPeriode> nyeVurderinger) {
        var eksisterende = hentEksisterendeGrunnlag(behandlingId);
        var eksisterendeVurderinger = eksisterende
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBostedsvilkårResultatHolder)
            .map(BostedsvilkårResultatHolder::getVurderinger)
            .orElse(List.of());

        var kombinerte = kombinerBosted(eksisterendeVurderinger, nyeVurderinger);
        var bistandHolder = eksisterende.flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBistandsvilkårResultatHolder).orElse(null);
        var livsoppholdHolder = eksisterende.flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getAndreLivsoppholdsytelserResultatHolder).orElse(null);
        var nyHolder = new BostedsvilkårResultatHolder(kombinerte);
        persister(eksisterende, new AktivitetspengerInngangsvilkårResultatGrunnlag(behandlingId, bistandHolder, livsoppholdHolder, nyHolder));
    }

    public void kopier(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        hentEksisterendeGrunnlag(eksisterendeBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = new AktivitetspengerInngangsvilkårResultatGrunnlag(
                nyBehandlingId,
                eksisterende.getBistandsvilkårResultatHolder().orElse(null),
                eksisterende.getAndreLivsoppholdsytelserResultatHolder().orElse(null),
                eksisterende.getBostedsvilkårResultatHolder().orElse(null)
            );
            persister(Optional.empty(), nyttGrunnlag);
        });
    }

    private List<BistandsvilkårResultatPeriode> kombinerBistand(
            List<BistandsvilkårResultatPeriode> eksisterende,
            List<BistandsvilkårResultatPeriode> nye) {
        var eksisterendeTidslinje = tilBistandTidslinje(eksisterende);
        var nyTidslinje = tilBistandTidslinje(nye);
        // Behold eksisterende der nye ikke overlapper, nye tar alltid presedens
        return eksisterendeTidslinje.disjoint(nyTidslinje).crossJoin(nyTidslinje).stream()
            .map(seg -> {
                var periode = DatoIntervallEntitet.fraOgMedTilOgMed(seg.getFom(), seg.getTom());
                var v = seg.getValue();
                return v.getPeriode().equals(periode) ? v : new BistandsvilkårResultatPeriode(periode, v);
            })
            .toList();
    }

    private List<AndreLivsoppholdsytelserResultatPeriode> kombinerYtelser(
            List<AndreLivsoppholdsytelserResultatPeriode> eksisterende,
            List<AndreLivsoppholdsytelserResultatPeriode> nye) {
        var eksisterendeTidslinje = tilLivsoppholdTidslinje(eksisterende);
        var nyTidslinje = tilLivsoppholdTidslinje(nye);
        return eksisterendeTidslinje.disjoint(nyTidslinje).crossJoin(nyTidslinje).stream()
            .map(seg -> {
                var periode = DatoIntervallEntitet.fraOgMedTilOgMed(seg.getFom(), seg.getTom());
                var v = seg.getValue();
                return v.getPeriode().equals(periode) ? v : new AndreLivsoppholdsytelserResultatPeriode(periode, v);
            })
            .toList();
    }

    private static LocalDateTimeline<BistandsvilkårResultatPeriode> tilBistandTidslinje(List<BistandsvilkårResultatPeriode> vurderinger) {
        return new LocalDateTimeline<>(vurderinger.stream()
            .map(v -> new LocalDateSegment<>(v.getPeriode().getFomDato(), v.getPeriode().getTomDato(), v))
            .toList());
    }

    private static LocalDateTimeline<AndreLivsoppholdsytelserResultatPeriode> tilLivsoppholdTidslinje(List<AndreLivsoppholdsytelserResultatPeriode> vurderinger) {
        return new LocalDateTimeline<>(vurderinger.stream()
            .map(v -> new LocalDateSegment<>(v.getPeriode().getFomDato(), v.getPeriode().getTomDato(), v))
            .toList());
    }

    private List<BostedsvilkårResultatPeriode> kombinerBosted(
            List<BostedsvilkårResultatPeriode> eksisterende,
            List<BostedsvilkårResultatPeriode> nye) {
        var eksisterendeTidslinje = tilBostedTidslinje(eksisterende);
        var nyTidslinje = tilBostedTidslinje(nye);
        return eksisterendeTidslinje.disjoint(nyTidslinje).crossJoin(nyTidslinje).stream()
            .map(seg -> {
                var periode = DatoIntervallEntitet.fraOgMedTilOgMed(seg.getFom(), seg.getTom());
                var v = seg.getValue();
                return v.getPeriode().equals(periode) ? v : new BostedsvilkårResultatPeriode(periode, v);
            })
            .toList();
    }

    private static LocalDateTimeline<BostedsvilkårResultatPeriode> tilBostedTidslinje(List<BostedsvilkårResultatPeriode> vurderinger) {
        return new LocalDateTimeline<>(vurderinger.stream()
            .map(v -> new LocalDateSegment<>(v.getPeriode().getFomDato(), v.getPeriode().getTomDato(), v))
            .toList());
    }

    private void persister(Optional<AktivitetspengerInngangsvilkårResultatGrunnlag> eksisterendeGrunnlag, AktivitetspengerInngangsvilkårResultatGrunnlag nyttGrunnlag) {
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private void deaktiverEksisterende(AktivitetspengerInngangsvilkårResultatGrunnlag grunnlag) {
        grunnlag.deaktiver();
        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    private Optional<AktivitetspengerInngangsvilkårResultatGrunnlag> hentEksisterendeGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT g FROM AktivitetspengerInngangsvilkårResultatGrunnlag g " +
                "WHERE g.behandlingId = :behandlingId " +
                "AND g.aktiv = true",
            AktivitetspengerInngangsvilkårResultatGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }
}
