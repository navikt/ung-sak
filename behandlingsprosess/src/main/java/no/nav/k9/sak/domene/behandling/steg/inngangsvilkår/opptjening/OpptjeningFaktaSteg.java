package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsVilkårTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderBekreftetOpptjening;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderOppgittOpptjening;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

/**
 * Steg 81 - Kontroller fakta for opptjening
 */
@BehandlingStegRef(kode = "VURDER_OPPTJ_FAKTA")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class OpptjeningFaktaSteg extends OpptjeningFaktaStegFelles {

    @Inject
    public OpptjeningFaktaSteg(BehandlingRepositoryProvider repositoryProvider,
                                 AksjonspunktutlederForVurderBekreftetOpptjening aksjonspunktutlederBekreftet,
                                 AksjonspunktutlederForVurderOppgittOpptjening aksjonspunktutlederOppgitt,
                                 @FagsakYtelseTypeRef OpptjeningsVilkårTjeneste opptjeningsVilkårTjeneste,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider, aksjonspunktutlederBekreftet, aksjonspunktutlederOppgitt, opptjeningsVilkårTjeneste, skjæringstidspunktTjeneste);
    }
}
