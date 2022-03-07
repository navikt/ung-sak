package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.delt;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak;

public class UtledetUtenlandsopphold {

    private Landkoder landkode = Landkoder.UDEFINERT;

    private UtenlandsoppholdÅrsak årsak;

    public UtledetUtenlandsopphold(Landkoder landkode, UtenlandsoppholdÅrsak årsak) {
        this.landkode = landkode;
        this.årsak = årsak;
    }

    public Landkoder getLandkode() {
        return landkode;
    }

    public UtenlandsoppholdÅrsak getÅrsak() {
        return årsak;
    }
}
