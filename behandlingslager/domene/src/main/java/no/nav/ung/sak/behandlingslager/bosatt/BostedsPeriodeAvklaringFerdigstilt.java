package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.hibernate.annotations.Immutable;

/**
 * Bostedsavklaring som er ferdigstilt, dvs. vedtatt i en behandling. Ferdigstilte avklaringer akkumuleres
 * og kopieres videre til nye behandlinger.
 * <p>
 * Ferdigstilte avklaringer kan splittes av senere ferdigstillinger og kan derfor dele referanse på tvers
 * av segmenter — det etterlyses aldri uttalelse på en ferdigstilt avklaring.
 */
@Entity(name = "BostedsPeriodeAvklaringFerdigstilt")
@Table(name = "BOSATT_PERIODE_AVKLARING")
@Immutable
public class BostedsPeriodeAvklaringFerdigstilt extends BostedsPeriodeAvklaring {

    public BostedsPeriodeAvklaringFerdigstilt() {
        // Hibernate
    }

    BostedsPeriodeAvklaringFerdigstilt(BostedsPeriodeAvklaring annenAvklaring) {
        super(annenAvklaring);
    }

    BostedsPeriodeAvklaringFerdigstilt(BostedsPeriodeAvklaring annenAvklaring, DatoIntervallEntitet nyPeriode) {
        super(annenAvklaring, nyPeriode);
    }
}
