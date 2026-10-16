package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AndreLivsoppholdsytelserVurderingerResponse(
    @NotNull @Valid List<AndreLivsoppholdsytelserVurderingDto> vurderinger
) {
}
