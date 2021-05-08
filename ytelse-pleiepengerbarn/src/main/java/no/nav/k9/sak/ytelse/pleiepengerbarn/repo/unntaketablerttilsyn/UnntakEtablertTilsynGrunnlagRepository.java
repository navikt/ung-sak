package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForSaksbehandlervurdering;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class UnntakEtablertTilsynGrunnlagRepository {


    private EntityManager entityManager;

    @Inject
    public UnntakEtablertTilsynGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public UnntakEtablertTilsynGrunnlag hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<UnntakEtablertTilsynGrunnlag> hentHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }

        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagre(UnntakEtablertTilsynGrunnlag unntakEtablertTilsynGrunnlag) {
        entityManager.persist(unntakEtablertTilsynGrunnlag);
        entityManager.flush();
    }

    public void lagre(Long behandlingId, UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        final Optional<UnntakEtablertTilsynGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        lagre(behandlingId, unntakEtablertTilsynForPleietrengende, eksisterendeGrunnlag);
    }
    /*

    public void lagre(Long behandlingId, UnntakEtablertTilsyn beredskap, UnntakEtablertTilsyn nattevåk) {
        final Optional<UnntakEtablertTilsynGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        final UnntakEtablertTilsyn omsorgenFor = eksisterendeGrunnlag.map(og -> new UnntakEtablertTilsyn(og.getBeredskap(), og.getNattevåk())).orElse(new OmsorgenFor());
        tilpassOmsorgenForMedPeriode(omsorgenFor, omsorgenForPeriode);

        lagre(behandlingId, omsorgenFor, eksisterendeGrunnlag);
    }

    public void lagreNyeVurderinger(Long behandlingId, List<OmsorgenForSaksbehandlervurdering> nyeVurderinger) {
        final Optional<OmsorgenForGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        final OmsorgenFor omsorgenFor = eksisterendeGrunnlag.map(og -> new OmsorgenFor(og.getOmsorgenFor())).orElse(new OmsorgenFor());
        tilpassOmsorgenForMedSaksbehandlervurdering(omsorgenFor, nyeVurderinger);

        lagre(behandlingId, omsorgenFor, eksisterendeGrunnlag);
    }
*/
    private void lagre(Long behandlingId, UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende, Optional<UnntakEtablertTilsynGrunnlag> eksisterendeGrunnlag) {
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            final UnntakEtablertTilsynGrunnlag eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        final UnntakEtablertTilsynGrunnlag grunnlagEntitet = new UnntakEtablertTilsynGrunnlag(behandlingId, unntakEtablertTilsynForPleietrengende);
        if (unntakEtablertTilsynForPleietrengende != null) {
            entityManager.persist(unntakEtablertTilsynForPleietrengende);
        }

        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<UnntakEtablertTilsynGrunnlag> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<UnntakEtablertTilsynGrunnlag> query = entityManager.createQuery(
            "FROM UnntakEtablertTilsynGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true", UnntakEtablertTilsynGrunnlag.class);

        query.setParameter("behandlingId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Behandling gammelBehandling, Behandling nyBehandling) {
        kopierGrunnlagFraEksisterendeBehandling(gammelBehandling.getId(), nyBehandling.getId());
    }


    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<UnntakEtablertTilsynGrunnlag> grunnlag = hentEksisterendeGrunnlag(gammelBehandlingId);
        grunnlag.ifPresent(entitet -> {
            lagre(nyBehandlingId, new UnntakEtablertTilsynForPleietrengende(entitet.getUnntakEtablertTilsynForPleietrengende()));
        });
    }

    void tilpassUnntakEtablertTilsynMedPeriode(UnntakEtablertTilsyn unntakEtablertTilsyn, UnntakEtablertTilsynPeriode periode) {
        final LocalDateTimeline<UnntakEtablertTilsynPeriode> tidslinje = toPeriodeTidslinje(unntakEtablertTilsyn);
        final LocalDateSegment<UnntakEtablertTilsynPeriode> newSegment = toSegment(periode);
        final LocalDateTimeline<UnntakEtablertTilsynPeriode> nyTidslinje = tidslinje.combine(newSegment, new LocalDateSegmentCombinator<UnntakEtablertTilsynPeriode, UnntakEtablertTilsynPeriode, UnntakEtablertTilsynPeriode>() {
            @Override
            public LocalDateSegment<UnntakEtablertTilsynPeriode> combine(LocalDateInterval datoInterval,
                                                                LocalDateSegment<UnntakEtablertTilsynPeriode> datoSegment,
                                                                LocalDateSegment<UnntakEtablertTilsynPeriode> datoSegment2) {
                var value = (datoSegment2 != null) ? datoSegment2.getValue() : datoSegment.getValue();
                return toSegment(datoInterval, value);
            }
        }, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        unntakEtablertTilsyn.setPerioder(nyTidslinje.compress().stream().map(s -> s.getValue()).collect(Collectors.toList()));
    }

    private LocalDateSegment<UnntakEtablertTilsynPeriode> toSegment(LocalDateInterval datoInterval, UnntakEtablertTilsynPeriode value) {
        return new LocalDateSegment<>(datoInterval, new UnntakEtablertTilsynPeriode(value, toDatoIntervallEntitet(datoInterval)));
    }

    private DatoIntervallEntitet toDatoIntervallEntitet(LocalDateInterval datoInterval) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(datoInterval.getFomDato(), datoInterval.getTomDato());
    }
