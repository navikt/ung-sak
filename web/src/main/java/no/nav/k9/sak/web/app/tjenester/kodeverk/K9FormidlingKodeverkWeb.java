package no.nav.k9.sak.web.app.tjenester.kodeverk;

import jakarta.validation.constraints.NotNull;
import no.nav.k9.formidling.kontrakt.kodeverk.AvsenderApplikasjon;

public class K9FormidlingKodeverkWeb {
    @NotNull
    public AvsenderApplikasjon avsenderApplikasjon;
}
