package no.nav.foreldrepenger.domene.medlem.kontrollerfakta;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;

@ApplicationScoped
public class AksjonspunktutlederForMedisinskvilkår implements AksjonspunktUtleder {

    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    AksjonspunktutlederForMedisinskvilkår() {
        //CDI
    }

    @Inject
    public AksjonspunktutlederForMedisinskvilkår(MedisinskGrunnlagRepository medisinskGrunnlagRepository, BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {

        // Vurder om det skal tas stilling til omsorgen for
        final var medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(param.getBehandlingId());
        if (medisinskGrunnlag.isPresent()) {
            final var grunnlag = medisinskGrunnlag.get();
            final var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(param.getRef());
            if (grunnlag.getPleietrengende() != null && grunnlag.getPleietrengende().getAktørId() != null && personopplysningerAggregat.isPresent()) {
                final var aggregat = personopplysningerAggregat.get();
                final var pleietrengende = grunnlag.getPleietrengende().getAktørId();

                final var pleietrengendeRelasjon = aggregat.getSøkersRelasjoner()
                    .stream()
                    .filter(it -> it.getTilAktørId().equals(pleietrengende))
                    .findFirst()
                    .map(PersonRelasjonEntitet::getRelasjonsrolle)
                    .orElse(RelasjonsRolleType.UDEFINERT);

                final var harSammeBosted = aggregat.harSøkerSammeAdresseSom(pleietrengende, RelasjonsRolleType.BARN);
                if (harSammeBosted && RelasjonsRolleType.BARN.equals(pleietrengendeRelasjon)) {
                    return List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING));
                }
            }
        }

        return List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING),
            AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR));
    }
}
