package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.AvklarAktiviteterHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FaktaOmBeregningHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FaktaOmFordelingHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBGTidsbegrensetArbeidsforholdHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBeregningsgrunnlagATFLHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBeregningsgrunnlagSNNyIArbeidslivetHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBruttoBeregningsgrunnlagSNHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.VurderVarigEndringEllerNyoppstartetSNHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.overstyring.OverstyrBeregningsgrunnlagHåndteringDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.AvklarteAktiviteterDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagATFLDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FordelBeregningsgrunnlagDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.OverstyrBeregningsaktiviteterDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.OverstyrBeregningsgrunnlagDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderFaktaOmBeregningDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderVarigEndringEllerNyoppstartetSNDto;

class MapDtoTilRequest {

    /**
     * Mapper aksjonspunktdto til håndteringdto i kalkulus.
     *
     * @param dto BekreftAksjonspunktDto
     * @return Dto for håndtering av aksjonspunk i Kalkulus
     */
    public static HåndterBeregningDto map(BekreftetAksjonspunktDto dto) {
        if (dto instanceof AvklarteAktiviteterDto) {
            AvklarteAktiviteterDto avklarteAktiviteterDto = (AvklarteAktiviteterDto) dto;
            return new AvklarAktiviteterHåndteringDto(OppdatererDtoMapper.mapAvklarteAktiviteterDto(avklarteAktiviteterDto));
        }
        if (dto instanceof VurderFaktaOmBeregningDto) {
            VurderFaktaOmBeregningDto faktaOmBeregningDto = (VurderFaktaOmBeregningDto) dto;
            return new FaktaOmBeregningHåndteringDto(OppdatererDtoMapper.mapTilFaktaOmBeregningLagreDto(faktaOmBeregningDto.getFakta()));
        }
        if (dto instanceof FastsettBeregningsgrunnlagATFLDto) {
            FastsettBeregningsgrunnlagATFLDto fastsettBeregningsgrunnlagATFLDto = (FastsettBeregningsgrunnlagATFLDto) dto;
            return new FastsettBeregningsgrunnlagATFLHåndteringDto(OppdatererDtoMapper.mapTilInntektPrAndelListe(fastsettBeregningsgrunnlagATFLDto.getInntektPrAndelList()), fastsettBeregningsgrunnlagATFLDto.getInntektFrilanser(), null);
        }
        if (dto instanceof FastsettBGTidsbegrensetArbeidsforholdDto) {
            FastsettBGTidsbegrensetArbeidsforholdDto fastsettBGTidsbegrensetArbeidsforholdDto = (FastsettBGTidsbegrensetArbeidsforholdDto) dto;
            return new FastsettBGTidsbegrensetArbeidsforholdHåndteringDto(OppdatererDtoMapper.mapFastsettBGTidsbegrensetArbeidsforholdDto(fastsettBGTidsbegrensetArbeidsforholdDto));
        }
        if (dto instanceof FastsettBruttoBeregningsgrunnlagSNDto) {
            FastsettBruttoBeregningsgrunnlagSNDto fastsettBruttoBeregningsgrunnlagSNDto = (FastsettBruttoBeregningsgrunnlagSNDto) dto;
            return new FastsettBruttoBeregningsgrunnlagSNHåndteringDto(OppdatererDtoMapper.mapFastsettBruttoBeregningsgrunnlagSNDto(fastsettBruttoBeregningsgrunnlagSNDto));
        }
        if (dto instanceof VurderVarigEndringEllerNyoppstartetSNDto) {
            VurderVarigEndringEllerNyoppstartetSNDto vurderVarigEndringEllerNyoppstartetSNDto = (VurderVarigEndringEllerNyoppstartetSNDto) dto;
            return new VurderVarigEndringEllerNyoppstartetSNHåndteringDto(OppdatererDtoMapper.mapdVurderVarigEndringEllerNyoppstartetSNDto(vurderVarigEndringEllerNyoppstartetSNDto));
        }
        if (dto instanceof FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto) {
            FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto fastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto = (FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto) dto;
            return new FastsettBeregningsgrunnlagSNNyIArbeidslivetHåndteringDto(OppdatererDtoMapper.mapFastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto(fastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto));
        }
        if (dto instanceof FordelBeregningsgrunnlagDto) {
            FordelBeregningsgrunnlagDto fordelBeregningsgrunnlagDto = (FordelBeregningsgrunnlagDto) dto;
            return new FaktaOmFordelingHåndteringDto(OppdatererDtoMapper.mapFordelBeregningsgrunnlagDto(fordelBeregningsgrunnlagDto));
        }
        throw new IllegalStateException("Aksjonspunkt er ikke mappet i kalkulus");
    }

    public static HåndterBeregningDto mapOverstyring(OverstyringAksjonspunktDto dto) {
        if (dto instanceof OverstyrBeregningsaktiviteterDto) {
            OverstyrBeregningsaktiviteterDto overstyrBeregningsaktiviteterDto = (OverstyrBeregningsaktiviteterDto) dto;
            return new no.nav.folketrygdloven.kalkulus.håndtering.v1.overstyring.OverstyrBeregningsaktiviteterDto(OppdatererDtoMapper.mapOverstyrBeregningsaktiviteterDto(overstyrBeregningsaktiviteterDto.getBeregningsaktivitetLagreDtoList()));
        }
        if (dto instanceof OverstyrBeregningsgrunnlagDto) {
            OverstyrBeregningsgrunnlagDto overstyrBeregningsgrunnlagDto = (OverstyrBeregningsgrunnlagDto) dto;
            return new OverstyrBeregningsgrunnlagHåndteringDto(OppdatererDtoMapper.mapTilFaktaOmBeregningLagreDto(overstyrBeregningsgrunnlagDto.getFakta()),
                OppdatererDtoMapper.mapFastsettBeregningsgrunnlagPeriodeAndeler(overstyrBeregningsgrunnlagDto.getOverstyrteAndeler()));
        }
        throw new IllegalStateException("Overstyringaksjonspunkt er ikke mappet i kalkulus");

    }
}
