package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record InformasjonsbrevValgResponseDto(
    @JsonProperty("informasjonbrevValg")
    List<InformasjonsbrevValgDto> informasjonbrevValg
) {

}
