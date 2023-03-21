package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import static no.nav.folketrygdloven.kalkulus.håndtering.v1.overstyring.OverstyrBeregningsaktiviteterDto.avbryt;

import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.AvklarAktiviteterHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FaktaOmBeregningHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FaktaOmFordelingHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.VurderTilkommetInntektHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBGTidsbegrensetArbeidsforholdHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBeregningsgrunnlagATFLHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBeregningsgrunnlagSNNyIArbeidslivetHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBruttoBeregningsgrunnlagSNHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.VurderVarigEndretArbeidssituasjonHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.VurderVarigEndringEllerNyoppstartetSNHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.overstyring.OverstyrBeregningsgrunnlagHåndteringDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.AvklarteAktiviteterDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.BekreftetBeregningsgrunnlagDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagATFLDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.OverstyrBeregningsaktiviteterDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.OverstyrBeregningsgrunnlagDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderFaktaOmBeregningDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderVarigEndretArbeidssituasjonDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling.VurderTilkomneInntektsforholdDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling.VurderTilkomneInntektsforholdDtoer;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.refusjon.VurderRefusjonBeregningsgrunnlagDto;

class MapDtoTilRequest {

    /**
     * Mapper aksjonspunktdto til håndteringdto i kalkulus.
     *
     * @param dto         BekreftAksjonspunktDto
     * @param begrunnelse begrunnelsen for aksjonspunktet. I k9sak lagres kun et aksjonspunkt for alle grunnlag og
     *                    det er begrunnelsen på dette aksjonspunktet som skal legges ved på alle aksjonspunktene som sendes til kalkulus
     * @return Dto for håndtering av aksjonspunk i Kalkulus
     */

    public static HåndterBeregningDto map(BekreftetBeregningsgrunnlagDto dto, String begrunnelse) {
        HåndterBeregningDto håndterBeregningDto = mapSpesifikkDto(dto);
        håndterBeregningDto.setBegrunnelse(dto.getBegrunnelse() != null ? dto.getBegrunnelse() : begrunnelse);
        return håndterBeregningDto;
    }

    public static HåndterBeregningDto mapSpesifikkDto(BekreftetBeregningsgrunnlagDto dto) {
        if (dto instanceof AvklarteAktiviteterDto avklarteAktiviteterDto) {
            return new AvklarAktiviteterHåndteringDto(OppdatererDtoMapper.mapAvklarteAktiviteterDto(avklarteAktiviteterDto));
        }
        if (dto instanceof VurderFaktaOmBeregningDto faktaOmBeregningDto) {
            return new FaktaOmBeregningHåndteringDto(OppdatererDtoMapper.mapTilFaktaOmBeregningLagreDto(faktaOmBeregningDto.getFakta()));
        }
        if (dto instanceof FastsettBeregningsgrunnlagATFLDto fastsettBeregningsgrunnlagATFLDto) {
            return new FastsettBeregningsgrunnlagATFLHåndteringDto(OppdatererDtoMapper.mapTilInntektPrAndelListe(fastsettBeregningsgrunnlagATFLDto.getInntektPrAndelList()), fastsettBeregningsgrunnlagATFLDto.getInntektFrilanser(), null);
        }
        if (dto instanceof FastsettBGTidsbegrensetArbeidsforholdDto fastsettBGTidsbegrensetArbeidsforholdDto) {
            return new FastsettBGTidsbegrensetArbeidsforholdHåndteringDto(OppdatererDtoMapper.mapFastsettBGTidsbegrensetArbeidsforholdDto(fastsettBGTidsbegrensetArbeidsforholdDto));
        }
        if (dto instanceof FastsettBruttoBeregningsgrunnlagSNDto fastsettBruttoBeregningsgrunnlagSNDto) {
            return new FastsettBruttoBeregningsgrunnlagSNHåndteringDto(OppdatererDtoMapper.mapFastsettBruttoBeregningsgrunnlagSNDto(fastsettBruttoBeregningsgrunnlagSNDto));
        }
        if (dto instanceof VurderVarigEndringEllerNyoppstartetSNDto vurderVarigEndringEllerNyoppstartetSNDto) {
            return new VurderVarigEndringEllerNyoppstartetSNHåndteringDto(OppdatererDtoMapper.mapVurderVarigEndringEllerNyoppstartetDto(vurderVarigEndringEllerNyoppstartetSNDto));
        }
        if (dto instanceof VurderVarigEndretArbeidssituasjonDto vurderVarigEndretArbeidssituasjonDto) {
            return new VurderVarigEndretArbeidssituasjonHåndteringDto(OppdatererDtoMapper.mapVurderVarigEndringEllerNyoppstartetDto(vurderVarigEndretArbeidssituasjonDto));
        }
        if (dto instanceof FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto fastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto) {
            return new FastsettBeregningsgrunnlagSNNyIArbeidslivetHåndteringDto(OppdatererDtoMapper.mapFastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto(fastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto));
        }
        if (dto instanceof VurderTilkomneInntektsforholdDto inntektsforholdDto) {
            return new VurderTilkommetInntektHåndteringDto(inntektsforholdDto.getTilkomneInntektsforholdPerioder());
        }
        if (dto instanceof FordelBeregningsgrunnlagDto fordelBeregningsgrunnlagDto) {
            return new FaktaOmFordelingHåndteringDto(OppdatererDtoMapper.mapFordelBeregningsgrunnlagDto(fordelBeregningsgrunnlagDto));
        }
        if (dto instanceof VurderRefusjonBeregningsgrunnlagDto) {
            return OppdatererDtoMapper.mapVurderRefusjonBeregningsgrunnlag((VurderRefusjonBeregningsgrunnlagDto) dto);
        }
        throw new IllegalStateException("Aksjonspunkt er ikke mappet i kalkulus");
    }

    public static HåndterBeregningDto mapOverstyring(OverstyringAksjonspunktDto dto) {
        if (dto instanceof OverstyrBeregningsaktiviteterDto overstyrBeregningsaktiviteterDto) {
            if (dto.skalAvbrytes()) {
                return avbryt();
            }
            var mappetDto = new no.nav.folketrygdloven.kalkulus.håndtering.v1.overstyring.OverstyrBeregningsaktiviteterDto(OppdatererDtoMapper.mapOverstyrBeregningsaktiviteterDto(overstyrBeregningsaktiviteterDto.getBeregningsaktivitetLagreDtoList()));
            mappetDto.setBegrunnelse(dto.getBegrunnelse());
            return mappetDto;
        }
        if (dto instanceof OverstyrBeregningsgrunnlagDto overstyrBeregningsgrunnlagDto) {
            if (dto.skalAvbrytes()) {
                return OverstyrBeregningsgrunnlagHåndteringDto.avbryt();
            }
            var mappetDto = new OverstyrBeregningsgrunnlagHåndteringDto(null,
                OppdatererDtoMapper.mapFastsettBeregningsgrunnlagPeriodeAndeler(overstyrBeregningsgrunnlagDto.getOverstyrteAndeler()));
            mappetDto.setBegrunnelse(dto.getBegrunnelse());
            return mappetDto;
        }
        throw new IllegalStateException("Overstyringaksjonspunkt er ikke mappet i kalkulus");

    }
}
