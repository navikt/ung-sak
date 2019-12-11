package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk;

import java.util.Map;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatusV2;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;

public class MapAktivitetStatusV2FraVLTilRegel {
    private static final Map<no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus, AktivitetStatusV2> MAP_AKTIVITETSTATUS =
        Map.of(
            no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus.ARBEIDSTAKER, AktivitetStatusV2.AT,
            no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus.FRILANSER, AktivitetStatusV2.FL,
            no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, AktivitetStatusV2.SN,
            AktivitetStatus.MILITÆR_ELLER_SIVIL, AktivitetStatusV2.MS,
            no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus.DAGPENGER, AktivitetStatusV2.DP,
            no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus.ARBEIDSAVKLARINGSPENGER, AktivitetStatusV2.AAP
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
