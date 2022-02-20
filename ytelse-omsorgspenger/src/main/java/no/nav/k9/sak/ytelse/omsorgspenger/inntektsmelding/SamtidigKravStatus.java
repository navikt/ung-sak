package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.Objects;

public class SamtidigKravStatus {

    private final KravStatus refusjonskrav;
    private final KravStatus søknad;
    private final KravStatus støttendeInntektsmelding;

    enum KravStatus {
        FINNES,
        FINNES_IKKE,
        TREKT;
    }

    private SamtidigKravStatus(KravStatus refusjonskrav, KravStatus søknad, KravStatus støttendeInntektsmelding) {
        this.refusjonskrav = refusjonskrav;
        this.søknad = søknad;
        this.støttendeInntektsmelding = støttendeInntektsmelding;
    }

    public static SamtidigKravStatus refusjonskravFinnes() {
        return new SamtidigKravStatus(KravStatus.FINNES, KravStatus.FINNES_IKKE, KravStatus.FINNES_IKKE);
    }

    public static SamtidigKravStatus refusjonskravOgSøknadFinnes() {
        return new SamtidigKravStatus(KravStatus.FINNES, KravStatus.FINNES, KravStatus.FINNES_IKKE);
    }

    public static SamtidigKravStatus refusjonskravTrekt() {
        return new SamtidigKravStatus(KravStatus.TREKT, KravStatus.FINNES_IKKE, KravStatus.FINNES_IKKE);
    }

    public static SamtidigKravStatus søknadFinnes() {
        return new SamtidigKravStatus(KravStatus.FINNES_IKKE, KravStatus.FINNES, KravStatus.FINNES_IKKE);
    }

    public static SamtidigKravStatus søknadTrekt() {
        return new SamtidigKravStatus(KravStatus.FINNES_IKKE, KravStatus.TREKT, KravStatus.FINNES_IKKE);
    }

    public static SamtidigKravStatus støttendeInntektsmeldingFinnes() {
        return new SamtidigKravStatus(KravStatus.FINNES_IKKE, KravStatus.FINNES_IKKE, KravStatus.FINNES);
    }

    public SamtidigKravStatus oppdaterRefusjonskravFinnes() {
        return oppdaterRefusjonskravStatus(KravStatus.FINNES);
    }

    public SamtidigKravStatus oppdaterRefusjonskravTrekt() {
        return oppdaterRefusjonskravStatus(KravStatus.TREKT);
    }

    public SamtidigKravStatus oppdaterRefusjonskravStatus(KravStatus refusjonskrav) {
        return new SamtidigKravStatus(refusjonskrav, søknad, støttendeInntektsmelding);
    }

    public SamtidigKravStatus oppdaterSøknadStatus(KravStatus søknad) {
        return new SamtidigKravStatus(refusjonskrav, søknad, støttendeInntektsmelding);
    }

    public SamtidigKravStatus oppdaterStøttendeImStatus(KravStatus støttendeInntektsmelding) {
        return new SamtidigKravStatus(refusjonskrav, søknad, støttendeInntektsmelding);
    }

    public KravStatus refusjonskrav() {
        return refusjonskrav;
    }

    public KravStatus søknad() {
        return søknad;
    }

    public KravStatus støttendeInntektsmelding() {
        return støttendeInntektsmelding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SamtidigKravStatus that = (SamtidigKravStatus) o;
        return refusjonskrav == that.refusjonskrav
            && søknad == that.søknad
            && støttendeInntektsmelding == that.støttendeInntektsmelding;
    }

    @Override
    public int hashCode() {
        return Objects.hash(refusjonskrav, søknad, støttendeInntektsmelding);
    }
}
