package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregat for bostedsavklaring knyttet til én vilkårsperiode.
 * {@code skjæringstidspunkt} tilsvarer fom-dato for vilkårsperioden og matcher
 * fom-dato til tilhørende Etterlysning og UttalelseV2.
 * {@code referanse} brukes som {@code grunnlagsreferanse} i etterlysning og uttalelse.
 * {@code erBosattITrondheim} angir om bruker er bosatt ved skjæringstidspunktet.
 * {@code fraflyttingsDato} angir eventuell dato for utflytting fra Trondheim (null dersom bruker ikke har flyttet ut).
 * {@code fraflyttingsÅrsak} angir årsaken til fraflytting (null dersom bruker er bosatt hele perioden).
 * {@code begrunnelseVedAnnet} er fritekstvurdering fra saksbehandler, påkrevd når årsak er ANNET.
 */
@Entity(name = "BostedsPeriodeAvklaring")
@Table(name = "BOSATT_PERIODE_AVKLARING")
public class BostedsPeriodeAvklaring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_PERIODE_AVKLARING")
    private Long id;

    @Column(name = "referanse", nullable = false, updatable = false)
    private UUID referanse = UUID.randomUUID();

    @Column(name = "skaeringstidspunkt", nullable = false, updatable = false)
    private LocalDate skjæringstidspunkt;

    @Column(name = "er_bosatt_i_trondheim", nullable = false, updatable = false)
    private boolean erBosattITrondheim;

    @Column(name = "fraflyttings_dato", updatable = false)
    private LocalDate fraflyttingsDato;

    @Enumerated(EnumType.STRING)
    @Column(name = "fraflyttings_aarsak", updatable = false)
    private FraflyttingsÅrsak fraflyttingsÅrsak;

    @Column(name = "begrunnelse_ved_annet", length = 4000)
    private String begrunnelseVedAnnet;

    public BostedsPeriodeAvklaring() {
        // Hibernate
    }

    public BostedsPeriodeAvklaring(LocalDate skjæringstidspunkt, boolean erBosattITrondheim, LocalDate fraflyttingsDato, FraflyttingsÅrsak fraflyttingsÅrsak) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.erBosattITrondheim = erBosattITrondheim;
        this.fraflyttingsDato = fraflyttingsDato;
        this.fraflyttingsÅrsak = fraflyttingsÅrsak;
    }

    public Long getId() {
        return id;
    }

    public UUID getReferanse() {
        return referanse;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public boolean isErBosattITrondheim() {
        return erBosattITrondheim;
    }

    public LocalDate getFraflyttingsDato() {
        return fraflyttingsDato;
    }

    public FraflyttingsÅrsak getFraflyttingsÅrsak() {
        return fraflyttingsÅrsak;
    }

    public String getBegrunnelseVedAnnet() {
        return begrunnelseVedAnnet;
    }

    public void setBegrunnelseVedAnnet(String begrunnelseVedAnnet) {
        this.begrunnelseVedAnnet = begrunnelseVedAnnet;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsPeriodeAvklaring that)) return false;
        return erBosattITrondheim == that.erBosattITrondheim
            && Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt)
            && Objects.equals(fraflyttingsDato, that.fraflyttingsDato)
            && fraflyttingsÅrsak == that.fraflyttingsÅrsak;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skjæringstidspunkt, erBosattITrondheim, fraflyttingsDato, fraflyttingsÅrsak);
    }

    @Override
    public String toString() {
        return "BostedsPeriodeAvklaring{skjæringstidspunkt=" + skjæringstidspunkt
            + ", referanse=" + referanse
            + ", erBosattITrondheim=" + erBosattITrondheim
            + ", fraflyttingsDato=" + fraflyttingsDato
            + ", fraflyttingsÅrsak=" + fraflyttingsÅrsak + '}';
    }
}
