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
    public SykdomGrunnlagRepository(EntityManager entityManager, SykdomVurderingRepository sykdomVurderingRepository,  SykdomDokumentRepository sykdomDokumentRepository) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.sykdomVurderingRepository = Objects.requireNonNull(sykdomVurderingRepository, "sykdomVurderingRepository");
        this.sykdomDokumentRepository = Objects.requireNonNull(sykdomDokumentRepository, "sykdomDokumentRepository");
    }

    
    public SykdomGrunnlagBehandling opprettGrunnlag(Saksnummer saksnummer, UUID behandlingUuid, AktørId søkerAktørId, AktørId pleietrengendeAktørId, List<Periode> vurderingsperioder) {
        final Optional<SykdomGrunnlagBehandling> grunnlagFraForrigeBehandling = hentGrunnlagFraForrigeBehandling(saksnummer, behandlingUuid);
        final Optional<SykdomGrunnlagBehandling> forrigeVersjon = hentGrunnlagForBehandling(behandlingUuid);
        
        final LocalDateTime opprettetTidspunkt = LocalDateTime.now();
               
        final LocalDateTimeline<Boolean> søktePerioderFraForrigeBehandling = hentSøktePerioderFraForrigeBehandling(grunnlagFraForrigeBehandling);
        final LocalDateTimeline<Boolean> vurderingsperioderTidslinje = toLocalDateTimeline(vurderingsperioder);
        final LocalDateTimeline<Boolean> søktePerioderTidslinje = søktePerioderFraForrigeBehandling.union(vurderingsperioderTidslinje, (interval, s1, s2) -> new LocalDateSegment<>(interval, true)).compress();
        
        final List<Periode> søktePerioder = toPeriodeList(søktePerioderTidslinje);
        final List<Periode> revurderingsperioder = toPeriodeList(kunPerioderSomIkkeFinnesI(søktePerioderTidslinje, søktePerioderFraForrigeBehandling));
               
        final List<SykdomVurderingVersjon> vurderinger = hentVurderinger(pleietrengendeAktørId);
        
        final SykdomInnleggelser innleggelser = sykdomDokumentRepository.hentInnleggelseOrNull(pleietrengendeAktørId);
        final SykdomDiagnosekoder diagnosekoder = sykdomDokumentRepository.hentDiagnosekoderOrNull(pleietrengendeAktørId);
        
        final SykdomGrunnlag grunnlag = new SykdomGrunnlag(
            UUID.randomUUID(),
            søktePerioder.stream().map(p -> new SykdomSøktPeriode(p.getFom(), p.getTom())).collect(Collectors.toList()),
            revurderingsperioder.stream().map(p -> new SykdomRevurderingPeriode(p.getFom(), p.getTom())).collect(Collectors.toList()),
            vurderinger,
            innleggelser,
            diagnosekoder,
            "VL",
            opprettetTidspunkt
        );
        
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
        
        final List<SykdomVurderingVersjon> vurderinger = new ArrayList<>(ktpVurderinger.stream().map(s -> s.getValue()).distinct().collect(Collectors.toList()));
        vurderinger.addAll(tooVurderinger.stream().map(s -> s.getValue()).distinct().collect(Collectors.toList()));
        return vurderinger;
    }

    private LocalDateTimeline<Boolean> hentSøktePerioderFraForrigeBehandling(
            final Optional<SykdomGrunnlagBehandling> grunnlagFraForrigeBehandling) {
        final LocalDateTimeline<Boolean> gamleSøktePerioder = grunnlagFraForrigeBehandling.map(sgb -> new LocalDateTimeline<Boolean>(
                    sgb.getGrunnlag().getSøktePerioder().stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true)).collect(Collectors.toList())
                )).orElse(new LocalDateTimeline<Boolean>(Collections.emptyList()));
        return gamleSøktePerioder;
    }
    
    private Optional<SykdomGrunnlagBehandling> hentGrunnlagFraForrigeBehandling(Saksnummer saksnummer, UUID behandlingUuid) {
        return hentSisteBehandlingMedUnntakAv(saksnummer, behandlingUuid)
                .map(forrigeBehandling -> hentGrunnlagForBehandling(forrigeBehandling).get());
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
    
    Optional<SykdomGrunnlagBehandling> hentGrunnlagForBehandling(UUID behandlingUuid) {
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
    
}
