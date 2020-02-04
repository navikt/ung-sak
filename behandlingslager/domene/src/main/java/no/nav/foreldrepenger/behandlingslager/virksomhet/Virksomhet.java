package no.nav.foreldrepenger.behandlingslager.virksomhet;

import java.time.LocalDate;

import no.nav.k9.kodeverk.organisasjon.Organisasjonstype;

public interface Virksomhet {

    String getOrgnr();

    String getNavn();

    LocalDate getRegistrert();

    LocalDate getOppstart();

    LocalDate getAvslutt();

    boolean skalRehentes();

    Organisasjonstype getOrganisasjonstype();
    
    boolean erKunstig();

}
