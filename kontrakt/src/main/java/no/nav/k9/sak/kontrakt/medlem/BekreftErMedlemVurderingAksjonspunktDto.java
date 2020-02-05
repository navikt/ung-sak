package no.nav.k9.sak.kontrakt.medlem;

public class BekreftErMedlemVurderingAksjonspunktDto {
    private String manuellVurderingTypeKode;
    private String begrunnelse;

    public BekreftErMedlemVurderingAksjonspunktDto(String manuellVurderingTypeKode, String begrunnelse) {
        this.manuellVurderingTypeKode = manuellVurderingTypeKode;
        this.begrunnelse = begrunnelse;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getManuellVurderingTypeKode() {
        return manuellVurderingTypeKode;
    }
}
