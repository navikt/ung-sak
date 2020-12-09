package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FaktaOmBeregningTilfelleDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.ArbeidstakerandelUtenIMMottarYtelseDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.AvklarteAktiviteterDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.BeregningsaktivitetLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.BesteberegningFødendeKvinneAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.BesteberegningFødendeKvinneDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.DagpengeAndelLagtTilBesteberegningDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsattBrukersAndel;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsatteAndelerTidsbegrensetDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsattePerioderTidsbegrensetDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsatteVerdierDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBgKunYtelseDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettEtterlønnSluttpakkeDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettMånedsinntektFLDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettMånedsinntektUtenInntektsmeldingAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettMånedsinntektUtenInntektsmeldingDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.InntektPrAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.MottarYtelseDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.RedigerbarAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.RefusjonskravPrArbeidsgiverVurderingDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderATogFLiSammeOrganisasjonAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderATogFLiSammeOrganisasjonDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderEtterlønnSluttpakkeDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderLønnsendringDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderMilitærDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderNyoppstartetFLDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderTidsbegrensetArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderteArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling.FordelBeregningsgrunnlagAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling.FordelBeregningsgrunnlagPeriodeDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling.FordelFastsatteVerdierDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling.FordelRedigerbarAndelDto;


public class OppdatererDtoMapper {

    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBGTidsbegrensetArbeidsforholdDto mapFastsettBGTidsbegrensetArbeidsforholdDto(FastsettBGTidsbegrensetArbeidsforholdDto tidsbegrensetDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBGTidsbegrensetArbeidsforholdDto(
            tidsbegrensetDto.getFastsatteTidsbegrensedePerioder() == null ? null : mapTilFastsattTidsbegrensetPerioder(tidsbegrensetDto.getFastsatteTidsbegrensedePerioder()),
            tidsbegrensetDto.getFrilansInntekt());
    }

    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.AvklarteAktiviteterDto mapAvklarteAktiviteterDto(AvklarteAktiviteterDto dto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.AvklarteAktiviteterDto(mapTilBeregningsaktivitetLagreDtoList(dto.getBeregningsaktivitetLagreDtoList()));
    }

    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto mapFastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto(FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto(dto.getBruttoBeregningsgrunnlag());
    }

    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBruttoBeregningsgrunnlagSNDto mapFastsettBruttoBeregningsgrunnlagSNDto(FastsettBruttoBeregningsgrunnlagSNDto dto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBruttoBeregningsgrunnlagSNDto(dto.getBruttoBeregningsgrunnlag());
    }

    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagDto mapFordelBeregningsgrunnlagDto(FordelBeregningsgrunnlagDto dto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagDto(mapTilEndredePerioderList(dto.getEndretBeregningsgrunnlagPerioder()));
    }

    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.VurderVarigEndringEllerNyoppstartetSNDto mapdVurderVarigEndringEllerNyoppstartetSNDto(VurderVarigEndringEllerNyoppstartetSNDto dto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.VurderVarigEndringEllerNyoppstartetSNDto(dto.getErVarigEndretNaering(), dto.getBruttoBeregningsgrunnlag());
    }

