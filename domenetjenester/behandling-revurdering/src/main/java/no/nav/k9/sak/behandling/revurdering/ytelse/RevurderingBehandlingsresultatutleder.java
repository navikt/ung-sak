package no.nav.k9.sak.behandling.revurdering.ytelse;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.sak.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.k9.sak.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.domene.medlem.MedlemTjeneste;

@Dependent
@FagsakYtelseTypeRef
@BehandlingTypeRef("BT-004")
public class RevurderingBehandlingsresultatutleder extends RevurderingBehandlingsresultatutlederFelles {

    @Inject
    public RevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider, // NOSONAR
                                                 VedtakVarselRepository vedtakVarselRepository,
                                                 BeregningTjeneste beregningsgrunnlagTjeneste,
                                                 @FagsakYtelseTypeRef HarEtablertYtelse harEtablertYtelse,
                                                 MedlemTjeneste medlemTjeneste) {
        super(repositoryProvider,
                vedtakVarselRepository,
                beregningsgrunnlagTjeneste,
                medlemTjeneste,
                harEtablertYtelse);
    }


}
