package no.nav.ung.sak.behandlingslager.vilkårsavklaring;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Avklaringen lagres i to varianter som skiller seg fra hverandre kun ved hvilken tabell de ligger i:
 * {@link VilkårPeriodeAvklaringForeslått} (foreslått og behandlet i gjeldende behandling) og
 * {@link VilkårPeriodeAvklaringFerdigstilt} (ferdig avklart/vedtatt). Selve dataene er identiske.
 * <p>
 * I motsetning til vilkårsspesifikke avklaringer (f.eks. {@code BostedsPeriodeAvklaring}) eksponeres årsaken
 * til at vilkåret ikke er oppfylt som en rå kode, siden fire ulike {@code *IkkeOppfyltÅrsak}-enums ikke kan deles
 * i én kolonne. Typingen gjenopprettes i hvert vilkårs egen mapper via {@code fraKode}.
 */
public interface VilkårPeriodeAvklaring {

    UUID getReferanse();

    DatoIntervallEntitet getPeriode();

    String getIkkeOppfyltÅrsakKode();

    String getBegrunnelse();

    boolean skalSendeVarsel();

    String getFritekstTilVarsel();

    String getBegrunnelseIkkeVarsel();

    Avklaringtype getAvklaringtype();

    String getVurdertAv();

    LocalDateTime getVurdertTidspunkt();

    /**
     * Om avklaringen krever at saksbehandler tar stilling til vilkåret manuelt, uavhengig av årsak —
     * dvs. at bruker er valgt å ikke varsles ved eventuelt negativt utfall.
     */
    default boolean krevesManuellVurdering() {
        return !skalSendeVarsel();
    }
}
