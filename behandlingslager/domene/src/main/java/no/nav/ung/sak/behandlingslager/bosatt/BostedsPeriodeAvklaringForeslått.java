package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Bostedsavklaring som er foreslått i den behandlingen holderen tilhører.
 * <p>
 * Tilhørighet til denne tabellen er i seg selv indikatoren på at avklaringen ble foreslått og behandlet
 * i gjeldende behandling — derfor har ikke avklaringen noe eget felt for status eller opprettende behandling.
 * Foreslåtte avklaringer kopieres aldri videre til en ny behandling, jf.
 * {@link BostedsGrunnlag#nyttGrunnlagForBehandlingMedReferanserFra}.
 */
@Entity(name = "BostedsPeriodeAvklaringForeslått")
@Table(name = "BOSATT_PERIODE_AVKLARING_FORESLATT")
@Immutable
public class BostedsPeriodeAvklaringForeslått extends BostedsPeriodeAvklaring {

    public BostedsPeriodeAvklaringForeslått() {
        // Hibernate
    }

    public BostedsPeriodeAvklaringForeslått(DatoIntervallEntitet periode,
                                            BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
                                            String begrunnelse,
                                            boolean skalSendeVarsel,
                                            String fritekstTilVarsel,
                                            String begrunnelseIkkeVarsel,
                                            String vurdertAv,
                                            LocalDateTime vurdertTidspunkt,
                                            Avklaringtype avklaringtype) {
        super(periode, ikkeOppfyltÅrsak, begrunnelse, skalSendeVarsel, fritekstTilVarsel, begrunnelseIkkeVarsel, vurdertAv, vurdertTidspunkt, avklaringtype);
    }

    public BostedsPeriodeAvklaringForeslått(BostedsPeriodeAvklaring annenAvklaring) {
        super(annenAvklaring);
    }
}
