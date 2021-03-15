package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

// TODO: PK-49128: Rename og ta ut overflødige metoder
public interface VilkårUtlederFeil extends DeklarerteFeil {
    VilkårUtlederFeil FEILFACTORY = FeilFactory.create(VilkårUtlederFeil.class);

    @TekniskFeil(feilkode = "FP-768019", feilmelding = "Kan ikke utlede vilkår for behandlingId %s, da behandlingsmotiv ikke kan avgjøres", logLevel = LogLevel.ERROR)
    Feil behandlingsmotivKanIkkeUtledes(Long behandlingId);
}
