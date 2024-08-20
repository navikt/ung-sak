package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
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
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

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
import no.nav.k9.aarskvantum.kontrakter.BarnFødselsdato;
import no.nav.k9.aarskvantum.kontrakter.BarnType;
import no.nav.k9.aarskvantum.kontrakter.FraværPeriode;
import no.nav.k9.aarskvantum.kontrakter.FraværÅrsak;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplan;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplanForBehandlinger;
import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.RammevedtakResponse;
import no.nav.k9.aarskvantum.kontrakter.RammevedtakV2Request;
import no.nav.k9.aarskvantum.kontrakter.SøknadÅrsak;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksplan;
import no.nav.k9.aarskvantum.kontrakter.Vilkår;
import no.nav.k9.aarskvantum.kontrakter.VurderteVilkår;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDager;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDagerV2;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumGrunnlag;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumGrunnlagV2;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumUtbetalingGrunnlag;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumUttrekk;
import no.nav.k9.felles.integrasjon.rest.DefaultJsonMapper;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.person.Diskresjonskode;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.k9.sak.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.omsorg.OmsorgenForTjeneste;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkårGrunnlag;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetTypeArbeidsgiver;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.OppgittFraværHolder;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.SamtidigKravStatus;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.FosterbarnRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.VedtatteRammevedtakTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;

