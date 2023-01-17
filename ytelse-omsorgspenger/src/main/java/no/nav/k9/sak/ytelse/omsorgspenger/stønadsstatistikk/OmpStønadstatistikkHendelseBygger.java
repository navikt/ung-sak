package no.nav.k9.sak.ytelse.omsorgspenger.stønadsstatistikk;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.aarskvantum.kontrakter.Vilkår;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
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
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkDetaljertUtfall;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkHendelse;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkInngangsvilkår;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkPeriode;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkUtbetalingsgrad;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkUtfall;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold.Builder;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.ReferanseType;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
public class OmpStønadstatistikkHendelseBygger implements StønadstatistikkHendelseBygger {

    private static final Logger logger = LoggerFactory.getLogger(OmpStønadstatistikkHendelseBygger.class);
    private StønadstatistikkPeriodetidslinjebygger stønadstatistikkPeriodetidslinjebygger;
    private BehandlingRepository behandlingRepository;
    private AktørTjeneste aktørTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    OmpStønadstatistikkHendelseBygger() {
        //for CDI proxy
    }

    @Inject
    public OmpStønadstatistikkHendelseBygger(StønadstatistikkPeriodetidslinjebygger stønadstatistikkPeriodetidslinjebygger,
                                             BehandlingRepository behandlingRepository,
                                             AktørTjeneste aktørTjeneste,
                                             BehandlingVedtakRepository behandlingVedtakRepository) {
        this.stønadstatistikkPeriodetidslinjebygger = stønadstatistikkPeriodetidslinjebygger;
        this.behandlingRepository = behandlingRepository;
        this.aktørTjeneste = aktørTjeneste;
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
        if (behandling.getType() == BehandlingType.UNNTAKSBEHANDLING) {
            //det er høy kost/nytte for å få med unntaksbehandlinger.
            //høy kostnad: må bygge opp hendelsen på helt annen måte (fra tilkjent ytelse isdf vilkår o.l.), må endre DTO mot DVH
            //lav nytte: det er veldig få slike behandlinger, og utfall er lagret som fritekst isdf koder
            logger.info("Lager ikke StønadstatistikkHendelse for unntaksbehandling");
            return null;
        }

        final PersonIdent søker = aktørTjeneste.hentPersonIdentForAktørId(behandling.getFagsak().getAktørId()).orElseThrow();

        final LocalDateTimeline<StønadstatistikkPeriodetidslinjebygger.InformasjonTilStønadstatistikkHendelse> periodetidslinje = stønadstatistikkPeriodetidslinjebygger.lagTidslinjeFor(behandling);

        final UUID forrigeBehandlingUuid = finnForrigeBehandlingUuid(behandling);
        final BehandlingVedtak behandlingVedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId()).orElseThrow();
        final LocalDateTime vedtakstidspunkt = behandlingVedtak.getVedtakstidspunkt();


