package no.nav.ung.sak.kontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

@ApplicationScoped
public class RegisterinntektKilde {

    private boolean aktivitetspengerInntektsgrunnlagEnabled;

    RegisterinntektKilde() {
        // for CDI proxy
    }

    @Inject
    public RegisterinntektKilde(@KonfigVerdi(value = "AKTIVITETSPENGER_INNTEKTSGRUNNLAG_ENABLED", required = false, defaultVerdi = "true") boolean aktivitetspengerInntektsgrunnlagEnabled) {
        this.aktivitetspengerInntektsgrunnlagEnabled = aktivitetspengerInntektsgrunnlagEnabled;
    }

    public InntektsKilde forYtelse(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case UNGDOMSYTELSE -> InntektsKilde.INNTEKT_UNGDOMSYTELSE;
            case AKTIVITETSPENGER -> aktivitetspengerInntektsgrunnlagEnabled ? InntektsKilde.INNTEKT_AKTIVITETSPENGER : InntektsKilde.INNTEKT_UNGDOMSYTELSE;
            default -> throw new IllegalArgumentException("Har ikke inntektskilde for kontroll av registerinntekt for ytelseType=" + ytelseType);
        };
    }
}
