package no.nav.k9.sak.domene.iay.modell;

import java.time.LocalDate;

import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class RefusjonskravDato {

    private Arbeidsgiver arbeidsgiver;

    private LocalDate førsteDagMedRefusjonskrav;

    private LocalDate førsteInnsendingAvRefusjonskrav;

    private boolean harRefusjonFraStart;

    RefusjonskravDato() {
    }

    public RefusjonskravDato(Arbeidsgiver arbeidsgiver, LocalDate førsteDagMedRefusjonskrav, LocalDate førsteInnsendingAvRefusjonskrav, boolean harRefusjonFraStart) {
        this.arbeidsgiver = arbeidsgiver;
        this.førsteDagMedRefusjonskrav = førsteDagMedRefusjonskrav;
        this.førsteInnsendingAvRefusjonskrav = førsteInnsendingAvRefusjonskrav;
        this.harRefusjonFraStart = harRefusjonFraStart;
    }

    public RefusjonskravDato(RefusjonskravDato refusjonskravDato) {
        this.arbeidsgiver = refusjonskravDato.getArbeidsgiver();
        this.førsteDagMedRefusjonskrav = refusjonskravDato.førsteDagMedRefusjonskrav;
        this.førsteInnsendingAvRefusjonskrav = refusjonskravDato.førsteInnsendingAvRefusjonskrav;
        this.harRefusjonFraStart = refusjonskravDato.harRefusjonFraStart;
    }

    /**
     * Virksomheten som har sendt inn inntektsmeldingen
     *
     * @return {@link Virksomhet}
     */
    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }


    public LocalDate getFørsteDagMedRefusjonskrav() {
        return førsteDagMedRefusjonskrav;
    }

    public LocalDate getFørsteInnsendingAvRefusjonskrav() {
        return førsteInnsendingAvRefusjonskrav;
    }

    public boolean getHarRefusjonFraStart() {
        return harRefusjonFraStart;
    }
}
