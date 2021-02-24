package no.nav.k9.sak.domene.iay.modell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

public class ValiderInntektsmelding {

    private static final Logger log = LoggerFactory.getLogger(ValiderInntektsmelding.class);

    public void valider(InntektsmeldingBuilder builder) { // NOSONAR
        var kladd = builder.getKladd();

        if (FagsakYtelseType.OMP.equals(kladd.getFagsakYtelseType())) {
            // valider kombo refusjonkrav/fravær
            if (harRefusjonskrav(kladd)) {
                if (!harFravær(kladd)) {
                    throw new IllegalArgumentException("Har refusjonskrav men ikke oppgitt fravær. Gir ikke mening. JournalpostId=" + kladd.getJournalpostId());
                } else {
                    // OK - vanligste forventet tilfelle - refusjonskrav med fravær
                }
            } else {
                if (harFravær(kladd)) {
                    if (harKunKorreksjon(kladd)) {
                        log.warn("Mottatt inntektsmelding uten refusjon, kun korreksjon av fravær, journalpostId [{}], fravær: {}", kladd.getJournalpostId(), kladd.getOppgittFravær());
                    } else {
                        log.warn("Mottatt inntektsmelding uten refusjon, med oppgitt fravær, journalpostId [{}], fravær: {}. Antyder mulig utbetaling til bruker", kladd.getJournalpostId(),
                            kladd.getOppgittFravær());
                    }
                } else {
                    // Må opplyse brutto inntekt
                    if (kladd.getInntektBeløp() == null) {
                        throw new IllegalArgumentException("Har ikke refusjon eller fravær. Må oppgi forventet bruttoinntekt, men mangler. JournalpostId=" + kladd.getJournalpostId());
                    } else if (kladd.getArbeidsforholdRef() == null) {
                        throw new IllegalArgumentException("Har ikke refusjon eller fravær. Må oppgi arbeidsforholdId, men er ikke satt. JournalpostId=" + kladd.getJournalpostId());
                    } else {
                        // OK - inntektsmelding opplyser kun brutto inntekt for arbeidsforhold uten inntekt
                    }
                }
            }
        }
        if (FagsakYtelseType.PSB.equals(kladd.getFagsakYtelseType())) {
            // valider kombo refusjonkrav/fravær
            // Må opplyse brutto inntekt
            if (kladd.getInntektBeløp() == null) {
                throw new IllegalArgumentException("Har ikke refusjon eller fravær. Må oppgi forventet bruttoinntekt, men mangler. JournalpostId=" + kladd.getJournalpostId());
            } else if (kladd.getArbeidsforholdRef() == null) {
                throw new IllegalArgumentException("Har ikke refusjon eller fravær. Må oppgi arbeidsforholdId, men er ikke satt. JournalpostId=" + kladd.getJournalpostId());
            } else {
                // OK - inntektsmelding opplyser kun brutto inntekt for arbeidsforhold uten inntekt
            }
        }
    }

    private boolean harRefusjonskrav(Inntektsmelding kladd) {
        return kladd.harRefusjonskrav();
    }

    private boolean harFravær(Inntektsmelding kladd) {
        return kladd.harFravær();
    }

    private boolean harKunKorreksjon(Inntektsmelding kladd) {
        return kladd.harKunKorreksjon();
    }

}