        return StønadstatistikkHendelse.forOmsorgspenger(søker,
            behandling.getFagsak().getSaksnummer(),
            HenvisningUtleder.utledHenvisning(behandling.getUuid()), // behandlingsreferanse i økonomisystemet: "henvisning"
            behandlingUuid,
            forrigeBehandlingUuid,
            vedtakstidspunkt,
            mapPerioder(periodetidslinje)
        );
    }

    private UUID finnForrigeBehandlingUuid(final Behandling behandling) {
        return behandling.getOriginalBehandlingId()
            .map(behandlingId -> behandlingRepository.hentBehandlingHvisFinnes(behandlingId)
                .map(Behandling::getUuid)
                .orElse(null)
            )
            .orElse(null);
    }

    private List<StønadstatistikkPeriode> mapPerioder(LocalDateTimeline<StønadstatistikkPeriodetidslinjebygger.InformasjonTilStønadstatistikkHendelse> periodetidslinje) {
        return periodetidslinje.toSegments().stream().map(this::mapPeriode).toList();
    }

    private StønadstatistikkPeriode mapPeriode(LocalDateSegment<StønadstatistikkPeriodetidslinjebygger.InformasjonTilStønadstatistikkHendelse> ds) {
        Year år = Year.of(ds.getFom().getYear());
        UttakResultatPeriode info = ds.getValue().getUttakresultat();
        final BigDecimal bruttoBeregningsgrunnlag = (ds.getValue().getBeregningsgrunnlagDto() != null) ? ds.getValue().getBeregningsgrunnlagDto().getÅrsinntektVisningstall() : BigDecimal.valueOf(-1);
        return StønadstatistikkPeriode.forOmsorgspenger(ds.getFom(), ds.getTom(), mapUtfall(ds.getValue()), mapUtbetalingsgrader(info, ds.getValue().getBeregningsresultatAndeler()), mapInngangsvilkår(ds.getValue(), år), bruttoBeregningsgrunnlag);
    }

    private StønadstatistikkUtfall mapUtfall(StønadstatistikkPeriodetidslinjebygger.InformasjonTilStønadstatistikkHendelse hendelse) {
        Set<Utfall> alleUtfall = EnumSet.noneOf(Utfall.class);
        if (hendelse.getVilkårFraK9sak() != null) {
            alleUtfall.addAll(hendelse.getVilkårFraK9sak().values().stream().map(VilkårUtfall::getUtfall).toList());
        }
        if (hendelse.getVilkårFraÅrskvantum() != null) {
            alleUtfall.addAll(hendelse.getVilkårFraÅrskvantum().values().stream().map(VilkårUtfall::getUtfall).toList());
        }

        if (alleUtfall.contains(Utfall.IKKE_OPPFYLT)) {
            return StønadstatistikkUtfall.IKKE_OPPFYLT;
        }
        if (alleUtfall.contains(Utfall.UDEFINERT)) {
            throw new IllegalArgumentException("Ikke-håndtert utfall-type: UDEFINERT");
        }
        if (alleUtfall.stream().anyMatch(utfall -> utfall == Utfall.OPPFYLT)) {
            return StønadstatistikkUtfall.OPPFYLT;
        }
        if (alleUtfall.isEmpty()) {
            throw new IllegalArgumentException("Ingen utfall å mappe");
        }
        throw new IllegalArgumentException("Har ikke mapping for utfall: " + alleUtfall);
    }

    private List<StønadstatistikkUtbetalingsgrad> mapUtbetalingsgrader(UttakResultatPeriode uttakResultatPerioder, List<BeregningsresultatAndel> beregningsresultatAndeler) {
        if (uttakResultatPerioder == null) {
            //skjer hvis alt for perioden er avslått i søknadsfrist-vilkåret
            return List.of();
        }
        List<UttakAktivitet> aktiviterter = uttakResultatPerioder.getUttakAktiviteter();
        return aktiviterter.stream().map(u -> {
            var a = u.getArbeidsforhold();
            String orgNr = a.getReferanseType() == ReferanseType.ORG_NR ? a.getIdentifikator() : null;
            String aktørId = a.getReferanseType() == ReferanseType.AKTØR_ID ? a.getIdentifikator() : null;
            String arbeidsforholdType = u.getType().getKode();
            final Arbeidsforhold arbeidsforholdFraUttaksplan = u.getArbeidsforhold(); //TODO er dette tilstrekkelig? PSB gjør noe utregning for denne
            final BigDecimal utbetalingsgrad = u.getUtbetalingsgrad();

            /*
             * Sjekk på om beregningsresultatAndeler != null gjøres grunnet
             * tidligere feil der uttaksgraden ikke ble satt til 0 når det
             * var avslag i beregning.
             *
             * Vi trenger denne sjekken videre for å kunne støtte full eksport.
             */
            if (beregningsresultatAndeler != null && skalFinnesAndeler(utbetalingsgrad)) {
                AktivitetStatus aktivitetstatus = utledAktivitetStatus(u.getType());
                final List<BeregningsresultatAndel> andeler = finnAndeler(aktivitetstatus, arbeidsforholdFraUttaksplan, beregningsresultatAndeler);
                return andeler.stream().map(andel -> {
                    // TODO: andel.getArbeidsforholdRef().getReferanse() må byttes til eksternReferanse.
                    final String arbeidsforholdRef = (andel.getArbeidsforholdRef() != null) ? andel.getArbeidsforholdRef().getReferanse() : null;


                    final StønadstatistikkArbeidsforhold arbeidsforhold = new StønadstatistikkArbeidsforhold(arbeidsforholdType, orgNr, aktørId, arbeidsforholdRef);
                    final int dagsats = andel.getDagsats();
                    return StønadstatistikkUtbetalingsgrad.forOmsorgspenger(andel.getAktivitetStatus().getKode(), arbeidsforhold, utbetalingsgrad, dagsats, andel.erBrukerMottaker()
                    );
                }).toList();
            } else {
                final StønadstatistikkArbeidsforhold arbeidsforhold = new StønadstatistikkArbeidsforhold(arbeidsforholdType, orgNr, aktørId, a.getArbeidsforholdId());
                return List.of(StønadstatistikkUtbetalingsgrad.forOmsorgspenger(null, arbeidsforhold, utbetalingsgrad, 0, true));
            }
        }).flatMap(Collection::stream).toList();
    }

    private AktivitetStatus utledAktivitetStatus(UttakArbeidType type) {
        return switch (type) {
            case ARBEIDSTAKER -> AktivitetStatus.ARBEIDSTAKER;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANSER -> AktivitetStatus.FRILANSER;
            case DAGPENGER -> AktivitetStatus.DAGPENGER;
            case SYKEPENGER_AV_DAGPENGER -> AktivitetStatus.SYKEPENGER_AV_DAGPENGER;
            case PLEIEPENGER_AV_DAGPENGER -> AktivitetStatus.PLEIEPENGER_AV_DAGPENGER;
            case KUN_YTELSE -> AktivitetStatus.KUN_YTELSE;
            case IKKE_YRKESAKTIV -> AktivitetStatus.IKKE_YRKESAKTIV;
            case INAKTIV -> AktivitetStatus.MIDLERTIDIG_INAKTIV;
            case IKKE_YRKESAKTIV_UTEN_ERSTATNING, //TODO mappe IKKE_YRKESAKTIV_UTEN_ERSTATNING?
                ANNET -> throw new IllegalArgumentException("Ikke støttet å mappe til AktivitetStatus: " + type);
        };
    }

    private boolean skalFinnesAndeler(final BigDecimal utbetalingsgrad) {
        return utbetalingsgrad.compareTo(BigDecimal.valueOf(0)) > 0;
    }

    private List<BeregningsresultatAndel> finnAndeler(AktivitetStatus aktivitetFraUttaksplan, Arbeidsforhold arbeidsforholdFraUttaksplan, List<BeregningsresultatAndel> beregningsresultatAndeler) {
        final AktivitetStatus as = medIkkeYrkesaktivSomArbeidstaker(aktivitetFraUttaksplan);
        return beregningsresultatAndeler.stream()
            .filter(a -> {
                if (a.getAktivitetStatus().erArbeidstaker() || a.getAktivitetStatus().erFrilanser()) {
                    final Arbeidsforhold beregningsarbeidsforhold = toArbeidsforhold(a);
                    return as == a.getAktivitetStatus() && arbeidsforholdFraUttaksplan.gjelderFor(beregningsarbeidsforhold);
                } else {
                    return as == a.getAktivitetStatus();
                }
            })
            .toList();
    }

    private static AktivitetStatus medIkkeYrkesaktivSomArbeidstaker(AktivitetStatus as) {
        if (as == AktivitetStatus.IKKE_YRKESAKTIV) {
            return AktivitetStatus.ARBEIDSTAKER;
        }
        return as;
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

    private List<StønadstatistikkInngangsvilkår> mapInngangsvilkår(StønadstatistikkPeriodetidslinjebygger.InformasjonTilStønadstatistikkHendelse info, Year år) {
        List<StønadstatistikkInngangsvilkår> resultat = new ArrayList<>();
        resultat.addAll(mapVilkårFraK9Årskvantum(info.getVilkårFraÅrskvantum()));
        resultat.addAll(mapVilkårFraK9Sak(info.getVilkårFraK9sak(), år));
        return resultat;
    }

    private List<StønadstatistikkInngangsvilkår> mapVilkårFraK9Sak(Map<VilkårType, VilkårUtfall> vilkår, Year år) {
        if (vilkår == null) {
            return List.of();
        }
        return vilkår.entrySet().stream()
            .filter(e -> !(år.getValue() == 2023 && e.getKey() == VilkårType.OMSORGEN_FOR && e.getValue().getUtfall() == Utfall.IKKE_VURDERT)) //feil i 2023 som gjorde at noen vedtak ble fattet med IKKE_VURDERT på omsorgsvilkåret
            .map(e -> mapVilkår(e.getKey(), e.getValue()))
            .toList();
    }

    private StønadstatistikkInngangsvilkår mapVilkår(VilkårType vilkårType, VilkårUtfall utfall) {
        String vilkårtype = mapK9SakVilkår(vilkårType);
        StønadstatistikkUtfall hovedutfall = mapUtfall(utfall.getUtfall());
        return new StønadstatistikkInngangsvilkår(vilkårtype, hovedutfall, mapDetaljertUtfall(utfall.getDetaljer()));
    }

    private List<StønadstatistikkDetaljertUtfall> mapDetaljertUtfall(Set<DetaljertVilkårUtfall> detaljer) {
        if (detaljer == null) {
            return List.of();
        }
        return detaljer.stream()
            .map(this::mapDetaljert)
            .toList();
    }

    private StønadstatistikkDetaljertUtfall mapDetaljert(DetaljertVilkårUtfall detaljer) {
        return new StønadstatistikkDetaljertUtfall(
            detaljer.getKravstiller(),
            detaljer.getType() != null ? detaljer.getType() : null,
            detaljer.getOrganisasjonsnummer(),
            detaljer.getAktørId(),
            detaljer.getArbeidsforholdId(),
            mapUtfall(detaljer.getUtfall())
        );
    }

    private List<StønadstatistikkInngangsvilkår> mapVilkårFraK9Årskvantum(Map<Vilkår, VilkårUtfall> vilkår) {
        if (vilkår == null) {
            return List.of();
        }
        return vilkår.entrySet().stream()
            .map(e -> mapVilkår(e.getKey(), e.getValue()))
            .toList();
    }

    private StønadstatistikkInngangsvilkår mapVilkår(Vilkår vilkårType, VilkårUtfall utfall) {
        String vilkårtype = mapÅrskvantumVilkår(vilkårType);
        StønadstatistikkUtfall hovedutfall = mapUtfall(utfall.getUtfall());
        return new StønadstatistikkInngangsvilkår(vilkårtype, hovedutfall, mapDetaljertUtfall(utfall.getDetaljer()));
    }

    private static StønadstatistikkUtfall mapUtfall(Utfall utfall) {
        return switch (utfall) {
            case OPPFYLT -> StønadstatistikkUtfall.OPPFYLT;
            case IKKE_OPPFYLT -> StønadstatistikkUtfall.IKKE_OPPFYLT;
            default -> throw new IllegalArgumentException("Ikke-støttet utfall: " + utfall);
        };
    }

    private static String mapK9SakVilkår(VilkårType vilkår) {
        return switch (vilkår) {
            case OMSORGEN_FOR -> "DVH_OMP_OMSORGSVILKÅRET";
            case K9_VILKÅRET -> "DVH_OMP_K9_VILKÅRET";
            case MEDLEMSKAPSVILKÅRET -> "DVH_OMP_MEDLEMSKAPSVILKÅRET";
            case ALDERSVILKÅR -> "DVH_OMP_ALDERSVILKÅR";
            case SØKNADSFRIST -> "DVH_OMP_SØKNADSFRIST";
            case SØKERSOPPLYSNINGSPLIKT -> "DVH_OMP_SØKERSOPPLYSNINGSPLIKT";
            case OPPTJENINGSPERIODEVILKÅR -> "DVH_OMP_OPPTJENINGSPERIODEVILKÅR";
            case OPPTJENINGSVILKÅRET -> "DVH_OMP_OPPTJENINGSVILKÅRET";
            case BEREGNINGSGRUNNLAGVILKÅR -> "DVH_OMP_BEREGNINGSGRUNNLAGVILKÅR";
            default ->
                throw new IllegalArgumentException("Ikke-støttet vilkårtype i stønadsstatistikk for OMP " + vilkår);
        };
    }

    private static String mapÅrskvantumVilkår(Vilkår vilkår) {
        return switch (vilkår) {
            case OMSORGSVILKÅRET -> "DVH_OMP_OMSORGSVILKÅRET";
            case NOK_DAGER -> "DVH_OMP_NOK_DAGER";
            case INNGANGSVILKÅR ->
                throw new IllegalArgumentException("Ikke et reelt vilkår. INNGANGSVILKÅR er en aggregert type");
            case ALDERSVILKÅR_SØKER -> "DVH_OMP_ALDERSVILKÅR_SØKER";
            case SMITTEVERN -> "DVH_OMP_SMITTEVERN";
            case UIDENTIFISERT_RAMMEVEDTAK -> "DVH_OMP_UIDENTIFISERT_RAMMEVEDTAK";
            case ARBEIDSFORHOLD -> "DVH_OMP_ARBEIDSFORHOLD";
            case FREMTIDIG_KRAV -> "DVH_OMP_FREMTIDIG_KRAV";
            case ANDRE_SKAL_DEKKE_DAGENE -> "DVH_OMP_ANDRE_SKAL_DEKKE_DAGENE";
            case FRAVÆR_FRA_ARBEID -> "DVH_OMP_FRAVÆR_FRA_ARBEID";
            case NYOPPSTARTET_HOS_ARBEIDSGIVER -> "DVH_OMP_NYOPPSTARTET_HOS_ARBEIDSGIVER";
        };
    }

}
