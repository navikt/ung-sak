package no.nav.ung.sak.kontrakt.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface AksjonspunktKode {

    @JsonIgnore
    String getKode();
}
