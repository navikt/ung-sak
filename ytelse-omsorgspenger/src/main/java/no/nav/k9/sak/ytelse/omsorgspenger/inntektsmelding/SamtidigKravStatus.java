package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.Map;
import java.util.Objects;

import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class SamtidigKravStatus {

    private final KravStatus søknad;
    private final KravStatus imMedRefusjonskrav;
    private final KravStatus imUtenRefusjonskrav;
    private final Map<InternArbeidsforholdRef, KravStatus> imMedRefusjonskravPrArbeidsforhold;

    public enum KravStatus {
        FINNES,
        FINNES_IKKE,
        TREKT;
    }

    public SamtidigKravStatus(KravStatus søknad, KravStatus imMedRefusjonskrav, KravStatus imUtenRefusjonskrav, Map<InternArbeidsforholdRef, KravStatus> imMedRefusjonskravPrArbeidsforhold) {
        this.søknad = søknad;
        this.imMedRefusjonskrav = imMedRefusjonskrav;
        this.imUtenRefusjonskrav = imUtenRefusjonskrav;
        this.imMedRefusjonskravPrArbeidsforhold = imMedRefusjonskravPrArbeidsforhold;
    }

    public KravStatus søknad() {
        return søknad;
    }

    /**
     * aggregert status på tvers av arbeidsforhold
     */
    public KravStatus inntektsmeldingMedRefusjonskrav() {
        return imMedRefusjonskrav;
    }

    public KravStatus inntektsmeldingMedRefusjonskrav(InternArbeidsforholdRef arbeidsforholdRef) {
        KravStatus kravStatus = imMedRefusjonskravPrArbeidsforhold.get(InternArbeidsforholdRef.nullRef());
        if (kravStatus == null) {
            kravStatus = imMedRefusjonskravPrArbeidsforhold.get(arbeidsforholdRef);
        }
        return kravStatus != null ? kravStatus : KravStatus.FINNES_IKKE;
    }

    /**
     * aggregert status på tvers av arbeidsforhold
     */
    public KravStatus inntektsmeldingUtenRefusjonskrav() {
        return imUtenRefusjonskrav;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SamtidigKravStatus that = (SamtidigKravStatus) o;
        return søknad == that.søknad
            && imMedRefusjonskrav == that.imMedRefusjonskrav
            && imUtenRefusjonskrav == that.imUtenRefusjonskrav
            && Objects.equals(imMedRefusjonskravPrArbeidsforhold, that.imMedRefusjonskravPrArbeidsforhold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(søknad, imMedRefusjonskrav, imUtenRefusjonskrav, imMedRefusjonskravPrArbeidsforhold);
    }

    @Override
    public String toString() {
        return "SamtidigKravStatus{" +
            "søknad=" + søknad +
            ", imMedRefusjonskrav=" + imMedRefusjonskrav +
            ", imUtenRefusjonskrav=" + imUtenRefusjonskrav +
            ", refusjonskravPrArbeidsforhol=" + imMedRefusjonskravPrArbeidsforhold +
            '}';
    }
}
