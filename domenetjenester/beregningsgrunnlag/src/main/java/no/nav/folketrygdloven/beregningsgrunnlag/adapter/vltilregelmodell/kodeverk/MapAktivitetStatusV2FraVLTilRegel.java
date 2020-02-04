package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk;

import java.util.Map;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatusV2;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.Inntektskategori;

public class MapAktivitetStatusV2FraVLTilRegel {
    private static final Map<no.nav.k9.kodeverk.iay.AktivitetStatus, AktivitetStatusV2> MAP_AKTIVITETSTATUS =
        Map.of(
            no.nav.k9.kodeverk.iay.AktivitetStatus.ARBEIDSTAKER, AktivitetStatusV2.AT,
            no.nav.k9.kodeverk.iay.AktivitetStatus.FRILANSER, AktivitetStatusV2.FL,
            no.nav.k9.kodeverk.iay.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, AktivitetStatusV2.SN,
            AktivitetStatus.MILITÆR_ELLER_SIVIL, AktivitetStatusV2.MS,
            no.nav.k9.kodeverk.iay.AktivitetStatus.DAGPENGER, AktivitetStatusV2.DP,
            no.nav.k9.kodeverk.iay.AktivitetStatus.ARBEIDSAVKLARINGSPENGER, AktivitetStatusV2.AAP
        );

    private static final Map<Inntektskategori, AktivitetStatusV2> MAP_INNTEKTSKATEGORI = Map.of(
        Inntektskategori.ARBEIDSAVKLARINGSPENGER, AktivitetStatusV2.AAP,
        Inntektskategori.ARBEIDSTAKER, AktivitetStatusV2.AT,
        Inntektskategori.SJØMANN, AktivitetStatusV2.AT,
        Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, AktivitetStatusV2.AT,
        Inntektskategori.DAGPENGER, AktivitetStatusV2.DP,
        Inntektskategori.FRILANSER, AktivitetStatusV2.FL,
        Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, AktivitetStatusV2.SN,
        Inntektskategori.DAGMAMMA, AktivitetStatusV2.SN,
        Inntektskategori.JORDBRUKER, AktivitetStatusV2.SN,
        Inntektskategori.FISKER, AktivitetStatusV2.SN
    );

    private MapAktivitetStatusV2FraVLTilRegel() {
        // skjul public constructor
    }

    public static AktivitetStatusV2 map(AktivitetStatus aktivitetStatus, Inntektskategori inntektskategori) {
        if (aktivitetStatus.equals(AktivitetStatus.BRUKERS_ANDEL)) {
            return MAP_INNTEKTSKATEGORI.get(inntektskategori);
        }
        return MAP_AKTIVITETSTATUS.get(aktivitetStatus);
    }
}
