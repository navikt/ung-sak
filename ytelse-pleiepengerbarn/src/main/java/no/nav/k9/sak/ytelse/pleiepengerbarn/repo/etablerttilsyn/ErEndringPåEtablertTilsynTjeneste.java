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

    public boolean erUhåndterteEndringerFraForrigeBehandling(BehandlingReferanse referanse) {
        LocalDateTimeline<Boolean> resultat = perioderMedEndringerFraForrigeBehandling(referanse);
        return !resultat.isEmpty();
    }

    /**
     * Differ mot det som er trukket inn på den aktuelle behandlingen, hvis ikke det er trukket inn noe sjekkes det mot forrige behandling
     * @param referanse referansen
     * @return true / false om det endringer som må tas hensyn til
     */
    public boolean erUhåndterteEndringerFraForrigeVersjon(BehandlingReferanse referanse) {
        LocalDateTimeline<Boolean> resultat = perioderMedEndringerFraEksisterendeVersjon(referanse);
        return !resultat.isEmpty();
    }

    public LocalDateTimeline<Boolean> perioderMedEndringerFraForrigeBehandling(BehandlingReferanse referanse) {
        LocalDateTimeline<Boolean> resultat = etablertTilsynTjeneste.finnForskjellerSidenForrigeBehandling(referanse);
        return endringerFra(referanse, resultat);
    }

    public LocalDateTimeline<Boolean> perioderMedEndringerFraEksisterendeVersjon(BehandlingReferanse referanse) {
        LocalDateTimeline<Boolean> resultat = etablertTilsynTjeneste.finnForskjellerFraEksisterendeVersjon(referanse);
        return endringerFra(referanse, resultat);
    }

    private LocalDateTimeline<Boolean> endringerFra(BehandlingReferanse referanse, LocalDateTimeline<Boolean> resultat) {
        resultat = SykdomUtils.kunPerioderSomIkkeFinnesI(resultat, SykdomUtils.toLocalDateTimeline(sykdomGrunnlagService.hentManglendeOmsorgenForPerioder(referanse.getBehandlingId())));
        //resultat = SykdomUtils.kunPerioderSomIkkeFinnesI(resultat, SykdomUtils.toLocalDateTimeline(utled(referanse.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR)));
        resultat = resultat.intersection(SykdomUtils.toLocalDateTimeline(perioderTilVurderingTjeneste.utledFullstendigePerioder(referanse.getBehandlingId())));
        return resultat;
    }
}
