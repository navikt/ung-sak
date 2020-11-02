package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.aarskvantum.kontrakter.Arbeidsforhold;
import no.nav.k9.aarskvantum.kontrakter.ArbeidsforholdStatus;
import no.nav.k9.aarskvantum.kontrakter.Barn;
import no.nav.k9.aarskvantum.kontrakter.BarnType;
import no.nav.k9.aarskvantum.kontrakter.FraværPeriode;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplan;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplanForBehandlinger;
import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDager;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumGrunnlag;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumUtbetalingGrunnlag;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
@Default
public class ÅrskvantumTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ÅrskvantumTjeneste.class);

    private final MapOppgittFraværOgVilkårsResultat mapOppgittFraværOgVilkårsResultat = new MapOppgittFraværOgVilkårsResultat();
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private ÅrskvantumKlient årskvantumKlient;
    private TpsTjeneste tpsTjeneste;
    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

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
                              @FagsakYtelseTypeRef("OMP") VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                              TrekkUtFraværTjeneste trekkUtFraværTjeneste) {
        this.grunnlagRepository = grunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.årskvantumKlient = årskvantumRestKlient;
        this.tpsTjeneste = tpsTjeneste;
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    public void deaktiverUttakForBehandling(UUID behandlingUuid) {
        årskvantumKlient.deaktiverUttakForBehandling(behandlingUuid);
    }

    public void bekreftUttaksplan(Long behandlingId) {
        årskvantumKlient.settUttaksplanTilManueltBekreftet(behandlingRepository.hentBehandling(behandlingId).getUuid());

    }

    public void slettUttaksplan(Long behandlingId) {
        årskvantumKlient.slettUttaksplan(behandlingRepository.hentBehandling(behandlingId).getUuid());
    }

    public ÅrskvantumGrunnlag hentInputTilBeregning(UUID behandlingUuid) {
        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingUuid));
        return hentForRef(ref);
    }

    private ÅrskvantumGrunnlag hentForRef(BehandlingReferanse ref) {
        var personMedRelasjoner = tpsTjeneste.hentBrukerForAktør(ref.getAktørId()).orElseThrow();

        var barna = personMedRelasjoner.getFamilierelasjoner()
            .stream()
            .filter(it -> it.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .filter(it -> !it.getPersonIdent().erFdatNummer())
            .map(this::innhentPersonopplysningForBarn)
            .map(this::mapBarn)
            .collect(Collectors.toList());

        var grunnlag = grunnlagRepository.hentOppgittFravær(ref.getBehandlingId());
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        var sakInntektsmeldinger = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());
        var inntektsmeldingAggregat = inntektArbeidYtelseGrunnlag.getInntektsmeldinger().orElseThrow();
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        var perioder = utledPerioderRelevantForBehandling(behandling, grunnlag);

        var fraværPerioder = mapUttaksPerioder(ref, vilkårene, inntektArbeidYtelseGrunnlag, sakInntektsmeldinger, perioder, behandling);

        /**
         * @deprecated FIXME TSF-1101 Frode Lindås: innsendingstidpsunktet er ikke nødvendigvis satt korrekt. Årskvantum etablerer ny måte å sortere
         *             uttaksplaner på.
         */
        @Deprecated
        var datoForSisteInntektsmelding = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes()
            .stream()
            .map(Inntektsmelding::getInnsendingstidspunkt)
            .max(LocalDateTime::compareTo)
            .orElseThrow();

        return new ÅrskvantumGrunnlag(ref.getSaksnummer().getVerdi(),
            datoForSisteInntektsmelding,
            ref.getBehandlingUuid().toString(),
            fraværPerioder,
            personMedRelasjoner.getPersonIdent().getIdent(),
            personMedRelasjoner.getFødselsdato(),
            personMedRelasjoner.getDødsdato(),
            barna);
    }

    private Set<no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode> utledPerioderRelevantForBehandling(Behandling behandling, OppgittFravær grunnlag) {
        var vilkårsperioder = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.OPPTJENINGSVILKÅRET);
        var fagsakFravær = trekkUtFraværTjeneste.fraværMedInnsendingstidspunktFraInntektsmeldingerPåFagsak(behandling);

        var behandlingFravær = grunnlag.getPerioder();
        return utledPerioder(vilkårsperioder, fagsakFravær, behandlingFravær);
    }

    Set<no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode> utledPerioder(NavigableSet<DatoIntervallEntitet> vilkårsperioder,
                                                                                                      List<no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode> fagsakFravær,
                                                                                                      Set<OppgittFraværPeriode> behandlingFravær) {
        return fagsakFravær.stream()
            .filter(it -> vilkårsperioder.stream().anyMatch(at -> at.overlapper(it.getPeriode().getPeriode())) || (Duration.ZERO.equals(it.getPeriode().getFraværPerDag()) &&
                behandlingFravær.stream()
                    .anyMatch(at -> at.getPeriode().overlapper(it.getPeriode().getPeriode()) && Duration.ZERO.equals(at.getFraværPerDag()) && matcherArbeidsforhold(it.getPeriode(), at))))
            .collect(Collectors.toSet());
    }

    private boolean matcherArbeidsforhold(OppgittFraværPeriode periode, OppgittFraværPeriode at) {
        if (periode.getAktivitetType().erArbeidstakerEllerFrilans() && at.getAktivitetType().erArbeidstakerEllerFrilans()) {
            return periode.getArbeidsgiver().equals(at.getArbeidsgiver()) && periode.getArbeidsforholdRef().equals(at.getArbeidsforholdRef());
        }
        return periode.getAktivitetType().equals(at.getAktivitetType());
    }

    public ÅrskvantumResultat beregnÅrskvantumUttak(BehandlingReferanse ref) {
        var årskvantumRequest = hentForRef(ref);

        try {
            log.debug("Sender request til årskvantum" +
                "\nrequest='{}'", JsonObjectMapper.getJson(årskvantumRequest));
        } catch (IOException e) {
            log.info("Feilet i serialisering av årskvantum request: " + årskvantumRequest);
        }
        return årskvantumKlient.hentÅrskvantumUttak(årskvantumRequest);
    }

    public ÅrskvantumUtbetalingGrunnlag hentUtbetalingGrunnlag(UUID behandlingUuid) {
        var inputTilBeregning = hentInputTilBeregning(behandlingUuid);
        return årskvantumKlient.hentUtbetalingGrunnlag(inputTilBeregning);
    }

    private ArrayList<FraværPeriode> mapUttaksPerioder(BehandlingReferanse ref,
                                                       Vilkårene vilkårene,
                                                       InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                       Set<Inntektsmelding> sakInntektsmeldinger,
                                                       Set<no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode> perioder,
                                                       Behandling behandling) {
        var fraværPerioder = new ArrayList<FraværPeriode>();
        var fraværsPerioderMedUtfallOgPerArbeidsgiver = mapOppgittFraværOgVilkårsResultat.utledPerioderMedUtfall(ref, iayGrunnlag, vilkårene, behandling.getFagsak().getPeriode(), perioder)
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        for (WrappedOppgittFraværPeriode wrappedOppgittFraværPeriode : fraværsPerioderMedUtfallOgPerArbeidsgiver) {
            var fraværPeriode = wrappedOppgittFraværPeriode.getPeriode();
            var periode = new LukketPeriode(fraværPeriode.getFom(), fraværPeriode.getTom());

            Arbeidsgiver arb = fraværPeriode.getArbeidsgiver();

            Arbeidsforhold arbeidsforhold;
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
            var uttaksperiodeOmsorgspenger = new FraværPeriode(arbeidsforhold,
                utledArbeidsforholdStatus(wrappedOppgittFraværPeriode),
                periode,
                fraværPeriode.getFraværPerDag(),
                true,
                kreverRefusjon,
                utledUtfallIngangsvilkår(wrappedOppgittFraværPeriode),
                wrappedOppgittFraværPeriode.getInnsendingstidspunkt());
            fraværPerioder.add(uttaksperiodeOmsorgspenger);
        }
        return fraværPerioder;
    }

    private Utfall utledUtfallIngangsvilkår(WrappedOppgittFraværPeriode wrappedOppgittFraværPeriode) {
        var erAvslåttInngangsvilkår = wrappedOppgittFraværPeriode.getErAvslåttInngangsvilkår();
        return erAvslåttInngangsvilkår != null && erAvslåttInngangsvilkår ? Utfall.AVSLÅTT : Utfall.INNVILGET;
    }

    private ArbeidsforholdStatus utledArbeidsforholdStatus(WrappedOppgittFraværPeriode wrappedOppgittFraværPeriode) {
        if (wrappedOppgittFraværPeriode.getErIkkeIArbeid() != null && wrappedOppgittFraværPeriode.getErIkkeIArbeid()) {
            return ArbeidsforholdStatus.AVSLUTTET;
        }
        if (wrappedOppgittFraværPeriode.getErIPermisjon() != null && wrappedOppgittFraværPeriode.getErIPermisjon()) {
            return ArbeidsforholdStatus.PERMITERT;
        }
        return ArbeidsforholdStatus.AKTIVT;
    }

    private Tuple<Familierelasjon, Optional<Personinfo>> innhentPersonopplysningForBarn(Familierelasjon it) {
        return new Tuple<>(it, tpsTjeneste.hentBrukerForFnr(it.getPersonIdent()));
    }

    private no.nav.k9.aarskvantum.kontrakter.Barn mapBarn(Tuple<Familierelasjon, Optional<Personinfo>> it) {
        var personinfo = it.getElement2().orElseThrow();
        return new Barn(personinfo.getPersonIdent().getIdent(), personinfo.getFødselsdato(), personinfo.getDødsdato(), it.getElement1().getHarSammeBosted(), BarnType.VANLIG);
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

    public ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUuid) {
        return årskvantumKlient.hentÅrskvantumForBehandling(behandlingUuid);
    }

    public FullUttaksplan hentFullUttaksplan(Saksnummer saksnummer) {
        return årskvantumKlient.hentFullUttaksplan(saksnummer);
    }

    public Periode hentPeriodeForFagsak(Saksnummer saksnummer) {
        return årskvantumKlient.hentPeriodeForFagsak(saksnummer);
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
