package no.nav.ung.sak.domene.iay.modell;

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

    /** Opprett ny versjon av grunnlag med angitt assignet grunnlagReferanse og opprettetTidspunkt. */
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

    /** @deprecated skal fjernes, skal ikke kunne utføres på klient siden (kun internt i abakus). */
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

    /** @deprecated skal fjernes  */
    @Deprecated(forRemoval = true)
    public InntektArbeidYtelseGrunnlagBuilder medData(InntektArbeidYtelseAggregatBuilder builder) {
        VersjonType versjon = builder.getVersjon();

        if (versjon == VersjonType.REGISTER) {
            medRegister(builder);
        }
        return this;
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
