package no.nav.k9.sak.mottak.inntektsmelding;

import no.nav.k9.sak.mottak.inntektsmelding.v2.MottattDokumentWrapperInntektsmelding;

public abstract class MottattInntektsmeldingWrapper<S> {

    private S skjema;
    private String namespace;

    protected MottattInntektsmeldingWrapper(S skjema, String namespace) {
        this.skjema = skjema;
        this.namespace = namespace;
    }

    @SuppressWarnings("rawtypes")
    public static MottattInntektsmeldingWrapper tilXmlWrapper(Object skjema) {
        if (skjema instanceof no.seres.xsd.nav.inntektsmelding_m._20180924.InntektsmeldingM) {
            return new no.nav.k9.sak.mottak.inntektsmelding.v1.MottattDokumentWrapperInntektsmelding((no.seres.xsd.nav.inntektsmelding_m._20180924.InntektsmeldingM) skjema);
        } else if (skjema instanceof no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM) {
            return new MottattDokumentWrapperInntektsmelding((no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM) skjema);
        }
        throw MottattDokumentFeil.FACTORY.ukjentSkjemaType(skjema.getClass().getCanonicalName()).toException();
    }

    public S getSkjema() {
        return this.skjema;
    }

    String getSkjemaType() {
        return this.namespace;
    }
}
