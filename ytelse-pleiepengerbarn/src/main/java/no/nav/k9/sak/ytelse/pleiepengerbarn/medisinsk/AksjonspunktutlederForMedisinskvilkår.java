package no.nav.k9.sak.ytelse.pleiepengerbarn.medisinsk;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;

@ApplicationScoped
public class AksjonspunktutlederForMedisinskvilkår implements AksjonspunktUtleder {

    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    AksjonspunktutlederForMedisinskvilkår() {
        //CDI
    }

    @Inject
    public AksjonspunktutlederForMedisinskvilkår(BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {

        // Vurder om det skal tas stilling til omsorgen for
        final var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(param.getRef(), param.getRef().getFagsakPeriode().getFomDato());
        if (param.getRef().getPleietrengendeAktørId() != null && personopplysningerAggregat.isPresent()) {
            final var aggregat = personopplysningerAggregat.get();
            final var pleietrengende = param.getRef().getPleietrengendeAktørId();

            final var pleietrengendeRelasjon = aggregat.getSøkersRelasjoner()
                .stream()
                .filter(it -> it.getTilAktørId().equals(pleietrengende))
                .findFirst()
                .map(PersonRelasjonEntitet::getRelasjonsrolle)
                .orElse(RelasjonsRolleType.UDEFINERT);

            if (RelasjonsRolleType.BARN.equals(pleietrengendeRelasjon)) {
                return List.of();
            }
        }

        return List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR));
    }
}
