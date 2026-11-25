package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bosatt;

import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.inngangsvilkår.avklaring.ForeslåttVilkårsavklaring;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * En bostedsavklaring slik den lagres.
 */
public record BostedAvklaring(
    BostedVarselInnhold innhold,
    String begrunnelseIkkeVarsel,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) implements ForeslåttVilkårsavklaring {

    @Override
    public VilkårPeriodeAvklaringForeslått tilEntitet(UUID referanse) {
        return BostedsAvklaringDataMapper.mapTilVilkårPeriodeAvklaring(this, referanse);
    }
}
