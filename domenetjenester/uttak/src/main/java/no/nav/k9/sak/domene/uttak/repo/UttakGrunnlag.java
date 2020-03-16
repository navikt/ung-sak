package no.nav.k9.sak.domene.uttak.repo;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity(name = "UttakGrunnlag")
@Table(name = "GR_UTTAK")
public class UttakGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_UTTAK")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ManyToOne
    @JoinColumn(name = "oppgitt_uttak_id", nullable = false, updatable = false)
    private UttakAktivitet oppgittUttak;

    @ManyToOne
    @JoinColumn(name = "fastsatt_uttak_id")
    private UttakAktivitet fastsattUttak;

    @ManyToOne
    @JoinColumn(name = "soeknadsperioder_id")
    private Søknadsperioder søknadsperioder;

    @ManyToOne
    @JoinColumn(name = "ferie_id")
    private Ferie oppgittFerie;

    @ManyToOne
    @JoinColumn(name = "tilsynsordning_id")
    private OppgittTilsynsordning oppgittTilsynsordning;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UttakGrunnlag() {
    }

    /** Opprett uten fastsattUttak - kommer fra saksbehandler senere. */
    public UttakGrunnlag(Long behandlingId, UttakAktivitet oppgittUttak, Søknadsperioder søknadsperioder, Ferie ferie, OppgittTilsynsordning tilsynsordning) {
        this(behandlingId, oppgittUttak, null, søknadsperioder, ferie, tilsynsordning);
    }

    public UttakGrunnlag(Long behandlingId, UttakAktivitet oppgittUttak, UttakAktivitet fastsattUttak, Søknadsperioder søknadsperioder, Ferie ferie, OppgittTilsynsordning tilsynsordning) {
        this.behandlingId = behandlingId;
        this.oppgittUttak = oppgittUttak;
        this.fastsattUttak = fastsattUttak;
        this.søknadsperioder = søknadsperioder;
        this.oppgittFerie = ferie;
        this.oppgittTilsynsordning = tilsynsordning;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public UttakAktivitet getOppgittUttak() {
        return oppgittUttak;
    }

    public UttakAktivitet getFastsattUttak() {
        return fastsattUttak;
    }

    public Søknadsperioder getOppgittSøknadsperioder() {
        return søknadsperioder;
    }

    public OppgittTilsynsordning getOppgittTilsynsordning() {
        return oppgittTilsynsordning;
    }

    public Ferie getOppgittFerie() {
        return oppgittFerie;
    }

    void setOppgittFordeling(UttakAktivitet oppgittFordeling) {
        this.oppgittUttak = oppgittFordeling;
    }

    void setOppgittFerie(Ferie ferie) {
        this.oppgittFerie = ferie;
    }
    
    void setOppgittTilsynsordning(OppgittTilsynsordning tilsynsordning) {
        this.oppgittTilsynsordning = tilsynsordning;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof UttakGrunnlag))
            return false;
        var that = this.getClass().cast(o);
        return Objects.equals(oppgittUttak, that.oppgittUttak)
            && Objects.equals(fastsattUttak, that.fastsattUttak)
            && Objects.equals(søknadsperioder, that.søknadsperioder)
            && Objects.equals(oppgittFerie, that.oppgittFerie)
            && Objects.equals(oppgittTilsynsordning, that.oppgittTilsynsordning);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgittUttak, fastsattUttak, søknadsperioder, oppgittFerie, oppgittTilsynsordning);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id +
            ", behandling=" + behandlingId +
            ", oppgittUttak=" + oppgittUttak +
            ", fastsattUttak=" + fastsattUttak +
            ", oppgittFerie = " + oppgittFerie +
            ", søknadsperioder = " + søknadsperioder +
            ", oppgittTilsynsordning = " + oppgittTilsynsordning +
            ", aktiv=" + aktiv +
            ", versjon=" + versjon +
            '>';
    }
}
