package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;

public record AndreLivsoppholdsytelserFaktaavklaringPeriodeDto(
    @NotNull @Valid ÅpenPeriode periode,
    @NotNull @Valid AndreLivsoppholdsytelserAvklaringIkkeOppfyltDto avklaring
) {

    public boolean skalSendeVarsel() {
        return avklaring != null && avklaring.skalSendeVarsel();
    }

}
