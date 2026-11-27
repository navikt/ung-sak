package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.andrelivsoppholdsytelser;

import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.inngangsvilkår.avklaring.ForeslåttVilkårsavklaring;

import java.time.LocalDateTime;
import java.util.UUID;

public record AndreLivsoppholdsytelserAvklaring(
    AndreLivsoppholdsytelserVarselInnhold innhold,
    String begrunnelseIkkeVarsel,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) implements ForeslåttVilkårsavklaring {

    @Override
    public VilkårPeriodeAvklaringForeslått tilEntitet(UUID referanse) {
        return AndreLivsoppholdsytelserAvklaringDataMapper.mapTilVilkårPeriodeAvklaring(this, referanse);
    }
}
