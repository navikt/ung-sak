package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.beregning.v1.FrisinnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedSøkerInfoDto;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnMapper;
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
                                                 @KonfigVerdi(value = "FRISINN_VILKARSPERIODER", defaultVerdi = "true") Boolean toggletVilkårsperioder) {
        this.uttakRepository = uttakRepository;
        this.toggletVilkårsperioder = toggletVilkårsperioder;
    }

    @Override
    public FrisinnGrunnlag lagYtelsespesifiktGrunnlag(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode) {
        var søknadsperiode = uttakRepository.hentOppgittSøknadsperioder(ref.getBehandlingId()).getMaksPeriode();
        var fastsattUttak = uttakRepository.hentFastsattUttak(ref.getBehandlingId());

        //TODO(OJR) fjern dette når man har fikset kalkulus
        boolean søkerYtelseForFrilans = fastsattUttak.getPerioder().stream()
                .anyMatch(p -> p.getPeriode().overlapper(søknadsperiode) && p.getAktivitetType() == UttakArbeidType.FRILANSER);

        boolean søkerYtelseForNæring = fastsattUttak.getPerioder().stream()
                .anyMatch(p -> p.getPeriode().overlapper(søknadsperiode) && p.getAktivitetType() == UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);
        //TODO(OJR) fjern mellom todoene

        List<PeriodeMedSøkerInfoDto> periodeMedSøkerInfoDtos;
        if (toggletVilkårsperioder) {
            periodeMedSøkerInfoDtos = FrisinnMapper.mapPeriodeMedSøkerInfoDto(fastsattUttak)
                .stream()
                .filter(p -> vilkårsperiode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(p.getPeriode().getFom(), p.getPeriode().getTom())))
                .collect(Collectors.toList());
        } else {
            periodeMedSøkerInfoDtos = FrisinnMapper.mapPeriodeMedSøkerInfoDto(fastsattUttak);
        }


        FrisinnGrunnlag frisinnGrunnlag = new FrisinnGrunnlag(søkerYtelseForFrilans, søkerYtelseForNæring);
        frisinnGrunnlag.medPerioderMedSøkerInfo(periodeMedSøkerInfoDtos);

        return frisinnGrunnlag;
    }

}
