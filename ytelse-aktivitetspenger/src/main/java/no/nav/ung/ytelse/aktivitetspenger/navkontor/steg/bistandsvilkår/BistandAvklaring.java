package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bistandsvilkår;

import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.inngangsvilkår.avklaring.ForeslåttVilkårsavklaring;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * En bistandsavklaring slik den lagres.
 */
public record BistandAvklaring(
    BistandVarselInnhold innhold,
    String begrunnelseIkkeVarsel,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) implements ForeslåttVilkårsavklaring {

    @Override
    public VilkårPeriodeAvklaringForeslått tilEntitet(UUID referanse) {
        return BistandAvklaringDataMapper.mapTilVilkårPeriodeAvklaring(this, referanse);
    }
}
