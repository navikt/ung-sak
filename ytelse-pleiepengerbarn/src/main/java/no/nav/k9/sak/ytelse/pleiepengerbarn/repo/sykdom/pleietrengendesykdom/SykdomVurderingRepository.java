package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.Person;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomSøktPeriode;

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

    public PleietrengendeSykdom hentEllerLagreSykdomVurderinger(AktørId pleietrengende, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        final var sykdomPerson = hentEllerLagrePerson(pleietrengende);
        return hentEllerLagre(new PleietrengendeSykdom(sykdomPerson, opprettetAv, opprettetTidspunkt));
    }

    public void lagre(PleietrengendeSykdomVurdering vurdering, AktørId pleietrengende) {
        final var sykdomPerson = hentEllerLagrePerson(pleietrengende);
        final var sykdomVurderinger = new PleietrengendeSykdom(sykdomPerson, vurdering.getOpprettetAv(), vurdering.getOpprettetTidspunkt());
        lagre(vurdering, sykdomVurderinger);
    }

    public void lagre(PleietrengendeSykdomVurdering vurdering, PleietrengendeSykdom pleietrengendeSykdom) {
        if (pleietrengendeSykdom.getId() == null) {
            pleietrengendeSykdom = hentEllerLagre(pleietrengendeSykdom);
        }

        final PleietrengendeSykdomVurdering sisteVurdering = pleietrengendeSykdom.getSisteVurdering();
        vurdering.setSykdomVurderinger(pleietrengendeSykdom);
        vurdering.setRangering((sisteVurdering == null) ? 0L : sisteVurdering.getRangering() + 1L);
        entityManager.persist(vurdering);
        entityManager.flush();
    }

    public void lagre(PleietrengendeSykdomVurderingVersjon versjon) {
        entityManager.persist(versjon);
        entityManager.flush();
    }

    public void lagre(PleietrengendeSykdomVurderingVersjonBesluttet besluttet) {
        String sql = "INSERT INTO pleietrengende_sykdom_vurdering_versjon_besluttet" +
            " (pleietrengende_sykdom_vurdering_versjon_id, endret_av, endret_tid)" +
            " VALUES(:id, :endretAv, :endretTid)" +
            " ON CONFLICT DO NOTHING";

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("id", besluttet.getSykdomVurderingVersjon().getId())
            .setParameter("endretAv", besluttet.getEndretAv())
            .setParameter("endretTid", besluttet.getEndretTidspunkt());


        query.executeUpdate();
        entityManager.flush();
    }

    public boolean hentErBesluttet(PleietrengendeSykdomVurderingVersjon vurderingVersjon) {
        PleietrengendeSykdomVurderingVersjonBesluttet besluttet = entityManager.find(PleietrengendeSykdomVurderingVersjonBesluttet.class, vurderingVersjon.getId());

        return besluttet != null;
    }

    public Optional<PleietrengendeSykdomVurdering> hentVurdering(AktørId pleietrengende, Long vurderingId) {
        final TypedQuery<PleietrengendeSykdomVurdering> q = entityManager.createQuery(
            "SELECT v " +
            "From PleietrengendeSykdomVurdering as v " +
                "inner join v.pleietrengendeSykdom as sv " +
                "inner join sv.person as p " +
            "where p.aktørId = :aktørId " +
                "and v.id = :vurderingId"
            , PleietrengendeSykdomVurdering.class);
        q.setParameter("aktørId", pleietrengende); // Sjekk mot pleietrengende gjøres av sikkerhetsgrunner.
        q.setParameter("vurderingId", vurderingId);

        return q.getResultList().stream().findFirst();
    }

    public List<Saksnummer> hentAlleSaksnummer(AktørId pleietrengende) {
        final TypedQuery<Saksnummer> q = entityManager.createQuery(
            "SELECT distinct sgb.saksnummer "
                + "FROM MedisinskGrunnlag as sgb "
                +   "inner join sgb.pleietrengende as p "
                + "where p.aktørId = :aktørId"
                , Saksnummer.class);

        q.setParameter("aktørId", pleietrengende);

        return q.getResultList();
    }

    public List<SykdomSøktPeriode> hentAlleSøktePerioder(Saksnummer saksnummer) {
        final TypedQuery<SykdomSøktPeriode> q = entityManager.createQuery(
            "SELECT ssp " +
                "FROM MedisinskGrunnlag as sgb " +
                "   inner join sgb.grunnlagsdata as sg " +
                "   inner join sg.søktePerioder as ssp " +
                "where sgb.saksnummer = :saksnummer "
                , SykdomSøktPeriode.class);
        q.setParameter("saksnummer", saksnummer);

        return q.getResultList();
    }

     public LocalDateTimeline<Set<Saksnummer>> hentSaksnummerForSøktePerioder(AktørId pleietrengendeAktørId) {
        final Collection<Saksnummer> saksnummere = hentAlleSaksnummer(pleietrengendeAktørId);
        final Collection<LocalDateSegment<Set<Saksnummer>>> segments = new ArrayList<>();

        for (Saksnummer saksnummer : saksnummere) {
            final Collection<SykdomSøktPeriode> søktePerioder = hentAlleSøktePerioder(saksnummer);
            for (SykdomSøktPeriode periode : søktePerioder) {
                Set<Saksnummer> sett = new HashSet<>();
                sett.add(saksnummer);
                segments.add(new LocalDateSegment<Set<Saksnummer>>(periode.getFom(), periode.getTom(), sett));
            }
        }

        final LocalDateTimeline<Set<Saksnummer>> tidslinje = new LocalDateTimeline<>(segments, new LocalDateSegmentCombinator<Set<Saksnummer>, Set<Saksnummer>, Set<Saksnummer>>() {
            @Override
            public LocalDateSegment<Set<Saksnummer>> combine(LocalDateInterval datoInterval, LocalDateSegment<Set<Saksnummer>> datoSegment, LocalDateSegment<Set<Saksnummer>> datoSegment2) {
                Set<Saksnummer> kombinerteSaksnumre = new HashSet<>(datoSegment.getValue());
                kombinerteSaksnumre.addAll(datoSegment2.getValue());

                return new LocalDateSegment<>(datoInterval, kombinerteSaksnumre);
            }
        });

        return tidslinje.compress();
    }

    public List<PleietrengendeSykdomVurderingVersjon> hentVurderingMedVersjonerForBehandling(UUID behandlingUuid, Long vurderingId) {
        final TypedQuery<PleietrengendeSykdomVurderingVersjon> q = entityManager.createQuery(
                "SELECT vv " +
                "From MedisinskGrunnlag as sgb " +
                    "inner join sgb.grunnlagsdata as sg " +
                    "inner join sg.vurderinger as vv " +
                    "inner join vv.sykdomVurdering as v " +
                "where sgb.behandlingUuid = :behandlingUuid " +
                    "and v.id = :vurderingId " +
                    "and sgb.versjon = " +
                        "( select max(sgb2.versjon) " +
                        "From MedisinskGrunnlag as sgb2 " +
                        "where sgb2.behandlingUuid = sgb.behandlingUuid )"
                , PleietrengendeSykdomVurderingVersjon.class);
            q.setParameter("vurderingId", vurderingId);
            q.setParameter("behandlingUuid", behandlingUuid);
        final var versjonBruktIGrunnlagOpt = q.getResultList().stream().findFirst();
        if (versjonBruktIGrunnlagOpt.isEmpty()) {
            return List.of();
        }
        final var versjonBruktIGrunnlag = versjonBruktIGrunnlagOpt.get();

        final TypedQuery<PleietrengendeSykdomVurdering> q2 = entityManager.createQuery(
                "SELECT v " +
                "From PleietrengendeSykdomVurdering as v " +
                "where v.id = :vurderingId"
                , PleietrengendeSykdomVurdering.class);
            q2.setParameter("vurderingId", vurderingId);
        final var sykdomVurdering = q2.getResultList().stream().findFirst().get();

        return sykdomVurdering.getSykdomVurderingVersjoner().stream()
                .filter(vv -> vv.getVersjon() <= versjonBruktIGrunnlag.getVersjon())
                .collect(Collectors.toList());
    }

    public Collection<PleietrengendeSykdomVurderingVersjon> hentBehandlingVurderingerFor(SykdomVurderingType sykdomVurderingType, UUID behandlingUuid) {
        final TypedQuery<PleietrengendeSykdomVurderingVersjon> q = entityManager.createQuery(
            "SELECT vv " +
            "From MedisinskGrunnlag as sgb " +
                "inner join sgb.grunnlagsdata as sg " +
                "inner join sg.vurderinger as vv " +
                "inner join vv.pleietrengendeSykdomVurdering as v " +
            "where sgb.behandlingUuid = :behandlingUuid " +
                "and v.type = :sykdomVurderingType " +
                "and sgb.versjon = " +
                    "( select max(sgb2.versjon) " +
                    "From MedisinskGrunnlag as sgb2 " +
                    "where sgb2.behandlingUuid = sgb.behandlingUuid )"
            , PleietrengendeSykdomVurderingVersjon.class);
        q.setParameter("sykdomVurderingType", sykdomVurderingType);
        q.setParameter("behandlingUuid", behandlingUuid);
        return q.getResultList();
    }

    public Collection<PleietrengendeSykdomVurderingVersjon> hentSisteVurderingerFor(SykdomVurderingType sykdomVurderingType,
                                                                                    AktørId pleietrengende) {
        final TypedQuery<PleietrengendeSykdomVurderingVersjon> q = entityManager.createQuery(
            "SELECT vv " +
            "From PleietrengendeSykdomVurderingVersjon as vv " +
                "inner join vv.pleietrengendeSykdomVurdering as v " +
                "inner join v.pleietrengendeSykdom as sv " +
                "inner join sv.person as p " +
            "where p.aktørId = :aktørId " +
                "and v.type = :sykdomVurderingType " +
                "and vv.versjon = " +
                    "( select max(vv2.versjon) " +
                    "From PleietrengendeSykdomVurderingVersjon vv2 " +
                    "where vv2.pleietrengendeSykdomVurdering = vv.pleietrengendeSykdomVurdering )"
            , PleietrengendeSykdomVurderingVersjon.class);
        q.setParameter("sykdomVurderingType", sykdomVurderingType);
        q.setParameter("aktørId", pleietrengende);
        return q.getResultList();
    }


    public PleietrengendeSykdomVurderingVersjon hentVurderingVersjon(Long vurderingVersjonId) {
        return null;
    }

    /*
    public void lagre(SykdomVurderinger vurderinger) {
        entityManager.persist(vurderinger);
        entityManager.flush();
    }
    */

    public Person hentEllerLagrePerson(AktørId aktørId) {
        return hentEllerLagre(new Person(aktørId, null));
    }

    public Person hentEllerLagre(Person person) {
        final EntityManager innerEntityManager = entityManager.getEntityManagerFactory().createEntityManager();
        final EntityTransaction transaction = innerEntityManager.getTransaction();
        transaction.begin();
        try {
            final Query q = innerEntityManager.createNativeQuery("INSERT INTO PERSON (ID, AKTOER_ID, NORSK_IDENTITETSNUMMER) VALUES (nextval('SEQ_PERSON'), :aktorId, :norskIdentitetsnummer) ON CONFLICT DO NOTHING");
            q.setParameter("aktorId", person.getAktørId().getId());
            q.setParameter("norskIdentitetsnummer", person.getNorskIdentitetsnummer());
            q.executeUpdate();
            transaction.commit();
        } catch (Throwable t) {
            transaction.rollback();
            throw t;
        } finally {
            innerEntityManager.close();
        }

        return findPerson(person.getAktørId());
    }

    PleietrengendeSykdom hentEllerLagre(PleietrengendeSykdom pleietrengendeSykdom) {
        if (pleietrengendeSykdom.getPerson().getId() == null) {
            pleietrengendeSykdom.setPerson(hentEllerLagre(pleietrengendeSykdom.getPerson()));
        }

        final EntityManager innerEntityManager = entityManager.getEntityManagerFactory().createEntityManager();
        final EntityTransaction transaction = innerEntityManager.getTransaction();
        transaction.begin();
        try {
            final Query q = innerEntityManager.createNativeQuery("INSERT INTO PLEIETRENGENDE_SYKDOM (ID, PLEIETRENGENDE_PERSON_ID, OPPRETTET_AV, OPPRETTET_TID) VALUES (nextval('SEQ_PLEIETRENGENDE_SYKDOM'), :personId, :opprettetAv, :opprettetTid) ON CONFLICT DO NOTHING");
            q.setParameter("personId", pleietrengendeSykdom.getPerson().getId());
            q.setParameter("opprettetAv", pleietrengendeSykdom.getOpprettetAv());
            q.setParameter("opprettetTid", pleietrengendeSykdom.getOpprettetTidspunkt());
            q.executeUpdate();
            transaction.commit();
        } catch (Throwable t) {
            transaction.rollback();
            throw t;
        } finally {
            innerEntityManager.close();
        }

        return hentVurderingerForBarn(pleietrengendeSykdom.getPerson().getAktørId()).get();
    }

    private Optional<PleietrengendeSykdom> hentVurderingerForBarn(AktørId pleietrengende) {
        final TypedQuery<PleietrengendeSykdom> q = entityManager.createQuery(
            "select sv " +
                "From PleietrengendeSykdom as sv " +
                    "inner join sv.person as p " +
                "where p.aktørId = :aktørId"
            , PleietrengendeSykdom.class);
        q.setParameter("aktørId", pleietrengende);

        return q.getResultList().stream().findFirst();
    }

    private Person findPerson(AktørId aktørId) {
        final TypedQuery<Person> q = entityManager.createQuery("select p From Person p where p.aktørId = :aktørId", Person.class);
        q.setParameter("aktørId", aktørId);
        return q.getResultList().stream().findFirst().orElse(null);
    }


    public LocalDateTimeline<PleietrengendeSykdomVurderingVersjon> getVurderingstidslinjeFor(SykdomVurderingType type, UUID behandlingUuid) {
        return PleietrengendeTidslinjeUtils.tilTidslinje(hentBehandlingVurderingerFor(type, behandlingUuid));
    }

    public LocalDateTimeline<PleietrengendeSykdomVurderingVersjon> getSisteVurderingstidslinjeFor(SykdomVurderingType type, AktørId pleietrengende) {
        return PleietrengendeTidslinjeUtils.tilTidslinje(hentSisteVurderingerFor(type, pleietrengende));
    }

    public List<SykdomPeriodeMedEndring> finnEndringer(LocalDateTimeline<PleietrengendeSykdomVurderingVersjon> tidslinje, PleietrengendeSykdomVurderingVersjon nyEndring) {
        final LocalDateTimeline<PleietrengendeSykdomVurderingVersjon> endret = new LocalDateTimeline<>(nyEndring.getPerioder().stream().map(p -> new LocalDateSegment<PleietrengendeSykdomVurderingVersjon>(p.getFom(), p.getTom(), p.getVurderingVersjon())).collect(Collectors.toList()));
        return endret.combine(tidslinje, new LocalDateSegmentCombinator<PleietrengendeSykdomVurderingVersjon, PleietrengendeSykdomVurderingVersjon, SykdomPeriodeMedEndring>() {
            @Override
            public LocalDateSegment<SykdomPeriodeMedEndring> combine(LocalDateInterval datoInterval,
                                                                     LocalDateSegment<PleietrengendeSykdomVurderingVersjon> datoSegment, LocalDateSegment<PleietrengendeSykdomVurderingVersjon> datoSegment2) {
                if (datoSegment2 == null) {
                    return null;
                }
                var nyVersjon = datoSegment.getValue();
                var gammelVersjon = datoSegment2.getValue();
                if (nyVersjon.getSykdomVurdering().getId() != null
                        && nyVersjon.getSykdomVurdering().getId().equals(gammelVersjon.getSykdomVurdering().getId())) {
                    return null;
                }

                final boolean endrerVurderingSammeBehandling = nyVersjon.getEndretForSøkersBehandlingUuid().equals(gammelVersjon.getEndretForSøkersBehandlingUuid());
                final boolean endrerAnnenVurdering = !endrerVurderingSammeBehandling;
                return new LocalDateSegment<>(datoInterval, new SykdomPeriodeMedEndring(new Periode(datoInterval.getFomDato(), datoInterval.getTomDato()), endrerVurderingSammeBehandling, endrerAnnenVurdering, gammelVersjon));
            }
        }, JoinStyle.LEFT_JOIN).compress().stream().map(l -> l.getValue()).collect(Collectors.toList());
    }
}
