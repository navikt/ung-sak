package no.nav.ung.sak.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.ung.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.ung.kodeverk.arbeidsforhold.BekreftetPermisjonStatus;
import no.nav.ung.kodeverk.arbeidsforhold.InntektPeriodeType;
import no.nav.ung.kodeverk.arbeidsforhold.InntektYtelseType;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.kodeverk.arbeidsforhold.LønnsinntektBeskrivelse;
import no.nav.ung.kodeverk.arbeidsforhold.NaturalYtelseType;
import no.nav.ung.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.ung.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.ung.kodeverk.arbeidsforhold.SkatteOgAvgiftsregelType;
import no.nav.ung.kodeverk.arbeidsforhold.UtsettelseÅrsak;
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

    static RelatertYtelseTilstand getFpsakRelatertYtelseTilstandForAbakusYtelseStatus(YtelseStatus dto) {
        if (dto == null)
            return null;

        String kode = dto.getKode();
        switch (kode) {
            case "OPPR":
                return RelatertYtelseTilstand.IKKE_STARTET;
            case "UBEH":
                return RelatertYtelseTilstand.ÅPEN;
            case "AVSLU":
                return RelatertYtelseTilstand.AVSLUTTET;
            case "LOP":
                return RelatertYtelseTilstand.LØPENDE;
            default:
                throw new IllegalArgumentException("Ukjent YtelseStatus: " + dto);
        }
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus getAbakusYtelseStatusForFpsakRelatertYtelseTilstand(RelatertYtelseTilstand tilstand) {
        var kode = tilstand.getKode();
        switch (kode) {
            case "IKKESTARTET":
                return no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus.OPPRETTET;
            case "ÅPEN":
                return no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus.UNDER_BEHANDLING;
            case "AVSLUTTET":
                return no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus.AVSLUTTET;
            case "LØPENDE":
                return no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus.LØPENDE;
            default:
                throw new IllegalArgumentException("Ukjent RelatertYtelseTilstand: " + kode);
        }
    }

    static InntektYtelseType mapUtbetaltYtelseTypeTilGrunnlag(no.nav.abakus.iaygrunnlag.kodeverk.InntektYtelseType type) {
        return type != null ? InntektYtelseType.valueOf(type.name()) : null;
    }

    static BekreftetPermisjonStatus getBekreftetPermisjonStatus(no.nav.abakus.iaygrunnlag.kodeverk.BekreftetPermisjonStatus kode) {
        return kode == null || "-".equals(kode.getKode())
            ? BekreftetPermisjonStatus.UDEFINERT
            : BekreftetPermisjonStatus.fraKode(kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.BekreftetPermisjonStatus mapBekreftetPermisjonStatus(BekreftetPermisjonStatus status) {
        return status == null || BekreftetPermisjonStatus.UDEFINERT.equals(status)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.BekreftetPermisjonStatus.fraKode(status.getKode());
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

    static no.nav.abakus.iaygrunnlag.kodeverk.InntektPeriodeType mapInntektPeriodeTypeTilDto(InntektPeriodeType hyppighet) {
        return hyppighet == null || InntektPeriodeType.UDEFINERT.equals(hyppighet)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.InntektPeriodeType.fraKode(hyppighet.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.Arbeidskategori mapArbeidskategoriTilDto(Arbeidskategori kode) {
        return kode == null || Arbeidskategori.UDEFINERT.equals(kode)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.Arbeidskategori.fraKode(kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType mapArbeidTypeTilDto(ArbeidType arbeidType) {
        return arbeidType == null || ArbeidType.UDEFINERT.equals(arbeidType)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.fraKode(arbeidType.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType mapPermisjonbeskrivelseTypeTilDto(PermisjonsbeskrivelseType kode) {
        return kode == null || PermisjonsbeskrivelseType.UDEFINERT.equals(kode)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.fraKode(kode.getKode());
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

    static no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType mapArbeidsforholdHandlingTypeTilDto(ArbeidsforholdHandlingType kode) {
        return kode == null || ArbeidsforholdHandlingType.UDEFINERT.equals(kode)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType.fraKode(kode.getKode());
    }

    static ArbeidType mapArbeidType(no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType dto) {
        return dto == null
            ? ArbeidType.UDEFINERT
            : ArbeidType.fraKode(dto.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.NaturalytelseType mapNaturalYtelseTilDto(NaturalYtelseType kode) {
        return kode == null || NaturalYtelseType.UDEFINERT.equals(kode)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.NaturalytelseType.fraKode(kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.UtsettelseÅrsakType mapUtsettelseÅrsakTilDto(UtsettelseÅrsak kode) {
        return kode == null || UtsettelseÅrsak.UDEFINERT.equals(kode)
            ? null
            : no.nav.abakus.iaygrunnlag.kodeverk.UtsettelseÅrsakType.fraKode(kode.getKode());
    }

    static InntektsKilde mapInntektsKildeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.InntektskildeType dto) {
        return dto == null
            ? InntektsKilde.UDEFINERT
            : InntektsKilde.fraKode(dto.getKode());
    }

    static ArbeidsforholdHandlingType mapArbeidsforholdHandlingTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType dto) {
        return dto == null
            ? ArbeidsforholdHandlingType.UDEFINERT
            : ArbeidsforholdHandlingType.fraKode(dto.getKode());
    }

    static NaturalYtelseType mapNaturalYtelseFraDto(no.nav.abakus.iaygrunnlag.kodeverk.NaturalytelseType dto) {
        return dto == null
            ? NaturalYtelseType.UDEFINERT
            : NaturalYtelseType.fraKode(dto.getKode());
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

    static PermisjonsbeskrivelseType mapPermisjonbeskrivelseTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType dto) {
        return dto == null
            ? PermisjonsbeskrivelseType.UDEFINERT
            : PermisjonsbeskrivelseType.fraKode(dto.getKode());
    }

    static Arbeidskategori mapArbeidskategoriFraDto(no.nav.abakus.iaygrunnlag.kodeverk.Arbeidskategori dto) {
        return dto == null
            ? Arbeidskategori.UDEFINERT
            : Arbeidskategori.fraKode(dto.getKode());
    }

    static InntektPeriodeType mapInntektPeriodeTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.InntektPeriodeType dto) {
        return dto == null
            ? InntektPeriodeType.UDEFINERT
            : InntektPeriodeType.fraKode(dto.getKode());
    }

}
