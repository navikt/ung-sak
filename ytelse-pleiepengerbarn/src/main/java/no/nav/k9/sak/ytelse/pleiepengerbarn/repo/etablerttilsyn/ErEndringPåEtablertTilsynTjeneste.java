package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.TreeSet;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagTjeneste;

@Dependent
public class ErEndringPåEtablertTilsynTjeneste {

    private EtablertTilsynTjeneste etablertTilsynTjeneste;
    private MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    @Inject
    public ErEndringPåEtablertTilsynTjeneste(EtablertTilsynTjeneste etablertTilsynTjeneste,
                                             MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste,
                                             @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.medisinskGrunnlagTjeneste = medisinskGrunnlagTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    /**
     * Differ mot det som er trukket inn på den aktuelle behandlingen, hvis ikke det er trukket inn noe sjekkes det mot forrige behandling
     * @param referanse referansen
     * @return true / false om det endringer som må tas hensyn til
     */
    public boolean erEndringerSidenBehandling(BehandlingReferanse referanse) {
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
        resultat = TidslinjeUtil.kunPerioderSomIkkeFinnesI(resultat, TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(medisinskGrunnlagTjeneste.hentManglendeOmsorgenForPerioder(referanse.getBehandlingId()))));
        //resultat = SykdomUtils.kunPerioderSomIkkeFinnesI(resultat, SykdomUtils.toLocalDateTimeline(utled(referanse.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR)));
        resultat = resultat.intersection(TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurderingTjeneste.utledFullstendigePerioder(referanse.getBehandlingId())));
        return resultat;
    }
}
