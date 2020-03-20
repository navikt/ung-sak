package no.nav.folketrygdloven.beregningsgrunnlag.output;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.OppdateringRespons;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class MapEndringsresultat {

    private MapEndringsresultat() {
        // Skjul
    }

    public static OppdaterBeregningsgrunnlagResultat mapFraOppdateringRespons(OppdateringRespons oppdateringRespons) {
        return oppdateringRespons == null ? null :
            new OppdaterBeregningsgrunnlagResultat(mapTilBeregningsgrunnlagEndring(oppdateringRespons.getBeregningsgrunnlagEndring()),
                mapFaktaOmBeregningVurderinger(oppdateringRespons.getFaktaOmBeregningVurderinger())
            );
    }

    private static FaktaOmBeregningVurderinger mapFaktaOmBeregningVurderinger(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.FaktaOmBeregningVurderinger faktaOmBeregningVurderinger) {
        if (faktaOmBeregningVurderinger == null) {
            return null;
        }
        FaktaOmBeregningVurderinger vurderinger = new FaktaOmBeregningVurderinger();
        vurderinger.setHarEtterlønnSluttpakkeEndring(mapTilToggle(faktaOmBeregningVurderinger.getHarEtterlønnSluttpakkeEndring()));
        vurderinger.setHarLønnsendringIBeregningsperiodenEndring(mapTilToggle(faktaOmBeregningVurderinger.getHarLønnsendringIBeregningsperiodenEndring()));
        vurderinger.setHarMilitærSiviltjenesteEndring(mapTilToggle(faktaOmBeregningVurderinger.getHarMilitærSiviltjenesteEndring()));
        vurderinger.setErSelvstendingNyIArbeidslivetEndring(mapTilToggle(faktaOmBeregningVurderinger.getErSelvstendingNyIArbeidslivetEndring()));
        vurderinger.setErNyoppstartetFLEndring(mapTilToggle(faktaOmBeregningVurderinger.getErNyoppstartetFLEndring()));
        vurderinger.setErMottattYtelseEndringer(mapTilErMottattYtelseEndringer(faktaOmBeregningVurderinger.getErMottattYtelseEndringer()));
        vurderinger.setErTidsbegrensetArbeidsforholdEndringer(mapTilErTidsbegrensetArbeidsforholdEndringer(faktaOmBeregningVurderinger.getErTidsbegrensetArbeidsforholdEndringer()));
        vurderinger.setVurderRefusjonskravGyldighetEndringer(mapTilRefusjonskravGyldighetEndringer(faktaOmBeregningVurderinger.getVurderRefusjonskravGyldighetEndringer()));
        return vurderinger;
    }

    private static List<ErTidsbegrensetArbeidsforholdEndring> mapTilErTidsbegrensetArbeidsforholdEndringer(List<no.nav.folketrygdloven.kalkulus.response.v1.håndtering.ErTidsbegrensetArbeidsforholdEndring> erTidsbegrensetArbeidsforholdEndringer) {
        return erTidsbegrensetArbeidsforholdEndringer == null ? null :
            erTidsbegrensetArbeidsforholdEndringer.stream()
            .map(MapEndringsresultat::mapErTidsbegrensetArbeidsforholdEndring)
            .collect(Collectors.toList());
    }

    private static ErTidsbegrensetArbeidsforholdEndring mapErTidsbegrensetArbeidsforholdEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.ErTidsbegrensetArbeidsforholdEndring erTidsbegrensetArbeidsforholdEndring) {
        return erTidsbegrensetArbeidsforholdEndring == null ? null :
            new ErTidsbegrensetArbeidsforholdEndring(
                mapArbeidsgiver(erTidsbegrensetArbeidsforholdEndring.getArbeidsgiver()),
                mapArbeidsforholdRef(erTidsbegrensetArbeidsforholdEndring.getArbeidsforholdRef()),
                mapTilToggle(erTidsbegrensetArbeidsforholdEndring.getErTidsbegrensetArbeidsforholdEndring())
            );
    }

    private static List<ErMottattYtelseEndring> mapTilErMottattYtelseEndringer(List<no.nav.folketrygdloven.kalkulus.response.v1.håndtering.ErMottattYtelseEndring> erMottattYtelseEndringer) {
        return erMottattYtelseEndringer == null ? null : erMottattYtelseEndringer.stream()
            .map(MapEndringsresultat::mapErMottattYtelseEndring)
            .collect(Collectors.toList());
    }

    private static ErMottattYtelseEndring mapErMottattYtelseEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.ErMottattYtelseEndring erMottattYtelseEndring) {
        if (erMottattYtelseEndring == null) {
            return null;
        }
        return new ErMottattYtelseEndring(
            AktivitetStatus.fraKode(erMottattYtelseEndring.getAktivitetStatus().getKode()),
            mapArbeidsgiver(erMottattYtelseEndring.getArbeidsgiver()),
            mapArbeidsforholdRef(erMottattYtelseEndring.getArbeidsforholdRef()),
            mapTilToggle(erMottattYtelseEndring.getErMottattYtelseEndring())
        );
    }

    private static List<RefusjonskravGyldighetEndring> mapTilRefusjonskravGyldighetEndringer(List<no.nav.folketrygdloven.kalkulus.response.v1.håndtering.RefusjonskravGyldighetEndring> vurderRefusjonskravGyldighetEndringer) {
        return vurderRefusjonskravGyldighetEndringer == null ? null : vurderRefusjonskravGyldighetEndringer.stream().map(MapEndringsresultat::mapRefusjonskravGyldighetEndring)
            .collect(Collectors.toList());
    }

    private static RefusjonskravGyldighetEndring mapRefusjonskravGyldighetEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.RefusjonskravGyldighetEndring refusjonskravGyldighetEndring) {
        return refusjonskravGyldighetEndring == null ? null :
            new RefusjonskravGyldighetEndring(new ToggleEndring(refusjonskravGyldighetEndring.getErGyldighetUtvidet().getFraVerdi(), refusjonskravGyldighetEndring.getErGyldighetUtvidet().getTilVerdi()),
                refusjonskravGyldighetEndring.getArbeidsgiver().getErOrganisasjon() ? Arbeidsgiver.virksomhet(refusjonskravGyldighetEndring.getArbeidsgiver().getIdent()) :
                    Arbeidsgiver.person(new AktørId(refusjonskravGyldighetEndring.getArbeidsgiver().getIdent())));
    }

    private static ToggleEndring mapTilToggle(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.ToggleEndring toggleEndring) {
        return toggleEndring == null ? null : new ToggleEndring(toggleEndring.getFraVerdi(), toggleEndring.getTilVerdi());
    }


    private static BeregningsgrunnlagEndring mapTilBeregningsgrunnlagEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.BeregningsgrunnlagEndring beregningsgrunnlagEndring) {
        return beregningsgrunnlagEndring == null ? null :
            new BeregningsgrunnlagEndring(mapTilPeriodeEndringer(beregningsgrunnlagEndring.getBeregningsgrunnlagPeriodeEndringer()));
    }

    private static List<BeregningsgrunnlagPeriodeEndring> mapTilPeriodeEndringer(List<no.nav.folketrygdloven.kalkulus.response.v1.håndtering.BeregningsgrunnlagPeriodeEndring> beregningsgrunnlagPeriodeEndringer) {
        return beregningsgrunnlagPeriodeEndringer == null ? null :
            beregningsgrunnlagPeriodeEndringer.stream().map(MapEndringsresultat::mapTilPeriodeEndring).collect(Collectors.toList());
    }

    private static BeregningsgrunnlagPeriodeEndring mapTilPeriodeEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.BeregningsgrunnlagPeriodeEndring beregningsgrunnlagPeriodeEndring) {
        return new BeregningsgrunnlagPeriodeEndring(
            mapAndelEndringer(beregningsgrunnlagPeriodeEndring.getBeregningsgrunnlagPrStatusOgAndelEndringer()),
            new Periode(beregningsgrunnlagPeriodeEndring.getPeriode().getFom(), beregningsgrunnlagPeriodeEndring.getPeriode().getTom())
        );
    }

    private static List<BeregningsgrunnlagPrStatusOgAndelEndring> mapAndelEndringer(List<no.nav.folketrygdloven.kalkulus.response.v1.håndtering.BeregningsgrunnlagPrStatusOgAndelEndring> beregningsgrunnlagPrStatusOgAndelEndringer) {
        return beregningsgrunnlagPrStatusOgAndelEndringer.stream().map(MapEndringsresultat::mapAndelEndring).collect(Collectors.toList());
    }

    private static BeregningsgrunnlagPrStatusOgAndelEndring mapAndelEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.BeregningsgrunnlagPrStatusOgAndelEndring beregningsgrunnlagPrStatusOgAndelEndring) {
        return new BeregningsgrunnlagPrStatusOgAndelEndring(
            mapInntektEndring(beregningsgrunnlagPrStatusOgAndelEndring.getInntektEndring()),
            mapInntektskategoriEndring(beregningsgrunnlagPrStatusOgAndelEndring.getInntektskategoriEndring()),
            AktivitetStatus.fraKode(beregningsgrunnlagPrStatusOgAndelEndring.getAktivitetStatus().getKode()),
            beregningsgrunnlagPrStatusOgAndelEndring.getArbeidsforholdType() == null ? null : OpptjeningAktivitetType.fraKode(beregningsgrunnlagPrStatusOgAndelEndring.getArbeidsforholdType().getKode()),
            mapArbeidsgiver(beregningsgrunnlagPrStatusOgAndelEndring.getArbeidsgiver()),
            mapArbeidsforholdRef(beregningsgrunnlagPrStatusOgAndelEndring.getArbeidsforholdRef())
        );
    }

    private static InternArbeidsforholdRef mapArbeidsforholdRef(String arbeidsforholdRef) {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : InternArbeidsforholdRef.ref(arbeidsforholdRef);
    }

    private static Arbeidsgiver mapArbeidsgiver(Aktør arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        return arbeidsgiver.getErOrganisasjon() ? Arbeidsgiver.virksomhet(arbeidsgiver.getIdent()) : Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdent()));
    }

    private static InntektskategoriEndring mapInntektskategoriEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.InntektskategoriEndring inntektskategoriEndring) {
        return inntektskategoriEndring == null ? null : new InntektskategoriEndring(
            inntektskategoriEndring.getFraVerdi() == null ? null : Inntektskategori.fraKode(inntektskategoriEndring.getFraVerdi().getKode()),
            Inntektskategori.fraKode(inntektskategoriEndring.getTilVerdi().getKode())
        );
    }

    private static InntektEndring mapInntektEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.InntektEndring inntektEndring) {
        return inntektEndring == null ? null : new InntektEndring(inntektEndring.getFraInntekt(), inntektEndring.getTilInntekt());
    }

}
