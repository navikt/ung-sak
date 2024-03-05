package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.Beløp;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.BeregningAktivitetEndring;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.OppdateringRespons;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.RefusjonoverstyringEndring;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.VarigEndretEllerNyoppstartetNæringEndring;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class MapEndringsresultat {

    private MapEndringsresultat() {
        // Skjul
    }

    public static OppdaterBeregningsgrunnlagResultat mapFraOppdateringRespons(OppdateringRespons oppdateringRespons, UUID bgReferanse) {
        return oppdateringRespons == null ? null :
            new OppdaterBeregningsgrunnlagResultat(mapTilBeregningsgrunnlagEndring(oppdateringRespons.getBeregningsgrunnlagEndring()),
                mapFaktaOmBeregningVurderinger(oppdateringRespons.getFaktaOmBeregningVurderinger()),
                mapVarigEndretNæringVurdering(oppdateringRespons.getVarigEndretNæringEndring()),
                mapEndringRefusjon(oppdateringRespons.getRefusjonoverstyringEndring()),
                bgReferanse,
                mapBeregningAktivitetEndringer(oppdateringRespons.getBeregningAktiviteterEndring()));
    }

    private static List<no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningAktivitetEndring> mapBeregningAktivitetEndringer(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.BeregningAktiviteterEndring beregningAktiviteterEndring) {
        return beregningAktiviteterEndring == null ? Collections.emptyList() :
            beregningAktiviteterEndring.getAktivitetEndringer().stream().map(MapEndringsresultat::mapTilAktivitetEndring).toList();
    }

    private static no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningAktivitetEndring mapTilAktivitetEndring(BeregningAktivitetEndring beregningAktivitetEndring) {
        return new no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningAktivitetEndring(
            mapNøkkel(beregningAktivitetEndring.getAktivitetNøkkel()),
            mapTilToggle(beregningAktivitetEndring.getSkalBrukesEndring()),
            mapDatoEndring(beregningAktivitetEndring.getTomDatoEndring())
        );
    }

    private static BeregningAktivitetNøkkel mapNøkkel(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.BeregningAktivitetNøkkel aktivitetNøkkel) {
        return new BeregningAktivitetNøkkel(
            OpptjeningAktivitetType.fraKode(aktivitetNøkkel.getOpptjeningAktivitetType().getKode()),
            aktivitetNøkkel.getFom(),
            mapArbeidsgiver(aktivitetNøkkel.getArbeidsgiver()),
            aktivitetNøkkel.getArbeidsforholdRef() != null ? mapArbeidsforholdRef(aktivitetNøkkel.getArbeidsforholdRef().getAbakusReferanse()) : InternArbeidsforholdRef.nullRef());
    }

    private static no.nav.folketrygdloven.beregningsgrunnlag.resultat.RefusjonoverstyringEndring mapEndringRefusjon(RefusjonoverstyringEndring refusjonoverstyringEndring) {
        if (refusjonoverstyringEndring == null) {
            return null;
        }
        return new no.nav.folketrygdloven.beregningsgrunnlag.resultat.RefusjonoverstyringEndring(
            mapRefusjonOverstyringPerioder(refusjonoverstyringEndring.getRefusjonperiodeEndringer())
        );
    }

    private static List<RefusjonoverstyringPeriodeEndring> mapRefusjonOverstyringPerioder(List<no.nav.folketrygdloven.kalkulus.response.v1.håndtering.RefusjonoverstyringPeriodeEndring> refusjonperiodeEndringer) {
        if (refusjonperiodeEndringer == null) {
            return null;
        }
        return refusjonperiodeEndringer.stream().map(MapEndringsresultat::mapRefusjonPeriodeEndring).collect(Collectors.toList());
    }

    private static RefusjonoverstyringPeriodeEndring mapRefusjonPeriodeEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.RefusjonoverstyringPeriodeEndring r) {
        return new RefusjonoverstyringPeriodeEndring(
            mapArbeidsgiver(r.getArbeidsgiver()),
            mapArbeidsforholdRef(r.getArbeidsforholdRef().toString()),
            mapDatoEndring(r.getFastsattRefusjonFomEndring()),
            r.getFastsattDelvisRefusjonFørDatoEndring() == null ? null :
                new BeløpEndring(Optional.ofNullable(r.getFastsattDelvisRefusjonFørDatoEndring().getFraRefusjon()).map(Beløp::verdi).orElse(null),
                    Optional.ofNullable(r.getFastsattDelvisRefusjonFørDatoEndring().getTilRefusjon()).map(Beløp::verdi).orElse(null))
        );
    }

    private static DatoEndring mapDatoEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.DatoEndring datoEndring) {
        return datoEndring == null ? null : new DatoEndring(datoEndring.getFraVerdi(), datoEndring.getTilVerdi());
    }

    private static VarigEndretNæringVurdering mapVarigEndretNæringVurdering(VarigEndretEllerNyoppstartetNæringEndring varigEndretEllerNyoppstartetNæringEndring) {
        if (harVerdier(varigEndretEllerNyoppstartetNæringEndring)) {
            return new VarigEndretNæringVurdering(
                mapTilToggle(varigEndretEllerNyoppstartetNæringEndring.getErVarigEndretNaeringEndring()),
                mapTilToggle(varigEndretEllerNyoppstartetNæringEndring.getErNyoppstartetNaeringEndring()));
        }
        return null;
    }

    private static boolean harVerdier(VarigEndretEllerNyoppstartetNæringEndring varigEndretEllerNyoppstartetNæringEndring) {
        return varigEndretEllerNyoppstartetNæringEndring != null && (varigEndretEllerNyoppstartetNæringEndring.getErVarigEndretNaeringEndring() != null || varigEndretEllerNyoppstartetNæringEndring.getErNyoppstartetNaeringEndring() != null);
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
                mapArbeidsforholdRef(erTidsbegrensetArbeidsforholdEndring.getArbeidsforholdRef().toString()),
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
            mapArbeidsforholdRef(erMottattYtelseEndring.getArbeidsforholdRef().toString()),
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
            mapNyeInntektsforholdEndringer(beregningsgrunnlagPeriodeEndring.getNyttInntektsforholdEndringer()), new Periode(beregningsgrunnlagPeriodeEndring.getPeriode().getFom(), beregningsgrunnlagPeriodeEndring.getPeriode().getTom())
        );
    }

    private static List<NyttInntektsforholdEndring> mapNyeInntektsforholdEndringer(List<no.nav.folketrygdloven.kalkulus.response.v1.håndtering.NyttInntektsforholdEndring> nyttInntektsforholdEndringer) {
        return nyttInntektsforholdEndringer.stream().map(MapEndringsresultat::mapInnteksforholdEndring).toList();
    }

    private static NyttInntektsforholdEndring mapInnteksforholdEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.NyttInntektsforholdEndring endring) {
        return new NyttInntektsforholdEndring(
            AktivitetStatus.fraKode(endring.getAktivitetStatus().getKode()),
            mapArbeidsgiver(endring.getArbeidsgiver()),
            mapInntektEndring(endring.getBruttoInntektPrÅrEndring()),
            mapTilToggle(endring.getSkalRedusereUtbetalingEndring())
        );
    }

    private static List<BeregningsgrunnlagPrStatusOgAndelEndring> mapAndelEndringer(List<no.nav.folketrygdloven.kalkulus.response.v1.håndtering.BeregningsgrunnlagPrStatusOgAndelEndring> beregningsgrunnlagPrStatusOgAndelEndringer) {
        return beregningsgrunnlagPrStatusOgAndelEndringer.stream().map(MapEndringsresultat::mapAndelEndring).collect(Collectors.toList());
    }

    private static BeregningsgrunnlagPrStatusOgAndelEndring mapAndelEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.BeregningsgrunnlagPrStatusOgAndelEndring beregningsgrunnlagPrStatusOgAndelEndring) {
        return new BeregningsgrunnlagPrStatusOgAndelEndring(
            mapInntektEndring(beregningsgrunnlagPrStatusOgAndelEndring.getInntektEndring()),
            mapInntektskategoriEndring(beregningsgrunnlagPrStatusOgAndelEndring.getInntektskategoriEndring()),
            mapRefusjonEndring(beregningsgrunnlagPrStatusOgAndelEndring.getRefusjonEndring()),
            AktivitetStatus.fraKode(beregningsgrunnlagPrStatusOgAndelEndring.getAktivitetStatus().getKode()),
            beregningsgrunnlagPrStatusOgAndelEndring.getArbeidsforholdType() == null ? null : OpptjeningAktivitetType.fraKode(beregningsgrunnlagPrStatusOgAndelEndring.getArbeidsforholdType().getKode()),
            mapArbeidsgiver(beregningsgrunnlagPrStatusOgAndelEndring.getArbeidsgiver()),
            mapArbeidsforholdRef(beregningsgrunnlagPrStatusOgAndelEndring.getArbeidsforholdRef().toString())
        );
    }

    private static RefusjonEndring mapRefusjonEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.RefusjonEndring refusjonEndring) {
        return refusjonEndring == null ? null : new RefusjonEndring(Optional.ofNullable(refusjonEndring.getFraRefusjon()).map(Beløp::verdi).orElse(null),
            Optional.ofNullable(refusjonEndring.getTilRefusjon()).map(Beløp::verdi).orElse(null));
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

    private static BeløpEndring mapInntektEndring(no.nav.folketrygdloven.kalkulus.response.v1.håndtering.InntektEndring inntektEndring) {
        return inntektEndring == null ? null : new BeløpEndring(Optional.ofNullable(inntektEndring.getFraInntekt()).map(Beløp::verdi).orElse(null),
            Optional.ofNullable(inntektEndring.getTilInntekt()).map(Beløp::verdi).orElse(null));
    }


}
