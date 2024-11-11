package no.nav.ung.sak.web.app.tjenester.los.dto;

import java.util.List;

import no.nav.ung.sak.behandlingslager.behandling.merknad.BehandlingMerknadType;

public class MerknadDto {

    private List<BehandlingMerknadType> merknadKoder;

    private String fritekst;

    public MerknadDto(List<BehandlingMerknadType> merknadKoder, String fritekst) {
        this.merknadKoder = merknadKoder;
        this.fritekst = fritekst;
    }

    public List<BehandlingMerknadType> getMerknadKoder() {
        return merknadKoder;
    }

    public String getFritekst() {
        return fritekst;
    }
}
