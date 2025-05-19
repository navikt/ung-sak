package no.nav.ung.sak.domene.iay.modell;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.sak.typer.AktørId;

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
     * @param aktørInntekt {@link AktørInntektBuilder}
     * @return this
     */
    public InntektArbeidYtelseAggregatBuilder leggTilAktørInntekt(AktørInntektBuilder aktørInntekt) {
        if (!aktørInntekt.getErOppdatering()) {
            // Hvis ny så skal den legges til, hvis ikke ligger den allerede der og blir manipulert.
            this.kladd.leggTilAktørInntekt(aktørInntekt.build());
        }
        return this;
    }

    /**
     * Oppretter builder for inntekter for en gitt aktør. Baserer seg på en kopi av forrige innslag for aktøren hvis det eksisterer.
     *
     * @param aktørId aktøren
     * @return builder {@link AktørInntektBuilder}
     */
    public AktørInntektBuilder getAktørInntektBuilder(AktørId aktørId) {
        Optional<AktørInntekt> aktørInntekt = kladd.getAktørInntekt().stream().filter(aa -> aktørId.equals(aa.getAktørId())).findFirst();
        final AktørInntektBuilder oppdatere = AktørInntektBuilder.oppdatere(aktørInntekt);
        oppdatere.medAktørId(aktørId);
        return oppdatere;
    }

    public InntektArbeidYtelseAggregat build() {
        return this.kladd;
    }

    public VersjonType getVersjon() {
        return versjon;
    }


    public static class AktørInntektBuilder {
        private final AktørInntekt kladd;
        private final boolean oppdatering;

        private AktørInntektBuilder(AktørInntekt aktørInntekt, boolean oppdatering) {
            this.kladd = aktørInntekt;
            this.oppdatering = oppdatering;
        }

        static AktørInntektBuilder ny() {
            return new AktørInntektBuilder(new AktørInntekt(), false);
        }

        static AktørInntektBuilder oppdatere(AktørInntekt oppdatere) {
            return new AktørInntektBuilder(oppdatere, true);
        }

        public static AktørInntektBuilder oppdatere(Optional<AktørInntekt> oppdatere) {
            return oppdatere.map(AktørInntektBuilder::oppdatere).orElseGet(AktørInntektBuilder::ny);
        }

        void medAktørId(AktørId aktørId) {
            this.kladd.setAktørId(aktørId);
        }

        public AktørInntektBuilder leggTilInntekt(InntektBuilder builder) {
            if (!builder.getErOppdatering()) {
                kladd.leggTilInntekt(builder.build());
            }
            return this;
        }

        public AktørInntekt build() {
            if (kladd.hasValues()) {
                return kladd;
            }
            throw new IllegalStateException();
        }

        boolean getErOppdatering() {
            return oppdatering;
        }

        public AktørInntektBuilder fjernInntekterFraKilde(InntektsKilde inntektsKilde) {
            kladd.fjernInntekterFraKilde(inntektsKilde);
            return this;
        }
    }

}
