package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.beregning.v1.FrisinnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedSøkerInfoDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.FrisinnBehandlingType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnMapper;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnSøknadsperiodeMapper;
import no.nav.vedtak.konfig.KonfigVerdi;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnYtelsesspesifiktGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<FrisinnGrunnlag> {

    private UttakRepository uttakRepository;
    private Boolean toggletVilkårsperioder;

    FrisinnYtelsesspesifiktGrunnlagMapper() {
    }

    @Inject
    public FrisinnYtelsesspesifiktGrunnlagMapper(UttakRepository uttakRepository,
                                                 @KonfigVerdi(value = "FRISINN_VILKARSPERIODER", defaultVerdi = "false") Boolean toggletVilkårsperioder) {
        this.uttakRepository = uttakRepository;
        this.toggletVilkårsperioder = toggletVilkårsperioder;
    }

    @Override
    public FrisinnGrunnlag lagYtelsespesifiktGrunnlag(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode) {
        var fastsattUttak = uttakRepository.hentFastsattUttak(ref.getBehandlingId());
        var origFastsattUttak = ref.getOriginalBehandlingId().map(origBehandlingId -> uttakRepository.hentFastsattUttak(origBehandlingId));

        List<PeriodeMedSøkerInfoDto> periodeMedSøkerInfoDtos;
        if (toggletVilkårsperioder) {
            periodeMedSøkerInfoDtos = FrisinnMapper.mapPeriodeMedSøkerInfoDto(fastsattUttak)
                .stream()
                .filter(p -> vilkårsperiode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(p.getPeriode().getFom(), p.getPeriode().getTom())))
                .collect(Collectors.toList());
        } else {
            periodeMedSøkerInfoDtos = FrisinnMapper.mapPeriodeMedSøkerInfoDto(fastsattUttak);
        }

        return new FrisinnGrunnlag(periodeMedSøkerInfoDtos, finnFrisinnBehandlingType(fastsattUttak, origFastsattUttak));
    }

    private FrisinnBehandlingType finnFrisinnBehandlingType(UttakAktivitet fastsattUttak, Optional<UttakAktivitet> origFastsattUttak) {
        List<Periode> søknadsperioder = FrisinnSøknadsperiodeMapper.map(fastsattUttak);
        return origFastsattUttak.map(FrisinnSøknadsperiodeMapper::map)
            .map(origSøknadsperioder -> {
                boolean harSøktOmNyPeriode = origSøknadsperioder.size() < søknadsperioder.size();
                if (harSøktOmNyPeriode) {
                    return FrisinnBehandlingType.NY_SØKNADSPERIODE;
                }
                return FrisinnBehandlingType.REVURDERING;
            }).orElse(FrisinnBehandlingType.NY_SØKNADSPERIODE);
    }

}
