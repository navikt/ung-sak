package no.nav.ung.sak.domene.iay.modell;

import no.nav.k9.felles.util.Tuple;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class InntektArbeidYtelseGrunnlagBuilder {

    private InntektArbeidYtelseGrunnlag kladd;

    protected InntektArbeidYtelseGrunnlagBuilder(InntektArbeidYtelseGrunnlag kladd) {
        this.kladd = kladd;
    }

    public static InntektArbeidYtelseGrunnlagBuilder nytt() {
        return ny(UUID.randomUUID(), LocalDateTime.now());
    }

    /**
     * Opprett ny versjon av grunnlag med angitt assignet grunnlagReferanse og opprettetTidspunkt.
     */
    public static InntektArbeidYtelseGrunnlagBuilder ny(UUID grunnlagReferanse, LocalDateTime opprettetTidspunkt) {
        return new InntektArbeidYtelseGrunnlagBuilder(new InntektArbeidYtelseGrunnlag(grunnlagReferanse, opprettetTidspunkt));
    }

    public static InntektArbeidYtelseGrunnlagBuilder oppdatere(InntektArbeidYtelseGrunnlag kladd) {
        return new InntektArbeidYtelseGrunnlagBuilder(new InntektArbeidYtelseGrunnlag(kladd));
    }

    public static InntektArbeidYtelseGrunnlagBuilder oppdatere(Optional<InntektArbeidYtelseGrunnlag> kladd) {
        return kladd.map(InntektArbeidYtelseGrunnlagBuilder::oppdatere).orElseGet(InntektArbeidYtelseGrunnlagBuilder::nytt);
    }

    // FIXME: Bør ikke være public, bryter encapsulation
    public InntektArbeidYtelseGrunnlag getKladd() {
        return kladd;
    }

    public void setOppgitteOpptjeninger(OppgittOpptjeningAggregat oppgittOpptjeningAggregat) {
        kladd.setOppgittOpptjeningAggregat(oppgittOpptjeningAggregat);
    }

    public ArbeidsforholdInformasjon getInformasjon() {
        var informasjon = kladd.getArbeidsforholdInformasjon();

        var informasjonEntitet = informasjon.orElseGet(() -> new ArbeidsforholdInformasjon());
        kladd.setInformasjon(informasjonEntitet);
        return informasjonEntitet;
    }

    public InntektArbeidYtelseGrunnlagBuilder medInformasjon(ArbeidsforholdInformasjon informasjon) {
        kladd.setInformasjon(informasjon);
        return this;
    }

    @Deprecated
    public void medSaksbehandlet(InntektArbeidYtelseAggregatBuilder builder) {
        kladd.setSaksbehandlet(builder == null ? null : builder.build());
    }

    /**
     * @deprecated skal fjernes, skal ikke kunne utføres på klient siden (kun internt i abakus).
     */
    @Deprecated(forRemoval = true)
    public void medRegister(InntektArbeidYtelseAggregatBuilder builder) {
        kladd.setRegister(builder == null ? null : builder.build());
    }

    public InntektArbeidYtelseGrunnlagBuilder medOppgittOpptjening(OppgittOpptjeningBuilder builder) {
        if (builder != null) {
            if (kladd.getOppgittOpptjening().isPresent()) {
                throw new IllegalStateException("Utviklerfeil: Er ikke lov å endre oppgitt opptjening!");
            }
            if (kladd.getOppgittOpptjeningAggregat().isPresent()) {
                throw new IllegalStateException("Utviklerfeil: Kan ikke bruke ny versjon av iay med flere oppgitte opptjeninger " +
                    "sammen med gammel versjon for enkeltopptjening!");
            }
            kladd.setOppgittOpptjening(builder.build());
        }
        return this;
    }

    public InntektArbeidYtelseGrunnlagBuilder medOppgittOpptjeningAggregat(Collection<OppgittOpptjeningBuilder> buildere) {
        if (buildere != null) {
            if (kladd.getOppgittOpptjeningAggregat().isPresent()) {
                throw new IllegalStateException("Utviklerfeil: Er ikke lov å endre aggregat for oppgitt opptjening!");
            }
            if (kladd.getOppgittOpptjening().isPresent()) {
                throw new IllegalStateException("Utviklerfeil: Kan ikke bruke gammel versjon av iay med én oppgitt opptjening " +
                    "sammen med ny versjon med flere oppgitte opptjeninger på samme behandling!");
            }
            var oppgitteOpptjeninger = buildere.stream().map(OppgittOpptjeningBuilder::build).collect(Collectors.toSet());
            kladd.setOppgittOpptjeningAggregat(new OppgittOpptjeningAggregat(oppgitteOpptjeninger));
        }
        return this;
    }

    public InntektArbeidYtelseGrunnlagBuilder medOverstyrtOppgittOpptjening(OppgittOpptjeningBuilder builder) {
        if (builder != null) {
            kladd.setOverstyrtOppgittOpptjening(builder.build());
        }
        return this;
    }

    public InntektArbeidYtelseGrunnlag build() {
        var k = kladd;
        kladd = null; // må ikke finne på å gjenbruke buildere her, tar heller straffen i en NPE ved første feilkall
        return k;
    }

    /**
     * @deprecated skal fjernes, skal kun kunne utføre {@link #medSaksbehandlet(InntektArbeidYtelseAggregatBuilder)} .
     */
    @Deprecated(forRemoval = true)
    public InntektArbeidYtelseGrunnlagBuilder medData(InntektArbeidYtelseAggregatBuilder builder) {
        VersjonType versjon = builder.getVersjon();

        if (versjon == VersjonType.REGISTER) {
            medRegister(builder);
        } else {
            medSaksbehandlet(builder);
        }
        return this;
    }

    public void ryddOppErstattedeArbeidsforhold(AktørId søker,
                                                List<Tuple<Arbeidsgiver, Tuple<InternArbeidsforholdRef, InternArbeidsforholdRef>>> erstattArbeidsforhold) {
        final Optional<InntektArbeidYtelseAggregat> registerFørVersjon = kladd.getRegisterVersjon();
        for (Tuple<Arbeidsgiver, Tuple<InternArbeidsforholdRef, InternArbeidsforholdRef>> tuple : erstattArbeidsforhold) {
            if (registerFørVersjon.isPresent()) {
                // TODO: Vurder konsekvensen av dette.
                var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(registerFørVersjon, VersjonType.REGISTER);
                builder.oppdaterArbeidsforholdReferanseEtterErstatting(
                    søker,
                    tuple.getElement1(),
                    tuple.getElement2().getElement1(),
                    tuple.getElement2().getElement2());
                medData(builder);
            }
        }
    }

    public Optional<ArbeidsforholdInformasjon> getArbeidsforholdInformasjon() {
        return kladd.getArbeidsforholdInformasjon();
    }

    protected void fjernSaksbehandlet() {
        kladd.fjernSaksbehandlet();
    }

    public InntektArbeidYtelseGrunnlagBuilder medErAktivtGrunnlag(boolean erAktivt) {
        kladd.setAktivt(erAktivt);
        return this;
    }

    public InntektArbeidYtelseGrunnlagBuilder medOppgitteOpptjeninger(List<OppgittOpptjening> oppgitteOpptjeninger) {
        setOppgitteOpptjeninger(new OppgittOpptjeningAggregat(oppgitteOpptjeninger));
        return this;
    }
}
