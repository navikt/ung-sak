package no.nav.k9.sak.kompletthet;

import java.util.Objects;

import no.nav.k9.kodeverk.dokument.DokumentTypeId;

public class ManglendeVedlegg {

    private final DokumentTypeId dokumentType;
    private final String arbeidsgiver;
    private final String arbeidsforholdId;
    private Boolean brukerHarSagtAtIkkeKommer = false;

    public ManglendeVedlegg(DokumentTypeId dokumentType) {
        this(dokumentType, null);
    }

    public ManglendeVedlegg(DokumentTypeId dokumentType, String arbeidsgiver) {
        this(dokumentType, arbeidsgiver, null, null);
    }


    public ManglendeVedlegg(DokumentTypeId dokumentType, String arbeidsgiver, Boolean brukerHarSagtAtIkkeKommer) {
        this(dokumentType, arbeidsgiver, null, brukerHarSagtAtIkkeKommer);
    }

    public ManglendeVedlegg(DokumentTypeId dokumentType, String arbeidsgiver, String arbeidsforholdId, Boolean brukerHarSagtAtIkkeKommer) {
        this.dokumentType = dokumentType;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdId = arbeidsforholdId;
        this.brukerHarSagtAtIkkeKommer = brukerHarSagtAtIkkeKommer;
    }

    public DokumentTypeId getDokumentType() {
        return dokumentType;
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public Boolean getBrukerHarSagtAtIkkeKommer() {
        return brukerHarSagtAtIkkeKommer;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdId=" + arbeidsforholdId +
            ", dokumentType=" + dokumentType +
            ", kommerIkke=" + brukerHarSagtAtIkkeKommer
            + ">";

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !(obj.getClass().equals(this.getClass()))) {
            return false;
        }
        ManglendeVedlegg other = (ManglendeVedlegg) obj;
        return Objects.equals(arbeidsgiver, other.arbeidsgiver)
            && Objects.equals(arbeidsforholdId, other.arbeidsforholdId)
            && Objects.equals(dokumentType, other.dokumentType);

    }

    @Override
    public int hashCode() {
        return Objects.hash(dokumentType, arbeidsgiver, arbeidsforholdId);
    }
}
