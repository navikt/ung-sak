package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;

@Dependent
public class ErEndringPåEtablertTilsynTjeneste {

    private EtablertTilsynTjeneste etablertTilsynTjeneste;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    @Inject
    public ErEndringPåEtablertTilsynTjeneste(EtablertTilsynTjeneste etablertTilsynTjeneste,
                                             SykdomGrunnlagService sykdomGrunnlagService,
                                             @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    public boolean erUhåndterteEndringer(BehandlingReferanse referanse) {
        LocalDateTimeline<Boolean> resultat = perioderMedEndringer(referanse);
        return !resultat.isEmpty();
    }

    public LocalDateTimeline<Boolean> perioderMedEndringer(BehandlingReferanse referanse) {
        LocalDateTimeline<Boolean> resultat = etablertTilsynTjeneste.finnForskjellerSidenForrigeBehandling(referanse);
        resultat = SykdomUtils.kunPerioderSomIkkeFinnesI(resultat, SykdomUtils.toLocalDateTimeline(sykdomGrunnlagService.hentManglendeOmsorgenForPerioder(referanse.getBehandlingId())));
        //resultat = SykdomUtils.kunPerioderSomIkkeFinnesI(resultat, SykdomUtils.toLocalDateTimeline(utled(referanse.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR)));
        resultat = resultat.intersection(SykdomUtils.toLocalDateTimeline(perioderTilVurderingTjeneste.utledFullstendigePerioder(referanse.getBehandlingId())));
        return resultat;
    }
}
