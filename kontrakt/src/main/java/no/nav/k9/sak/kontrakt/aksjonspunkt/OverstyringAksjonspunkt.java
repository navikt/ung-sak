package no.nav.k9.sak.kontrakt.aksjonspunkt;

public interface OverstyringAksjonspunkt {
    String getAvslagskode();

    String getBegrunnelse();

    boolean getErVilkarOk();
}
