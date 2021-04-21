package no.nav.k9.sak.ytelse.pleiepengerbarn.registerdata;

import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.domene.registerinnhenting.DiffUtvidetBehandlingsgrunnlagTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PSBDiffUtvidetBehandlingsgrunnlagTjeneste implements DiffUtvidetBehandlingsgrunnlagTjeneste {

    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomVurderingService sykdomVurderingService;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    PSBDiffUtvidetBehandlingsgrunnlagTjeneste() {
        // CDI
    }

    @Inject
    public PSBDiffUtvidetBehandlingsgrunnlagTjeneste(SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                                     SykdomVurderingService sykdomVurderingService,
                                                     @FagsakYtelseTypeRef("PSB") VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomVurderingService = sykdomVurderingService;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public void leggTilSnapshot(BehandlingReferanse ref, EndringsresultatSnapshot snapshot) {
        snapshot.leggTil(sykdomGrunnlagRepository.finnAktivGrunnlagId(ref.getBehandlingUuid()));
    }

    @Override
    public void leggTilDiffResultat(BehandlingReferanse ref, EndringsresultatDiff sporedeEndringerDiff) {
        sporedeEndringerDiff.hentDelresultat(SykdomGrunnlag.class)
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> diffSykdom(ref, idEndring)));
    }

    private DiffResult diffSykdom(BehandlingReferanse ref, EndringsresultatDiff idEndring) {
        var sykdomGrunnlag = sykdomGrunnlagRepository.hentGrunnlagForId((UUID) idEndring.getGrunnlagId1());
        if (sykdomGrunnlag.isEmpty()) {
            return SykdomDiffResult.ingenDiff();
        }

        var perioder = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        List<Periode> nyeVurderingsperioder = SykdomUtils.toPeriodeList(perioder);
        var utledGrunnlag = sykdomGrunnlagRepository.utledGrunnlag(ref.getSaksnummer(), ref.getBehandlingUuid(), ref.getPleietrengendeAktørId(), nyeVurderingsperioder);
        var sykdomGrunnlagSammenlikningsresultat = sykdomVurderingService.sammenlignGrunnlag(sykdomGrunnlag, utledGrunnlag);

        return new SykdomDiffResult(sykdomGrunnlagSammenlikningsresultat);
    }
}
