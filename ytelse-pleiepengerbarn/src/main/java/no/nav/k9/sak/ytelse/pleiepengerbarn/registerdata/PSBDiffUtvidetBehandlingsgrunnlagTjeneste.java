package no.nav.k9.sak.ytelse.pleiepengerbarn.registerdata;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.domene.registerinnhenting.DiffUtvidetBehandlingsgrunnlagTjeneste;
import no.nav.k9.sak.domene.registerinnhenting.EndringsresultatSjekker;
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

    private static final Logger log = LoggerFactory.getLogger(PSBDiffUtvidetBehandlingsgrunnlagTjeneste.class);

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
        var uuid = UUID.randomUUID();
        log.info("Legger til snapshot med uuid={}", uuid);
        snapshot.leggTil(EndringsresultatSnapshot.medSnapshot(SykdomGrunnlag.class, uuid)); // For å tvinge frem at det alltid er endring
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
        var manglendeOmsorgenForPerioder = sykdomGrunnlagService.hentManglendeOmsorgenForPerioder(ref.getBehandlingId());
        var utledetGrunnlag = sykdomGrunnlagRepository.utledGrunnlag(ref.getSaksnummer(), ref.getBehandlingUuid(), ref.getPleietrengendeAktørId(), nyeVurderingsperioder, manglendeOmsorgenForPerioder);
        var sykdomGrunnlagSammenlikningsresultat = sykdomGrunnlagService.sammenlignGrunnlag(sykdomGrunnlag, utledetGrunnlag);
        return new SykdomDiffResult(sykdomGrunnlagSammenlikningsresultat);
    }
}
