package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.aarskvantum.kontrakter.Arbeidsforhold;
import no.nav.k9.aarskvantum.kontrakter.Barn;
import no.nav.k9.aarskvantum.kontrakter.FraværPeriode;
import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDager;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumGrunnlag;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
@Default
public class ÅrskvantumTjenesteImpl implements ÅrskvantumTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ÅrskvantumTjenesteImpl.class);

    private final MapOppgittFraværOgVilkårsResultat mapOppgittFraværOgVilkårsResultat = new MapOppgittFraværOgVilkårsResultat();
    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private ÅrskvantumKlient årskvantumKlient;
    private TpsTjeneste tpsTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    ÅrskvantumTjenesteImpl() {
        // CDI
    }

    @Inject
    public ÅrskvantumTjenesteImpl(OmsorgspengerGrunnlagRepository grunnlagRepository,
                                  VilkårResultatRepository vilkårResultatRepository,
                                  InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                  ÅrskvantumRestKlient årskvantumRestKlient,
                                  TpsTjeneste tpsTjeneste) {
        this.grunnlagRepository = grunnlagRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.årskvantumKlient = årskvantumRestKlient;
        this.tpsTjeneste = tpsTjeneste;

    }

    @Override
    public ÅrskvantumResultat hentÅrskvantumUttak(BehandlingReferanse ref) {

        var personMedRelasjoner = tpsTjeneste.hentBrukerForAktør(ref.getAktørId()).orElseThrow();

        var barna = personMedRelasjoner.getFamilierelasjoner()
            .stream()
            .filter(it -> it.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(this::innhentPersonopplysningForBarn)
            .map(this::mapBarn)
            .collect(Collectors.toList());

        var grunnlag = grunnlagRepository.hentOppgittFravær(ref.getBehandlingId());
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        var inntektsmeldingAggregat = inntektArbeidYtelseGrunnlag.getInntektsmeldinger().orElseThrow();

        var fraværPerioder = mapUttaksPerioder(grunnlag, vilkårene, inntektsmeldingAggregat);

        var datoForSisteInntektsmelding = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes()
            .stream()
            .map(Inntektsmelding::getInnsendingstidspunkt)
            .max(LocalDateTime::compareTo)
            .orElseThrow();

        var årskvantumRequest = new ÅrskvantumGrunnlag(ref.getSaksnummer().getVerdi(),
            datoForSisteInntektsmelding,
            ref.getBehandlingUuid().toString(),
            fraværPerioder,
            personMedRelasjoner.getPersonIdent().getIdent(),
            personMedRelasjoner.getFødselsdato(),
            barna);

        try {
            log.debug("Sender request til årskvantum" +
                "\nrequest='{}'", JsonObjectMapper.getJson(årskvantumRequest));
        } catch (IOException e) {
            log.info("Feilet i serialisering av årskvantum request: " + årskvantumRequest);
        }
        return årskvantumKlient.hentÅrskvantumUttak(årskvantumRequest);
    }

    @NotNull
    private ArrayList<FraværPeriode> mapUttaksPerioder(OppgittFravær grunnlag, Vilkårene vilkårene, InntektsmeldingAggregat inntektsmeldingAggregat) {
        var fraværPerioder = new ArrayList<FraværPeriode>();
        var fraværsPerioderMedUtfallOgPerArbeidsgiver = mapOppgittFraværOgVilkårsResultat.utledPerioderMedUtfallHvisAvslåttVilkår(grunnlag, vilkårene)
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
                kreverRefusjon = kreverArbeidsgiverRefusjon(inntektsmeldingAggregat, arb, arbeidsforholdRef, fraværPeriode.getPeriode());
                arbeidsforhold = new Arbeidsforhold(fraværPeriode.getAktivitetType().getKode(),
                    arb.getOrgnr(),
                    arb.getAktørId() != null ? arb.getAktørId().getId() : null,
                    arbeidsforholdId);
            }
            var uttaksperiodeOmsorgspenger = new FraværPeriode(arbeidsforhold,
                periode,
                fraværPeriode.getFraværPerDag(),
                true,
                kreverRefusjon,
                wrappedOppgittFraværPeriode.getErAvslått() ? Utfall.AVSLÅTT : Utfall.INNVILGET);
            fraværPerioder.add(uttaksperiodeOmsorgspenger);
        }
        return fraværPerioder;
    }

    @NotNull
    private Tuple<Familierelasjon, Optional<Personinfo>> innhentPersonopplysningForBarn(Familierelasjon it) {
        return new Tuple<>(it, tpsTjeneste.hentBrukerForFnr(it.getPersonIdent()));
    }

    @NotNull
    private no.nav.k9.aarskvantum.kontrakter.Barn mapBarn(Tuple<Familierelasjon, Optional<Personinfo>> it) {
        return new Barn(it.getElement2().orElseThrow().getPersonIdent().getIdent(), it.getElement2().orElseThrow().getFødselsdato(), it.getElement2().orElseThrow().getDødsdato(), it.getElement1().getHarSammeBosted());
    }

    private boolean kreverArbeidsgiverRefusjon(InntektsmeldingAggregat inntektsmeldingAggregat,
                                               Arbeidsgiver arbeidsgiver,
                                               InternArbeidsforholdRef arbeidsforholdRef,
                                               DatoIntervallEntitet periode) {
        var inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerFor(arbeidsgiver);
        var inntektsmeldingSomMatcherUttak = inntektsmeldinger.stream()
            .filter(it -> it.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef)) // TODO: Bør vi matcher på gjelderfor her? Perioder som er sendt inn med arbeidsforholdsId vil da matche med inntekstmeldinger uten for samme arbeidsgiver men hvor perioden overlapper
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
                .max(Comparator.comparing(Inntektsmelding::getInnsendingstidspunkt))
                .map(Inntektsmelding::getRefusjonBeløpPerMnd)
                .map(Beløp::getVerdi)
                .orElse(BigDecimal.ZERO);
            return BigDecimal.ZERO.compareTo(verdi) < 0;
        }
    }

    @Override
    public ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUuid) {
        return årskvantumKlient.hentÅrskvantumForBehandling(behandlingUuid);
    }

    @Override
    public Periode hentPeriodeForFagsak(Saksnummer saksnummer) {
        return årskvantumKlient.hentPeriodeForFagsak(saksnummer.getVerdi());
    }

}
