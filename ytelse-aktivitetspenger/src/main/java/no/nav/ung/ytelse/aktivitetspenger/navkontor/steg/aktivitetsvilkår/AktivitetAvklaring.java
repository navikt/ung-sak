package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.aktivitetsvilkår;

import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.inngangsvilkår.avklaring.ForeslåttVilkårsavklaring;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * En aktivitetsavklaring slik den lagres: varslingsinnholdet ({@link AktivitetVarselInnhold}) sammen med
 * saksbehandlerens begrunnelser og sporingen av hvem som vurderte. Disse feltene er bevisst holdt utenfor
 * innholdet, siden de ikke vises for bruker og derfor ikke skal påvirke om et varsel må sendes på nytt.
 */
public record AktivitetAvklaring(
    AktivitetVarselInnhold innhold,
    String begrunnelse,
    String begrunnelseIkkeVarsel,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) implements ForeslåttVilkårsavklaring {

    @Override
    public VilkårPeriodeAvklaringForeslått tilEntitet(UUID referanse) {
        return AktivitetAvklaringDataMapper.mapTilVilkårPeriodeAvklaring(this, referanse);
    }
}
