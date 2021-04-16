package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class OmsorgenForGrunnlagRepository {

    private EntityManager entityManager;


    @Inject
    public OmsorgenForGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    
    public OmsorgenForGrunnlag hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<OmsorgenForGrunnlag> hentHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }

        return hentEksisterendeGrunnlag(behandlingId);
    }

    public void lagre(Long behandlingId, OmsorgenFor omsorgenFor) {
        final Optional<OmsorgenForGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        lagre(behandlingId, omsorgenFor, eksisterendeGrunnlag);
    }
    
    public void lagre(Long behandlingId, OmsorgenForPeriode omsorgenForPeriode) {
        final Optional<OmsorgenForGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        final OmsorgenFor omsorgenFor = eksisterendeGrunnlag.map(og -> new OmsorgenFor(og.getOmsorgenFor())).orElse(new OmsorgenFor());
        tilpassOmsorgenForMedPeriode(omsorgenFor, omsorgenForPeriode);
        
        lagre(behandlingId, omsorgenFor, eksisterendeGrunnlag);
    }
    
    public void lagreNyeVurderinger(Long behandlingId, List<OmsorgenForSaksbehandlervurdering> nyeVurderinger) {
        final Optional<OmsorgenForGrunnlag> eksisterendeGrunnlag = hentEksisterendeGrunnlag(behandlingId);
        final OmsorgenFor omsorgenFor = eksisterendeGrunnlag.map(og -> new OmsorgenFor(og.getOmsorgenFor())).orElse(new OmsorgenFor());
        tilpassOmsorgenForMedSaksbehandlervurdering(omsorgenFor, nyeVurderinger);
        
        lagre(behandlingId, omsorgenFor, eksisterendeGrunnlag);
    }
    
    private void lagre(Long behandlingId, OmsorgenFor omsorgenFor, Optional<OmsorgenForGrunnlag> eksisterendeGrunnlag) {
        if (eksisterendeGrunnlag.isPresent()) {
            // deaktiver eksisterende grunnlag

            final OmsorgenForGrunnlag eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }

        final OmsorgenForGrunnlag grunnlagEntitet = new OmsorgenForGrunnlag(behandlingId, omsorgenFor);
        if (omsorgenFor != null) {
            entityManager.persist(omsorgenFor);
        }

        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    private Optional<OmsorgenForGrunnlag> hentEksisterendeGrunnlag(Long id) {
        final TypedQuery<OmsorgenForGrunnlag> query = entityManager.createQuery(
            "FROM OmsorgenForGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true", OmsorgenForGrunnlag.class);

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
        Optional<OmsorgenForGrunnlag> søknadEntitet = hentEksisterendeGrunnlag(gammelBehandlingId);
        søknadEntitet.ifPresent(entitet -> {
            lagre(nyBehandlingId, new OmsorgenFor(entitet.getOmsorgenFor()));
        });
    }
    
    void tilpassOmsorgenForMedPeriode(OmsorgenFor omsorgenFor, OmsorgenForPeriode periode) {
        final LocalDateTimeline<OmsorgenForPeriode> tidslinje = toTidslinje(omsorgenFor);
        final LocalDateSegment<OmsorgenForPeriode> newSegment = toSegment(periode);
        final LocalDateTimeline<OmsorgenForPeriode> nyTidslinje = tidslinje.combine(newSegment, new LocalDateSegmentCombinator<OmsorgenForPeriode, OmsorgenForPeriode, OmsorgenForPeriode>() {
            @Override
            public LocalDateSegment<OmsorgenForPeriode> combine(LocalDateInterval datoInterval,
                    LocalDateSegment<OmsorgenForPeriode> datoSegment,
                    LocalDateSegment<OmsorgenForPeriode> datoSegment2) {
                var value = (datoSegment2 != null) ? datoSegment2.getValue() : datoSegment.getValue();
                return toSegment(datoInterval, value);
            }
        }, JoinStyle.CROSS_JOIN);
        
        omsorgenFor.setPerioder(nyTidslinje.compress().stream().map(s -> s.getValue()).collect(Collectors.toList()));
    }
    
    private LocalDateSegment<OmsorgenForPeriode> toSegment(LocalDateInterval datoInterval, OmsorgenForPeriode value) {
        return new LocalDateSegment<OmsorgenForPeriode>(datoInterval, new OmsorgenForPeriode(value, toDatoIntervallEntitet(datoInterval)));
    }

    private DatoIntervallEntitet toDatoIntervallEntitet(LocalDateInterval datoInterval) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(datoInterval.getFomDato(), datoInterval.getTomDato());
    }
    
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
        }, JoinStyle.CROSS_JOIN);
        
        omsorgenFor.setPerioder(nyTidslinje.compress().stream().map(s -> s.getValue()).collect(Collectors.toList()));
    }

    private LocalDateTimeline<OmsorgenForSaksbehandlervurdering> toTidslinje(
            List<OmsorgenForSaksbehandlervurdering> saksbehandlervurderinger) {
        final var newSegments = saksbehandlervurderinger.stream()
                .map(saksbehandlervurdering -> new LocalDateSegment<OmsorgenForSaksbehandlervurdering>(saksbehandlervurdering.getPeriode().getFomDato(), saksbehandlervurdering.getPeriode().getTomDato(), saksbehandlervurdering))
                .collect(Collectors.toList());
        final var tidslinjeForNytt = new LocalDateTimeline<OmsorgenForSaksbehandlervurdering>(newSegments);
        return tidslinjeForNytt;
    }

    private LocalDateTimeline<OmsorgenForPeriode> toTidslinje(OmsorgenFor omsorgenFor) {
        final List<LocalDateSegment<OmsorgenForPeriode>> segments = omsorgenFor.getPerioder().stream()
                .map(p -> toSegment(p))
                .collect(Collectors.toList());
        
        final var tidslinje = new LocalDateTimeline<OmsorgenForPeriode>(segments);
        return tidslinje;
    }

    private LocalDateSegment<OmsorgenForPeriode> toSegment(OmsorgenForPeriode periode) {
        return new LocalDateSegment<OmsorgenForPeriode>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), periode);
    }
}
