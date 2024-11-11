package no.nav.ung.sak.kontrakt.aksjonspunkt;

import no.nav.ung.sak.typer.Periode;

public interface OverstyringAksjonspunkt {
    String getAvslagskode();

    String getBegrunnelse();

    boolean getErVilkarOk();

    Periode getPeriode();

    boolean skalAvbrytes();

    default String getInnvilgelseMerknadKode() {
        return null;
    }

}
