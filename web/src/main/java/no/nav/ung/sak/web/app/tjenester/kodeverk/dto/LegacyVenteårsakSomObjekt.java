package no.nav.ung.sak.web.app.tjenester.kodeverk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;

/**
 * Konverterer VenteårsakSomObjekt.kanVelges property til string i json serialisering, for kompatibilitet med gammalt
 * endepunkt for retur av kodeverk objekt. Fjernast når det gamle endepunktet er borte.
 */
public class LegacyVenteårsakSomObjekt extends VenteårsakSomObjekt {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Override
    public boolean getKanVelges() {
        return super.getKanVelges();
    }

    public LegacyVenteårsakSomObjekt(Venteårsak from) {
        super(from);
    }
}
