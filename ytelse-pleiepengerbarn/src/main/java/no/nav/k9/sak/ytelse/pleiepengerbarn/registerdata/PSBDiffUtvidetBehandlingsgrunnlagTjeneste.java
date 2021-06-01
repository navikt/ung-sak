package no.nav.k9.sak.ytelse.pleiepengerbarn.registerdata;

import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.domene.registerinnhenting.DiffUtvidetBehandlingsgrunnlagTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PSBDiffUtvidetBehandlingsgrunnlagTjeneste implements DiffUtvidetBehandlingsgrunnlagTjeneste {

    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    PSBDiffUtvidetBehandlingsgrunnlagTjeneste() {
        // CDI
    }

    @Inject
    public PSBDiffUtvidetBehandlingsgrunnlagTjeneste(SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                                     SykdomGrunnlagService sykdomGrunnlagService,
                                                     @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public void leggTilSnapshot(BehandlingReferanse ref, EndringsresultatSnapshot snapshot) {
        snapshot.leggTil(EndringsresultatSnapshot.medSnapshot(SykdomGrunnlag.class, UUID.randomUUID())); // For å tvinge frem at det alltid er endring
    }

    @Override
    public void leggTilDiffResultat(BehandlingReferanse ref, EndringsresultatDiff sporedeEndringerDiff) {
        sporedeEndringerDiff.hentDelresultat(SykdomGrunnlag.class)
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> diffSykdom(ref, idEndring)));
    }

    private DiffResult diffSykdom(BehandlingReferanse ref, EndringsresultatDiff idEndring) {
        var sykdomGrunnlag = sykdomGrunnlagRepository.hentGrunnlagForBehandling(ref.getBehandlingUuid())
            .map(SykdomGrunnlagBehandling::getGrunnlag);

        var perioder = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        List<Periode> nyeVurderingsperioder = SykdomUtils.toPeriodeList(perioder);
        var utledGrunnlag = sykdomGrunnlagService.utledGrunnlagMedManglendeOmsorgFjernet(ref.getSaksnummer(), ref.getBehandlingUuid(), ref.getBehandlingId(), ref.getPleietrengendeAktørId(), nyeVurderingsperioder);
        var sykdomGrunnlagSammenlikningsresultat = sykdomGrunnlagService.sammenlignGrunnlag(sykdomGrunnlag, utledGrunnlag);

        return new SykdomDiffResult(sykdomGrunnlagSammenlikningsresultat);
    }
}
