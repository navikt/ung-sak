package no.nav.ung.sak.mottak.inntektsmelding;

import no.nav.ung.sak.mottak.inntektsmelding.v2.MottattDokumentWrapperInntektsmelding;
import no.nav.ung.sak.typer.JournalpostId;

public abstract class MottattInntektsmeldingWrapper<S> {

    private S skjema;
    private String namespace;
    private JournalpostId journalpostId;

    protected MottattInntektsmeldingWrapper(JournalpostId journalpostId, S skjema, String namespace) {
        this.journalpostId = journalpostId;
        this.skjema = skjema;
        this.namespace = namespace;
    }

    @SuppressWarnings("rawtypes")
    public static MottattInntektsmeldingWrapper tilXmlWrapper(JournalpostId journalpostId, Object skjema) {
        if (skjema instanceof no.seres.xsd.nav.inntektsmelding_m._20180924.InntektsmeldingM) {
            return new no.nav.ung.sak.mottak.inntektsmelding.v1.MottattDokumentWrapperInntektsmelding(journalpostId, (no.seres.xsd.nav.inntektsmelding_m._20180924.InntektsmeldingM) skjema);
        } else if (skjema instanceof no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM) {
            return new MottattDokumentWrapperInntektsmelding(journalpostId, (no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM) skjema);
        }
        throw MottattInntektsmeldingException.ukjentSkjemaType(skjema.getClass().getCanonicalName());
    }

    public S getSkjema() {
        return this.skjema;
    }

    String getSkjemaType() {
        return this.namespace;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }
}
