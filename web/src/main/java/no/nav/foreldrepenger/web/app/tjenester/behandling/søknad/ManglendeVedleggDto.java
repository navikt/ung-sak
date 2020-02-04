package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import no.nav.k9.kodeverk.dokument.DokumentTypeId;

public class ManglendeVedleggDto {
    private DokumentTypeId dokumentType;
    private ArbeidsgiverDto arbeidsgiver;
    private boolean brukerHarSagtAtIkkeKommer = false;

    public DokumentTypeId getDokumentType() {
        return dokumentType;
    }

    public void setDokumentType(DokumentTypeId dokumentType) {
        this.dokumentType = dokumentType;
    }

    public ArbeidsgiverDto getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(ArbeidsgiverDto arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public boolean getBrukerHarSagtAtIkkeKommer() {
        return brukerHarSagtAtIkkeKommer;
    }

    public void setBrukerHarSagtAtIkkeKommer(boolean brukerHarSagtAtIkkeKommer) {
        this.brukerHarSagtAtIkkeKommer = brukerHarSagtAtIkkeKommer;
    }
}
