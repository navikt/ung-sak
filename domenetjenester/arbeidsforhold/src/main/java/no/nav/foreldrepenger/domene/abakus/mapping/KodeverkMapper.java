package no.nav.foreldrepenger.domene.abakus.mapping;

import java.util.Map;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.TemaUnderkategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektPeriodeType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NæringsinntektType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.OffentligYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PensjonTrygdType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.SkatteOgAvgiftsregelType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.YtelseType;
import no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.YtelseStatus;

public final class KodeverkMapper {
    private static final Map<String, String> YTELSETYPE_FPSAK_TIL_ABAKUS;
    private static final Map<String, String> YTELSETYPE_ABAKUS_TIL_FPSAK;

    private KodeverkMapper() {
    }

    static {
        YTELSETYPE_FPSAK_TIL_ABAKUS = Map.of(
            "FORELDREPENGER", "FP",
            "ENGANGSSTØNAD", "ES",
            "SVANGERSKAPSPENGER", "SVP",
            "ARBEIDSAVKLARINGSPENGER", "AAP",
            "DAGPENGER", "DAG",
            "SYKEPENGER", "SP",
            "PÅRØRENDESYKDOM", "PS",
            "ENSLIG_FORSØRGER", "EF");

        YTELSETYPE_ABAKUS_TIL_FPSAK = YTELSETYPE_FPSAK_TIL_ABAKUS.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.YtelseType mapYtelseTypeTilDto(RelatertYtelseType ytelseType) {
        if (ytelseType == null || "-".equals(ytelseType.getKode())) {
            return null;
        }
        String kode = getAbakusYtelseTypeFraFpsak(ytelseType.getKode());
        return new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.YtelseType(kode);
    }

    private static String getFpsakYtelseTypeFraAbakus(String kode) {
        return YTELSETYPE_ABAKUS_TIL_FPSAK.get(kode);
    }

    private static String getAbakusYtelseTypeFraFpsak(String kode) {
        return YTELSETYPE_FPSAK_TIL_ABAKUS.get(kode);
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtbetaltYtelseType mapYtelseTypeTilDto(no.nav.foreldrepenger.domene.iay.modell.kodeverk.YtelseType ytelseType) {
        if (ytelseType == null || "-".equals(ytelseType.getKode())) {
            return null;
        }
        switch (ytelseType.getKodeverk()) {
            case no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtbetaltYtelseFraOffentligeType.KODEVERK:
                return new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtbetaltYtelseFraOffentligeType(ytelseType.getKode());
            case no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtbetaltNæringsYtelseType.KODEVERK:
                return new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtbetaltNæringsYtelseType(ytelseType.getKode());
            case no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtbetaltPensjonTrygdType.KODEVERK:
                return new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtbetaltPensjonTrygdType(ytelseType.getKode());
            default:
                throw new IllegalArgumentException("Ukjent YtelseType: " + ytelseType + ", kan ikke mappes til "
                    + no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtbetaltYtelseType.class.getName());
        }

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

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.YtelseStatus getAbakusYtelseStatusForFpsakRelatertYtelseTilstand(RelatertYtelseTilstand tilstand) {
        var kode = tilstand.getKode();
        switch (kode) {
            case "IKKESTARTET":
                return no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.YtelseStatus.OPPRETTET;
            case "ÅPEN":
                return no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.YtelseStatus.UNDER_BEHANDLING;
            case "AVSLUTTET":
                return no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.YtelseStatus.AVSLUTTET;
            case "LØPENDE":
                return no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.YtelseStatus.LØPENDE;
            default:
                throw new IllegalArgumentException("Ukjent RelatertYtelseTilstand: " + kode);
        }
    }

    static YtelseType mapUtbetaltYtelseTypeTilGrunnlag(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtbetaltYtelseType type) {
        if (type == null)
            return OffentligYtelseType.UDEFINERT;
        var kodeverk = (no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.Kodeverk) type; // NOSONAR
        String kode = kodeverk.getKode();
        switch (kodeverk.getKodeverk()) {
            case no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtbetaltYtelseFraOffentligeType.KODEVERK:
                return OffentligYtelseType.fraKode(kode);
            case no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtbetaltNæringsYtelseType.KODEVERK:
                return NæringsinntektType.fraKode(kode);
            case no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtbetaltPensjonTrygdType.KODEVERK:
                return PensjonTrygdType.fraKode(kode);
            default:
                throw new IllegalArgumentException("Ukjent UtbetaltYtelseType: " + type);
        }
    }

    static TemaUnderkategori getTemaUnderkategori(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.TemaUnderkategori kode) {
        return kode == null || "-".equals(kode.getKode())
            ? TemaUnderkategori.UDEFINERT
            : TemaUnderkategori.fraKode(kode.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.TemaUnderkategori getBehandlingsTemaUnderkategori(TemaUnderkategori kode) {
        return kode == null || TemaUnderkategori.UDEFINERT.equals(kode)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.TemaUnderkategori(kode.getKode());
    }

    static BekreftetPermisjonStatus getBekreftetPermisjonStatus(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.BekreftetPermisjonStatus kode) {
        return kode == null || "-".equals(kode.getKode())
            ? BekreftetPermisjonStatus.UDEFINERT
            : BekreftetPermisjonStatus.fraKode(kode.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.BekreftetPermisjonStatus mapBekreftetPermisjonStatus(BekreftetPermisjonStatus status) {
        return status == null || BekreftetPermisjonStatus.UDEFINERT.equals(status)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.BekreftetPermisjonStatus(status.getKode());
    }

    static Fagsystem mapFagsystemFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.Fagsystem dto) {
        return dto == null
            ? Fagsystem.UDEFINERT
            : Fagsystem.fraKode(dto.getKode());
    }

    static RelatertYtelseType mapYtelseTypeFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.YtelseType ytelseType) {
        if (ytelseType == null)
            return RelatertYtelseType.UDEFINERT;
        String relatertYtelseKode = KodeverkMapper.getFpsakYtelseTypeFraAbakus(ytelseType.getKode());
        return RelatertYtelseType.fraKode(relatertYtelseKode);
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.Fagsystem mapFagsystemTilDto(Fagsystem kode) {
        return kode == null || Fagsystem.UDEFINERT.equals(kode)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.Fagsystem(kode.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.InntektPeriodeType mapInntektPeriodeTypeTilDto(InntektPeriodeType hyppighet) {
        return hyppighet == null || InntektPeriodeType.UDEFINERT.equals(hyppighet)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.InntektPeriodeType(hyppighet.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.Arbeidskategori mapArbeidskategoriTilDto(Arbeidskategori kode) {
        return kode == null || Arbeidskategori.UDEFINERT.equals(kode)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.Arbeidskategori(kode.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.ArbeidType mapArbeidTypeTilDto(ArbeidType arbeidType) {
        return arbeidType == null || ArbeidType.UDEFINERT.equals(arbeidType)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.ArbeidType(arbeidType.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType mapPermisjonbeskrivelseTypeTilDto(PermisjonsbeskrivelseType kode) {
        return kode == null || PermisjonsbeskrivelseType.UDEFINERT.equals(kode)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType(kode.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.InntektskildeType mapInntektsKildeTilDto(InntektsKilde kode) {
        return kode == null || InntektsKilde.UDEFINERT.equals(kode)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.InntektskildeType(kode.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.InntektspostType mapInntektspostTypeTilDto(InntektspostType kode) {
        return kode == null || InntektspostType.UDEFINERT.equals(kode)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.InntektspostType(kode.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.SkatteOgAvgiftsregelType mapSkatteOgAvgiftsregelTilDto(SkatteOgAvgiftsregelType kode) {
        return kode == null || SkatteOgAvgiftsregelType.UDEFINERT.equals(kode)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.SkatteOgAvgiftsregelType(kode.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType mapArbeidsforholdHandlingTypeTilDto(ArbeidsforholdHandlingType kode) {
        return kode == null || ArbeidsforholdHandlingType.UDEFINERT.equals(kode)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType(kode.getKode());
    }

    static ArbeidType mapArbeidType(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.ArbeidType dto) {
        return dto == null
            ? ArbeidType.UDEFINERT
            : ArbeidType.fraKode(dto.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.InntektsmeldingInnsendingsårsakType mapInntektsmeldingInnsendingsårsak(InntektsmeldingInnsendingsårsak kode) {
        return kode == null || InntektsmeldingInnsendingsårsak.UDEFINERT.equals(kode)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.InntektsmeldingInnsendingsårsakType(kode.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.NaturalytelseType mapNaturalYtelseTilDto(NaturalYtelseType kode) {
        return kode == null || NaturalYtelseType.UDEFINERT.equals(kode)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.NaturalytelseType(kode.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtsettelseÅrsakType mapUtsettelseÅrsakTilDto(UtsettelseÅrsak kode) {
        return kode == null || UtsettelseÅrsak.UDEFINERT.equals(kode)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtsettelseÅrsakType(kode.getKode());
    }

    static InntektsKilde mapInntektsKildeFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.InntektskildeType dto) {
        return dto == null
            ? InntektsKilde.UDEFINERT
            : InntektsKilde.fraKode(dto.getKode());
    }

    static ArbeidsforholdHandlingType mapArbeidsforholdHandlingTypeFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType dto) {
        return dto == null
            ? ArbeidsforholdHandlingType.UDEFINERT
            : ArbeidsforholdHandlingType.fraKode(dto.getKode());
    }

    static NaturalYtelseType mapNaturalYtelseFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.NaturalytelseType dto) {
        return dto == null
            ? NaturalYtelseType.UDEFINERT
            : NaturalYtelseType.fraKode(dto.getKode());
    }

    static VirksomhetType mapVirksomhetTypeFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.VirksomhetType dto) {
        return dto == null
            ? VirksomhetType.UDEFINERT
            : VirksomhetType.fraKode(dto.getKode());
    }

    static SkatteOgAvgiftsregelType mapSkatteOgAvgiftsregelFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.SkatteOgAvgiftsregelType dto) {
        return dto == null
            ? SkatteOgAvgiftsregelType.UDEFINERT
            : SkatteOgAvgiftsregelType.fraKode(dto.getKode());
    }

    static InntektspostType mapInntektspostTypeFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.InntektspostType dto) {
        return dto == null
            ? InntektspostType.UDEFINERT
            : InntektspostType.fraKode(dto.getKode());
    }

    static PermisjonsbeskrivelseType mapPermisjonbeskrivelseTypeFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType dto) {
        return dto == null
            ? PermisjonsbeskrivelseType.UDEFINERT
            : PermisjonsbeskrivelseType.fraKode(dto.getKode());
    }

    static Arbeidskategori mapArbeidskategoriFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.Arbeidskategori dto) {
        return dto == null
            ? Arbeidskategori.UDEFINERT
            : Arbeidskategori.fraKode(dto.getKode());
    }

    static InntektPeriodeType mapInntektPeriodeTypeFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.InntektPeriodeType dto) {
        return dto == null
            ? InntektPeriodeType.UDEFINERT
            : InntektPeriodeType.fraKode(dto.getKode());
    }

    static InntektsmeldingInnsendingsårsak mapInntektsmeldingInnsendingsårsakFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.InntektsmeldingInnsendingsårsakType dto) {
        return dto == null
            ? InntektsmeldingInnsendingsårsak.UDEFINERT
            : InntektsmeldingInnsendingsårsak.fraKode(dto.getKode());
    }

    static UtsettelseÅrsak mapUtsettelseÅrsakFraDto(no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.UtsettelseÅrsakType dto) {
        return dto == null
            ? UtsettelseÅrsak.UDEFINERT
            : UtsettelseÅrsak.fraKode(dto.getKode());
    }

    static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.VirksomhetType mapVirksomhetTypeTilDto(VirksomhetType kode) {
        return kode == null || VirksomhetType.UDEFINERT.equals(kode)
            ? null
            : new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.VirksomhetType(kode.getKode());
    }

    public static no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.YtelseType mapFagsakYtelseTypeTilDto(FagsakYtelseType ytelseType) {
        if (ytelseType == null)
            return null;
        return new no.nav.foreldrepenger.kontrakter.iaygrunnlag.kodeverk.YtelseType(ytelseType.getKode());
    }

}
