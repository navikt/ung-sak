package no.nav.ung.sak.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.kodeverk.arbeidsforhold.InntektYtelseType;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.kodeverk.arbeidsforhold.LønnsinntektBeskrivelse;
import no.nav.ung.kodeverk.arbeidsforhold.SkatteOgAvgiftsregelType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

public final class KodeverkMapper {

    private KodeverkMapper() {
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.YtelseType mapYtelseTypeTilDto(FagsakYtelseType ytelseType) {
        if (ytelseType == null || "-".equals(ytelseType.getKode())) {
            return null;
        }
        return no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.fraKode(ytelseType.getKode());
    }

    private static String getFpsakYtelseTypeFraAbakus(String kode) {
        // gjør mapping for å sjekke konsistens
        return FagsakYtelseType.fraKode(kode).getKode();
    }

    static InntektYtelseType mapUtbetaltYtelseTypeTilGrunnlag(no.nav.abakus.iaygrunnlag.kodeverk.InntektYtelseType type) {
        return type != null ? InntektYtelseType.valueOf(type.name()) : null;
    }

    static Fagsystem mapFagsystemFraDto(no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem dto) {
        return dto == null
            ? Fagsystem.UDEFINERT
            : Fagsystem.fraKode(dto.getKode());
    }

    static FagsakYtelseType mapYtelseTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType ytelseType) {
        if (ytelseType == null)
            return FagsakYtelseType.UDEFINERT;
        String relatertYtelseKode = KodeverkMapper.getFpsakYtelseTypeFraAbakus(ytelseType.getKode());
        return FagsakYtelseType.fraKode(relatertYtelseKode);
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem mapFagsystemTilDto(Fagsystem kode) {
        return kode == null || Fagsystem.UDEFINERT.equals(kode)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem.fraKode(kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType mapArbeidTypeTilDto(ArbeidType arbeidType) {
        return arbeidType == null || ArbeidType.UDEFINERT.equals(arbeidType)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.fraKode(arbeidType.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.InntektskildeType mapInntektsKildeTilDto(InntektsKilde kode) {
        return kode == null || InntektsKilde.UDEFINERT.equals(kode)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.InntektskildeType.fraKode(kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.InntektspostType mapInntektspostTypeTilDto(InntektspostType kode) {
        return kode == null || InntektspostType.UDEFINERT.equals(kode)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.InntektspostType.fraKode(kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.SkatteOgAvgiftsregelType mapSkatteOgAvgiftsregelTilDto(SkatteOgAvgiftsregelType kode) {
        return kode == null || SkatteOgAvgiftsregelType.UDEFINERT.equals(kode)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.SkatteOgAvgiftsregelType.fraKode(kode.getKode());
    }

    static ArbeidType mapArbeidType(no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType dto) {
        return dto == null
            ? ArbeidType.UDEFINERT
            : ArbeidType.fraKode(dto.getKode());
    }


    static InntektsKilde mapInntektsKildeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.InntektskildeType dto) {
        return dto == null
            ? InntektsKilde.UDEFINERT
            : InntektsKilde.fraKode(dto.getKode());
    }

    static SkatteOgAvgiftsregelType mapSkatteOgAvgiftsregelFraDto(no.nav.abakus.iaygrunnlag.kodeverk.SkatteOgAvgiftsregelType dto) {
        return dto == null
            ? SkatteOgAvgiftsregelType.UDEFINERT
            : SkatteOgAvgiftsregelType.fraKode(dto.getKode());
    }

    static LønnsinntektBeskrivelse mapLønnsinntektBeskrivelseFraDto(no.nav.abakus.iaygrunnlag.kodeverk.LønnsinntektBeskrivelse dto) {
        return dto == null
            ? LønnsinntektBeskrivelse.UDEFINERT
            : LønnsinntektBeskrivelse.fraKode(dto.getKode());
    }


    static InntektspostType mapInntektspostTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.InntektspostType dto) {
        return dto == null
            ? InntektspostType.UDEFINERT
            : InntektspostType.fraKode(dto.getKode());
    }

    public static no.nav.abakus.iaygrunnlag.kodeverk.YtelseType mapFagsakYtelseTypeTilDto(FagsakYtelseType ytelseType) {
        if (ytelseType == null)
            return null;
        return no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.fraKode(ytelseType.getKode());
    }

}
