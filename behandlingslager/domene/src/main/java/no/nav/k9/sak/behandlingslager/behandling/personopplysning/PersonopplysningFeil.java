package no.nav.k9.sak.behandlingslager.behandling.personopplysning;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;


public interface PersonopplysningFeil extends DeklarerteFeil  {

    PersonopplysningFeil FACTORY = FeilFactory.create(PersonopplysningFeil.class);

    @TekniskFeil(feilkode = "FP-124903", feilmelding = "Må basere seg på eksisterende versjon av personopplysning", logLevel = LogLevel.WARN)
    Feil måBasereSegPåEksisterendeVersjon();
}

