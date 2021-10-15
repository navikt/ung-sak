package no.nav.k9.sak.ytelse.omsorgspenger.repo;

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

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "OmsorgspengerGrunnlag")
@Table(name = "GR_OMP_AKTIVITET")
public class OmsorgspengerGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_OMP_AKTIVITET")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "fravaer_id")
    private OppgittFravær samletOppgittFravær;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "fravaer_id_fra_soeknad")
    private OppgittFravær oppgittFraværFraSøknad;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "fravaer_id_fra_korrigering_im")
    private OppgittFravær oppgittFraværFraKorrigeringIm;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    OmsorgspengerGrunnlag() {
    }

    /** Opprett uten oppgittFravær - kommer fra saksbehandler senere. */
    public OmsorgspengerGrunnlag(Long behandlingId, OppgittFravær samletOppgittFravær, OppgittFravær oppgittFraværFraSøknad, OppgittFravær oppgittFraværFraKorrigeringIm) {
        this.behandlingId = behandlingId;
        this.samletOppgittFravær = samletOppgittFravær;
        this.oppgittFraværFraSøknad = oppgittFraværFraSøknad;
        this.oppgittFraværFraKorrigeringIm = oppgittFraværFraKorrigeringIm;
    }

    Long getId() {
        return id;
    }

    public OppgittFravær getSamletOppgittFravær() {
        return samletOppgittFravær;
    }

    public OppgittFravær getOppgittFraværFraSøknad() {
        return oppgittFraværFraSøknad;
    }

    public OppgittFravær getOppgittFraværFraKorrigeringIm() {
        return oppgittFraværFraKorrigeringIm;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof OmsorgspengerGrunnlag))
            return false;
        var that = this.getClass().cast(o);
        return Objects.equals(samletOppgittFravær, that.samletOppgittFravær)
            && Objects.equals(oppgittFraværFraSøknad, that.oppgittFraværFraSøknad)
            && Objects.equals(oppgittFraværFraKorrigeringIm, that.oppgittFraværFraKorrigeringIm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(samletOppgittFravær, oppgittFraværFraSøknad, oppgittFraværFraKorrigeringIm);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id +
            ", behandling=" + behandlingId +
            ", samletOppgittFravær=" + samletOppgittFravær +
            ", oppgittFraværFraSøknad=" + oppgittFraværFraSøknad +
            ", oppgittFraværFraKorrigeringIm=" + oppgittFraværFraKorrigeringIm +
            ", aktiv=" + aktiv +
            ", versjon=" + versjon +
            '>';
    }

}
