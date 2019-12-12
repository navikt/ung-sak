package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.GrunnbeløpTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.vedtak.konfig.KonfigVerdi;

@FagsakYtelseTypeRef
@ApplicationScoped
public class GrunnbeløpTjenesteImpl extends GrunnbeløpTjeneste {

    private Integer grunnbeløpMilitærHarKravPåFP;

    protected GrunnbeløpTjenesteImpl() {
        super(null);
        // for CDI proxy
    }

    /**
     * @param grunnbeløpMilitærHarKravPå - Antall grunnbeløp søker har krav på hvis det søkes om foreldrepenger og søker har militærstatus
     *            (positivt heltall)
     */
    @Inject
    public GrunnbeløpTjenesteImpl(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                  @KonfigVerdi(value = "fp.militær.grunnbeløp.minstekrav", defaultVerdi = "3") Integer grunnbeløpMilitærHarKravPå) {
        super(beregningsgrunnlagRepository);
        this.grunnbeløpMilitærHarKravPåFP = grunnbeløpMilitærHarKravPå;
    }

    @Override
    public Integer finnAntallGrunnbeløpMilitærHarKravPå() {
        return grunnbeløpMilitærHarKravPåFP;
    }
}
