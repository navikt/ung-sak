package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDateTime;
import java.util.UUID;

public record VilkårsavklaringDto(
    @NotNull UUID referanse,
    @NotNull @Valid Periode periode,
    @NotNull Avklaringtype avklaringtype,
    // Koder som strenger: årsak og kilde er interfaces med én enum per vilkår
    @NotNull String ikkeOppfyltÅrsak,
    String begrunnelse,
    @NotNull boolean skalSendeVarsel,
    String fritekstTilVarsel,
    String begrunnelseIkkeVarsel,
    @NotNull String kilde,
    String kildeFritekst,
    @NotNull String vurdertAv,
    @NotNull LocalDateTime vurdertTidspunkt,
    @NotNull boolean foreslåttIBehandlingen,
    @Valid UttalelseDto uttalelse
) {
}
