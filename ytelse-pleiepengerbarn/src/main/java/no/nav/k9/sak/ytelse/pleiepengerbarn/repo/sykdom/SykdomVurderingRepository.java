package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class SykdomVurderingRepository {

    private EntityManager entityManager;

    SykdomVurderingRepository() {
        // CDI
    }

    @Inject
    public SykdomVurderingRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    /////////////////////////////

    public SykdomVurderinger hentEllerLagreSykdomVurderinger(AktørId pleietrengende, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        final var sykdomPerson = hentEllerLagrePerson(pleietrengende);
        return hentEllerLagre(new SykdomVurderinger(sykdomPerson, opprettetAv, opprettetTidspunkt));
    }

    public void lagre(SykdomVurdering vurdering, AktørId pleietrengende) {
        final var sykdomPerson = hentEllerLagrePerson(pleietrengende);
        final var sykdomVurderinger = new SykdomVurderinger(sykdomPerson, vurdering.getOpprettetAv(), vurdering.getOpprettetTidspunkt());
        lagre(vurdering, sykdomVurderinger);
    }

    public void lagre(SykdomVurdering vurdering, SykdomVurderinger sykdomVurderinger) {
        if (sykdomVurderinger.getId() == null) {
            sykdomVurderinger = hentEllerLagre(sykdomVurderinger);
        }

        final SykdomVurdering sisteVurdering = sykdomVurderinger.getSisteVurdering();
        vurdering.setSykdomVurderinger(sykdomVurderinger);
        vurdering.setRangering((sisteVurdering == null) ? 0L : sisteVurdering.getRangering() + 1L);
        entityManager.persist(vurdering);
        entityManager.flush();
    }

    public void lagre(SykdomVurderingVersjon versjon) {
        entityManager.persist(versjon);
        entityManager.flush();
    }

    public Optional<SykdomVurdering> hentVurdering(AktørId pleietrengende, Long vurderingId) {
        final TypedQuery<SykdomVurdering> q = entityManager.createQuery(
            "SELECT v " +
            "From SykdomVurdering as v " +
                "inner join v.sykdomVurderinger as sv " +
                "inner join sv.person as p " +
            "where p.aktoerId = :aktoerId " +
                "and v.id = :vurderingId"
            , SykdomVurdering.class);
        q.setParameter("aktoerId", pleietrengende); // Sjekk mot pleietrengende gjøres av sikkerhetsgrunner.
        q.setParameter("vurderingId", vurderingId);

        return q.getResultList().stream().findFirst();
    }

    public List<Saksnummer> hentAlleSaksnummer(AktørId pleietrengende) {
        final TypedQuery<Saksnummer> q = entityManager.createQuery(
            "SELECT distinct saksnummer " +
                "FROM SykdomGrunnlagBehandling as sgb " +
                "   inner join SykdomGrunnlag as sg " +
                "   inner join SykdomVurderingVersjon as svv " +
                "   inner join SykdomVurdering as sv " +
                "   inner join SykdomVurderinger as svs " +
                "   inner join SykdomPerson as sp " +
                "where sp.aktoerId = :aktoerId"
                , Saksnummer.class);

        q.setParameter("aktoerId", pleietrengende);

        return q.getResultList();
    }

    public List<SykdomSøktPeriode> hentAlleSøktePerioder(Saksnummer saksnummer) {
        final TypedQuery<SykdomSøktPeriode> q = entityManager.createQuery(
            "SELECT ssp " +
                "FROM SykdomGrunnlagBehandling as sgb " +
                "   inner join SykdomGrunnlag as sg " +
                "   inner join SykdomSøktPeriode as ssp " +
                "where sgb.saksnummer = :saksnummer "
                , SykdomSøktPeriode.class);
        q.setParameter("saksnummer", saksnummer);

        return q.getResultList();
    }

     public LocalDateTimeline<HashSet<Saksnummer>> hentSaksnummerForSøktePerioder(AktørId pleietrengendeAktørId) {
        final Collection<Saksnummer> saksnummere =  hentAlleSaksnummer(pleietrengendeAktørId);

        final Collection<LocalDateSegment<HashSet<Saksnummer>>> segments = new ArrayList<>();

        for (Saksnummer saksnummer : saksnummere) {
            final Collection<SykdomSøktPeriode> søktePerioder = hentAlleSøktePerioder(saksnummer);
            for (SykdomSøktPeriode periode : søktePerioder) {
                HashSet<Saksnummer> sett = new HashSet<>();
                sett.add(saksnummer);
                segments.add(new LocalDateSegment<HashSet<Saksnummer>>(periode.getFom(), periode.getTom(), sett));
            }
        }

        final LocalDateTimeline<HashSet<Saksnummer>> tidslinje = new LocalDateTimeline<>(segments, new LocalDateSegmentCombinator<HashSet<Saksnummer>, HashSet<Saksnummer>, HashSet<Saksnummer>>() {
            @Override
            public LocalDateSegment<HashSet<Saksnummer>> combine(LocalDateInterval datoInterval, LocalDateSegment<HashSet<Saksnummer>> datoSegment, LocalDateSegment<HashSet<Saksnummer>> datoSegment2) {
                HashSet<Saksnummer> kombinerteSaksnumre = new HashSet<>(datoSegment.getValue());
                kombinerteSaksnumre.addAll(datoSegment2.getValue());

                return new LocalDateSegment<>(datoInterval, kombinerteSaksnumre);
            }
        });

        final LocalDateTimeline<HashSet<Saksnummer>> saksnummerForPerioder = tidslinje.compress();
        return saksnummerForPerioder;
    }

    public List<SykdomVurderingVersjon> hentVurderingMedVersjonerForBehandling(UUID behandlingUuid, Long vurderingId) {
        throw new UnsupportedOperationException("Ikke implementert ennå.");
    }

    public Collection<SykdomVurderingVersjon> hentBehandlingVurderingerFor(SykdomVurderingType sykdomVurderingType, UUID behandlingUuid) {
        final TypedQuery<SykdomVurderingVersjon> q = entityManager.createQuery(
            "SELECT vv " +
            "From SykdomGrunnlagBehandling as sgb " +
                "inner join sgb.grunnlag as sg " +
                "inner join sg.vurderinger as vv " +
                "inner join vv.sykdomVurdering as v " +
            "where sgb.behandlingUuid = :behandlingUuid " +
                "and v.type = :sykdomVurderingType " +
                "and sgb.versjon = " +
                    "( select max(sgb2.versjon) " +
                    "From SykdomGrunnlagBehandling as sgb2 " +
                    "where sgb2.behandlingUuid = sgb.behandlingUuid )"
            , SykdomVurderingVersjon.class);
        q.setParameter("sykdomVurderingType", sykdomVurderingType);
        q.setParameter("behandlingUuid", behandlingUuid);
        return (Collection<SykdomVurderingVersjon>) q.getResultList();
    }

    public Collection<SykdomVurderingVersjon> hentSisteVurderingerFor(SykdomVurderingType sykdomVurderingType,
            AktørId pleietrengende) {
        final TypedQuery<SykdomVurderingVersjon> q = entityManager.createQuery(
            "SELECT vv " +
            "From SykdomVurderingVersjon as vv " +
                "inner join vv.sykdomVurdering as v " +
                "inner join v.sykdomVurderinger as sv " +
                "inner join sv.person as p " +
            "where p.aktoerId = :aktoerId " +
                "and v.type = :sykdomVurderingType " +
                "and vv.versjon = " +
                    "( select max(vv2.versjon) " +
                    "From SykdomVurderingVersjon vv2 " +
                    "where vv2.sykdomVurdering = vv.sykdomVurdering )"
            , SykdomVurderingVersjon.class);
        q.setParameter("sykdomVurderingType", sykdomVurderingType);
        q.setParameter("aktoerId", pleietrengende);
        return (Collection<SykdomVurderingVersjon>) q.getResultList();
    }


    public SykdomVurderingVersjon hentVurderingVersjon(Long vurderingVersjonId) {
        return null;
    }

    /*
    public void lagre(SykdomVurderinger vurderinger) {
        entityManager.persist(vurderinger);
        entityManager.flush();
    }
    */

    public SykdomPerson hentEllerLagrePerson(AktørId aktørId) {
        return hentEllerLagre(new SykdomPerson(aktørId, null));
    }

    public SykdomPerson hentEllerLagre(SykdomPerson person) {
        final EntityManager innerEntityManager = entityManager.getEntityManagerFactory().createEntityManager();
        final EntityTransaction transaction = innerEntityManager.getTransaction();
        transaction.begin();
        try {
            final Query q = innerEntityManager.createNativeQuery("INSERT INTO SYKDOM_PERSON (ID, AKTOER_ID, NORSK_IDENTITETSNUMMER) VALUES (nextval('SEQ_SYKDOM_PERSON'), :aktorId, :norskIdentitetsnummer) ON CONFLICT DO NOTHING");
            q.setParameter("aktorId", person.getAktoerId().getId());
            q.setParameter("norskIdentitetsnummer", person.getNorskIdentitetsnummer());
            q.executeUpdate();
            transaction.commit();
        } catch (Throwable t) {
            transaction.rollback();
            throw t;
        } finally {
            innerEntityManager.close();
        }

        return findPerson(person.getAktoerId());
    }

    SykdomVurderinger hentEllerLagre(SykdomVurderinger sykdomVurderinger) {
        if (sykdomVurderinger.getPerson().getId() == null) {
            sykdomVurderinger.setPerson(hentEllerLagre(sykdomVurderinger.getPerson()));
        }

        final EntityManager innerEntityManager = entityManager.getEntityManagerFactory().createEntityManager();
        final EntityTransaction transaction = innerEntityManager.getTransaction();
        transaction.begin();
        try {
            final Query q = innerEntityManager.createNativeQuery("INSERT INTO SYKDOM_VURDERINGER (ID, SYK_PERSON_ID, OPPRETTET_AV, OPPRETTET_TID) VALUES (nextval('SEQ_SYKDOM_VURDERINGER'), :personId, :opprettetAv, :opprettetTid) ON CONFLICT DO NOTHING");
            q.setParameter("personId", sykdomVurderinger.getPerson().getId());
            q.setParameter("opprettetAv", sykdomVurderinger.getOpprettetAv());
            q.setParameter("opprettetTid", sykdomVurderinger.getOpprettetTidspunkt());
            q.executeUpdate();
            transaction.commit();
        } catch (Throwable t) {
            transaction.rollback();
            throw t;
        } finally {
            innerEntityManager.close();
        }

        return hentVurderingerForBarn(sykdomVurderinger.getPerson().getAktoerId()).get();
    }

    private Optional<SykdomVurderinger> hentVurderingerForBarn(AktørId pleietrengende) {
        final TypedQuery<SykdomVurderinger> q = entityManager.createQuery(
            "select sv " +
                "From SykdomVurderinger as sv " +
                    "inner join sv.person as p " +
                "where p.aktoerId = :aktoerId"
            , SykdomVurderinger.class);
        q.setParameter("aktoerId", pleietrengende);

        return q.getResultList().stream().findFirst();
    }

    private SykdomPerson findPerson(AktørId aktørId) {
        final TypedQuery<SykdomPerson> q = entityManager.createQuery("select p From SykdomPerson p where p.aktoerId = :aktoerId", SykdomPerson.class);
        q.setParameter("aktoerId", aktørId);
        return q.getResultList().stream().findFirst().orElse(null);
    }


    public LocalDateTimeline<SykdomVurderingVersjon> getVurderingstidslinjeFor(SykdomVurderingType type, UUID behandlingUuid) {
        return tilTidslinje(hentBehandlingVurderingerFor(type, behandlingUuid));
    }
    
    public LocalDateTimeline<SykdomVurderingVersjon> getSisteVurderingstidslinjeFor(SykdomVurderingType type, AktørId pleietrengende) {
        return tilTidslinje(hentSisteVurderingerFor(type, pleietrengende));
    }
    
    
    private LocalDateTimeline<SykdomVurderingVersjon> tilTidslinje(Collection<SykdomVurderingVersjon> vurderinger) {
        final Collection<LocalDateSegment<SykdomVurderingVersjon>> segments = new ArrayList<>();
        for (SykdomVurderingVersjon vurdering : vurderinger) {
            for (SykdomVurderingPeriode periode : vurdering.getPerioder()) {
                segments.add(new LocalDateSegment<SykdomVurderingVersjon>(periode.getFom(), periode.getTom(), vurdering));
            }
        }

        final LocalDateTimeline<SykdomVurderingVersjon> tidslinje = new LocalDateTimeline<>(segments, new LocalDateSegmentCombinator<SykdomVurderingVersjon, SykdomVurderingVersjon, SykdomVurderingVersjon>() {
            @Override
            public LocalDateSegment<SykdomVurderingVersjon> combine(LocalDateInterval datoInterval,
                    LocalDateSegment<SykdomVurderingVersjon> datoSegment,
                    LocalDateSegment<SykdomVurderingVersjon> datoSegment2) {
                final Long rangering1 = datoSegment.getValue().getSykdomVurdering().getRangering();
                final Long rangering2 = datoSegment2.getValue().getSykdomVurdering().getRangering();
                final Long versjon1 = datoSegment.getValue().getVersjon();
                final Long versjon2 = datoSegment2.getValue().getVersjon();

                final SykdomVurderingVersjon valgtVurdering;
                if (rangering1.compareTo(rangering2) > 0) {
                    valgtVurdering = datoSegment.getValue();
                } else if (rangering1.compareTo(rangering2) < 0) {
                    valgtVurdering = datoSegment2.getValue();
                } else {
                    valgtVurdering = (versjon1.compareTo(versjon2) > 0) ? datoSegment.getValue() : datoSegment2.getValue();
                }

                return new LocalDateSegment<>(datoInterval, valgtVurdering);
            }
        });

        return tidslinje.compress();
    }
    
    public List<SykdomPeriodeMedEndring> finnEndringer(LocalDateTimeline<SykdomVurderingVersjon> tidslinje, SykdomVurderingVersjon nyEndring) {
        final LocalDateTimeline<SykdomVurderingVersjon> endret = new LocalDateTimeline<>(nyEndring.getPerioder().stream().map(p -> new LocalDateSegment<SykdomVurderingVersjon>(p.getFom(), p.getTom(), p.getVurderingVersjon())).collect(Collectors.toList()));
        return endret.combine(tidslinje, new LocalDateSegmentCombinator<SykdomVurderingVersjon, SykdomVurderingVersjon, SykdomPeriodeMedEndring>() {
            @Override
            public LocalDateSegment<SykdomPeriodeMedEndring> combine(LocalDateInterval datoInterval,
                    LocalDateSegment<SykdomVurderingVersjon> datoSegment, LocalDateSegment<SykdomVurderingVersjon> datoSegment2) {
                if (datoSegment2 == null) {
                    return null;
                }
                var nyVersjon = datoSegment.getValue();
                var gammelVersjon = datoSegment2.getValue();
                if (nyVersjon.getSykdomVurdering().getId() != null
                        && nyVersjon.getSykdomVurdering().getId().equals(gammelVersjon.getSykdomVurdering().getId())) {
                    return null;
                }
                
                final boolean endrerVurderingSammeBehandling = nyVersjon.getEndretBehandlingUuid().equals(gammelVersjon.getEndretBehandlingUuid());
                final boolean endrerAnnenVurdering = !endrerVurderingSammeBehandling;
                return new LocalDateSegment<>(datoInterval, new SykdomPeriodeMedEndring(new Periode(datoInterval.getFomDato(), datoInterval.getTomDato()), endrerVurderingSammeBehandling, endrerAnnenVurdering, gammelVersjon));
            }
        }, JoinStyle.LEFT_JOIN).compress().stream().map(l -> l.getValue()).collect(Collectors.toList());
    }
}
