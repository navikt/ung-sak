package no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt;

import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.Periode;

import java.util.List;

public interface Vurdering {

    String getVurderingstekst();

    Resultat getResultat();

    List<Periode> getPerioder();

}