    public static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.BeregningsaktivitetLagreDto> mapOverstyrBeregningsaktiviteterDto(List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList) {
        return beregningsaktivitetLagreDtoList.stream().map(OppdatererDtoMapper::mapTilBeregningsaktivitetLagreDto).collect(Collectors.toList());
    }


    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FaktaBeregningLagreDto mapTilFaktaOmBeregningLagreDto(FaktaBeregningLagreDto fakta) {
        return fakta == null ? null : new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FaktaBeregningLagreDto(
                fakta.getVurderNyoppstartetFL() == null ? null : mapVurderNyoppstartetFLDto(fakta.getVurderNyoppstartetFL()),
                fakta.getVurderTidsbegrensetArbeidsforhold() == null ? null : mapTidsbegrensetArbeidsforhold(fakta.getVurderTidsbegrensetArbeidsforhold()),
                fakta.getVurderNyIArbeidslivet() == null ? null : mapVurderNyIArbeidslivet(fakta.getVurderNyIArbeidslivet()),
                fakta.getFastsettMaanedsinntektFL() == null ? null : mapFastsettMånedsinntektFL(fakta.getFastsettMaanedsinntektFL()),
                fakta.getVurdertLonnsendring() == null ? null : mapVurderLønnsendringDto(fakta.getVurdertLonnsendring()),
                fakta.getFastsattUtenInntektsmelding() == null ? null : mapFastsattUtenInntektsmeldingDto(fakta.getFastsattUtenInntektsmelding()),
                fakta.getVurderATogFLiSammeOrganisasjon() == null ? null : mapVurderAtOgFLiSammeOrganisasjonDto(fakta.getVurderATogFLiSammeOrganisasjon()),
                fakta.getBesteberegningAndeler() == null ? null : mapBesteberegningFødendeKvinneDto(fakta.getBesteberegningAndeler()),
                fakta.getFaktaOmBeregningTilfeller() == null ? null : mapFaktaOmBeregningTilfeller(fakta.getFaktaOmBeregningTilfeller()),
                fakta.getKunYtelseFordeling() == null ? null : mapFastsettKunYtelseDto(fakta.getKunYtelseFordeling()),
                fakta.getVurderEtterlønnSluttpakke() == null ? null : mapVurderEtterlønnSluttpakke(fakta.getVurderEtterlønnSluttpakke()),
                fakta.getFastsettEtterlønnSluttpakke() == null ? null : mapFastsettEtterlønnSluttpakker(fakta.getFastsettEtterlønnSluttpakke()),
                fakta.getMottarYtelse() == null ? null : mapMottarYtelse(fakta.getMottarYtelse()),
                fakta.getVurderMilitaer() == null ? null : mapVurderMilitær(fakta.getVurderMilitaer()),
                fakta.getRefusjonskravGyldighet() == null ? null : mapRefusjonskravPrArbeidsgiverVurderingDto(fakta.getRefusjonskravGyldighet())
        );
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.RefusjonskravPrArbeidsgiverVurderingDto> mapRefusjonskravPrArbeidsgiverVurderingDto(List<RefusjonskravPrArbeidsgiverVurderingDto> refusjonskravGyldighet) {
        return refusjonskravGyldighet.stream().map(OppdatererDtoMapper::mapRefusjonskravGyldighet).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.RefusjonskravPrArbeidsgiverVurderingDto mapRefusjonskravGyldighet(RefusjonskravPrArbeidsgiverVurderingDto refusjonskravPrArbeidsgiverVurderingDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.RefusjonskravPrArbeidsgiverVurderingDto(
            refusjonskravPrArbeidsgiverVurderingDto.getArbeidsgiverId(),
            refusjonskravPrArbeidsgiverVurderingDto.isSkalUtvideGyldighet()
            );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderMilitærDto mapVurderMilitær(VurderMilitærDto vurderMilitaer) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderMilitærDto(vurderMilitaer.getHarMilitaer());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.MottarYtelseDto mapMottarYtelse(MottarYtelseDto mottarYtelse) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.MottarYtelseDto(
            mottarYtelse.getFrilansMottarYtelse(),
            mottarYtelse.getArbeidstakerUtenIMMottarYtelse() == null ? null : mapArbeidstakterUtenIMMottarYtelseListe(mottarYtelse.getArbeidstakerUtenIMMottarYtelse()));
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.ArbeidstakerandelUtenIMMottarYtelseDto> mapArbeidstakterUtenIMMottarYtelseListe(List<ArbeidstakerandelUtenIMMottarYtelseDto> arbeidstakerUtenIMMottarYtelse) {
        return arbeidstakerUtenIMMottarYtelse.stream().map(OppdatererDtoMapper::mapArbeidstakterUtenIMMottarYtelse).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.ArbeidstakerandelUtenIMMottarYtelseDto mapArbeidstakterUtenIMMottarYtelse(ArbeidstakerandelUtenIMMottarYtelseDto arbeidstakerandelUtenIMMottarYtelseDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.ArbeidstakerandelUtenIMMottarYtelseDto(
            arbeidstakerandelUtenIMMottarYtelseDto.getAndelsnr(),
            arbeidstakerandelUtenIMMottarYtelseDto.getMottarYtelse()
        );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettEtterlønnSluttpakkeDto mapFastsettEtterlønnSluttpakker(FastsettEtterlønnSluttpakkeDto fastsettEtterlønnSluttpakke) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettEtterlønnSluttpakkeDto(fastsettEtterlønnSluttpakke.getFastsattPrMnd());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderEtterlønnSluttpakkeDto mapVurderEtterlønnSluttpakke(VurderEtterlønnSluttpakkeDto vurderEtterlønnSluttpakke) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderEtterlønnSluttpakkeDto(
            vurderEtterlønnSluttpakke.erEtterlønnSluttpakke()
        );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettBgKunYtelseDto mapFastsettKunYtelseDto(FastsettBgKunYtelseDto kunYtelseFordeling) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettBgKunYtelseDto(
            mapKunYtelseAndeler(kunYtelseFordeling.getAndeler()),
            kunYtelseFordeling.getSkalBrukeBesteberegning()
        );
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsattBrukersAndel> mapKunYtelseAndeler(List<FastsattBrukersAndel> andeler) {
        return andeler.stream().map(OppdatererDtoMapper::mapFastsattBrukersAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsattBrukersAndel mapFastsattBrukersAndel(FastsattBrukersAndel fastsattBrukersAndel) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsattBrukersAndel(
            fastsattBrukersAndel.getAndelsnr(),
            fastsattBrukersAndel.getNyAndel(),
            fastsattBrukersAndel.getLagtTilAvSaksbehandler(),
            fastsattBrukersAndel.getFastsattBeløp(),
            fastsattBrukersAndel.getInntektskategori() == null ?
                null : new no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori(fastsattBrukersAndel.getInntektskategori().getKode())
        );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderATogFLiSammeOrganisasjonDto mapVurderAtOgFLiSammeOrganisasjonDto(VurderATogFLiSammeOrganisasjonDto vurderATogFLiSammeOrganisasjon) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderATogFLiSammeOrganisasjonDto(
            mapVurderAtOgFLiSammeOranisasjonAndelListe(vurderATogFLiSammeOrganisasjon.getVurderATogFLiSammeOrganisasjonAndelListe())
        );
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderATogFLiSammeOrganisasjonAndelDto> mapVurderAtOgFLiSammeOranisasjonAndelListe(List<VurderATogFLiSammeOrganisasjonAndelDto> vurderATogFLiSammeOrganisasjonAndelListe) {
        return vurderATogFLiSammeOrganisasjonAndelListe.stream().map(OppdatererDtoMapper::mapVurderATOgFLiSammeOrgAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderATogFLiSammeOrganisasjonAndelDto mapVurderATOgFLiSammeOrgAndel(VurderATogFLiSammeOrganisasjonAndelDto vurderATogFLiSammeOrganisasjonAndelDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderATogFLiSammeOrganisasjonAndelDto(vurderATogFLiSammeOrganisasjonAndelDto.getAndelsnr(), vurderATogFLiSammeOrganisasjonAndelDto.getArbeidsinntekt());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektUtenInntektsmeldingDto mapFastsattUtenInntektsmeldingDto(FastsettMånedsinntektUtenInntektsmeldingDto fastsattUtenInntektsmelding) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektUtenInntektsmeldingDto(mapFastsattUtenInntektsmeldingAndelListe(fastsattUtenInntektsmelding.getAndelListe()));
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektUtenInntektsmeldingAndelDto> mapFastsattUtenInntektsmeldingAndelListe(List<FastsettMånedsinntektUtenInntektsmeldingAndelDto> andelListe) {
        return andelListe.stream().map(OppdatererDtoMapper::mapAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektUtenInntektsmeldingAndelDto mapAndel(FastsettMånedsinntektUtenInntektsmeldingAndelDto fastsettMånedsinntektUtenInntektsmeldingAndelDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektUtenInntektsmeldingAndelDto(
            fastsettMånedsinntektUtenInntektsmeldingAndelDto.getAndelsnr(),
                fastsettMånedsinntektUtenInntektsmeldingAndelDto.getFastsattBeløp(),
                fastsettMånedsinntektUtenInntektsmeldingAndelDto.getInntektskategori() == null ?  null :
                    new no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori(fastsettMånedsinntektUtenInntektsmeldingAndelDto.getInntektskategori().getKode()));
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderLønnsendringDto mapVurderLønnsendringDto(VurderLønnsendringDto vurdertLonnsendring) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderLønnsendringDto(vurdertLonnsendring.erLønnsendringIBeregningsperioden());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektFLDto mapFastsettMånedsinntektFL(FastsettMånedsinntektFLDto fastsettMaanedsinntektFL) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektFLDto(fastsettMaanedsinntektFL.getMaanedsinntekt());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto mapVurderNyIArbeidslivet(VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto vurderNyIArbeidslivet) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto(vurderNyIArbeidslivet.erNyIArbeidslivet());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderTidsbegrensetArbeidsforholdDto mapTidsbegrensetArbeidsforhold(VurderTidsbegrensetArbeidsforholdDto vurderTidsbegrensetArbeidsforhold) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderTidsbegrensetArbeidsforholdDto(
            mapVurderteArbeidsforhold(vurderTidsbegrensetArbeidsforhold.getFastsatteArbeidsforhold())
        );
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderteArbeidsforholdDto> mapVurderteArbeidsforhold(List<VurderteArbeidsforholdDto> fastsatteArbeidsforhold) {
        return fastsatteArbeidsforhold.stream().map(OppdatererDtoMapper::mapVurdertArbeidsforhold).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderteArbeidsforholdDto mapVurdertArbeidsforhold(VurderteArbeidsforholdDto vurderteArbeidsforholdDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderteArbeidsforholdDto(
            vurderteArbeidsforholdDto.getAndelsnr(),
            vurderteArbeidsforholdDto.isTidsbegrensetArbeidsforhold()
        );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderNyoppstartetFLDto mapVurderNyoppstartetFLDto(VurderNyoppstartetFLDto vurderNyoppstartetFL) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderNyoppstartetFLDto(vurderNyoppstartetFL.erErNyoppstartetFL());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.BesteberegningFødendeKvinneDto mapBesteberegningFødendeKvinneDto(BesteberegningFødendeKvinneDto besteberegningAndeler) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.BesteberegningFødendeKvinneDto(
            mapBesteberegningAndeler(besteberegningAndeler.getBesteberegningAndelListe()),
            besteberegningAndeler.getNyDagpengeAndel() == null ? null : mapNyDagpengeandel(besteberegningAndeler.getNyDagpengeAndel())
            );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.DagpengeAndelLagtTilBesteberegningDto mapNyDagpengeandel(DagpengeAndelLagtTilBesteberegningDto nyDagpengeAndel) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.DagpengeAndelLagtTilBesteberegningDto(nyDagpengeAndel.getFastsatteVerdier().getFastsattBeløp(),
            new no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori(nyDagpengeAndel.getFastsatteVerdier().getInntektskategori().getKode()));
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.BesteberegningFødendeKvinneAndelDto> mapBesteberegningAndeler(List<BesteberegningFødendeKvinneAndelDto> besteberegningAndelListe) {
        return besteberegningAndelListe.stream().map(OppdatererDtoMapper::mapBesteberegningAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.BesteberegningFødendeKvinneAndelDto mapBesteberegningAndel(BesteberegningFødendeKvinneAndelDto besteberegningFødendeKvinneAndelDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.BesteberegningFødendeKvinneAndelDto(
            besteberegningFødendeKvinneAndelDto.getAndelsnr(),
            besteberegningFødendeKvinneAndelDto.getFastsatteVerdier().getFastsattBeløp(),
            new no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori(besteberegningFødendeKvinneAndelDto.getFastsatteVerdier().getInntektskategori().getKode()),
            besteberegningFødendeKvinneAndelDto.getLagtTilAvSaksbehandler());
    }

    private static FaktaOmBeregningTilfelleDto mapFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> tilfeller) {
        return new FaktaOmBeregningTilfelleDto(tilfeller.stream().map(FaktaOmBeregningTilfelle::getKode).map(no.nav.folketrygdloven.kalkulus.kodeverk.FaktaOmBeregningTilfelle::new).collect(Collectors.toList()));
    }

    public static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.InntektPrAndelDto> mapTilInntektPrAndelListe(List<InntektPrAndelDto> inntektPrAndelList) {
        if(inntektPrAndelList == null){
            return Collections.emptyList();
        }
        return inntektPrAndelList.stream().map(OppdatererDtoMapper::mapInntektPrAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.InntektPrAndelDto mapInntektPrAndel(InntektPrAndelDto inntektPrAndelDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.InntektPrAndelDto(inntektPrAndelDto.getInntekt(), inntektPrAndelDto.getAndelsnr());
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsattePerioderTidsbegrensetDto> mapTilFastsattTidsbegrensetPerioder(List<FastsattePerioderTidsbegrensetDto> fastsatteTidsbegrensedePerioder) {
        return fastsatteTidsbegrensedePerioder.stream().map(OppdatererDtoMapper::mapTilFastsattTidsbegrensetPeriode).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsattePerioderTidsbegrensetDto mapTilFastsattTidsbegrensetPeriode(FastsattePerioderTidsbegrensetDto fastsattePerioderTidsbegrensetDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsattePerioderTidsbegrensetDto(fastsattePerioderTidsbegrensetDto.getPeriodeFom(), fastsattePerioderTidsbegrensetDto.getPeriodeTom(), mapTilFastsattTidsbegrensetAndeler(fastsattePerioderTidsbegrensetDto.getFastsatteTidsbegrensedeAndeler()));
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsatteAndelerTidsbegrensetDto> mapTilFastsattTidsbegrensetAndeler(List<FastsatteAndelerTidsbegrensetDto> fastsatteTidsbegrensedeAndeler) {
        return fastsatteTidsbegrensedeAndeler.stream().map(OppdatererDtoMapper::mapTilFastsattTidsbegrensetAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsatteAndelerTidsbegrensetDto mapTilFastsattTidsbegrensetAndel(FastsatteAndelerTidsbegrensetDto fastsatteAndelerTidsbegrensetDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsatteAndelerTidsbegrensetDto(fastsatteAndelerTidsbegrensetDto.getAndelsnr(), fastsatteAndelerTidsbegrensetDto.getBruttoFastsattInntekt());
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.BeregningsaktivitetLagreDto> mapTilBeregningsaktivitetLagreDtoList(List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList) {
        return beregningsaktivitetLagreDtoList.stream().map(OppdatererDtoMapper::mapTilBeregningsaktivitetLagreDto).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.BeregningsaktivitetLagreDto mapTilBeregningsaktivitetLagreDto(BeregningsaktivitetLagreDto beregningsaktivitetLagreDto) {
        return no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.BeregningsaktivitetLagreDto.builder()
            .medArbeidsforholdRef(beregningsaktivitetLagreDto.getArbeidsforholdRef())
            .medArbeidsgiverIdentifikator(beregningsaktivitetLagreDto.getArbeidsgiverIdentifikator())
            .medFom(beregningsaktivitetLagreDto.getFom())
            .medOppdragsgiverOrg(beregningsaktivitetLagreDto.getOppdragsgiverOrg())
            .medOpptjeningAktivitetType(new no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType(beregningsaktivitetLagreDto.getOpptjeningAktivitetType().getKode()))
            .medSkalBrukes(beregningsaktivitetLagreDto.getSkalBrukes())
            .medTom(beregningsaktivitetLagreDto.getTom())
            .build();
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagPeriodeDto> mapTilEndredePerioderList(List<FordelBeregningsgrunnlagPeriodeDto> endretBeregningsgrunnlagPerioder) {
        return endretBeregningsgrunnlagPerioder.stream().map(OppdatererDtoMapper::mapTilFordelBeregningsgrunnlagPeriodeDto).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagPeriodeDto mapTilFordelBeregningsgrunnlagPeriodeDto(FordelBeregningsgrunnlagPeriodeDto fordelBeregningsgrunnlagPeriodeDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagPeriodeDto(mapFordelBeregningsgrunnlagPeriodeAndeler(fordelBeregningsgrunnlagPeriodeDto.getAndeler()), fordelBeregningsgrunnlagPeriodeDto.getFom(), fordelBeregningsgrunnlagPeriodeDto.getTom());
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagAndelDto> mapFordelBeregningsgrunnlagPeriodeAndeler(List<FordelBeregningsgrunnlagAndelDto> andeler) {
        return andeler.stream().map(OppdatererDtoMapper::mapFordelBeregningsgrunnlagPeriodeAndelDto).collect(Collectors.toList());
    }

    public static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettBeregningsgrunnlagAndelDto> mapFastsettBeregningsgrunnlagPeriodeAndeler(List<FastsettBeregningsgrunnlagAndelDto> andeler) {
        return andeler.stream().map(OppdatererDtoMapper::mapFastsettBeregningsgrunnlagPeriodeAndelDto).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagAndelDto mapFordelBeregningsgrunnlagPeriodeAndelDto(FordelBeregningsgrunnlagAndelDto fordelBeregningsgrunnlagAndelDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagAndelDto(
            mapTilFordelRedigerbarAndelDto(fordelBeregningsgrunnlagAndelDto),
            mapTilFordelFastsatteVerdier(fordelBeregningsgrunnlagAndelDto.getFastsatteVerdier()),
            fordelBeregningsgrunnlagAndelDto.getForrigeInntektskategori() == null ? null : new Inntektskategori(fordelBeregningsgrunnlagAndelDto.getForrigeInntektskategori().getKode()),
            fordelBeregningsgrunnlagAndelDto.getForrigeRefusjonPrÅr(),
            fordelBeregningsgrunnlagAndelDto.getForrigeArbeidsinntektPrÅr());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettBeregningsgrunnlagAndelDto mapFastsettBeregningsgrunnlagPeriodeAndelDto(FastsettBeregningsgrunnlagAndelDto fastsettBeregningsgrunnlagAndelDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettBeregningsgrunnlagAndelDto(
            mapTilRedigerbarAndelDto(fastsettBeregningsgrunnlagAndelDto),
            mapTilFastsatteVerdier(fastsettBeregningsgrunnlagAndelDto.getFastsatteVerdier()),
            fastsettBeregningsgrunnlagAndelDto.getForrigeInntektskategori() == null ? null : new Inntektskategori(fastsettBeregningsgrunnlagAndelDto.getForrigeInntektskategori().getKode()),
            fastsettBeregningsgrunnlagAndelDto.getForrigeRefusjonPrÅr(),
            fastsettBeregningsgrunnlagAndelDto.getForrigeArbeidsinntektPrÅr());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelFastsatteVerdierDto mapTilFordelFastsatteVerdier(FordelFastsatteVerdierDto fastsatteVerdier) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelFastsatteVerdierDto(
            fastsatteVerdier.getRefusjonPrÅr(),
            fastsatteVerdier.getFastsattÅrsbeløp(),
            new Inntektskategori(fastsatteVerdier.getInntektskategori().getKode()),
            fastsatteVerdier.getFastsattÅrsbeløpInklNaturalytelse() == null ? fastsatteVerdier.getFastsattÅrsbeløp() : fastsatteVerdier.getFastsattÅrsbeløpInklNaturalytelse()
        );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsatteVerdierDto mapTilFastsatteVerdier(FastsatteVerdierDto fastsatteVerdier) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsatteVerdierDto(
            fastsatteVerdier.getFastsattBeløp(),
            new Inntektskategori(fastsatteVerdier.getInntektskategori().getKode()),
            false);
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelRedigerbarAndelDto mapTilFordelRedigerbarAndelDto(FordelRedigerbarAndelDto redigerbarAndel) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelRedigerbarAndelDto(
            redigerbarAndel.getAndelsnr(),
            redigerbarAndel.getArbeidsgiverId(),
            redigerbarAndel.getArbeidsforholdId().getReferanse(),
            redigerbarAndel.getNyAndel(),
            redigerbarAndel.getAktivitetStatus() == null ? null : new AktivitetStatus(redigerbarAndel.getAktivitetStatus().getKode()),
            redigerbarAndel.getArbeidsforholdType() == null ? null : new OpptjeningAktivitetType(redigerbarAndel.getArbeidsforholdType().getKode()),
            redigerbarAndel.getLagtTilAvSaksbehandler(),
            redigerbarAndel.getBeregningsperiodeFom(),
            redigerbarAndel.getBeregningsperiodeTom(),
            null /* AndelKilde */);
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.RedigerbarAndelDto mapTilRedigerbarAndelDto(RedigerbarAndelDto redigerbarAndel) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.RedigerbarAndelDto(
            redigerbarAndel.getAndelsnr(),
            redigerbarAndel.getLagtTilAvSaksbehandler());
    }
}
