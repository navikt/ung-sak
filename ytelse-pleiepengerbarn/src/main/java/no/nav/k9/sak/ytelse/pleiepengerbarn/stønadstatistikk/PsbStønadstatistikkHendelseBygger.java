package no.nav.k9.sak.ytelse.pleiepengerbarn.stønadstatistikk;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.oppdrag.kontrakt.sporing.HenvisningUtleder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.hendelse.stønadstatistikk.StønadstatistikkHendelseBygger;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkArbeidsforhold;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkDiagnosekode;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkGraderingMotTilsyn;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkHendelse;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkInngangsvilkår;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkPeriode;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkUtbetalingsgrad;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkUtfall;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold.Builder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.MapFraUttaksplan;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDiagnosekode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.stønadstatistikk.StønadstatistikkPeriodetidslinjebygger.InformasjonTilStønadstatistikkHendelse;
import no.nav.pleiepengerbarn.uttak.kontrakter.GraderingMotTilsyn;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PsbStønadstatistikkHendelseBygger implements StønadstatistikkHendelseBygger {

    private static final Logger logger = LoggerFactory.getLogger(PsbStønadstatistikkHendelseBygger.class);
    private StønadstatistikkPeriodetidslinjebygger stønadstatistikkPeriodetidslinjebygger;
    private BehandlingRepository behandlingRepository;
    private AktørTjeneste aktørTjeneste;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private BehandlingVedtakRepository behandlingVedtakRepository;


    @Inject
    public PsbStønadstatistikkHendelseBygger(StønadstatistikkPeriodetidslinjebygger stønadstatistikkPeriodetidslinjebygger,
            BehandlingRepository behandlingRepository,
            AktørTjeneste aktørTjeneste,
            SykdomGrunnlagService sykdomGrunnlagService,
            BehandlingVedtakRepository behandlingVedtakRepository) {
        this.stønadstatistikkPeriodetidslinjebygger = stønadstatistikkPeriodetidslinjebygger;
        this.behandlingRepository = behandlingRepository;
        this.aktørTjeneste = aktørTjeneste;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
    }

    @Override
    public StønadstatistikkHendelse lagHendelse(UUID behandlingUuid) {
        final Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid);
        final BehandlingResultatType behandlingResultatType = behandling.getBehandlingResultatType();
        if (behandlingResultatType.isBehandlingHenlagt() || behandlingResultatType.isBehandlingsresultatHenlagt()) {
            logger.info("Lager ikke StønadstatistikkHendelse siden behandingen er henlagt: " + behandlingUuid.toString());
            return null;
        }

        final PersonIdent søker = aktørTjeneste.hentPersonIdentForAktørId(behandling.getFagsak().getAktørId()).get();
        final PersonIdent pleietrengende= aktørTjeneste.hentPersonIdentForAktørId(behandling.getFagsak().getPleietrengendeAktørId()).get();
        final List<SykdomDiagnosekode> diagnosekoder = hentDiagnosekoder(behandlingUuid);
        final LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> periodetidslinje = stønadstatistikkPeriodetidslinjebygger.lagTidslinjeFor(behandling);

        final UUID forrigeBehandlingUuid = finnForrigeBehandlingUuid(behandling);
        final BehandlingVedtak behandlingVedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId()).orElseThrow();
        final LocalDateTime vedtakstidspunkt = behandlingVedtak.getVedtakstidspunkt();


        final StønadstatistikkHendelse stønadstatistikkHendelse = new StønadstatistikkHendelse(
                behandling.getFagsakYtelseType(),
                søker,
                pleietrengende,
                diagnosekoder.stream().map(d -> new StønadstatistikkDiagnosekode(d.getDiagnosekode())).toList(),
                behandling.getFagsak().getSaksnummer(),
                HenvisningUtleder.utledHenvisning(behandling.getUuid()), // behandlingsreferanse i økonomisystemet: "henvisning"
                behandlingUuid,
                forrigeBehandlingUuid,
                vedtakstidspunkt,
                mapPerioder(periodetidslinje)
                );

        return stønadstatistikkHendelse;
    }

    private List<SykdomDiagnosekode> hentDiagnosekoder(UUID behandlingUuid) {
        final SykdomGrunnlag sykdomGrunnlag = sykdomGrunnlagService.hentGrunnlag(behandlingUuid).getGrunnlag();
        if (sykdomGrunnlag.getDiagnosekoder() == null || sykdomGrunnlag.getDiagnosekoder().getDiagnosekoder() == null) {
            return List.of();
        }
        final List<SykdomDiagnosekode> diagnosekoder = sykdomGrunnlag.getDiagnosekoder().getDiagnosekoder();
        return diagnosekoder;
    }

    private UUID finnForrigeBehandlingUuid(final Behandling behandling) {
        return behandling.getOriginalBehandlingId()
                .map(behandlingId -> behandlingRepository.hentBehandlingHvisFinnes(behandlingId)
                    .map(Behandling::getUuid)
                    .orElse(null)
                )
                .orElse(null);
    }

    private List<StønadstatistikkPeriode> mapPerioder(LocalDateTimeline<InformasjonTilStønadstatistikkHendelse> periodetidslinje) {
        return periodetidslinje.toSegments().stream().map(entry -> mapPeriode(entry)).toList();
    }

    private StønadstatistikkPeriode mapPeriode(LocalDateSegment<InformasjonTilStønadstatistikkHendelse> ds) {
        final UttaksperiodeInfo info = ds.getValue().getUttaksperiodeInfo();
        final BigDecimal bruttoBeregningsgrunnlag = (ds.getValue().getBeregningsgrunnlagDto() != null) ? ds.getValue().getBeregningsgrunnlagDto().getÅrsinntektVisningstall() : BigDecimal.valueOf(-1);
        return new StønadstatistikkPeriode(
                ds.getFom(),
                ds.getTom(),
                mapUtfall(info.getUtfall()),
                info.getUttaksgrad(),
                mapUtbetalingsgrader(info.getUtbetalingsgrader(), ds.getValue().getBeregningsresultatAndeler()),
                info.getSøkersTapteArbeidstid(),
                info.getOppgittTilsyn(),
                mapÅrsaker(info.getårsaker()),
                mapInngangsvilkår(info.getInngangsvilkår()),
                info.getPleiebehov(),
                mapGraderingMotTilsyn(info.getGraderingMotTilsyn()),
                mapUtfall(info.getNattevåk()),
                mapUtfall(info.getBeredskap()),
                info.getSøkersTapteTimer(),
                bruttoBeregningsgrunnlag
                );
    }

    private StønadstatistikkUtfall mapUtfall(Utfall utfall) {
        if (utfall == null) {
            return null;
        }
        switch (utfall) {
        case OPPFYLT: return StønadstatistikkUtfall.OPPFYLT;
        case IKKE_OPPFYLT: return StønadstatistikkUtfall.IKKE_OPPFYLT;
        default: throw new IllegalArgumentException("Utfallet '" + utfall.toString() + "' er ikke støttet.");
        }
    }

    private List<StønadstatistikkUtbetalingsgrad> mapUtbetalingsgrader(List<Utbetalingsgrader> utbetalingsgrader, List<BeregningsresultatAndel> beregningsresultatAndeler) {
        return utbetalingsgrader.stream().map(u -> {
            var a = u.getArbeidsforhold();
            final Arbeidsforhold arbeidsforholdFraUttaksplan = MapFraUttaksplan.buildArbeidsforhold(toUttakArbeidType(u), u);
            final BigDecimal utbetalingsgrad = u.getUtbetalingsgrad();

            /* 
             * Sjekk på om beregningsresultatAndeler != null gjøres grunnet
             * tidligere feil der uttaksgraden ikke ble satt til 0 når det
             * var avslag i beregning.
             * 
             * Vi trenger denne sjekken videre for å kunne støtte full eksport.
             */
            if (beregningsresultatAndeler != null && skalFinnesAndeler(utbetalingsgrad)) {
                final List<BeregningsresultatAndel> andeler = finnAndeler(AktivitetStatus.fraKode(a.getType()), arbeidsforholdFraUttaksplan, beregningsresultatAndeler);
                return andeler.stream().map(andel -> {
                    // TODO: andel.getArbeidsforholdRef().getReferanse() må byttes til eksternReferanse.
                    final String arbeidsforholdRef = (andel.getArbeidsforholdRef() != null) ? andel.getArbeidsforholdRef().getReferanse() : null;
                    final StønadstatistikkArbeidsforhold arbeidsforhold = new StønadstatistikkArbeidsforhold(a.getType(), a.getOrganisasjonsnummer(), a.getAktørId(), arbeidsforholdRef);
                    final int dagsats = andel.getDagsats();
                    return new StønadstatistikkUtbetalingsgrad(
                            andel.getAktivitetStatus().getKode(),
                            arbeidsforhold,
                            u.getNormalArbeidstid(),
                            u.getFaktiskArbeidstid(),
                            utbetalingsgrad,
                            dagsats,
                            andel.erBrukerMottaker()
                            );
                }).toList();
            } else {
                final StønadstatistikkArbeidsforhold arbeidsforhold = new StønadstatistikkArbeidsforhold(a.getType(), a.getOrganisasjonsnummer(), a.getAktørId(), a.getArbeidsforholdId());
                return List.of(new StønadstatistikkUtbetalingsgrad(
                        null,
                        arbeidsforhold,
                        u.getNormalArbeidstid(),
                        u.getFaktiskArbeidstid(),
                        utbetalingsgrad,
                        0,
                        true
                        ));
            }
        }).flatMap(Collection::stream).toList();
    }

    private boolean skalFinnesAndeler(final BigDecimal utbetalingsgrad) {
        return utbetalingsgrad.compareTo(BigDecimal.valueOf(0)) > 0;
    }

    private UttakArbeidType toUttakArbeidType(Utbetalingsgrader data) {
        var type = UttakArbeidType.fraKode(data.getArbeidsforhold().getType());
        if (UttakArbeidType.IKKE_YRKESAKTIV.equals(type)) {
            type = UttakArbeidType.ARBEIDSTAKER;
        }
        return type;
    }

    private List<BeregningsresultatAndel> finnAndeler(AktivitetStatus aktivitetFraUttaksplan, Arbeidsforhold arbeidsforholdFraUttaksplan, List<BeregningsresultatAndel> beregningsresultatAndeler) {
        final List<BeregningsresultatAndel> kandidater = beregningsresultatAndeler.stream()
                .filter(a -> {
                    if (a.getAktivitetStatus().erArbeidstaker() || a.getAktivitetStatus().erFrilanser()) {
                        final Arbeidsforhold beregningsarbeidsforhold = toArbeidsforhold(a);
                        return aktivitetFraUttaksplan == a.getAktivitetStatus() && arbeidsforholdFraUttaksplan.gjelderFor(beregningsarbeidsforhold);
                    } else {
                        return aktivitetFraUttaksplan == a.getAktivitetStatus();
                    }
                })
                .toList();

        return kandidater;
    }

    private Arbeidsforhold toArbeidsforhold(BeregningsresultatAndel andel) {
        final Builder b = Arbeidsforhold.builder();
        andel.getArbeidsgiver().ifPresent(a -> {
            if (a.getErVirksomhet()) {
                b.medOrgnr(a.getIdentifikator());
            } else {
                b.medAktørId(a.getIdentifikator());
            }
        });
        if (andel.getAktivitetStatus().erFrilanser()) {
            b.medFrilanser(true);
        }
        b.medArbeidsforholdId(andel.getArbeidsforholdRef().getReferanse());
        return b.build();
    }

    private List<String> mapÅrsaker(Set<Årsak> årsaker) {
        return årsaker.stream().map(å -> å.toString()).toList();
    }

    private List<StønadstatistikkInngangsvilkår> mapInngangsvilkår(Map<String, Utfall> inngangsvilkår) {
        return inngangsvilkår.entrySet().stream().map(entry -> new StønadstatistikkInngangsvilkår(entry.getKey(), mapUtfall(entry.getValue()))).toList();
    }

    private StønadstatistikkGraderingMotTilsyn mapGraderingMotTilsyn(GraderingMotTilsyn graderingMotTilsyn) {
        if (graderingMotTilsyn == null) {
            return null;
        }
        final String årsak = (graderingMotTilsyn.getOverseEtablertTilsynÅrsak() != null) ? graderingMotTilsyn.getOverseEtablertTilsynÅrsak().toString() : null;
        return new StønadstatistikkGraderingMotTilsyn(graderingMotTilsyn.getEtablertTilsyn(),
                årsak,
                graderingMotTilsyn.getAndreSøkeresTilsyn(),
                graderingMotTilsyn.getTilgjengeligForSøker());
    }
}
