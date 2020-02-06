package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk.MapOpptjeningAktivitetTypeFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Aktivitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.AktivPeriode;
import no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.AktivitetStatusModell;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class MapBeregningAktiviteterFraVLTilRegel {

    public static final String INGEN_AKTIVITET_MELDING = "Må ha aktiviteter for å sette status.";

    public MapBeregningAktiviteterFraVLTilRegel() {
    }

    public AktivitetStatusModell mapForSkjæringstidspunkt(BeregningsgrunnlagInput input) {
        LocalDate opptjeningSkjæringstidspunkt = input.getSkjæringstidspunktOpptjening();

        AktivitetStatusModell modell = new AktivitetStatusModell();
        modell.setSkjæringstidspunktForOpptjening(opptjeningSkjæringstidspunkt);

        var relevanteAktiviteter = input.getOpptjeningAktiviteterForBeregning();
        
        if (relevanteAktiviteter.isEmpty()) { // For enklere feilsøking når det mangler aktiviteter
            throw new IllegalStateException(INGEN_AKTIVITET_MELDING);
        } else {
            relevanteAktiviteter.forEach(opptjeningsperiode -> modell.leggTilEllerOppdaterAktivPeriode(lagAktivPeriode(input.getInntektsmeldinger(), opptjeningsperiode)));
        }
        return modell;
    }

    private AktivPeriode lagAktivPeriode(Collection<Inntektsmelding> inntektsmeldinger,
                                         OpptjeningAktiviteter.OpptjeningPeriode opptjeningsperiode) {
        Aktivitet aktivitetType = MapOpptjeningAktivitetTypeFraVLTilRegel.map(opptjeningsperiode.getOpptjeningAktivitetType());
        Periode gjeldendePeriode = opptjeningsperiode.getPeriode();

        if (Aktivitet.FRILANSINNTEKT.equals(aktivitetType)) {
            return AktivPeriode.forFrilanser(gjeldendePeriode);
        } else if (Aktivitet.ARBEIDSTAKERINNTEKT.equals(aktivitetType)) {
            var opptjeningArbeidsgiverAktørId = opptjeningsperiode.getArbeidsgiverAktørId();
            var opptjeningArbeidsgiverOrgnummer = opptjeningsperiode.getArbeidsgiverOrgNummer();
            var opptjeningArbeidsforhold = Optional.ofNullable(opptjeningsperiode.getArbeidsforholdId()).orElse(InternArbeidsforholdRef.nullRef());
            return lagAktivPeriodeForArbeidstaker(inntektsmeldinger, gjeldendePeriode, opptjeningArbeidsgiverAktørId,
                opptjeningArbeidsgiverOrgnummer, opptjeningArbeidsforhold);
        } else {
            return AktivPeriode.forAndre(aktivitetType, gjeldendePeriode);
        }
    }

    private AktivPeriode lagAktivPeriodeForArbeidstaker(Collection<Inntektsmelding> inntektsmeldinger,
                                                        Periode gjeldendePeriode,
                                                        String opptjeningArbeidsgiverAktørId,
                                                        String opptjeningArbeidsgiverOrgnummer,
                                                        InternArbeidsforholdRef arbeidsforholdRef) {
        if (opptjeningArbeidsgiverAktørId != null) {
            return lagAktivePerioderForArbeidstakerHosPrivatperson(opptjeningArbeidsgiverAktørId, gjeldendePeriode);
        } else if (opptjeningArbeidsgiverOrgnummer != null) {
            return lagAktivePerioderForArbeidstakerHosVirksomhet(inntektsmeldinger, gjeldendePeriode, opptjeningArbeidsgiverOrgnummer, arbeidsforholdRef);
        } else {
            throw new IllegalStateException("Må ha en arbeidsgiver som enten er aktør eller virksomhet når aktivitet er " + Aktivitet.ARBEIDSTAKERINNTEKT);
        }
    }

    private AktivPeriode lagAktivePerioderForArbeidstakerHosPrivatperson(String aktørId, Periode gjeldendePeriode) {
        return AktivPeriode.forArbeidstakerHosPrivatperson(gjeldendePeriode, aktørId);
    }

    private AktivPeriode lagAktivePerioderForArbeidstakerHosVirksomhet(Collection<Inntektsmelding> inntektsmeldinger,
                                                                       Periode gjeldendePeriode,
                                                                       String opptjeningArbeidsgiverOrgnummer,
                                                                       InternArbeidsforholdRef arbeidsforholdRef) {
        if (harInntektsmeldingForArbeidsforhold(inntektsmeldinger, opptjeningArbeidsgiverOrgnummer, arbeidsforholdRef)) {
            return AktivPeriode.forArbeidstakerHosVirksomhet(gjeldendePeriode, opptjeningArbeidsgiverOrgnummer, arbeidsforholdRef.getReferanse());
        } else {
            return AktivPeriode.forArbeidstakerHosVirksomhet(gjeldendePeriode, opptjeningArbeidsgiverOrgnummer, null);
        }
    }

    private boolean harInntektsmeldingForArbeidsforhold(Collection<Inntektsmelding> inntektsmeldinger,
                                                        String orgnummer,
                                                        InternArbeidsforholdRef arbeidsforholdRef) {
        if (!arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()) {
            return false;
        } else {
            return inntektsmeldinger.stream()
                .anyMatch(im -> im.gjelderForEtSpesifiktArbeidsforhold()
                    && Objects.equals(im.getArbeidsgiver().getOrgnr(), orgnummer)
                    && Objects.equals(im.getArbeidsforholdRef().getReferanse(), arbeidsforholdRef.getReferanse()));
        }
    }
}