@ApplicationScoped
@Default
public class ÅrskvantumTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ÅrskvantumTjeneste.class);

    private MapOppgittFraværOgVilkårsResultat mapOppgittFraværOgVilkårsResultat;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private ÅrskvantumKlient årskvantumKlient;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private AktørTjeneste aktørTjeneste;
    private TpsTjeneste tpsTjeneste;
    private FosterbarnRepository fosterbarnRepository;
    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private VedtatteRammevedtakTjeneste vedtatteRammevedtakTjeneste;
    private boolean rammevedtakSammenstillingIÅrskvantum;
    private OmsorgenForTjeneste omsorgenForTjeneste;

    ÅrskvantumTjeneste() {
        // CDI
    }

    @Inject
    public ÅrskvantumTjeneste(BehandlingRepository behandlingRepository,
                              OmsorgspengerGrunnlagRepository grunnlagRepository,
                              BasisPersonopplysningTjeneste personopplysningTjeneste, VilkårResultatRepository vilkårResultatRepository,
                              InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                              ÅrskvantumRestKlient årskvantumRestKlient,
                              AktørTjeneste aktørTjeneste,
                              TpsTjeneste tpsTjeneste,
                              FosterbarnRepository fosterbarnRepository,
                              @FagsakYtelseTypeRef(OMSORGSPENGER) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                              TrekkUtFraværTjeneste trekkUtFraværTjeneste,
                              OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste,
                              MottatteDokumentRepository mottatteDokumentRepository,
                              OmsorgenForTjeneste omsorgenForTjeneste,
                              VedtatteRammevedtakTjeneste vedtatteRammevedtakTjeneste,
                            @KonfigVerdi(value = "OMP_RAMMEVEDTAK_SAMMENSTILLNG_AARSKVANTUM", defaultVerdi = "false") boolean rammevedtakSammenstillingIÅrskvantum) {
        this.grunnlagRepository = grunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.årskvantumKlient = årskvantumRestKlient;
        this.aktørTjeneste = aktørTjeneste;
        this.tpsTjeneste = tpsTjeneste;
        this.fosterbarnRepository = fosterbarnRepository;
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.opptjeningTjeneste = opptjeningTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.vedtatteRammevedtakTjeneste = vedtatteRammevedtakTjeneste;
        this.rammevedtakSammenstillingIÅrskvantum = rammevedtakSammenstillingIÅrskvantum;
        this.mapOppgittFraværOgVilkårsResultat = new MapOppgittFraværOgVilkårsResultat();
        this.omsorgenForTjeneste = omsorgenForTjeneste;
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

    public ÅrskvantumGrunnlagV2 hentInputTilBeregningV2(UUID behandlingUuid) {
        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingUuid));
        return hentForRefV2(ref);
    }

    public Integer utførAktørbytte(PersonIdent nyPersonident, Set<PersonIdent> gamlePersonidenter) {
        return årskvantumKlient.oppdaterPersonident(nyPersonident, gamlePersonidenter.stream().toList());
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
        var opptjeningAktiveter = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, vilkårsperioder);
        var fraværPerioder = mapUttaksPerioder(ref, vilkårene, inntektArbeidYtelseGrunnlag, opptjeningAktiveter, relevantePerioder, behandling);

        if (fraværPerioder.isEmpty()) {
            // kan ikke være empty når vi sender årskvantum
            throw new IllegalStateException("Har ikke fraværs perioder for fagsak.periode[" + ref.getFagsakPeriode() + "]"
                + ",\trelevant perioder=" + relevantePerioder
                + ",\toppgitt fravær er tom="
                + ",\tvilkårsperioder[" + vilkårType + "]=" + vilkårsperioder
                + ",\tfagsakFravær=" + fagsakFravær);
        }

        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(ref.getBehandlingId(), ref.getAktørId(), hentInformasjonsperiode(vilkårsperioder, oppgittFravær)).orElseThrow();
        PersonopplysningEntitet søkerPersonopplysninger = personopplysninger.getSøker();

        var alleBarna = hentOgMapBarn(personopplysninger, behandling);

        return new ÅrskvantumGrunnlag(ref.getSaksnummer().getVerdi(),
            ref.getBehandlingUuid().toString(),
            fraværPerioder,
            tpsTjeneste.hentFnrForAktør(ref.getAktørId()).getIdent(),
            søkerPersonopplysninger.getFødselsdato(),
            søkerPersonopplysninger.getDødsdato(),
            new ArrayList<>(alleBarna));
    }

    private ÅrskvantumGrunnlagV2 hentForRefV2(BehandlingReferanse ref) {
        var oppgittFravær = grunnlagRepository.hentSammenslåtteFraværPerioder(ref.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());

        VilkårType vilkårType = VilkårType.OPPTJENINGSVILKÅRET;
        var vilkårsperioder = perioderTilVurderingTjeneste.utled(behandling.getId(), vilkårType);
        var fagsakFravær = trekkUtFraværTjeneste.fraværFraKravDokumenterPåFagsakMedSøknadsfristVurdering(behandling);
        var relevantePerioder = utledPerioder(vilkårsperioder, fagsakFravær, oppgittFravær);

        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId());
        var opptjeningAktiveter = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, vilkårsperioder);
        var fraværPerioder = mapUttaksPerioder(ref, vilkårene, inntektArbeidYtelseGrunnlag, opptjeningAktiveter, relevantePerioder, behandling);

        if (fraværPerioder.isEmpty()) {
            // kan ikke være empty når vi sender årskvantum
            throw new IllegalStateException("Har ikke fraværs perioder for fagsak.periode[" + ref.getFagsakPeriode() + "]"
                + ",\trelevant perioder=" + relevantePerioder
                + ",\toppgitt fravær er tom="
                + ",\tvilkårsperioder[" + vilkårType + "]=" + vilkårsperioder
                + ",\tfagsakFravær=" + fagsakFravær);
        }

        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(ref.getBehandlingId(), ref.getAktørId(), hentInformasjonsperiode(vilkårsperioder, oppgittFravær)).orElseThrow();
        PersonopplysningEntitet søkerPersonopplysninger = personopplysninger.getSøker();

        var alleBarna = hentOgMapBarn(personopplysninger, behandling);

        VedtatteRammevedtakTjeneste.InnvilgedeOgAvslåtteRammevedtak innvilgedeOgAvslåtteRammevedtak = vedtatteRammevedtakTjeneste.hentK9sakRammevedtak(ref.getAktørId());

        return new ÅrskvantumGrunnlagV2(ref.getSaksnummer().getVerdi(),
            ref.getBehandlingUuid().toString(),
            fraværPerioder,
            tpsTjeneste.hentFnrForAktør(ref.getAktørId()).getIdent(),
            søkerPersonopplysninger.getFødselsdato(),
            søkerPersonopplysninger.getDødsdato(),
            new ArrayList<>(alleBarna),
            innvilgedeOgAvslåtteRammevedtak.innvilgede(),
            innvilgedeOgAvslåtteRammevedtak.avslåtte());
    }

    private static DatoIntervallEntitet omsluttende(Collection<DatoIntervallEntitet> perioder) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(
            perioder.stream().map(DatoIntervallEntitet::getFomDato).min(Comparator.naturalOrder()).orElseThrow(),
            perioder.stream().map(DatoIntervallEntitet::getTomDato).max(Comparator.naturalOrder()).orElseThrow()
        );
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
        if (rammevedtakSammenstillingIÅrskvantum){
            var årskvantumRequest = hentForRefV2(ref);
            return årskvantumKlient.hentÅrskvantumUttak(årskvantumRequest);
        }
        var årskvantumRequest = hentForRef(ref);
        return årskvantumKlient.hentÅrskvantumUttak(årskvantumRequest);
    }

    public ÅrskvantumUtbetalingGrunnlag hentUtbetalingGrunnlag(UUID behandlingUuid) {
        if (rammevedtakSammenstillingIÅrskvantum){
            var inputTilBeregning = hentInputTilBeregningV2(behandlingUuid);
            return årskvantumKlient.hentUtbetalingGrunnlag(inputTilBeregning);
        }
        var inputTilBeregning = hentInputTilBeregning(behandlingUuid);
        return årskvantumKlient.hentUtbetalingGrunnlag(inputTilBeregning);
    }

    public RammevedtakResponse hentRammevedtak(AktørId søkerAktørId, LukketPeriode periode, List<Personinfo> barna) {
        PersonIdent personIdent = aktørTjeneste.hentPersonIdentForAktørId(søkerAktørId).orElseThrow();

        if (rammevedtakSammenstillingIÅrskvantum){
            VedtatteRammevedtakTjeneste.InnvilgedeOgAvslåtteRammevedtak k9sakRammevedtak = vedtatteRammevedtakTjeneste.hentK9sakRammevedtak(søkerAktørId);
            RammevedtakV2Request request = new RammevedtakV2Request(personIdent.getIdent(), barna.stream().map(this::mapBarnFødseldato).toList(), periode, k9sakRammevedtak.innvilgede(), k9sakRammevedtak.avslåtte());
            return årskvantumKlient.hentRammevedtak(request);
        }

        var alleBarnasFnr = barna.stream().map(Personinfo::getPersonIdent).toList();
        return årskvantumKlient.hentRammevedtak(personIdent, alleBarnasFnr, periode);
    }

    private BarnFødselsdato mapBarnFødseldato(Personinfo barn) {
        return new BarnFødselsdato(
            barn.getPersonIdent().getIdent(),
            barn.getFødselsdato()
        );
    }

    public RammevedtakResponse hentRammevedtak(LukketPeriode periode, Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling);
        AktørId søkerAktørId = behandling.getAktørId();
        PersonIdent personIdent = aktørTjeneste.hentPersonIdentForAktørId(søkerAktørId).orElseThrow();

        var vilkårsperioder = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.OPPTJENINGSVILKÅRET);
        var oppgittFravær = grunnlagRepository.hentSammenslåtteFraværPerioder(ref.getBehandlingId());

        var informasjonsperiode = (oppgittFravær.isEmpty()) ? DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()) : hentInformasjonsperiode(vilkårsperioder, oppgittFravær);
        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(ref.getBehandlingId(), ref.getAktørId(), informasjonsperiode).orElseThrow();
        Set<Barn> barna = hentOgMapBarn(personopplysninger, behandling);

        if (rammevedtakSammenstillingIÅrskvantum){
            VedtatteRammevedtakTjeneste.InnvilgedeOgAvslåtteRammevedtak k9sakRammevedtak = vedtatteRammevedtakTjeneste.hentK9sakRammevedtak(søkerAktørId);
            List<BarnFødselsdato> barnFødselsdato = barna.stream().map(b -> new BarnFødselsdato(b.getPersonIdent(), b.getFødselsdato())).toList();
            RammevedtakV2Request request = new RammevedtakV2Request(personIdent.getIdent(), barnFødselsdato, periode, k9sakRammevedtak.innvilgede(), k9sakRammevedtak.avslåtte());
            logRequestInDev(request); //logg request for evt. feilsøking
            return årskvantumKlient.hentRammevedtak(request);
        } else if (Environment.current().isDev()){
            //logg request for feilsøking
            VedtatteRammevedtakTjeneste.InnvilgedeOgAvslåtteRammevedtak k9sakRammevedtak = vedtatteRammevedtakTjeneste.hentK9sakRammevedtak(søkerAktørId);
            List<BarnFødselsdato> barnFødselsdato = barna.stream().map(b -> new BarnFødselsdato(b.getPersonIdent(), b.getFødselsdato())).toList();
            RammevedtakV2Request request = new RammevedtakV2Request(personIdent.getIdent(), barnFødselsdato, periode, k9sakRammevedtak.innvilgede(), k9sakRammevedtak.avslåtte());
            logRequestInDev(request);
        }

        var alleBarnasFnr = barna.stream().map(barn -> PersonIdent.fra(barn.getPersonIdent())).toList();
        return årskvantumKlient.hentRammevedtak(personIdent, alleBarnasFnr, periode);
    }

    private static void logRequestInDev(RammevedtakV2Request request){
        if (Environment.current().isDev()){
            String requestString = toJson(request);
            String base64encoded = Base64.getEncoder().encodeToString(requestString.getBytes(StandardCharsets.UTF_8));
            logger.info("Rammevedtak-request: {}", base64encoded);
        }
    }

    private static String toJson(RammevedtakV2Request request){
        try {
            return DefaultJsonMapper.getObjectMapper().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private DatoIntervallEntitet hentInformasjonsperiode(Set<DatoIntervallEntitet> vilkårsperioder, Set<OppgittFraværPeriode> oppgittFravær) {
        return vilkårsperioder.isEmpty() //vilkårsperioder er tom hvis hele kravet er trekt
            ? omsluttende(oppgittFravær.stream().map(OppgittFraværPeriode::getPeriode).toList())
            : omsluttende(vilkårsperioder);
    }

    public ÅrskvantumUttrekk hentUttrekk() {
        return årskvantumKlient.hentUttrekk();
    }

    private List<FraværPeriode> mapUttaksPerioder(BehandlingReferanse ref,
                                                  Vilkårene vilkårene,
                                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
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

        NavigableSet<DatoIntervallEntitet> periodene = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.OMSORGEN_FOR);
        LocalDateTimeline<OmsorgenForVilkårGrunnlag> samletOmsorgenForTidslinje = (harOmsorgenForBlittVurdertIK9sak(fagsakPeriode.getFomDato())) ? omsorgenForTjeneste.mapGrunnlag(ref, periodene) : LocalDateTimeline.empty();

        for (WrappedOppgittFraværPeriode wrappedOppgittFraværPeriode : fraværsPerioderMedUtfallOgPerArbeidsgiver) {
            var fraværPeriode = wrappedOppgittFraværPeriode.getPeriode();
            var periode = new LukketPeriode(fraværPeriode.getFom(), fraværPeriode.getTom());
            Optional<UUID> opprinneligBehandlingUuid = hentBehandlingUuid(fraværPeriode.getJournalpostId(), mottatteDokumenter);

            Arbeidsgiver arb = fraværPeriode.getArbeidsgiver();

            Arbeidsforhold arbeidsforhold;

            SamtidigKravStatus.KravStatus refusjonskravStatusForArbeidsforholdet = wrappedOppgittFraværPeriode.getSamtidigeKrav().inntektsmeldingMedRefusjonskrav(fraværPeriode.getArbeidsforholdRef());
            boolean kreverRefusjon = refusjonskravStatusForArbeidsforholdet == SamtidigKravStatus.KravStatus.FINNES;
            if (arb == null) {
                arbeidsforhold = new Arbeidsforhold(fraværPeriode.getAktivitetType().getKode(), null, null, null);
            } else {
                var arbeidsforholdRef = fraværPeriode.getArbeidsforholdRef() == null ? InternArbeidsforholdRef.nullRef() : fraværPeriode.getArbeidsforholdRef();
                String arbeidsforholdId = arbeidsforholdRef.getReferanse();
                arbeidsforhold = new Arbeidsforhold(fraværPeriode.getAktivitetType().getKode(),
                    arb.getOrgnr(),
                    arb.getAktørId() != null ? arb.getAktørId().getId() : null,
                    arbeidsforholdId);
            }
            var arbeidforholdStatus = utledArbeidsforholdStatus(wrappedOppgittFraværPeriode);
            var avvikImSøknad = utedAvvikImSøknad(wrappedOppgittFraværPeriode);
            var uttaksperiodeOmsorgspenger = new FraværPeriode(arbeidsforhold,
                periode,
                fraværPeriode.getFraværPerDag(),
                true,
                kreverRefusjon,
                wrappedOppgittFraværPeriode.getInnsendingstidspunkt(),
                utledFraværÅrsak(fraværPeriode),
                utledSøknadÅrsak(fraværPeriode),
                opprinneligBehandlingUuid.map(UUID::toString).orElse(null),
                avvikImSøknad,
                utledVurderteVilkår(arbeidforholdStatus, wrappedOppgittFraværPeriode),
                List.of(),
                harOmsorgenForBlittManueltVurdert(samletOmsorgenForTidslinje, fraværPeriode).orElse(null));
            fraværPerioder.add(uttaksperiodeOmsorgspenger);
        }
        return fraværPerioder;
    }

    private Optional<Boolean> harOmsorgenForBlittManueltVurdert(LocalDateTimeline<OmsorgenForVilkårGrunnlag> samletOmsorgenForTidslinje, OppgittFraværPeriode fraværPeriode) {
        if (samletOmsorgenForTidslinje.isEmpty()) {
            // omsorgen for ble ikke vurdert i k9
            return Optional.empty();
        }
        Optional<Boolean> harBlittManueltVurdert = samletOmsorgenForTidslinje.stream()
            .filter(omsorgsperiode -> omsorgsperiode.overlapper(new LocalDateSegment(fraværPeriode.getPeriode().toLocalDateInterval(), true)))
            .findFirst()
            .map(omsorgenFor -> omsorgenFor.getValue().getHarBlittVurdertSomOmsorgsPerson());

        return Optional.of(harBlittManueltVurdert.isPresent());
    }

    private boolean harOmsorgenForBlittVurdertIK9sak(LocalDate fraværsDato) {
        // hvis fraværet er før 2023 blir vilkåret vurdert i årskvantum
        return fraværsDato.isAfter(LocalDate.of(2022, Month.DECEMBER, 31));
    }

    private VurderteVilkår utledVurderteVilkår(ArbeidsforholdStatus arbeidsforholdStatus, WrappedOppgittFraværPeriode wrappedOppgittFraværPeriode) {

        NavigableMap<Vilkår, Utfall> vilkårMap = new TreeMap<>();
        vilkårMap.put(Vilkår.ARBEIDSFORHOLD, arbeidsforholdStatus == ArbeidsforholdStatus.AKTIVT ? Utfall.INNVILGET : Utfall.AVSLÅTT);
        vilkårMap.put(Vilkår.INNGANGSVILKÅR, utledUtfallIngangsvilkår(wrappedOppgittFraværPeriode));
        vilkårMap.put(Vilkår.FRAVÆR_FRA_ARBEID, Utfall.INNVILGET);

        if (wrappedOppgittFraværPeriode.getUtfallNyoppstartetVilkår() != null) {
            vilkårMap.put(Vilkår.NYOPPSTARTET_HOS_ARBEIDSGIVER, wrappedOppgittFraværPeriode.getUtfallNyoppstartetVilkår());
        }

        if (harOmsorgenForBlittVurdertIK9sak(wrappedOppgittFraværPeriode.getPeriode().getFom())) {
            vilkårMap.put(Vilkår.OMSORGSVILKÅRET, utledUtfallOmsorgenFor(wrappedOppgittFraværPeriode));
        }
        return new VurderteVilkår(vilkårMap);
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

    private Utfall utledUtfallOmsorgenFor(WrappedOppgittFraværPeriode wrappedOppgittFraværPeriode) {
        var erAvslåttOmsorgenFor = wrappedOppgittFraværPeriode.getAvslåttOmsorgenFor();
        return erAvslåttOmsorgenFor != null && erAvslåttOmsorgenFor ? Utfall.AVSLÅTT : Utfall.INNVILGET;
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

    private Set<Barn> hentOgMapBarn(PersonopplysningerAggregat personopplysninger, Behandling behandling) {
        var barna = personopplysninger.getSøkersRelasjoner()
            .stream()
            .filter(relasjon -> relasjon.getRelasjonsrolle() == RelasjonsRolleType.BARN)
            .filter(relasjon -> !tpsTjeneste.hentFnrForAktør(relasjon.getTilAktørId()).erFdatNummer())
            .map(barnRelasjon -> personopplysninger.getPersonopplysning(barnRelasjon.getTilAktørId()))
            .map(barnPersonopplysninger -> mapBarn(personopplysninger, personopplysninger.getSøker(), barnPersonopplysninger, behandling.getFagsak().getPeriode()))
            .toList();
        var fosterbarna = fosterbarnRepository.hentHvisEksisterer(behandling.getId())
            .map(grunnlag -> grunnlag.getFosterbarna().getFosterbarn().stream()
                .map(fosterbarn -> innhentPersonopplysningForBarn(fosterbarn.getAktørId()))
                .map(personinfo -> mapFosterbarn(personinfo, behandling.getFagsak().getPeriode()))
                .collect(Collectors.toSet())
            ).orElse(Set.of());
        return Stream.concat(barna.stream(), fosterbarna.stream()).collect(Collectors.toSet());
    }

    private Personinfo innhentPersonopplysningForBarn(AktørId aktørId) {
        return tpsTjeneste.hentBrukerForAktør(aktørId).orElseThrow(() -> new IllegalStateException("Finner ikke ident for aktørid"));
    }

    private no.nav.k9.aarskvantum.kontrakter.Barn mapBarn(PersonopplysningerAggregat personopplysningerAggregat, PersonopplysningEntitet personinfoSøker, PersonopplysningEntitet personinfoBarn, DatoIntervallEntitet fagsakPeriode) {
        List<PersonAdresseEntitet> søkersAdresser = personopplysningerAggregat.getAdresserFor(personinfoSøker.getAktørId());
        List<PersonAdresseEntitet> barnetsAdresser = personopplysningerAggregat.getAdresserFor(personinfoBarn.getAktørId());

        var tidslinjeDeltBosted = AdresseSammenligner.perioderDeltBosted(søkersAdresser, barnetsAdresser);
        var tidslinjeSammeBosted = AdresseSammenligner.sammeBostedsadresse(søkersAdresser, barnetsAdresser);

        PersonIdent personIdentBarn = tpsTjeneste.hentFnrForAktør(personinfoBarn.getAktørId());
        var sammeBostedPerioder = (erKode6(personIdentBarn)) ? utledSammeBostedPerioder(personinfoBarn.getFødselsdato(), fagsakPeriode) : tilLukketPeriode(tidslinjeSammeBosted);

        return new Barn(personIdentBarn.getIdent(), personinfoBarn.getFødselsdato(), personinfoBarn.getDødsdato(), tilLukketPeriode(tidslinjeDeltBosted), sammeBostedPerioder, BarnType.VANLIG);
    }

    private boolean erKode6(PersonIdent personIdentBarn) {
        var kode = tpsTjeneste.hentDiskresjonskodeForAktør(personIdentBarn);
        if (kode.isPresent()) {
            var diskresjonskode = Diskresjonskode.fraKode(kode.get());
            return (Diskresjonskode.KODE6.equals(diskresjonskode));
        }
        return false;
    }

    private static List<LukketPeriode> tilLukketPeriode(LocalDateTimeline<Boolean> tidslinje) {
        return tidslinje.filterValue(Boolean.TRUE::equals)
            .stream()
            .map(segment -> new LukketPeriode(segment.getFom(), segment.getTom()))
            .toList();
    }

    private Barn mapFosterbarn(Personinfo personinfo, DatoIntervallEntitet fagsakPeriode) {
        //midlertidig løsning, skal helst ha reell start-dato. Ønsket fikset ved at vi informasjon om fosterbarn fra register
        var sammeBostedPerioder = utledSammeBostedPerioder(personinfo.getFødselsdato(), fagsakPeriode);

        return new Barn(personinfo.getPersonIdent().getIdent(), personinfo.getFødselsdato(), personinfo.getDødsdato(), List.of(), sammeBostedPerioder, BarnType.FOSTERBARN);
    }

    private List<LukketPeriode> utledSammeBostedPerioder(LocalDate barnFødselsdato, DatoIntervallEntitet fagsakPeriode) {
        LocalDateTimeline<Boolean> erBarn = new LocalDateTimeline<>(barnFødselsdato, barnFødselsdato.plusYears(18).withMonth(12).withDayOfMonth(31), true);
        LocalDateTimeline<Boolean> harFagsak = new LocalDateTimeline<>(fagsakPeriode.getFomDato(), fagsakPeriode.getTomDato(), true);
        LocalDateTimeline<Boolean> harFagsakOgErBarn = erBarn.intersection(harFagsak, StandardCombinators::alwaysTrueForMatch);
        return harFagsakOgErBarn.stream()
            .map(segment -> new LukketPeriode(segment.getFom(), segment.getTom()))
            .toList();
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

    public Uttaksplan getSisteUttaksplan(UUID behandlingUuid){
        if (rammevedtakSammenstillingIÅrskvantum){
            return årskvantumKlient.hentÅrskvantumForBehandlingV2(behandlingUuid).getSisteUttaksplan();
        }
        return årskvantumKlient.hentÅrskvantumForBehandling(behandlingUuid).getSisteUttaksplan();
    }

    public ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUuid) {
        return årskvantumKlient.hentÅrskvantumForBehandling(behandlingUuid);
    }

    public ÅrskvantumForbrukteDagerV2 hentÅrskvantumForBehandlingV2(UUID behandlingUuid) {
        return årskvantumKlient.hentÅrskvantumForBehandlingV2(behandlingUuid);
    }

    public FullUttaksplan hentFullUttaksplan(Saksnummer saksnummer) {
        return årskvantumKlient.hentFullUttaksplan(saksnummer);
    }

    public FullUttaksplanForBehandlinger hentUttaksplanForBehandling(Saksnummer saksnummer, UUID behandlingUuid) {
        var alleBehandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);

        var relevantBehandling = alleBehandlinger.stream().filter(it -> it.getUuid().equals(behandlingUuid)).findAny().orElseThrow();
        return hentUttaksplanForBehandling(saksnummer, relevantBehandling.getId());
    }

    public FullUttaksplanForBehandlinger hentUttaksplanForBehandling(Saksnummer saksnummer, Long behandlingId) {
        var alleBehandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);

        var relevantBehandling = alleBehandlinger.stream().filter(it -> it.getId().equals(behandlingId)).findAny().orElseThrow();

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
