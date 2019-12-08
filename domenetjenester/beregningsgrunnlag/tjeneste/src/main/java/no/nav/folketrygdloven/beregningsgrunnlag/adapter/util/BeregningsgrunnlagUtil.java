package no.nav.folketrygdloven.beregningsgrunnlag.adapter.util;

import static no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus.ARBEIDSTAKER;
import static no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus.FRILANSER;
import static no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus.KOMBINERT_AT_FL;
import static no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus.KOMBINERT_AT_FL_SN;
import static no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus.KOMBINERT_AT_SN;
import static no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus.KOMBINERT_FL_SN;

import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;

public final class BeregningsgrunnlagUtil {
    private static final Set<AktivitetStatus> ATFL_STATUSER = Set.of(ARBEIDSTAKER, FRILANSER, KOMBINERT_AT_FL);
    private static final Set<AktivitetStatus> ATFL_SN_STATUSER = Set.of(KOMBINERT_AT_SN, KOMBINERT_FL_SN, KOMBINERT_AT_FL_SN);

    private BeregningsgrunnlagUtil() {
    }

    public static Long nullSafeLong(Long input) {
        if (input != null) {
            return input;
        }
        return 0L;
    }

    public static boolean erATFL(AktivitetStatus aktivitetStatus) {
        return ATFL_STATUSER.contains(aktivitetStatus);
    }

    public static boolean erATFL_SN(AktivitetStatus aktivitetStatus) {
        return ATFL_SN_STATUSER.contains(aktivitetStatus);
    }
}
