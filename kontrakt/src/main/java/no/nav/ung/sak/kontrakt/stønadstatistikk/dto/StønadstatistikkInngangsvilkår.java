package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.VilkårType;

public record StønadstatistikkInngangsvilkår(
    @NotNull VilkårType vilkår,
    @NotNull StønadstatistikkUtfall utfall
) {
}
