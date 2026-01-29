package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.sak.felles.BaseEntitet;
import no.nav.ung.sak.felles.tid.PostgreSQLRangeType;
import no.nav.ung.sak.felles.tid.Range;
import no.nav.ung.sak.felles.tid.DatoIntervallEntitet;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.felles.typer.Periode;
import org.hibernate.annotations.Type;

import java.time.LocalDate;

@Entity(name = "InntektAbonnement")
@Table(name = "INNTEKT_ABONNEMENT")
public class InntektAbonnement extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_INNTEKT_ABONNEMENT")
    private Long id;

    @Column(name = "abonnement_id", nullable = false, unique = true)
    private String abonnementId;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", nullable = false)))
    private AktørId aktørId;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "siste_bruksdag", nullable = false)
    private LocalDate sisteBruksdag;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    public InntektAbonnement() {
    }

    public InntektAbonnement(String abonnementId, AktørId aktørId, Periode periode, LocalDate sisteBruksdag) {
        this.abonnementId = abonnementId;
        this.aktørId = aktørId;
        setPeriode(periode.getFom(), periode.getTom());
        setSisteBruksdag(sisteBruksdag);
    }

    public Long getId() {
        return id;
    }

    public String getAbonnementId() {
        return abonnementId;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public void setPeriode(LocalDate fom, LocalDate tom) {
        if ((fom == null || fom.equals(Tid.TIDENES_BEGYNNELSE))) {
            throw new IllegalArgumentException(String.format("Alle saker må angi en startdato: [%s, %s]", fom, tom));
        }
        if (tom == null || tom.equals(Tid.TIDENES_ENDE)) {
            throw new IllegalArgumentException();
        }
        this.periode = DatoIntervallEntitet.fra(fom, tom).toRange();
    }

    public LocalDate getSisteBruksdag() {
        return sisteBruksdag;
    }

    public void setSisteBruksdag(LocalDate sisteBruksdag) {
        this.sisteBruksdag = sisteBruksdag;
    }

    public boolean erAktiv() {
        return aktiv;
    }
    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }
}
