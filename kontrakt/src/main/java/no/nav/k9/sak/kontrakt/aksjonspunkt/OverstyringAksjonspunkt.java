package no.nav.k9.sak.kontrakt.aksjonspunkt;

import no.nav.k9.sak.typer.Periode;

public interface OverstyringAksjonspunkt {
    String getAvslagskode();

    String getBegrunnelse();

    boolean getErVilkarOk();

    Periode getPeriode();
}
