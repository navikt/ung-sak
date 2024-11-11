package no.nav.ung.sak.kontrakt.aksjonspunkt;

/**
 * Aksjonspunkt Dto som lar seg avslå (der vilkår kan settes Ok/Ikke OK)
 *
 */
public interface AvslagbartAksjonspunktDto extends AksjonspunktKode {

    String getAvslagskode();

    String getBegrunnelse();

    Boolean getErVilkarOk();
}
