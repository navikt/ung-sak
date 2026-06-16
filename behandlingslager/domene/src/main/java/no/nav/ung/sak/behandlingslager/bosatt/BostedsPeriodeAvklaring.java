package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.PostgreSQLRangeType;
import no.nav.ung.sak.domene.typer.tid.Range;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregat for bostedsavklaring knyttet til én vilkårsperiode.
 * {@code skjæringstidspunkt} tilsvarer fom-dato for vilkårsperioden og matcher
 * {@code referanse} referanse for å kunne garantere at etterlysning/uttalelse linkes til riktig vurdering
 * {@code erBosattITrondheim} angir om bruker er bosatt ved skjæringstidspunktet.
 * {@code fraflyttingsDato} angir eventuell dato for utflytting fra Trondheim (null dersom bruker ikke har flyttet ut).
 * {@code fraflyttingsÅrsak} angir årsaken til fraflytting (null dersom bruker er bosatt hele perioden).
 */
@Entity(name = "BostedsPeriodeAvklaring")
@Table(name = "BOSATT_PERIODE_AVKLARING")
@Immutable
public class BostedsPeriodeAvklaring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_PERIODE_AVKLARING")
    private Long id;

    @Column(name = "referanse", nullable = false, updatable = false)
    private UUID referanse = UUID.randomUUID();

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "er_bosatt_i_trondheim", nullable = false, updatable = false)
    private boolean erBosattITrondheim;

    @Enumerated(EnumType.STRING)
    @Column(name = "ikke_oppfylt_aarsak", updatable = false)
    private BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak;

    @Enumerated(EnumType.STRING)
    @Column(name = "kilde", nullable = false, updatable = false)
    private Kilde kilde;

    @Column(name = "vurdert_av", updatable = false)
    private String vurdertAv;

    @Column(name = "vurdert_tidspunkt", updatable = false)
    private LocalDateTime vurdertTidspunkt;

    public BostedsPeriodeAvklaring() {
        // Hibernate
    }

    public BostedsPeriodeAvklaring(DatoIntervallEntitet periode, boolean erBosattITrondheim, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, Kilde kilde) {
        this.periode = periode.toRange();
        this.erBosattITrondheim = erBosattITrondheim;
        this.ikkeOppfyltÅrsak = ikkeOppfyltÅrsak;
        this.kilde = kilde;
    }

    public BostedsPeriodeAvklaring(DatoIntervallEntitet periode, boolean erBosattITrondheim, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, Kilde kilde, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        this.periode = periode.toRange();
        this.erBosattITrondheim = erBosattITrondheim;
        this.ikkeOppfyltÅrsak = ikkeOppfyltÅrsak;
        this.kilde = kilde;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
    }

    public BostedsPeriodeAvklaring medNyPeriode(DatoIntervallEntitet nyPeriode) {
        return new BostedsPeriodeAvklaring(
            nyPeriode,
            this.isErBosattITrondheim(),
            this.getIkkeOppfyltÅrsak(),
            this.getKilde(),
            this.vurdertAv,
            this.vurdertTidspunkt
        );
    }

    public Long getId() {
        return id;
    }

    public UUID getReferanse() {
        return referanse;
    }

    public boolean isErBosattITrondheim() {
        return erBosattITrondheim;
    }

    public BostedsvilkårIkkeOppfyltÅrsak getIkkeOppfyltÅrsak() {
        return ikkeOppfyltÅrsak;
    }

    public Kilde getKilde() {
        return kilde;
    }

    public String getVurdertAv() {
        return vurdertAv;
    }

    public LocalDateTime getVurdertTidspunkt() {
        return vurdertTidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsPeriodeAvklaring that)) return false;
        return erBosattITrondheim == that.erBosattITrondheim
            && ikkeOppfyltÅrsak == that.ikkeOppfyltÅrsak
            && kilde == that.kilde
            && Objects.equals(vurdertAv, that.vurdertAv)
            && Objects.equals(vurdertTidspunkt, that.vurdertTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(erBosattITrondheim, ikkeOppfyltÅrsak, kilde, vurdertAv, vurdertTidspunkt);
    }

    @Override
    public String toString() {
        return "BostedsPeriodeAvklaring{referanse=" + referanse
            + ", erBosattITrondheim=" + erBosattITrondheim
            + ", fraflyttingsÅrsak=" + ikkeOppfyltÅrsak
            + ", kilde=" + kilde
            + ", vurdertAv=" + vurdertAv
            + ", vurdertTidspunkt=" + vurdertTidspunkt + '}';
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }
}
