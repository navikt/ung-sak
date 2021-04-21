package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.endring;

import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;

@ApplicationScoped
@GrunnlagRef("SykdomGrunnlag")
@FagsakYtelseTypeRef("PSB")
class StartpunktUtlederSykdom implements EndringStartpunktUtleder {

    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomVurderingService sykdomVurderingService;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    StartpunktUtlederSykdom() {
        // For CDI
    }

    @Inject
    StartpunktUtlederSykdom(SykdomGrunnlagRepository sykdomGrunnlagRepository,
                            SykdomVurderingService sykdomVurderingService,
                            @FagsakYtelseTypeRef("PSB") VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomVurderingService = sykdomVurderingService;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        var sykdomGrunnlag = sykdomGrunnlagRepository.hentGrunnlagForId((UUID) grunnlagId1);
        if (sykdomGrunnlag.isEmpty()) {
            return StartpunktType.UDEFINERT;
        }

        var perioder = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        List<Periode> nyeVurderingsperioder = SykdomUtils.toPeriodeList(perioder);
        var utledGrunnlag = sykdomGrunnlagRepository.utledGrunnlag(ref.getSaksnummer(), ref.getBehandlingUuid(), ref.getPleietrengendeAktørId(), nyeVurderingsperioder);
        var sykdomGrunnlagSammenlikningsresultat = sykdomVurderingService.sammenlignGrunnlag(sykdomGrunnlag, utledGrunnlag);

        var erIngenEndringIGrunnlaget = sykdomGrunnlagSammenlikningsresultat.getDiffPerioder().isEmpty();
        return erIngenEndringIGrunnlaget ? StartpunktType.UDEFINERT : StartpunktType.INNGANGSVILKÅR_MEDISINSK;
    }

}
