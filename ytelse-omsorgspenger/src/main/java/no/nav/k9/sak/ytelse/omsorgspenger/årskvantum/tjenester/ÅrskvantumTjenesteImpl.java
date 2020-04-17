package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.kontrakt.uttak.FraværPeriodeOmsorgspenger;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.Barn;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResterendeDager;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.time.LocalDateTime;

@ApplicationScoped
@Default
public class ÅrskvantumTjenesteImpl implements ÅrskvantumTjeneste {

    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private ÅrskvantumKlient årskvantumKlient;
    private TpsTjeneste tpsTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    public ÅrskvantumTjenesteImpl(OmsorgspengerGrunnlagRepository grunnlagRepository,
                                  InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                  ÅrskvantumRestKlient årskvantumRestKlient,
                                  TpsTjeneste tpsTjeneste) {
        this.grunnlagRepository = grunnlagRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.årskvantumKlient = årskvantumRestKlient;
        this.tpsTjeneste = tpsTjeneste;

    }

    @Override
    public ÅrskvantumResultat hentÅrskvantumUttak(BehandlingReferanse ref) {

        var årskvantumRequest = new ÅrskvantumRequest();

        var personMedRelasjoner = tpsTjeneste.hentBrukerForAktør(ref.getAktørId());

        for (Familierelasjon familierelasjon :personMedRelasjoner.get().getFamilierelasjoner()) {
            if (familierelasjon.getRelasjonsrolle().equals(RelasjonsRolleType.BARN) && familierelasjon.getHarSammeBosted()) {
                var barn = tpsTjeneste.hentBrukerForFnr(familierelasjon.getPersonIdent());
                årskvantumRequest.getBarna().add(new Barn(familierelasjon.getPersonIdent(), barn.get().getFødselsdato(), barn.get().getDødsdato(), familierelasjon.getHarSammeBosted()));
            }
        }

        var grunnlag = grunnlagRepository.hentOppgittFravær(ref.getBehandlingId());

        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId());

        LocalDateTime datoForSisteInntektsmelding = inntektArbeidYtelseGrunnlag.getInntektsmeldinger().get().getInntektsmeldingerSomSkalBrukes().stream().map(
            Inntektsmelding::getInnsendingstidspunkt).max(LocalDateTime::compareTo).get();

        årskvantumRequest.setBehandlingUUID(ref.getBehandlingUuid().toString());
        årskvantumRequest.setSaksnummer(ref.getSaksnummer().getVerdi());
        årskvantumRequest.setAktørId(ref.getAktørId().getId());
        årskvantumRequest.setPersonIdent(personMedRelasjoner.get().getPersonIdent());
        årskvantumRequest.setSøkersFødselsdato(personMedRelasjoner.get().getFødselsdato());
        årskvantumRequest.setInnsendingstidspunkt(datoForSisteInntektsmelding);

        for (OppgittFraværPeriode fraværPeriode : grunnlag.getPerioder()) {

            FraværPeriodeOmsorgspenger
                uttaksperiodeOmsorgspenger = new FraværPeriodeOmsorgspenger(new Periode(fraværPeriode.getFom(), fraværPeriode.getTom()),
                                                                            null,
                                                                            null,
                                                                            fraværPeriode.getFraværPerDag(), null);
            Arbeidsgiver arb = fraværPeriode.getArbeidsgiver();

            if (arb == null) {
                uttaksperiodeOmsorgspenger.setUttakArbeidsforhold(new UttakArbeidsforhold(null, null, fraværPeriode.getAktivitetType(), null));
            } else {
                String arbeidsforholdId = fraværPeriode.getArbeidsforholdRef() == null ? null : fraværPeriode.getArbeidsforholdRef().getReferanse();
                uttaksperiodeOmsorgspenger.setUttakArbeidsforhold(new UttakArbeidsforhold(arb.getOrgnr(),
                    arb.getAktørId(),
                    fraværPeriode.getAktivitetType(),
                    arbeidsforholdId));
            }
            årskvantumRequest.getUttaksperioder().add(uttaksperiodeOmsorgspenger);
        }
        return årskvantumKlient.hentÅrskvantumUttak(årskvantumRequest);
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
