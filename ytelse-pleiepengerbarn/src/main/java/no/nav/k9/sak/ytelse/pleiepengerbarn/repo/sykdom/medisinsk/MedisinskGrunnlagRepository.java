package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.Person;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.PersonRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomVurderingVersjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.SykdomVurderingRepository;

@Dependent
public class MedisinskGrunnlagRepository {

    private EntityManager entityManager;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private PersonRepository personRepository;
    private PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository;

    MedisinskGrunnlagRepository() {
        // CDI
    }

    @Inject
    public MedisinskGrunnlagRepository(
            EntityManager entityManager,
            SykdomVurderingRepository sykdomVurderingRepository,
            PersonRepository personRepository,
            PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.sykdomVurderingRepository = Objects.requireNonNull(sykdomVurderingRepository, "sykdomVurderingRepository");
        this.personRepository = Objects.requireNonNull(personRepository, "personRepository");
        this.pleietrengendeSykdomDokumentRepository = Objects.requireNonNull(pleietrengendeSykdomDokumentRepository, "sykdomDokumentRepository");
    }

    public List<Saksnummer> hentAlleSaksnummer(AktørId pleietrengende) {
        final TypedQuery<Saksnummer> q = entityManager.createQuery(
            "SELECT distinct sgb.saksnummer "
                + "FROM MedisinskGrunnlag as sgb "
                + "inner join sgb.pleietrengende as p "
                + "where p.aktørId = :aktørId"
            , Saksnummer.class);

        q.setParameter("aktørId", pleietrengende);

        return q.getResultList();
    }

    public List<MedisinskGrunnlagsdataSøktPeriode> hentAlleSøktePerioder(Saksnummer saksnummer) {
        final TypedQuery<MedisinskGrunnlagsdataSøktPeriode> q = entityManager.createQuery(
            "SELECT ssp " +
                "FROM MedisinskGrunnlag as sgb " +
                "   inner join sgb.grunnlagsdata as sg " +
                "   inner join sg.søktePerioder as ssp " +
                "where sgb.saksnummer = :saksnummer "
            , MedisinskGrunnlagsdataSøktPeriode.class);
        q.setParameter("saksnummer", saksnummer);

        return q.getResultList();
    }

    public LocalDateTimeline<Set<Saksnummer>> hentSaksnummerForSøktePerioder(AktørId pleietrengendeAktørId) {
        final Collection<Saksnummer> saksnummere = hentAlleSaksnummer(pleietrengendeAktørId);
        final Collection<LocalDateSegment<Set<Saksnummer>>> segments = new ArrayList<>();

        for (Saksnummer saksnummer : saksnummere) {
            final Collection<MedisinskGrunnlagsdataSøktPeriode> søktePerioder = hentAlleSøktePerioder(saksnummer);
            for (MedisinskGrunnlagsdataSøktPeriode periode : søktePerioder) {
                Set<Saksnummer> sett = new HashSet<>();
                sett.add(saksnummer);
                segments.add(new LocalDateSegment<>(periode.getFom(), periode.getTom(), sett));
            }
        }

        final LocalDateTimeline<Set<Saksnummer>> tidslinje = new LocalDateTimeline<>(segments, TidslinjeUtil::union);

        return tidslinje.compress();
    }

    public MedisinskGrunnlagsdata utledGrunnlag(Saksnummer saksnummer, UUID behandlingUuid, AktørId pleietrengendeAktørId, NavigableSet<DatoIntervallEntitet> vurderingsperioder, NavigableSet<DatoIntervallEntitet> søknadsperioderSomSkalFjernes) {
        final Optional<MedisinskGrunnlag> grunnlagFraForrigeBehandling = hentGrunnlagFraForrigeBehandling(saksnummer, behandlingUuid);

        return utledGrunnlag(pleietrengendeAktørId, vurderingsperioder, søknadsperioderSomSkalFjernes, grunnlagFraForrigeBehandling, behandlingUuid);
    }

    private MedisinskGrunnlagsdata utledGrunnlag(AktørId pleietrengendeAktørId, NavigableSet<DatoIntervallEntitet> vurderingsperioder, NavigableSet<DatoIntervallEntitet> søknadsperioderSomSkalFjernes, Optional<MedisinskGrunnlag> grunnlagFraForrigeBehandling, UUID behandlingUuid) {
        final var opprettetTidspunkt = LocalDateTime.now();

        final var søktePerioderFraForrigeBehandling = TidslinjeUtil.kunPerioderSomIkkeFinnesI(hentSøktePerioderFraForrigeBehandling(grunnlagFraForrigeBehandling), TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(søknadsperioderSomSkalFjernes)));
        final var vurderingsperioderTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(vurderingsperioder);
        final var søktePerioderTidslinje = søktePerioderFraForrigeBehandling.union(vurderingsperioderTidslinje, (interval, s1, s2) -> new LocalDateSegment<>(interval, true)).compress();

