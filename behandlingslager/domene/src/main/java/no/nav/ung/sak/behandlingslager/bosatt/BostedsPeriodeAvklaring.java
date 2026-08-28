package no.nav.ung.sak.behandlingslager.bosatt;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Avklaringen lagres i to varianter som skiller seg fra hverandre kun ved hvilken tabell de ligger i:
 * {@link BostedsPeriodeAvklaringForeslått} (foreslått og behandlet i gjeldende behandling) og
 * {@link BostedsPeriodeAvklaringFerdigstilt} (ferdig avklart/vedtatt). Selve dataene er identiske,
 */
public interface BostedsPeriodeAvklaring {

    UUID getReferanse();

    DatoIntervallEntitet getPeriode();

    BostedsvilkårIkkeOppfyltÅrsak getIkkeOppfyltÅrsak();

    String getBegrunnelse();

    boolean skalSendeVarsel();

    String getFritekstTilVarsel();

    String getBegrunnelseIkkeVarsel();

    Avklaringtype getAvklaringtype();

    String getVurdertAv();

    LocalDateTime getVurdertTidspunkt();
}
