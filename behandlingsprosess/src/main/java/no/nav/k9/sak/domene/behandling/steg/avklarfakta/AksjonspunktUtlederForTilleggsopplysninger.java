package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import static java.util.Collections.emptyList;
import static no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;

/**
 * Aksjonspunkt for avklaring v tilleggopplysninger som oppgis i søknad.
 */
@ApplicationScoped
public class AksjonspunktUtlederForTilleggsopplysninger implements AksjonspunktUtleder {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();
    private SøknadRepository søknadRepository;

    AksjonspunktUtlederForTilleggsopplysninger() {
    }

    @Inject
    public AksjonspunktUtlederForTilleggsopplysninger(SøknadRepository søknadRepository) {
        this.søknadRepository = søknadRepository;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        final Optional<SøknadEntitet> søknad = søknadRepository.hentSøknadHvisEksisterer(param.getBehandlingId());

        final Optional<String> tilleggsopplysninger = søknad.map(SøknadEntitet::getTilleggsopplysninger);
        if (tilleggsopplysninger.isPresent()) {
            return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TILLEGGSOPPLYSNINGER);
        }
        return INGEN_AKSJONSPUNKTER;
    }

}
