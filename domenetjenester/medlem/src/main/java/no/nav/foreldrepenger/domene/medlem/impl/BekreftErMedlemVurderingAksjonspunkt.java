package no.nav.foreldrepenger.domene.medlem.impl;

import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;

public class BekreftErMedlemVurderingAksjonspunkt {

    private MedlemskapManuellVurderingType manuellVurderingTypeKode;

    private String begrunnelse;

    public BekreftErMedlemVurderingAksjonspunkt(MedlemskapManuellVurderingType manuellVurderingTypeKode, String begrunnelse) {
        this.manuellVurderingTypeKode = manuellVurderingTypeKode;
        this.begrunnelse = begrunnelse;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public MedlemskapManuellVurderingType getManuellVurderingTypeKode() {
        return manuellVurderingTypeKode;
    }
}
