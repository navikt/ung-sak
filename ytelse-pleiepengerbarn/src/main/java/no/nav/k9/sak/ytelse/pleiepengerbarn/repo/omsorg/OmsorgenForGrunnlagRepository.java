package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;

@Dependent
public class OmsorgenForGrunnlagRepository {

    private KantIKantVurderer påTversAvHelgErKantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();
    private final EntityManager entityManager;


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

        return hentOgFiksGrunnlag(behandlingId);
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

    private Optional<OmsorgenForGrunnlag> hentEksisterendeGrunnlag(Long behandlingId) {
        final TypedQuery<OmsorgenForGrunnlag> query = entityManager.createQuery(
            "FROM OmsorgenForGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true", OmsorgenForGrunnlag.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Optional<OmsorgenForGrunnlag> hentOgFiksGrunnlag(Long behandlingId) {
        Optional<OmsorgenForGrunnlag> optionalGrunnlag = hentEksisterendeGrunnlag(behandlingId);

        if (optionalGrunnlag.isPresent()) {
            OmsorgenForGrunnlag omsorgenForGrunnlag = optionalGrunnlag.get();

            List<OmsorgenForPeriode> perioderKopi = omsorgenForGrunnlag.getOmsorgenFor().getPerioder()
                .stream()
                .map(p -> new OmsorgenForPeriode(p))
                .collect(Collectors.toList());

            boolean endret = reparerHelgehull(perioderKopi);

            if (endret) {
                OmsorgenFor nyOmsorgenFor = new OmsorgenFor(omsorgenForGrunnlag.getOmsorgenFor());
                nyOmsorgenFor.setPerioder(perioderKopi);
                OmsorgenForGrunnlag nyttGrunnlag = new OmsorgenForGrunnlag(behandlingId, nyOmsorgenFor);
                lagre(behandlingId, nyOmsorgenFor);
                return hentEksisterendeGrunnlag(behandlingId);
            }
        }

        return optionalGrunnlag;
    }

    boolean reparerHelgehull(List<OmsorgenForPeriode> perioder) {
        perioder.sort(Comparator.comparing(p -> p.getPeriode().getFomDato()));
        ListIterator<OmsorgenForPeriode> iterator = perioder.listIterator();
        boolean endret = false;

        while (iterator.hasNext()) {
            OmsorgenForPeriode forrigePeriode = null;
            if (iterator.hasPrevious()) {
                forrigePeriode = perioder.get(iterator.previousIndex());
            }
            OmsorgenForPeriode periode = iterator.next();

            if (periode.getResultat() == Resultat.IKKE_VURDERT) {
                if (iterator.hasNext()) { //foretrekke å vokse mot neste periode, dersom helgehull
                    OmsorgenForPeriode nestePeriode = perioder.get(iterator.nextIndex());
                    boolean tilstøtendeMotNeste = periode.getPeriode().getTomDato().plusDays(1).equals(nestePeriode.getPeriode().getFomDato());

                    if (!tilstøtendeMotNeste && påTversAvHelgErKantIKantVurderer.erKantIKant(periode.getPeriode(), nestePeriode.getPeriode())) {
                        if (nestePeriode.getResultat() != Resultat.IKKE_OPPFYLT) {
                            OmsorgenForPeriode erstatterPeriode = new OmsorgenForPeriode(periode, DatoIntervallEntitet.fra(periode.getPeriode().getFomDato(), nestePeriode.getPeriode().getFomDato().minusDays(1)));
                            iterator.set(erstatterPeriode);
                            periode = erstatterPeriode;
                            endret = true;
                        }
                    }
                }

                if (forrigePeriode != null) {
                    boolean tilstøtendeMotForrige = forrigePeriode.getPeriode().getTomDato().plusDays(1).equals(periode.getPeriode().getFomDato());

                    if (!tilstøtendeMotForrige && påTversAvHelgErKantIKantVurderer.erKantIKant(forrigePeriode.getPeriode(), periode.getPeriode())) {
                        if (!forrigePeriode.getResultat().equals(Resultat.IKKE_OPPFYLT)) {
                            iterator.set(new OmsorgenForPeriode(periode, DatoIntervallEntitet.fra(forrigePeriode.getPeriode().getTomDato().plusDays(1), periode.getPeriode().getTomDato())));
                            endret = true;
                        }
                    }
                }
            }

            if (periode.getResultat() == Resultat.OPPFYLT && iterator.hasNext()) {
                OmsorgenForPeriode nestePeriode = perioder.get(iterator.nextIndex());

                boolean tilstøtendeMotNeste = periode.getPeriode().getTomDato().plusDays(1).equals(nestePeriode.getPeriode().getFomDato());
                if (nestePeriode.getResultat() == Resultat.OPPFYLT && !tilstøtendeMotNeste && påTversAvHelgErKantIKantVurderer.erKantIKant(periode.getPeriode(), nestePeriode.getPeriode())) {
                    //antar siste periode har mest oppdaterter opplysninger å kopiere fra
                    OmsorgenForPeriode helgeperiode = new OmsorgenForPeriode(nestePeriode, DatoIntervallEntitet.fraOgMedTilOgMed(periode.getPeriode().getTomDato().plusDays(1), nestePeriode.getPeriode().getFomDato().minusDays(1)));
                    iterator.add(helgeperiode);
                    endret = true;
                }
            }
        }

        return endret;
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<OmsorgenForGrunnlag> søknadEntitet = hentEksisterendeGrunnlag(gammelBehandlingId);
        søknadEntitet.ifPresent(entitet -> lagre(nyBehandlingId, new OmsorgenFor(entitet.getOmsorgenFor())));
    }

    void tilpassOmsorgenForMedPeriode(OmsorgenFor omsorgenFor, OmsorgenForPeriode periode) {
        final LocalDateTimeline<OmsorgenForPeriode> tidslinje = toTidslinje(omsorgenFor);
        final LocalDateSegment<OmsorgenForPeriode> newSegment = toSegment(periode);
        final LocalDateTimeline<OmsorgenForPeriode> nyTidslinje = tidslinje.combine(newSegment, (datoInterval, datoSegment, datoSegment2) -> {
            var value = (datoSegment2 != null) ? datoSegment2.getValue() : datoSegment.getValue();
            return toSegment(datoInterval, value);
        }, JoinStyle.CROSS_JOIN);

        omsorgenFor.setPerioder(nyTidslinje.compress()
            .stream()
            .map(LocalDateSegment::getValue)
            .collect(Collectors.toList()));
    }

    private LocalDateSegment<OmsorgenForPeriode> toSegment(LocalDateInterval datoInterval, OmsorgenForPeriode value) {
        return new LocalDateSegment<>(datoInterval, new OmsorgenForPeriode(value, toDatoIntervallEntitet(datoInterval)));
    }

    private DatoIntervallEntitet toDatoIntervallEntitet(LocalDateInterval datoInterval) {
        return DatoIntervallEntitet.fra(datoInterval);
    }

    void tilpassOmsorgenForMedSaksbehandlervurdering(OmsorgenFor omsorgenFor, List<OmsorgenForSaksbehandlervurdering> saksbehandlervurderinger) {
        final var tidslinje = toTidslinje(omsorgenFor);
        final var tidslinjeForNytt = toTidslinje(saksbehandlervurderinger);
        final LocalDateTimeline<OmsorgenForPeriode> nyTidslinje = tidslinje.combine(tidslinjeForNytt, mergeElementer(), JoinStyle.CROSS_JOIN);

        omsorgenFor.setPerioder(nyTidslinje.compress().stream().map(LocalDateSegment::getValue).collect(Collectors.toList()));
    }

    private LocalDateSegmentCombinator<OmsorgenForPeriode, OmsorgenForSaksbehandlervurdering, OmsorgenForPeriode> mergeElementer() {
        return (datoInterval, datoSegment, datoSegment2) -> {
            if (datoSegment == null) {
                return new LocalDateSegment<>(datoInterval, new OmsorgenForPeriode(new OmsorgenForPeriode(), toDatoIntervallEntitet(datoInterval), datoSegment2.getValue()));
            }
            if (datoSegment2 == null) {
                return toSegment(datoInterval, datoSegment.getValue());
            }

            final OmsorgenForPeriode omsorgenForPeriode = new OmsorgenForPeriode(datoSegment.getValue(), toDatoIntervallEntitet(datoInterval), datoSegment2.getValue());
            return new LocalDateSegment<>(datoInterval, omsorgenForPeriode);
        };
    }

    private LocalDateTimeline<OmsorgenForSaksbehandlervurdering> toTidslinje(
        List<OmsorgenForSaksbehandlervurdering> saksbehandlervurderinger) {
        final var newSegments = saksbehandlervurderinger.stream()
            .map(saksbehandlervurdering -> new LocalDateSegment<>(saksbehandlervurdering.getPeriode().getFomDato(), saksbehandlervurdering.getPeriode().getTomDato(), saksbehandlervurdering))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(newSegments);
    }

    private LocalDateTimeline<OmsorgenForPeriode> toTidslinje(OmsorgenFor omsorgenFor) {
        final List<LocalDateSegment<OmsorgenForPeriode>> segments = omsorgenFor.getPerioder().stream()
            .map(this::toSegment)
            .collect(Collectors.toList());

        return new LocalDateTimeline<>(segments);
    }

    private LocalDateSegment<OmsorgenForPeriode> toSegment(OmsorgenForPeriode periode) {
        return new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), periode);
    }
}
