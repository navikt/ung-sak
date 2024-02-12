package no.nav.k9.sak.domene.person.personopplysning;

import java.time.LocalDate;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonstatusEntitet;

@Dependent
public class UtlandVurdererTjeneste {

    private PersonopplysningTjeneste personopplysningTjeneste;
    private static final Logger log = LoggerFactory.getLogger(UtlandVurdererTjeneste.class);

    UtlandVurdererTjeneste() {
    }

    @Inject
    public UtlandVurdererTjeneste(PersonopplysningTjeneste personopplysningTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    /**
     * Best effort utenlandssjekk. Denne kan gi true, men likevel ikke være utenlandsk.
     */
    public boolean erUtenlandssak(Behandling behandling) {
        return erOpprettetEllerUtført(behandling, AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK)
               || erOpprettetEllerUtført(behandling, AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE)
               || harPleietrengendeDnr(behandling);
    }

    private boolean harPleietrengendeDnr(Behandling behandling) {
        if (behandling.getFagsak().getPleietrengendeAktørId() == null) {
            return false;
        }

        Optional<PersonopplysningerAggregat> personopplysningerAggregat = personopplysningTjeneste
            .hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(behandling.getId(), behandling.getAktørId(), LocalDate.now());

        if (personopplysningerAggregat.isEmpty()) {
            return false;
        }

        PersonstatusEntitet personstatusFor = personopplysningerAggregat.get()
            .getPersonstatusFor(behandling.getFagsak().getPleietrengendeAktørId());
        if (personstatusFor == null) {
            log.info("Mangler personstatus");
            return false;
        }

        return personstatusFor.getPersonstatus() == PersonstatusType.ADNR;

    }

    private static boolean erOpprettetEllerUtført(Behandling behandling, AksjonspunktDefinisjon ap) {
        return behandling.getAksjonspunktMedDefinisjonOptional(ap)
            .map(it -> !it.erAvbrutt())
            .isPresent();
    }
}
