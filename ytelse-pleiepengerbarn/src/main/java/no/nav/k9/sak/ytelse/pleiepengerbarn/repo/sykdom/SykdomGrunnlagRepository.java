package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import static no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils.kunPerioderSomIkkeFinnesI;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils.toLocalDateTimeline;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils.toPeriodeList;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class SykdomGrunnlagRepository {

    private EntityManager entityManager;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomDokumentRepository sykdomDokumentRepository;

    SykdomGrunnlagRepository() {
        // CDI
    }

    @Inject
    public SykdomGrunnlagRepository(EntityManager entityManager, SykdomVurderingRepository sykdomVurderingRepository, SykdomDokumentRepository sykdomDokumentRepository) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.sykdomVurderingRepository = Objects.requireNonNull(sykdomVurderingRepository, "sykdomVurderingRepository");
        this.sykdomDokumentRepository = Objects.requireNonNull(sykdomDokumentRepository, "sykdomDokumentRepository");
    }

    public SykdomGrunnlag utledGrunnlag(Saksnummer saksnummer, UUID behandlingUuid, AktørId pleietrengendeAktørId, List<Periode> vurderingsperioder, List<Periode> søknadsperioderSomSkalFjernes) {
        final Optional<SykdomGrunnlagBehandling> grunnlagFraForrigeBehandling = hentGrunnlagFraForrigeBehandling(saksnummer, behandlingUuid);

        return utledGrunnlag(pleietrengendeAktørId, vurderingsperioder, søknadsperioderSomSkalFjernes, grunnlagFraForrigeBehandling);
    }

    private SykdomGrunnlag utledGrunnlag(AktørId pleietrengendeAktørId, List<Periode> vurderingsperioder, List<Periode> søknadsperioderSomSkalFjernes, Optional<SykdomGrunnlagBehandling> grunnlagFraForrigeBehandling) {
        final LocalDateTime opprettetTidspunkt = LocalDateTime.now();

        final LocalDateTimeline<Boolean> søktePerioderFraForrigeBehandling = kunPerioderSomIkkeFinnesI(hentSøktePerioderFraForrigeBehandling(grunnlagFraForrigeBehandling), toLocalDateTimeline(søknadsperioderSomSkalFjernes));
        final LocalDateTimeline<Boolean> vurderingsperioderTidslinje = toLocalDateTimeline(vurderingsperioder);
        final LocalDateTimeline<Boolean> søktePerioderTidslinje = søktePerioderFraForrigeBehandling.union(vurderingsperioderTidslinje, (interval, s1, s2) -> new LocalDateSegment<>(interval, true)).compress();

        final List<Periode> søktePerioder = toPeriodeList(søktePerioderTidslinje);
        final List<Periode> revurderingsperioder = toPeriodeList(kunPerioderSomIkkeFinnesI(søktePerioderTidslinje, søktePerioderFraForrigeBehandling));

        final List<SykdomVurderingVersjon> vurderinger = hentVurderinger(pleietrengendeAktørId);

        final SykdomInnleggelser innleggelser = sykdomDokumentRepository.hentInnleggelseOrNull(pleietrengendeAktørId);
        final SykdomDiagnosekoder diagnosekoder = sykdomDokumentRepository.hentDiagnosekoderOrNull(pleietrengendeAktørId);

        List<SykdomDokument> godkjenteLegeerklæringer = sykdomDokumentRepository.hentAlleDokumenterFor(pleietrengendeAktørId).stream()
                .filter(d -> d.getType() == SykdomDokumentType.LEGEERKLÆRING_SYKEHUS)
                .collect(Collectors.toList());
        
        return new SykdomGrunnlag(
            UUID.randomUUID(),
            søktePerioder.stream().map(p -> new SykdomSøktPeriode(p.getFom(), p.getTom())).collect(Collectors.toList()),
            revurderingsperioder.stream().map(p -> new SykdomRevurderingPeriode(p.getFom(), p.getTom())).collect(Collectors.toList()),
            vurderinger,
            godkjenteLegeerklæringer,
            innleggelser,
            diagnosekoder,
            "VL",
            opprettetTidspunkt
        );
    }


    public SykdomGrunnlagBehandling utledOgLagreGrunnlag(Saksnummer saksnummer, UUID behandlingUuid, AktørId søkerAktørId, AktørId pleietrengendeAktørId, List<Periode> vurderingsperioder, List<Periode> søknadsperioderSomSkalFjernes) {
        final Optional<SykdomGrunnlagBehandling> grunnlagFraForrigeBehandling = hentGrunnlagFraForrigeBehandling(saksnummer, behandlingUuid);
        final Optional<SykdomGrunnlagBehandling> forrigeVersjon = hentGrunnlagForBehandling(behandlingUuid);

        final LocalDateTime opprettetTidspunkt = LocalDateTime.now();

        final SykdomGrunnlag grunnlag = utledGrunnlag(pleietrengendeAktørId, vurderingsperioder, søknadsperioderSomSkalFjernes, grunnlagFraForrigeBehandling);

        final SykdomPerson søker = sykdomVurderingRepository.hentEllerLagrePerson(søkerAktørId);
        final SykdomPerson pleietrengende = sykdomVurderingRepository.hentEllerLagrePerson(pleietrengendeAktørId);
        final long behandlingsnummer = grunnlagFraForrigeBehandling.map(sgb -> sgb.getBehandlingsnummer() + 1L).orElse(0L);
        final long versjon = forrigeVersjon.map(sgb -> sgb.getVersjon() + 1L).orElse(0L);
        final SykdomGrunnlagBehandling sgb = new SykdomGrunnlagBehandling(grunnlag, søker, pleietrengende, saksnummer, behandlingUuid, behandlingsnummer, versjon, "VL", opprettetTidspunkt);

        entityManager.persist(sgb);
        entityManager.flush();

        return sgb;
    }

    private List<SykdomVurderingVersjon> hentVurderinger(AktørId pleietrengende) {
        final LocalDateTimeline<SykdomVurderingVersjon> ktpVurderinger = sykdomVurderingRepository.getSisteVurderingstidslinjeFor(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, pleietrengende);
        final LocalDateTimeline<SykdomVurderingVersjon> tooVurderinger = sykdomVurderingRepository.getSisteVurderingstidslinjeFor(SykdomVurderingType.TO_OMSORGSPERSONER, pleietrengende);

        final List<SykdomVurderingVersjon> vurderinger = ktpVurderinger.stream().map(LocalDateSegment::getValue).distinct().collect(Collectors.toCollection(ArrayList::new));
        vurderinger.addAll(tooVurderinger.stream().map(LocalDateSegment::getValue).distinct().collect(Collectors.toList()));
        return vurderinger;
    }

    private LocalDateTimeline<Boolean> hentSøktePerioderFraForrigeBehandling(
        final Optional<SykdomGrunnlagBehandling> grunnlagFraForrigeBehandling) {
        final LocalDateTimeline<Boolean> gamleSøktePerioder = grunnlagFraForrigeBehandling.map(sgb -> new LocalDateTimeline<Boolean>(
            sgb.getGrunnlag().getSøktePerioder().stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true)).collect(Collectors.toList())
        )).orElse(new LocalDateTimeline<Boolean>(Collections.emptyList()));
        return gamleSøktePerioder;
    }

    public Optional<SykdomGrunnlagBehandling> hentGrunnlagFraForrigeBehandling(Saksnummer saksnummer, UUID behandlingUuid) {
        return hentSisteBehandlingMedUnntakAv(saksnummer, behandlingUuid)
            .map(forrigeBehandling -> hentGrunnlagForBehandling(forrigeBehandling).orElseThrow());
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(UUID behandlingUuid) {
        var funnetId = hentGrunnlagForBehandling(behandlingUuid)
            .map(SykdomGrunnlagBehandling::getGrunnlag)
            .map(SykdomGrunnlag::getReferanse);

        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(SykdomGrunnlag.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(SykdomGrunnlag.class));
    }
    
    public boolean harHattGodkjentLegeerklæringMedUnntakAv(AktørId pleietrengende, UUID behandlingUuid) {
        final TypedQuery<Long> q = entityManager.createQuery(
                "select l.id "
                + "from SykdomGrunnlagBehandling as sgb "
                + "  inner join sgb.pleietrengende as p "
                + "  inner join sgb.grunnlag as g "
                + "  inner join g.godkjenteLegeerklæringer as l "
                + "where p.aktørId = :aktørId "
                + "  and sgb.behandlingUuid <> :behandlingUuid"
                , Long.class);

        q.setParameter("aktørId", pleietrengende);
        q.setParameter("behandlingUuid", behandlingUuid);

        return !q.getResultList().isEmpty();
    }

    public Optional<UUID> hentSisteBehandling(Saksnummer saksnummer) {
        final TypedQuery<UUID> q = entityManager.createQuery(
            "Select sgb.behandlingUuid "
                + "From SykdomGrunnlagBehandling as sgb "
                + "Where sgb.saksnummer = :saksnummer "
                + "  And sgb.behandlingsnummer = ( "
                + "    Select max(sgb2.behandlingsnummer) "
                + "    From SykdomGrunnlagBehandling as sgb2 "
                + "    Where sgb2.saksnummer = :saksnummer "
                + "  ) "
            , UUID.class);

        q.setParameter("saksnummer", saksnummer);

        return q.getResultList().stream().findFirst();
    }

    Optional<UUID> hentSisteBehandlingMedUnntakAv(Saksnummer saksnummer, UUID behandlingUuid) {
        final TypedQuery<UUID> q = entityManager.createQuery(
            "Select sgb.behandlingUuid "
                + "From SykdomGrunnlagBehandling as sgb "
                + "Where sgb.saksnummer = :saksnummer "
                + "  And sgb.behandlingsnummer = ( "
                + "    Select max(sgb2.behandlingsnummer) "
                + "    From SykdomGrunnlagBehandling as sgb2 "
                + "    Where sgb2.saksnummer = :saksnummer "
                + "      And sgb2.behandlingUuid <> :behandlingUuid "
                + "  ) "
                + "  And sgb.behandlingUuid <> :behandlingUuid"
            , UUID.class);

        q.setParameter("saksnummer", saksnummer);
        q.setParameter("behandlingUuid", behandlingUuid);

        return q.getResultList().stream().findFirst();
    }

    public Optional<SykdomGrunnlagBehandling> hentGrunnlagForBehandling(UUID behandlingUuid) {
        final TypedQuery<SykdomGrunnlagBehandling> q = entityManager.createQuery(
            "SELECT sgb "
                + "FROM SykdomGrunnlagBehandling as sgb "
                + "where sgb.behandlingUuid = :behandlingUuid "
                + "  and sgb.versjon = ( "
                + "    select max(sgb2.versjon) "
                + "    From SykdomGrunnlagBehandling as sgb2 "
                + "    where sgb2.behandlingUuid = sgb.behandlingUuid "
                + "  )"
            , SykdomGrunnlagBehandling.class);

        q.setParameter("behandlingUuid", behandlingUuid);

        return q.getResultList().stream().findFirst();
    }

    public Optional<SykdomGrunnlag> hentGrunnlagForId(UUID grunnlagReferanse) {
        Objects.requireNonNull(grunnlagReferanse);
        final TypedQuery<SykdomGrunnlag> q = entityManager.createQuery(
            "SELECT sg "
                + "FROM SykdomGrunnlag as sg "
                + "WHERE sg.id = :id", SykdomGrunnlag.class);

        q.setParameter("id", grunnlagReferanse);

        return q.getResultList().stream().findFirst();
    }

}