/*
    void tilpassOmsorgenForMedSaksbehandlervurdering(OmsorgenFor omsorgenFor, List<OmsorgenForSaksbehandlervurdering> saksbehandlervurderinger) {
        final var tidslinje = toTidslinje(omsorgenFor);
        final var tidslinjeForNytt = toTidslinje(saksbehandlervurderinger);
        final LocalDateTimeline<OmsorgenForPeriode> nyTidslinje = tidslinje.combine(tidslinjeForNytt, new LocalDateSegmentCombinator<OmsorgenForPeriode, OmsorgenForSaksbehandlervurdering, OmsorgenForPeriode>() {
            @Override
            public LocalDateSegment<OmsorgenForPeriode> combine(LocalDateInterval datoInterval,
                                                                LocalDateSegment<OmsorgenForPeriode> datoSegment,
                                                                LocalDateSegment<OmsorgenForSaksbehandlervurdering> datoSegment2) {
                if (datoSegment == null) {
                    return new LocalDateSegment<OmsorgenForPeriode>(datoInterval, new OmsorgenForPeriode(new OmsorgenForPeriode(), toDatoIntervallEntitet(datoInterval), datoSegment2.getValue()));
                }
                if (datoSegment2 == null) {
                    return toSegment(datoInterval, datoSegment.getValue());
                }

                final OmsorgenForPeriode omsorgenForPeriode = new OmsorgenForPeriode(datoSegment.getValue(), toDatoIntervallEntitet(datoInterval), datoSegment2.getValue());
                return new LocalDateSegment<OmsorgenForPeriode>(datoInterval, omsorgenForPeriode);
            }
        }, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        omsorgenFor.setPerioder(nyTidslinje.compress().stream().map(s -> s.getValue()).collect(Collectors.toList()));
    }
*/
    private LocalDateTimeline<OmsorgenForSaksbehandlervurdering> toTidslinje(
        List<OmsorgenForSaksbehandlervurdering> saksbehandlervurderinger) {
        final var newSegments = saksbehandlervurderinger.stream()
            .map(saksbehandlervurdering -> new LocalDateSegment<OmsorgenForSaksbehandlervurdering>(saksbehandlervurdering.getPeriode().getFomDato(), saksbehandlervurdering.getPeriode().getTomDato(), saksbehandlervurdering))
            .collect(Collectors.toList());
        final var tidslinjeForNytt = new LocalDateTimeline<OmsorgenForSaksbehandlervurdering>(newSegments);
        return tidslinjeForNytt;
    }

    private LocalDateTimeline<UnntakEtablertTilsynPeriode> toPeriodeTidslinje(UnntakEtablertTilsyn unntakEtablertTilsyn) {
        final List<LocalDateSegment<UnntakEtablertTilsynPeriode>> segments = unntakEtablertTilsyn.getPerioder().stream()
            .map(p -> toSegment(p))
            .collect(Collectors.toList());

        final var tidslinje = new LocalDateTimeline<>(segments);
        return tidslinje;
    }

    private LocalDateTimeline<UnntakEtablertTilsynBeskrivelse> toBeskrivelseTidslinje(UnntakEtablertTilsyn unntakEtablertTilsyn) {
        final List<LocalDateSegment<UnntakEtablertTilsynBeskrivelse>> segments = unntakEtablertTilsyn.getBeskrivelser().stream()
            .map(b -> toSegment(b))
            .collect(Collectors.toList());

        final var tidslinje = new LocalDateTimeline<>(segments);
        return tidslinje;
    }

    private LocalDateSegment<UnntakEtablertTilsynPeriode> toSegment(UnntakEtablertTilsynPeriode periode) {
        return new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), periode);
    }

    private LocalDateSegment<UnntakEtablertTilsynBeskrivelse> toSegment(UnntakEtablertTilsynBeskrivelse beskrivelse) {
        return new LocalDateSegment<>(beskrivelse.getPeriode().getFomDato(), beskrivelse.getPeriode().getTomDato(), beskrivelse);
    }
}
