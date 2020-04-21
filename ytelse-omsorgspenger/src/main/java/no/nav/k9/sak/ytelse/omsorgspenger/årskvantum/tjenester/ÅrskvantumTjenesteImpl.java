package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.uttak.FraværPeriodeOmsorgspenger;
import no.nav.k9.sak.kontrakt.uttak.OmsorgspengerUtfall;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.Barn;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResterendeDager;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;

@ApplicationScoped
@Default
public class ÅrskvantumTjenesteImpl implements ÅrskvantumTjeneste {

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

        var årskvantumRequest = new ÅrskvantumRequest();

        var personMedRelasjoner = tpsTjeneste.hentBrukerForAktør(ref.getAktørId());

        for (Familierelasjon familierelasjon : personMedRelasjoner.get().getFamilierelasjoner()) {
            if (familierelasjon.getRelasjonsrolle().equals(RelasjonsRolleType.BARN) && familierelasjon.getHarSammeBosted()) {
                var barn = tpsTjeneste.hentBrukerForFnr(familierelasjon.getPersonIdent());
                årskvantumRequest.getBarna().add(new Barn(familierelasjon.getPersonIdent(), barn.get().getFødselsdato(), barn.get().getDødsdato(), familierelasjon.getHarSammeBosted()));
            }
        }

        var grunnlag = grunnlagRepository.hentOppgittFravær(ref.getBehandlingId());

        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId());

        var inntektsmeldingAggregat = inntektArbeidYtelseGrunnlag.getInntektsmeldinger().orElseThrow();
        var datoForSisteInntektsmelding = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes()
            .stream()
            .map(Inntektsmelding::getInnsendingstidspunkt)
            .max(LocalDateTime::compareTo)
            .orElseThrow();

        årskvantumRequest.setBehandlingUUID(ref.getBehandlingUuid().toString());
        årskvantumRequest.setSaksnummer(ref.getSaksnummer().getVerdi());
        årskvantumRequest.setAktørId(ref.getAktørId().getId());
        årskvantumRequest.setPersonIdent(personMedRelasjoner.get().getPersonIdent());
        årskvantumRequest.setSøkersFødselsdato(personMedRelasjoner.get().getFødselsdato());
        årskvantumRequest.setInnsendingstidspunkt(datoForSisteInntektsmelding);

        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());

        for (WrappedOppgittFraværPeriode wrappedOppgittFraværPeriode : mapOppgittFraværOgVilkårsResultat.utledPerioderMedUtfallHvisAvslåttVilkår(grunnlag, vilkårene)) {
            var fraværPeriode = wrappedOppgittFraværPeriode.getPeriode();
            var periode = new Periode(fraværPeriode.getFom(), fraværPeriode.getTom());
            var uttaksperiodeOmsorgspenger = new FraværPeriodeOmsorgspenger(periode,
                null,
                wrappedOppgittFraværPeriode.getErAvslått() ? OmsorgspengerUtfall.AVSLÅTT : null,
                fraværPeriode.getFraværPerDag(),
                null);
            Arbeidsgiver arb = fraværPeriode.getArbeidsgiver();

            if (arb == null) {
                uttaksperiodeOmsorgspenger.setUttakArbeidsforhold(new UttakArbeidsforhold(null, null, fraværPeriode.getAktivitetType(), null));
            } else {
                var arbeidsforholdRef = fraværPeriode.getArbeidsforholdRef() == null ? InternArbeidsforholdRef.nullRef() : fraværPeriode.getArbeidsforholdRef();
                String arbeidsforholdId = arbeidsforholdRef.getReferanse();
                var kreverRefusjon = kreverArbeidsgiverRefusjon(inntektsmeldingAggregat, arb, arbeidsforholdRef, fraværPeriode.getPeriode());
                uttaksperiodeOmsorgspenger.setKreverRefusjon(kreverRefusjon);
                uttaksperiodeOmsorgspenger.setUttakArbeidsforhold(new UttakArbeidsforhold(arb.getOrgnr(),
                    arb.getAktørId(),
                    fraværPeriode.getAktivitetType(),
                    arbeidsforholdId));
            }
            årskvantumRequest.getUttaksperioder().add(uttaksperiodeOmsorgspenger);
        }
        return årskvantumKlient.hentÅrskvantumUttak(årskvantumRequest);
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
    public ÅrskvantumResultat hentÅrskvantumForBehandling(BehandlingReferanse ref) {
        return årskvantumKlient.hentÅrskvantumForBehandling(ref.getBehandlingUuid());
    }

    @Override
    public Periode hentPeriodeForFagsak(Saksnummer saksnummer) {
        return årskvantumKlient.hentPeriodeForFagsak(saksnummer.getVerdi());
    }

    @Override
    public ÅrskvantumResterendeDager hentResterendeKvantum(String aktørid) {
        return årskvantumKlient.hentResterendeKvantum(aktørid);
    }

}
