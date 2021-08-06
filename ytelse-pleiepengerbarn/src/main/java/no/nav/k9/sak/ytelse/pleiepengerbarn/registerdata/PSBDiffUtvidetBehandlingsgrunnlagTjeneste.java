package no.nav.k9.sak.ytelse.pleiepengerbarn.registerdata;

import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.domene.registerinnhenting.DiffUtvidetBehandlingsgrunnlagTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomGrunnlagSammenlikningsresultat;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PSBDiffUtvidetBehandlingsgrunnlagTjeneste implements DiffUtvidetBehandlingsgrunnlagTjeneste {

    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste;
    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    PSBDiffUtvidetBehandlingsgrunnlagTjeneste() {
        // CDI
    }

    @Inject
    public PSBDiffUtvidetBehandlingsgrunnlagTjeneste(SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                                     SykdomGrunnlagService sykdomGrunnlagService,
                                                     ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste,
                                                     EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                                     @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.erEndringPåEtablertTilsynTjeneste = erEndringPåEtablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public void leggTilSnapshot(BehandlingReferanse ref, EndringsresultatSnapshot snapshot) {
        var uuid = UUID.randomUUID();
        snapshot.leggTil(EndringsresultatSnapshot.medSnapshot(SykdomGrunnlag.class, uuid)); // For å tvinge frem at det alltid er endring
    }

    @Override
    public void leggTilDiffResultat(BehandlingReferanse ref, EndringsresultatDiff idDiff, EndringsresultatDiff sporedeEndringerDiff) {
        idDiff.hentDelresultat(SykdomGrunnlag.class)
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> diffSykdom(ref)));
    }

    private DiffResult diffSykdom(BehandlingReferanse ref) {
        var sykdomGrunnlagSammenlikningsresultat = diffSykdomsOpplysninger(ref);
        var etablertTilsyn = diffEtablertTilsyn(ref);
        var nattevåkBeredskap = diffNattevåkBeredskap(ref);

        return new SyktBarnGrunnlagDiff(sykdomGrunnlagSammenlikningsresultat, etablertTilsyn, nattevåkBeredskap);
    }

    private List<DatoIntervallEntitet> diffNattevåkBeredskap(BehandlingReferanse ref) {
        return endringUnntakEtablertTilsynTjeneste.utledRelevanteEndringerSidenForrigeBehandling(ref.getBehandlingId(), ref.getPleietrengendeAktørId());
    }

    private LocalDateTimeline<Boolean> diffEtablertTilsyn(BehandlingReferanse referanse) {
        return erEndringPåEtablertTilsynTjeneste.perioderMedEndringerFraForrigeBehandling(referanse);
    }

    private SykdomGrunnlagSammenlikningsresultat diffSykdomsOpplysninger(BehandlingReferanse ref) {
        var sykdomGrunnlag = sykdomGrunnlagRepository.hentGrunnlagForBehandling(ref.getBehandlingUuid())
            .map(SykdomGrunnlagBehandling::getGrunnlag);
        var perioder = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        List<Periode> nyeVurderingsperioder = SykdomUtils.toPeriodeList(perioder);
        var manglendeOmsorgenForPerioder = sykdomGrunnlagService.hentManglendeOmsorgenForPerioder(ref.getBehandlingId());
        var utledetGrunnlag = sykdomGrunnlagRepository.utledGrunnlag(ref.getSaksnummer(), ref.getBehandlingUuid(), ref.getPleietrengendeAktørId(), nyeVurderingsperioder, manglendeOmsorgenForPerioder);
        return sykdomGrunnlagService.sammenlignGrunnlag(sykdomGrunnlag, utledetGrunnlag);
    }
}
