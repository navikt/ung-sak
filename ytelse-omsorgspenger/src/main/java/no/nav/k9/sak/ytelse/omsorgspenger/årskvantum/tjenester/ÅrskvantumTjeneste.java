package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.aarskvantum.kontrakter.Arbeidsforhold;
import no.nav.k9.aarskvantum.kontrakter.ArbeidsforholdStatus;
import no.nav.k9.aarskvantum.kontrakter.AvvikImSøknad;
import no.nav.k9.aarskvantum.kontrakter.Barn;
import no.nav.k9.aarskvantum.kontrakter.BarnType;
import no.nav.k9.aarskvantum.kontrakter.FraværPeriode;
import no.nav.k9.aarskvantum.kontrakter.FraværÅrsak;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplan;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplanForBehandlinger;
import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.RammevedtakResponse;
import no.nav.k9.aarskvantum.kontrakter.SøknadÅrsak;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Vilkår;
import no.nav.k9.aarskvantum.kontrakter.VurderteVilkår;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDager;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumGrunnlag;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumUtbetalingGrunnlag;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumUttrekk;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.k9.sak.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetTypeArbeidsgiver;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.OppgittFraværHolder;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.SamtidigKravStatus;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;

@ApplicationScoped
@Default
public class ÅrskvantumTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ÅrskvantumTjeneste.class);

    private final MapOppgittFraværOgVilkårsResultat mapOppgittFraværOgVilkårsResultat = new MapOppgittFraværOgVilkårsResultat();
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private ÅrskvantumKlient årskvantumKlient;
    private TpsTjeneste tpsTjeneste;
    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Boolean skruPåAvslagSøknadManglerIm;
    private Boolean brukFerdigutledetFlaggRefusjon;

    ÅrskvantumTjeneste() {
        // CDI
    }

    @Inject
    public ÅrskvantumTjeneste(BehandlingRepository behandlingRepository,
                              OmsorgspengerGrunnlagRepository grunnlagRepository,
                              VilkårResultatRepository vilkårResultatRepository,
                              InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                              ÅrskvantumRestKlient årskvantumRestKlient,
                              TpsTjeneste tpsTjeneste,
                              @FagsakYtelseTypeRef("OMP") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                              TrekkUtFraværTjeneste trekkUtFraværTjeneste,
                              OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste,
                              MottatteDokumentRepository mottatteDokumentRepository,
                              @KonfigVerdi(value = "OMP_AVSLAG_SOKNAD_MANGLER_IM", defaultVerdi = "false") Boolean skruPåAvslagSøknadManglerIm,
                              @KonfigVerdi(value = "OMP_AARSKVANTUMTJENESTE_BRUK_FERDIGUTLEDET_FLAGG_REFUSJON", defaultVerdi = "true") Boolean brukFerdigutledetFlaggRefusjon) {
        this.grunnlagRepository = grunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.årskvantumKlient = årskvantumRestKlient;
        this.tpsTjeneste = tpsTjeneste;
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.opptjeningTjeneste = opptjeningTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.skruPåAvslagSøknadManglerIm = skruPåAvslagSøknadManglerIm;
        this.brukFerdigutledetFlaggRefusjon = brukFerdigutledetFlaggRefusjon;
    }

    public void bekreftUttaksplan(Long behandlingId) {
        årskvantumKlient.settUttaksplanTilManueltBekreftet(behandlingRepository.hentBehandling(behandlingId).getUuid());
    }

    public void innvilgeEllerAvslåPeriodeneManuelt(Long behandlingId, boolean innvilgePeriodene, Optional<Integer> antallDager) {
        årskvantumKlient.innvilgeEllerAvslåPeriodeneManuelt(behandlingRepository.hentBehandling(behandlingId).getUuid(), innvilgePeriodene, antallDager);
    }

    public void slettUttaksplan(Long behandlingId) {
        årskvantumKlient.slettUttaksplan(behandlingRepository.hentBehandling(behandlingId).getUuid());
    }

    public ÅrskvantumGrunnlag hentInputTilBeregning(UUID behandlingUuid) {
        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingUuid));
        return hentForRef(ref);
    }

    private ÅrskvantumGrunnlag hentForRef(BehandlingReferanse ref) {

        var oppgittFravær = grunnlagRepository.hentSammenslåtteFraværPerioder(ref.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());

        VilkårType vilkårType = VilkårType.OPPTJENINGSVILKÅRET;
        var vilkårsperioder = perioderTilVurderingTjeneste.utled(behandling.getId(), vilkårType);
        var fagsakFravær = trekkUtFraværTjeneste.fraværFraKravDokumenterPåFagsakMedSøknadsfristVurdering(behandling);
        var relevantePerioder = utledPerioder(vilkårsperioder, fagsakFravær, oppgittFravær);

        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId());
        var sakInntektsmeldinger = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());
        var opptjeningAktiveter = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, vilkårsperioder);
        var fraværPerioder = mapUttaksPerioder(ref, vilkårene, inntektArbeidYtelseGrunnlag, sakInntektsmeldinger, opptjeningAktiveter, relevantePerioder, behandling);

        if (fraværPerioder.isEmpty()) {
            // kan ikke være empty når vi sender årskvantum
            throw new IllegalStateException("Har ikke fraværs perioder for fagsak.periode[" + ref.getFagsakPeriode() + "]"
                + ",\trelevant perioder=" + relevantePerioder
                + ",\toppgitt fravær er tom="
                + ",\tvilkårsperioder[" + vilkårType + "]=" + vilkårsperioder
                + ",\tfagsakFravær=" + fagsakFravær);
        }

        var personMedRelasjoner = tpsTjeneste.hentBrukerForAktør(ref.getAktørId()).orElseThrow();

        var barna = personMedRelasjoner.getFamilierelasjoner()
            .stream()
            .filter(it -> it.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .filter(it -> !it.getPersonIdent().erFdatNummer())
            .map(this::innhentPersonopplysningForBarn)
            .map((Tuple<Familierelasjon, Optional<Personinfo>> relasjonBarn) -> mapBarn(personMedRelasjoner, relasjonBarn))
            .toList();

        return new ÅrskvantumGrunnlag(ref.getSaksnummer().getVerdi(),
            ref.getBehandlingUuid().toString(),
            fraværPerioder,
            personMedRelasjoner.getPersonIdent().getIdent(),
            personMedRelasjoner.getFødselsdato(),
            personMedRelasjoner.getDødsdato(),
            barna);
    }

    Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> utledPerioder(NavigableSet<DatoIntervallEntitet> vilkårsperioder,
                                                                                         Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> fraværPåFagsak,
                                                                                         Set<OppgittFraværPeriode> fraværPåBehandling) {

        LocalDateTimeline<Boolean> tildslinjeVilkårsperioder = new LocalDateTimeline<>(vilkårsperioder.stream().map(vilkårperiode -> new LocalDateSegment<>(vilkårperiode.toLocalDateInterval(), true)).toList());
        LocalDateTimeline<Boolean> tidslinjeNulledePerioderForBehandling = new LocalDateTimeline<>(fraværPåBehandling.stream()
            .filter(fraværBehandling -> Duration.ZERO.equals(fraværBehandling.getFraværPerDag()))
            .map(fraværBehandling -> new LocalDateSegment<>(fraværBehandling.getFom(), fraværBehandling.getTom(), true)).toList(), StandardCombinators::alwaysTrueForMatch);

        LocalDateTimeline<Boolean> tidlinjeAllePerioderForBehandling = tildslinjeVilkårsperioder.crossJoin(tidslinjeNulledePerioderForBehandling).compress();
        var resultat = new LinkedHashMap<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>>();
        for (var entry : fraværPåFagsak.entrySet()) {
            var behandlingTidslinje = entry.getValue().intersection(tidlinjeAllePerioderForBehandling);
            if (!behandlingTidslinje.isEmpty()) {
                resultat.put(entry.getKey(), behandlingTidslinje);
            }
        }
        return resultat;
    }

    public ÅrskvantumResultat beregnÅrskvantumUttak(BehandlingReferanse ref) {
        var årskvantumRequest = hentForRef(ref);

        return årskvantumKlient.hentÅrskvantumUttak(årskvantumRequest);
    }

    public ÅrskvantumUtbetalingGrunnlag hentUtbetalingGrunnlag(UUID behandlingUuid) {
        var inputTilBeregning = hentInputTilBeregning(behandlingUuid);
        return årskvantumKlient.hentUtbetalingGrunnlag(inputTilBeregning);
    }

    public RammevedtakResponse hentRammevedtak(PersonIdent personIdent, LukketPeriode periode) {
        return årskvantumKlient.hentRammevedtak(personIdent, periode);
    }

    public ÅrskvantumUttrekk hentUttrekk() {
        return årskvantumKlient.hentUttrekk();
    }

    private List<FraværPeriode> mapUttaksPerioder(BehandlingReferanse ref,
                                                  Vilkårene vilkårene,
                                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                  Set<Inntektsmelding> sakInntektsmeldinger,
                                                  NavigableMap<DatoIntervallEntitet, List<OpptjeningAktivitetPeriode>> opptjeningAktiveter,
                                                  Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> perioder,
                                                  Behandling behandling) {
        var fraværPerioder = new ArrayList<FraværPeriode>();
        var fagsakPeriode = behandling.getFagsak().getPeriode();
        var fraværsPerioderMedUtfallOgPerArbeidsgiver = mapOppgittFraværOgVilkårsResultat.utledPerioderMedUtfall(ref, iayGrunnlag, opptjeningAktiveter, vilkårene, fagsakPeriode, perioder)
            .values()
            .stream()
            .flatMap(Collection::stream)
            .toList();
        var mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(ref.getFagsakId()).stream()
            .collect(Collectors.toMap(MottattDokument::getJournalpostId, e -> e));

        for (WrappedOppgittFraværPeriode wrappedOppgittFraværPeriode : fraværsPerioderMedUtfallOgPerArbeidsgiver) {
            var fraværPeriode = wrappedOppgittFraværPeriode.getPeriode();
            var periode = new LukketPeriode(fraværPeriode.getFom(), fraværPeriode.getTom());
            Optional<UUID> opprinneligBehandlingUuid = hentBehandlingUuid(fraværPeriode.getJournalpostId(), mottatteDokumenter);

            Arbeidsgiver arb = fraværPeriode.getArbeidsgiver();

            Arbeidsforhold arbeidsforhold;

            boolean kreverRefusjonFerdigutledet = wrappedOppgittFraværPeriode.getSamtidigeKrav().inntektsmeldingMedRefusjonskrav() == SamtidigKravStatus.KravStatus.FINNES;
            boolean refusjonskravTrekt = wrappedOppgittFraværPeriode.getSamtidigeKrav().inntektsmeldingMedRefusjonskrav() == SamtidigKravStatus.KravStatus.TREKT;
            boolean kreverRefusjon = false;
            if (arb == null) {
                arbeidsforhold = new Arbeidsforhold(fraværPeriode.getAktivitetType().getKode(), null, null, null);
            } else {
                var arbeidsforholdRef = fraværPeriode.getArbeidsforholdRef() == null ? InternArbeidsforholdRef.nullRef() : fraværPeriode.getArbeidsforholdRef();
                String arbeidsforholdId = arbeidsforholdRef.getReferanse();
                kreverRefusjon = kreverArbeidsgiverRefusjon(sakInntektsmeldinger, arb, arbeidsforholdRef, fraværPeriode.getPeriode());
                arbeidsforhold = new Arbeidsforhold(fraværPeriode.getAktivitetType().getKode(),
                    arb.getOrgnr(),
                    arb.getAktørId() != null ? arb.getAktørId().getId() : null,
                    arbeidsforholdId);
            }

            if (kreverRefusjon != kreverRefusjonFerdigutledet) {
                if (refusjonskravTrekt) {
                    logger.info("Flagg for krever refusjon er ulikt på ny/gammel utledning. Gammel utlednig sa {}, ny sier {}. Skjer her fordi krav er trekt", kreverRefusjon, kreverRefusjonFerdigutledet);
                } else {
                    logger.warn("Flagg for krever refusjon er ulikt på ny/gammel utledning. Gammel utlednig sa {}, ny sier {}.", kreverRefusjon, kreverRefusjonFerdigutledet);
                }
            }
            var arbeidforholdStatus = utledArbeidsforholdStatus(wrappedOppgittFraværPeriode);
            var utfallInngangsvilkår = utledUtfallIngangsvilkår(wrappedOppgittFraværPeriode);
            var avvikImSøknad = utedAvvikImSøknad(wrappedOppgittFraværPeriode);
            var uttaksperiodeOmsorgspenger = new FraværPeriode(arbeidsforhold,
                arbeidforholdStatus,
                periode,
                fraværPeriode.getFraværPerDag(),
                true,
                brukFerdigutledetFlaggRefusjon ? kreverRefusjonFerdigutledet : kreverRefusjon,
                utfallInngangsvilkår,
                wrappedOppgittFraværPeriode.getInnsendingstidspunkt(),
                utledFraværÅrsak(fraværPeriode),
                utledSøknadÅrsak(fraværPeriode),
                opprinneligBehandlingUuid.map(UUID::toString).orElse(null),
                avvikImSøknad,
                utledVurderteVilkår(arbeidforholdStatus, utfallInngangsvilkår, avvikImSøknad, fraværPeriode));
            fraværPerioder.add(uttaksperiodeOmsorgspenger);
        }
        return fraværPerioder;
    }

    private VurderteVilkår utledVurderteVilkår(ArbeidsforholdStatus arbeidsforholdStatus, Utfall utfallInngangsvilkår, AvvikImSøknad avvikImSøknad, OppgittFraværPeriode fraværPeriode) {
        NavigableMap<Vilkår, Utfall> vilkårMap = new TreeMap<>();
        vilkårMap.put(Vilkår.ARBEIDSFORHOLD, arbeidsforholdStatus == ArbeidsforholdStatus.AKTIVT ? Utfall.INNVILGET : Utfall.AVSLÅTT);
        vilkårMap.put(Vilkår.INNGANGSVILKÅR, utfallInngangsvilkår);
        vilkårMap.put(Vilkår.FRAVÆR_FRA_ARBEID, skruPåAvslagSøknadManglerIm && avslåFraværFraArbeidPgaManglendeIm(avvikImSøknad, fraværPeriode.getAktivitetType(), fraværPeriode.getSøknadÅrsak()) ? Utfall.AVSLÅTT : Utfall.INNVILGET);
        return new VurderteVilkår(vilkårMap);
    }

    private boolean avslåFraværFraArbeidPgaManglendeIm(AvvikImSøknad avvikImSøknad, UttakArbeidType aktivitetType, no.nav.k9.kodeverk.uttak.SøknadÅrsak søknadÅrsak) {
        return aktivitetType == UttakArbeidType.ARBEIDSTAKER
            && avvikImSøknad == AvvikImSøknad.SØKNAD_UTEN_MATCHENDE_IM
            && søknadÅrsak != no.nav.k9.kodeverk.uttak.SøknadÅrsak.ARBEIDSGIVER_KONKURS
            && søknadÅrsak != no.nav.k9.kodeverk.uttak.SøknadÅrsak.KONFLIKT_MED_ARBEIDSGIVER;
    }

    private AvvikImSøknad utedAvvikImSøknad(WrappedOppgittFraværPeriode oppgittFraværPeriode) {
        var kravStatus = oppgittFraværPeriode.getSamtidigeKrav();
        var søknad = kravStatus.søknad();
        var refusjonskrav = kravStatus.inntektsmeldingMedRefusjonskrav();
        var imUtenRefusjonskrav = kravStatus.inntektsmeldingUtenRefusjonskrav();

        if (søknad == SamtidigKravStatus.KravStatus.FINNES) {
            if (refusjonskrav == SamtidigKravStatus.KravStatus.FINNES) {
                return AvvikImSøknad.IM_REFUSJONSKRAV_TRUMFER_SØKNAD;
            }
            if (imUtenRefusjonskrav != SamtidigKravStatus.KravStatus.FINNES) {
                return AvvikImSøknad.SØKNAD_UTEN_MATCHENDE_IM;
            }
            return AvvikImSøknad.INGEN_AVVIK;
        }
        return AvvikImSøknad.UDEFINERT;
    }

    private Utfall utledUtfallIngangsvilkår(WrappedOppgittFraværPeriode wrappedOppgittFraværPeriode) {
        var erAvslåttInngangsvilkår = wrappedOppgittFraværPeriode.getErAvslåttInngangsvilkår();
        return erAvslåttInngangsvilkår != null && erAvslåttInngangsvilkår ? Utfall.AVSLÅTT : Utfall.INNVILGET;
    }

    private FraværÅrsak utledFraværÅrsak(OppgittFraværPeriode periode) {
        return switch (periode.getFraværÅrsak()) {
            case ORDINÆRT_FRAVÆR -> FraværÅrsak.ORDINÆRT_FRAVÆR;
            case SMITTEVERNHENSYN -> FraværÅrsak.SMITTEVERNHENSYN;
            case STENGT_SKOLE_ELLER_BARNEHAGE -> FraværÅrsak.STENGT_SKOLE_ELLER_BARNEHAGE;
            default -> FraværÅrsak.UDEFINERT;
        };
    }

    private SøknadÅrsak utledSøknadÅrsak(OppgittFraværPeriode periode) {
        return switch (periode.getSøknadÅrsak()) {
            case ARBEIDSGIVER_KONKURS -> SøknadÅrsak.ARBEIDSGIVER_KONKURS;
            case NYOPPSTARTET_HOS_ARBEIDSGIVER -> SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER;
            case KONFLIKT_MED_ARBEIDSGIVER -> SøknadÅrsak.KONFLIKT_MED_ARBEIDSGIVER;
            default -> SøknadÅrsak.UDEFINERT;
        };
    }

    private ArbeidsforholdStatus utledArbeidsforholdStatus(WrappedOppgittFraværPeriode wrappedOppgittFraværPeriode) {
        if (wrappedOppgittFraværPeriode.getArbeidStatus() != null && ArbeidStatus.AVSLUTTET.equals(wrappedOppgittFraværPeriode.getArbeidStatus())) {
            return ArbeidsforholdStatus.AVSLUTTET;
        }
        if (wrappedOppgittFraværPeriode.getErIPermisjon() != null && wrappedOppgittFraværPeriode.getErIPermisjon()) {
            return ArbeidsforholdStatus.PERMITERT;
        }
        if (wrappedOppgittFraværPeriode.getArbeidStatus() != null && ArbeidStatus.IKKE_EKSISTERENDE.equals(wrappedOppgittFraværPeriode.getArbeidStatus())) {
            return ArbeidsforholdStatus.AVSLUTTET; // TODO: sett ikke eksisterende
        }
        return ArbeidsforholdStatus.AKTIVT;
    }

    private Tuple<Familierelasjon, Optional<Personinfo>> innhentPersonopplysningForBarn(Familierelasjon it) {
        return new Tuple<>(it, tpsTjeneste.hentBrukerForFnr(it.getPersonIdent()));
    }

    private no.nav.k9.aarskvantum.kontrakter.Barn mapBarn(Personinfo personinfoSøker, Tuple<Familierelasjon, Optional<Personinfo>> relasjonMedBarn) {
        var personinfoBarn = relasjonMedBarn.getElement2().orElseThrow();
        var harSammeBosted = relasjonMedBarn.getElement1().getHarSammeBosted(personinfoSøker, personinfoBarn);
        var perioderMedSammeBosted = relasjonMedBarn.getElement1().getPerioderMedSammeBosted(personinfoSøker, personinfoBarn);
        var lukketPeriodeMedSammeBosted = perioderMedSammeBosted.stream().map(p -> new LukketPeriode(p.getFom(), p.getTom())).collect(Collectors.toList());
        return new Barn(personinfoBarn.getPersonIdent().getIdent(), personinfoBarn.getFødselsdato(), personinfoBarn.getDødsdato(), harSammeBosted, lukketPeriodeMedSammeBosted, BarnType.VANLIG);
    }

    private boolean kreverArbeidsgiverRefusjon(Set<Inntektsmelding> sakInntektsmeldinger,
                                               Arbeidsgiver arbeidsgiver,
                                               InternArbeidsforholdRef arbeidsforholdRef,
                                               DatoIntervallEntitet periode) {
        var alleInntektsmeldinger = sakInntektsmeldinger;
        var inntektsmeldinger = getInntektsmeldingerFor(alleInntektsmeldinger, arbeidsgiver);
        var inntektsmeldingSomMatcherUttak = inntektsmeldinger.stream()
            .filter(it -> it.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef)) // TODO: Bør vi matcher på gjelderfor her? Perioder som er sendt inn med arbeidsforholdsId vil da matche med
            // inntekstmeldinger uten for samme arbeidsgiver men hvor perioden overlapper
            .filter(it -> it.getOppgittFravær().stream()
                .anyMatch(fravære -> periode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fravære.getFom(), fravære.getTom()))))
            .map(Inntektsmelding::getRefusjonBeløpPerMnd)
            .collect(Collectors.toSet());

        if (inntektsmeldingSomMatcherUttak.isEmpty()) {
            return false;
        } else if (inntektsmeldingSomMatcherUttak.size() == 1) {
            var verdi = Optional.ofNullable(inntektsmeldingSomMatcherUttak.iterator().next()).map(Beløp::getVerdi).orElse(BigDecimal.ZERO);
            return BigDecimal.ZERO.compareTo(verdi) < 0;
        } else {
            // Tar nyeste
            var verdi = inntektsmeldinger.stream()
                .filter(it -> it.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef))
                .filter(it -> it.getOppgittFravær().stream()
                    .anyMatch(fravære -> periode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fravære.getFom(), fravære.getTom()))))
                .max(Inntektsmelding.COMP_REKKEFØLGE)
                .map(Inntektsmelding::getRefusjonBeløpPerMnd)
                .map(Beløp::getVerdi)
                .orElse(BigDecimal.ZERO);
            return BigDecimal.ZERO.compareTo(verdi) < 0;
        }
    }

    private Set<Inntektsmelding> getInntektsmeldingerFor(Set<Inntektsmelding> alleInntektsmeldinger, Arbeidsgiver arbeidsgiver) {
        return alleInntektsmeldinger.stream()
            .filter(i -> i.getArbeidsgiver().equals(arbeidsgiver))
            .collect(Collectors.toSet());
    }

    private Optional<UUID> hentBehandlingUuid(JournalpostId journalpostId, Map<JournalpostId, MottattDokument> mottatteDokumenter) {
        if (journalpostId == null) {
            // Journalposter er ikke tilgjengelig på IM før juni 2021
            return Optional.empty();
        }

        var mottattDokument = mottatteDokumenter.get(journalpostId);
        if (mottattDokument == null) {
            return Optional.empty();
        }
        var behandlingUuid = behandlingRepository.hentBehandling(mottattDokument.getBehandlingId()).getUuid();
        return Optional.of(behandlingUuid);
    }

    public ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUuid) {
        return årskvantumKlient.hentÅrskvantumForBehandling(behandlingUuid);
    }

    public FullUttaksplan hentFullUttaksplan(Saksnummer saksnummer) {
        return årskvantumKlient.hentFullUttaksplan(saksnummer);
    }

    public FullUttaksplanForBehandlinger hentUttaksplanForBehandling(Saksnummer saksnummer, UUID behandlingUuid) {
        var alleBehandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);

        var relevantBehandling = alleBehandlinger.stream().filter(it -> it.getUuid().equals(behandlingUuid)).findAny().orElseThrow();

        var relevanteBehandlinger = alleBehandlinger.stream()
            .filter(Behandling::erYtelseBehandling)
            .filter(it -> !it.erHenlagt())
            .filter(it -> it.getOpprettetTidspunkt().isBefore(relevantBehandling.getOpprettetTidspunkt())
                || it.getOpprettetTidspunkt().isEqual(relevantBehandling.getOpprettetTidspunkt()))
            .map(Behandling::getUuid)
            .collect(Collectors.toList());

        return årskvantumKlient.hentFullUttaksplanForBehandling(relevanteBehandlinger);
    }

}
