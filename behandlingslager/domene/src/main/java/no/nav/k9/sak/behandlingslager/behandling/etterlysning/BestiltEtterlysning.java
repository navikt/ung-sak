package no.nav.k9.sak.behandlingslager.behandling.etterlysning;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;

@Entity(name = "BestiltEtterlysning")
@Table(name = "ETTERLYSNING")
@DynamicInsert
@DynamicUpdate
public class BestiltEtterlysning extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ETTERLYSNING")
    private Long id;

    @Column(name = "fagsak_id", nullable = false)
    private Long fagsakId;

    @Column(name = "behandling_id", nullable = false)
    private Long behandlingId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom")),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom"))
    })
    private DatoIntervallEntitet periode;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Column(name = "dokument_mal", nullable = false)
    private String dokumentMal;

    BestiltEtterlysning() {
    }

    public BestiltEtterlysning(Long fagsakId, Long behandlingId, DatoIntervallEntitet periode, Arbeidsgiver arbeidsgiver, String dokumentMal) {
        this.fagsakId = Objects.requireNonNull(fagsakId);
        this.behandlingId = Objects.requireNonNull(behandlingId);
        this.periode = Objects.requireNonNull(periode);
        this.arbeidsgiver = arbeidsgiver;
        this.dokumentMal = Objects.requireNonNull(dokumentMal);
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public String getDokumentMal() {
        return dokumentMal;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public boolean getErArbeidsgiverMottaker() {
        return arbeidsgiver != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BestiltEtterlysning that = (BestiltEtterlysning) o;
        return Objects.equals(fagsakId, that.fagsakId) &&
            Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            Objects.equals(periode, that.periode) &&
            Objects.equals(dokumentMal, that.dokumentMal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsakId, behandlingId, periode, arbeidsgiver, dokumentMal);
    }

    @Override
    public String toString() {
        return "BestiltEtterlysning{" +
            "fagsakId=" + fagsakId +
            ", behandlingId=" + behandlingId +
            ", periode=" + periode +
            ", dokumentMal='" + dokumentMal + '\'' +
            '}';
    }

    public boolean erTilsvarendeBestiltTidligere(BestiltEtterlysning etterlysning) {
        if (etterlysning == null) {
            return false;
        }
        if (!Objects.equals(dokumentMal, etterlysning.dokumentMal)) {
            return false;
        }
        var arbeidsgiverErLik = Objects.equals(arbeidsgiver, etterlysning.arbeidsgiver);
        var periodeErTilsvarende = periode.overlapper(etterlysning.periode);

        return Objects.equals(fagsakId, etterlysning.fagsakId) &&
            arbeidsgiverErLik &&
            periodeErTilsvarende;
    }

    public boolean erBestiltTilSammeMottakerIDenneBehandlingen(BestiltEtterlysning etterlysning) {
        if (etterlysning == null) {
            return false;
        }
        if (!Objects.equals(dokumentMal, etterlysning.dokumentMal)) {
            return false;
        }
        var arbeidsgiverErLik = Objects.equals(arbeidsgiver, etterlysning.arbeidsgiver);

        return Objects.equals(fagsakId, etterlysning.fagsakId) &&
            Objects.equals(behandlingId, etterlysning.behandlingId) &&
            arbeidsgiverErLik;
    }
}
