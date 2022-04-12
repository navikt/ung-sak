package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.Objects;

public class SamtidigKravStatus {

    private final KravStatus søknad;
    private final KravStatus imMedRefusjonskrav;
    private final KravStatus imUtenRefusjonskrav;

    public enum KravStatus {
        FINNES,
        FINNES_IKKE,
        TREKT;
    }

    public SamtidigKravStatus(KravStatus søknad, KravStatus imMedRefusjonskrav, KravStatus imUtenRefusjonskrav) {
        this.søknad = søknad;
        this.imMedRefusjonskrav = imMedRefusjonskrav;
        this.imUtenRefusjonskrav = imUtenRefusjonskrav;
    }

    public static SamtidigKravStatus søknadFinnes() {
        return new SamtidigKravStatus(KravStatus.FINNES, KravStatus.FINNES_IKKE, KravStatus.FINNES_IKKE);
    }

    public static SamtidigKravStatus søknadTrekt() {
        return new SamtidigKravStatus(KravStatus.TREKT, KravStatus.FINNES_IKKE, KravStatus.FINNES_IKKE);
    }

    public static SamtidigKravStatus refusjonskravOgSøknadFinnes() {
        return new SamtidigKravStatus(KravStatus.FINNES, KravStatus.FINNES, KravStatus.FINNES_IKKE);
    }

    public static SamtidigKravStatus refusjonskravFinnes() {
        return new SamtidigKravStatus(KravStatus.FINNES_IKKE, KravStatus.FINNES, KravStatus.FINNES_IKKE);
    }

    public static SamtidigKravStatus refusjonskravTrekt() {
        return new SamtidigKravStatus(KravStatus.FINNES_IKKE, KravStatus.TREKT, KravStatus.FINNES_IKKE);
    }

    public static SamtidigKravStatus inntektsmeldingUtenRefusjonskravFinnes() {
        return new SamtidigKravStatus(KravStatus.FINNES_IKKE, KravStatus.FINNES_IKKE, KravStatus.FINNES);
    }

    public static SamtidigKravStatus inntektsmeldingUtenRefusjonskravTrekt() {
        return new SamtidigKravStatus(KravStatus.FINNES_IKKE, KravStatus.FINNES_IKKE, KravStatus.TREKT);
    }

    public SamtidigKravStatus oppdaterRefusjonskravFinnes() {
        return oppdaterRefusjonskravStatus(KravStatus.FINNES);
    }

    public SamtidigKravStatus oppdaterRefusjonskravTrekt() {
        return oppdaterRefusjonskravStatus(KravStatus.TREKT);
    }

    public SamtidigKravStatus oppdaterRefusjonskravStatus(KravStatus refusjonskrav) {
        return new SamtidigKravStatus(søknad, refusjonskrav, imUtenRefusjonskrav);
    }

    public SamtidigKravStatus oppdaterSøknadStatus(KravStatus søknad) {
        return new SamtidigKravStatus(søknad, imMedRefusjonskrav, imUtenRefusjonskrav);
    }

    public SamtidigKravStatus oppdaterInntektsmeldingUtenRefusjonskravStatus(KravStatus støttendeInntektsmelding) {
        return new SamtidigKravStatus(søknad, imMedRefusjonskrav, støttendeInntektsmelding);
    }

    public KravStatus søknad() {
        return søknad;
    }

    public KravStatus inntektsmeldingMedRefusjonskrav() {
        return imMedRefusjonskrav;
    }

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
            && imUtenRefusjonskrav == that.imUtenRefusjonskrav;
    }

    @Override
    public int hashCode() {
        return Objects.hash(søknad, imMedRefusjonskrav, imUtenRefusjonskrav);
    }

    @Override
    public String toString() {
        return "SamtidigKravStatus{" +
            "søknad=" + søknad +
            ", imMedRefusjonskrav=" + imMedRefusjonskrav +
            ", imUtenRefusjonskrav=" + imUtenRefusjonskrav +
            '}';
    }
}
