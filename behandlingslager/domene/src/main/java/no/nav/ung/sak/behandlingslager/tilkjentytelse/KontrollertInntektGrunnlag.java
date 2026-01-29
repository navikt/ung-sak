package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.*;
import no.nav.ung.sak.felles.BaseEntitet;
import no.nav.ung.sak.felles.diff.ChangeTracked;
import org.hibernate.annotations.Immutable;

import java.util.*;

@Entity(name = "KontrollertInntektGrunnlag")
@Table(name = "GR_KONTROLLERT_INNTEKT")
public class KontrollertInntektGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_KONTROLLERT_INNTEKT")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ManyToOne
    @Immutable
    @ChangeTracked
    @JoinColumn(name = "kontrollert_inntekt_perioder_id", nullable = false, updatable = false, unique = true)
    private KontrollertInntektPerioder kontrollertInntektPerioder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;


    public KontrollertInntektGrunnlag() {
    }

    KontrollertInntektGrunnlag(Long behandlingId, KontrollertInntektGrunnlag grunnlag) {
        this.behandlingId = behandlingId;
        this.kontrollertInntektPerioder = grunnlag.kontrollertInntektPerioder;
    }

    KontrollertInntektGrunnlag(Long behandlingId, KontrollertInntektPerioder perioder) {
        this.behandlingId = behandlingId;
        this.kontrollertInntektPerioder = perioder;
    }

    public Long getId() {
        return id;
    }

    public KontrollertInntektPerioder getKontrollertInntektPerioder() {
        return kontrollertInntektPerioder;
    }


    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof KontrollertInntektGrunnlag that)) return false;
        return Objects.equals(kontrollertInntektPerioder, that.kontrollertInntektPerioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kontrollertInntektPerioder);
    }
}
