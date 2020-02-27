package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;

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
