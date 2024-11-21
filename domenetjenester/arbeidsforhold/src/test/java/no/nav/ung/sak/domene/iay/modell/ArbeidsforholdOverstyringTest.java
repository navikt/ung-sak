package no.nav.ung.sak.domene.iay.modell;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.ung.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.ung.kodeverk.arbeidsforhold.BekreftetPermisjonStatus;

public class ArbeidsforholdOverstyringTest {

    @Test
    public void er_overstyrt() {
        var overstyring1 = new ArbeidsforholdOverstyring();
        overstyring1.setHandling(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER);
        assertThat(overstyring1.erOverstyrt()).isTrue();

        var overstyring2 = new ArbeidsforholdOverstyring();
        overstyring2.setHandling(ArbeidsforholdHandlingType.BRUK);
        overstyring2.setBekreftetPermisjon(new BekreftetPermisjon(BekreftetPermisjonStatus.BRUK_PERMISJON));
        assertThat(overstyring2.erOverstyrt()).isTrue();

        var overstyring3 = new ArbeidsforholdOverstyring();
        overstyring3.setHandling(ArbeidsforholdHandlingType.BRUK);
        overstyring3.setBekreftetPermisjon(new BekreftetPermisjon());
        assertThat(overstyring3.erOverstyrt()).isFalse();
    }

}