        final var søktePerioder = TidslinjeUtil.tilDatoIntervallEntiteter(søktePerioderTidslinje);

        final var vurderinger = hentVurderinger(pleietrengendeAktørId);

        final var innleggelser = pleietrengendeSykdomDokumentRepository.hentInnleggelseOrNull(pleietrengendeAktørId);
        final var diagnosekoder = pleietrengendeSykdomDokumentRepository.hentDiagnosekoderOrNull(pleietrengendeAktørId);

        final var sykdomDokumenter = pleietrengendeSykdomDokumentRepository.hentAlleDokumenterFor(pleietrengendeAktørId);
        final var godkjenteLegeerklæringer = sykdomDokumenter.stream()
            .filter(d -> d.getType() == SykdomDokumentType.LEGEERKLÆRING_SYKEHUS)
            .collect(Collectors.toList());

        final var harAndreMedisinskeDokumenter = !sykdomDokumenter.isEmpty();

        List<PleietrengendeSykdomDokument> sykdomsdokumenterPåBehandling = sykdomDokumenter.stream().filter(it -> it.getSøkersBehandlingUuid().equals(behandlingUuid)).toList();
        return new MedisinskGrunnlagsdata(
            UUID.randomUUID(),
            søktePerioder.stream().map(p -> new MedisinskGrunnlagsdataSøktPeriode(p.getFomDato(), p.getTomDato())).collect(Collectors.toList()),
            vurderinger,
            godkjenteLegeerklæringer,
            sykdomsdokumenterPåBehandling,
            harAndreMedisinskeDokumenter,
            innleggelser,
            diagnosekoder,
            "VL",
            opprettetTidspunkt
        );
    }


    public MedisinskGrunnlag utledOgLagreGrunnlag(Saksnummer saksnummer, UUID behandlingUuid, AktørId søkerAktørId, AktørId pleietrengendeAktørId, NavigableSet<DatoIntervallEntitet> vurderingsperioder, NavigableSet<DatoIntervallEntitet> søknadsperioderSomSkalFjernes) {
        final Optional<MedisinskGrunnlag> grunnlagFraForrigeBehandling = hentGrunnlagFraForrigeBehandling(saksnummer, behandlingUuid);
        final Optional<MedisinskGrunnlag> forrigeVersjon = hentGrunnlagForBehandling(behandlingUuid);

        final LocalDateTime opprettetTidspunkt = LocalDateTime.now();

        final MedisinskGrunnlagsdata grunnlag = utledGrunnlag(pleietrengendeAktørId, vurderingsperioder, søknadsperioderSomSkalFjernes, grunnlagFraForrigeBehandling, behandlingUuid);

        final Person søker = personRepository.hentEllerLagrePerson(søkerAktørId);
        final Person pleietrengende = personRepository.hentEllerLagrePerson(pleietrengendeAktørId);
        final long behandlingsnummer = grunnlagFraForrigeBehandling.map(sgb -> sgb.getBehandlingsnummer() + 1L).orElse(0L);
        final long versjon = forrigeVersjon.map(sgb -> sgb.getVersjon() + 1L).orElse(0L);
        final MedisinskGrunnlag sgb = new MedisinskGrunnlag(grunnlag, søker, pleietrengende, saksnummer, behandlingUuid, behandlingsnummer, versjon, "VL", opprettetTidspunkt);

        entityManager.persist(sgb);
        entityManager.flush();

        return sgb;
    }

    private List<PleietrengendeSykdomVurderingVersjon> hentVurderinger(AktørId pleietrengende) {
        final LocalDateTimeline<PleietrengendeSykdomVurderingVersjon> ktpVurderinger = sykdomVurderingRepository.getSisteVurderingstidslinjeFor(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, pleietrengende);
        final LocalDateTimeline<PleietrengendeSykdomVurderingVersjon> tooVurderinger = sykdomVurderingRepository.getSisteVurderingstidslinjeFor(SykdomVurderingType.TO_OMSORGSPERSONER, pleietrengende);
        final LocalDateTimeline<PleietrengendeSykdomVurderingVersjon> sluVurderinger = sykdomVurderingRepository.getSisteVurderingstidslinjeFor(SykdomVurderingType.LIVETS_SLUTTFASE, pleietrengende);
        final LocalDateTimeline<PleietrengendeSykdomVurderingVersjon> lasVurderinger = sykdomVurderingRepository.getSisteVurderingstidslinjeFor(SykdomVurderingType.LANGVARIG_SYKDOM, pleietrengende);

        final List<PleietrengendeSykdomVurderingVersjon> vurderinger = ktpVurderinger.stream().map(LocalDateSegment::getValue).distinct().collect(Collectors.toCollection(ArrayList::new));
        vurderinger.addAll(tooVurderinger.stream().map(LocalDateSegment::getValue).distinct().toList());
        vurderinger.addAll(sluVurderinger.stream().map(LocalDateSegment::getValue).distinct().toList());
        vurderinger.addAll(lasVurderinger.stream().map(LocalDateSegment::getValue).distinct().toList());
        return vurderinger;
    }

    private LocalDateTimeline<Boolean> hentSøktePerioderFraForrigeBehandling(Optional<MedisinskGrunnlag> grunnlagFraForrigeBehandling) {
        return grunnlagFraForrigeBehandling.map(sgb -> new LocalDateTimeline<>(sgb.getGrunnlagsdata()
                .getSøktePerioder()
                .stream()
                .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true))
                .collect(Collectors.toList())))
            .orElse(new LocalDateTimeline<>(Collections.emptyList()));
    }

    public Optional<MedisinskGrunnlag> hentGrunnlagFraForrigeBehandling(Saksnummer saksnummer, UUID behandlingUuid) {
        return hentSisteBehandlingFør(saksnummer, behandlingUuid)
            .map(forrigeBehandling -> hentGrunnlagForBehandling(forrigeBehandling).orElseThrow());
    }

    public boolean harHattGodkjentLegeerklæringMedUnntakAv(AktørId pleietrengende, UUID behandlingUuid) {
        final TypedQuery<Long> q = entityManager.createQuery(
            "select l.id "
                + "from MedisinskGrunnlag as sgb "
                + "  inner join sgb.pleietrengende as p "
                + "  inner join sgb.grunnlagsdata as g "
                + "  inner join g.godkjenteLegeerklæringer as l "
                + "where p.aktørId = :aktørId "
                + "  and sgb.behandlingUuid <> :behandlingUuid"
            , Long.class);

        q.setParameter("aktørId", pleietrengende);
        q.setParameter("behandlingUuid", behandlingUuid);

        return !q.getResultList().isEmpty();
    }

    Optional<UUID> hentSisteBehandlingFør(Saksnummer saksnummer, UUID behandlingUuid) {
        final TypedQuery<UUID> q = entityManager.createQuery(
            "Select sgb.behandlingUuid "
                + "From MedisinskGrunnlag as sgb "
                + "Where sgb.saksnummer = :saksnummer "
                + "  And sgb.behandlingsnummer = ( "
                + "    Select max(sgb2.behandlingsnummer) "
                + "    From MedisinskGrunnlag as sgb2 "
                + "    Where sgb2.saksnummer = :saksnummer "
                + "      And sgb2.behandlingUuid <> :behandlingUuid "
                + " And (sgb2.behandlingsnummer < (Select max(sgb3.behandlingsnummer)  "
                + "                    From MedisinskGrunnlag as sgb3 "
                + "                   Where sgb3.saksnummer = :saksnummer "
                + "                   And sgb3.behandlingUuid = :behandlingUuid) "
                + " Or not exists (Select 1  From MedisinskGrunnlag as sgb3 Where sgb3.saksnummer = :saksnummer And sgb3.behandlingUuid = :behandlingUuid))"
                + "  )"
                + "  And sgb.behandlingUuid <> :behandlingUuid"
            , UUID.class);

        q.setParameter("saksnummer", saksnummer);
        q.setParameter("behandlingUuid", behandlingUuid);

        return q.getResultList().stream().findFirst();
    }

    public Optional<MedisinskGrunnlag> hentGrunnlagForBehandling(UUID behandlingUuid) {
        final TypedQuery<MedisinskGrunnlag> q = entityManager.createQuery(
            "SELECT sgb "
                + "FROM MedisinskGrunnlag as sgb "
                + "where sgb.behandlingUuid = :behandlingUuid "
                + "  and sgb.versjon = ( "
                + "    select max(sgb2.versjon) "
                + "    From MedisinskGrunnlag as sgb2 "
                + "    where sgb2.behandlingUuid = sgb.behandlingUuid "
                + "  )"
            , MedisinskGrunnlag.class);

        q.setParameter("behandlingUuid", behandlingUuid);

        return q.getResultList().stream().findFirst();
    }

}
