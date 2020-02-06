package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static java.util.stream.Collectors.toList;

import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FordelBeregningsgrunnlagAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FordelBeregningsgrunnlagDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FordelBeregningsgrunnlagPeriodeDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FordelingDto;

@ApplicationScoped
public class FordelBeregningsgrunnlagDtoTjeneste {

    private FordelBeregningsgrunnlagAndelDtoTjeneste andelDtoTjeneste;
    private RefusjonEllerGraderingArbeidsforholdDtoTjeneste arbeidsforholdDtoTjeneste;
    private ManuellBehandlingRefusjonGraderingDtoTjeneste manuellBehandling;

    FordelBeregningsgrunnlagDtoTjeneste() {
        // Hibernate
    }


    @Inject
    public FordelBeregningsgrunnlagDtoTjeneste(FordelBeregningsgrunnlagAndelDtoTjeneste andelDtoTjeneste,
                                               RefusjonEllerGraderingArbeidsforholdDtoTjeneste arbeidsforholdDtoTjeneste,
                                               ManuellBehandlingRefusjonGraderingDtoTjeneste manuellBehandling) {
        this.andelDtoTjeneste = andelDtoTjeneste;
        this.arbeidsforholdDtoTjeneste = arbeidsforholdDtoTjeneste;
        this.manuellBehandling = manuellBehandling;
    }

    public void lagDto(BeregningsgrunnlagInput input,
                       FordelingDto dto) {
        FordelBeregningsgrunnlagDto bgDto = new FordelBeregningsgrunnlagDto();
        settEndretArbeidsforholdDto(input, bgDto);
        bgDto.setFordelBeregningsgrunnlagPerioder(lagPerioder(input));
        dto.setFordelBeregningsgrunnlag(bgDto);
    }

    private List<FordelBeregningsgrunnlagPeriodeDto> lagPerioder(BeregningsgrunnlagInput input) {
        List<BeregningsgrunnlagPeriode> bgPerioder = input.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder();
        List<FordelBeregningsgrunnlagPeriodeDto> fordelPerioder = bgPerioder.stream()
            .map(periode -> mapTilPeriodeDto(input, periode))
            .sorted(Comparator.comparing(FordelBeregningsgrunnlagPeriodeDto::getFom)).collect(toList());
        return fordelPerioder;
    }

    private FordelBeregningsgrunnlagPeriodeDto mapTilPeriodeDto(BeregningsgrunnlagInput input,
                                                                BeregningsgrunnlagPeriode periode) {
        FordelBeregningsgrunnlagPeriodeDto fordelBGPeriode = new FordelBeregningsgrunnlagPeriodeDto();
        fordelBGPeriode.setFom(periode.getBeregningsgrunnlagPeriodeFom());
        fordelBGPeriode.setTom(periode.getBeregningsgrunnlagPeriodeTom());
        List<FordelBeregningsgrunnlagAndelDto> fordelAndeler = lagFordelBGAndeler(fordelBGPeriode, input, periode);
        fordelBGPeriode.setFordelBeregningsgrunnlagAndeler(fordelAndeler);
        return fordelBGPeriode;
    }

    private void settEndretArbeidsforholdDto(BeregningsgrunnlagInput input, FordelBeregningsgrunnlagDto bgDto) {
        arbeidsforholdDtoTjeneste
            .lagListeMedDtoForArbeidsforholdSomSøkerRefusjonEllerGradering(input)
            .forEach(bgDto::leggTilArbeidsforholdTilFordeling);
    }

    private List<FordelBeregningsgrunnlagAndelDto> lagFordelBGAndeler(FordelBeregningsgrunnlagPeriodeDto fordelBGPeriode,
                                                                      BeregningsgrunnlagInput input,
                                                                      BeregningsgrunnlagPeriode periode) {
        var beregningAktivitetAggregat = input.getBeregningsgrunnlagGrunnlag().getGjeldendeAktiviteter();
        var aktivitetGradering = input.getAktivitetGradering();
        List<FordelBeregningsgrunnlagAndelDto> fordelAndeler = andelDtoTjeneste.lagEndretBgAndelListe(input, periode);
        fordelBGPeriode.setHarPeriodeAarsakGraderingEllerRefusjon(manuellBehandling
            .skalSaksbehandlerRedigereInntekt(beregningAktivitetAggregat, aktivitetGradering, periode, input.getInntektsmeldinger()));
        fordelBGPeriode.setSkalKunneEndreRefusjon(manuellBehandling
            .skalSaksbehandlerRedigereRefusjon(beregningAktivitetAggregat, aktivitetGradering, periode, input.getInntektsmeldinger()));
        RefusjonDtoTjeneste.slåSammenRefusjonForAndelerISammeArbeidsforhold(fordelAndeler);
        return fordelAndeler;
    }

}
