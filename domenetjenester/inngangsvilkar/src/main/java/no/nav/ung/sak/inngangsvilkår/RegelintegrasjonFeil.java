package no.nav.ung.sak.inngangsvilkår;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface RegelintegrasjonFeil extends DeklarerteFeil {
    RegelintegrasjonFeil FEILFACTORY = FeilFactory.create(RegelintegrasjonFeil.class);

    @TekniskFeil(feilkode = "FP-384251", feilmelding = "Ikke mulig å utlede gyldig vilkårsresultat fra enkeltvilkår", logLevel = LogLevel.WARN)
    Feil kanIkkeUtledeVilkårsresultatFraRegelmotor();

    @TekniskFeil(feilkode = "FP-384255", feilmelding = "Ikke mulig å oversette adopsjonsgrunnlag til regelmotor for behandlingId %s", logLevel = LogLevel.WARN)
    Feil kanIkkeOversetteAdopsjonsgrunnlag(String behandlingId);

    @TekniskFeil(feilkode = "FP-384257", feilmelding = "Kunne ikke serialisere regelinput for vilkår: %s", logLevel = LogLevel.WARN)
    Feil kanIkkeSerialisereRegelinput(String vilkårType, Exception e);

}
