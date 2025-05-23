package no.nav.ung.sak.domene.iay.modell;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;

/**
 * Builder for å håndtere en gitt versjon {@link VersjonType} av grunnlaget.
 * <p>
 * Holder styr på om det er en oppdatering av eksisterende informasjon, om det gjelder før eller etter skjæringstidspunktet
 * og om det er registerdata eller saksbehandlers beslutninger.
 * <p>
 * NB! Viktig at denne builderen hentes fra repository for å sikre at den er rett tilstand ved oppdatering. Hvis ikke kan data gå tapt.
 */
public class InntektArbeidYtelseAggregatBuilder {

    private final InntektArbeidYtelseAggregat kladd;
    private final VersjonType versjon;

    private InntektArbeidYtelseAggregatBuilder(InntektArbeidYtelseAggregat kladd, VersjonType versjon) {
        this.kladd = kladd;
        this.versjon = versjon;
    }

    public static InntektArbeidYtelseAggregatBuilder oppdatere(Optional<InntektArbeidYtelseAggregat> oppdatere, VersjonType versjon) {
        return builderFor(oppdatere, UUID.randomUUID(), LocalDateTime.now(), versjon);
    }

    public static InntektArbeidYtelseAggregatBuilder builderFor(Optional<InntektArbeidYtelseAggregat> kopierDataFra,
                                                                UUID angittReferanse, LocalDateTime angittTidspunkt, VersjonType versjon) {
        return kopierDataFra
            .map(kopier -> new InntektArbeidYtelseAggregatBuilder(new InntektArbeidYtelseAggregat(angittReferanse, angittTidspunkt, kopier), versjon))
            .orElseGet(() -> new InntektArbeidYtelseAggregatBuilder(new InntektArbeidYtelseAggregat(angittReferanse, angittTidspunkt), versjon));
    }

    /**
     * Legger til inntekter for en gitt aktør hvis det ikke er en oppdatering av eksisterende.
     * Ved oppdatering eksisterer koblingen for denne aktøren allerede så en kopi av forrige innslag manipuleres før lagring.
     *
     * @param inntekter {@link InntekterBuilder}
     * @return this
     */
    public InntektArbeidYtelseAggregatBuilder leggTilInntekter(InntekterBuilder inntekter) {
        if (!inntekter.getErOppdatering()) {
            // Hvis ny så skal den legges til, hvis ikke ligger den allerede der og blir manipulert.
            this.kladd.setInntekter(inntekter.build());
        }
        return this;
    }

    /**
     * Oppretter builder for inntekter. Baserer seg på en kopi av forrige innslag hvis det eksisterer.
     *
     * @return builder {@link InntekterBuilder}
     */
    public InntekterBuilder getInntekterBuilder() {
        return InntekterBuilder.oppdatere(Optional.ofNullable(kladd.getInntekter()));
    }

    public InntektArbeidYtelseAggregat build() {
        return this.kladd;
    }

    public VersjonType getVersjon() {
        return versjon;
    }


    public static class InntekterBuilder {
        private final Inntekter kladd;
        private final boolean oppdatering;

        private InntekterBuilder(Inntekter inntekter, boolean oppdatering) {
            this.kladd = inntekter;
            this.oppdatering = oppdatering;
        }

        static InntekterBuilder ny() {
            return new InntekterBuilder(new Inntekter(), false);
        }

        static InntekterBuilder oppdatere(Inntekter oppdatere) {
            return new InntekterBuilder(oppdatere, true);
        }

        public static InntekterBuilder oppdatere(Optional<Inntekter> oppdatere) {
            return oppdatere.map(InntekterBuilder::oppdatere).orElseGet(InntekterBuilder::ny);
        }

        public InntekterBuilder leggTilInntekt(InntektBuilder builder) {
            if (!builder.getErOppdatering()) {
                kladd.leggTilInntekt(builder.build());
            }
            return this;
        }

        public Inntekter build() {
            if (kladd.hasValues()) {
                return kladd;
            }
            throw new IllegalStateException();
        }

        boolean getErOppdatering() {
            return oppdatering;
        }

        public InntekterBuilder fjernInntekterFraKilde(InntektsKilde inntektsKilde) {
            kladd.fjernInntekterFraKilde(inntektsKilde);
            return this;
        }
    }

}
