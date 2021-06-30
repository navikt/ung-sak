package no.nav.k9.sak.behandlingslager.behandling.søknadsfrist;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "AvklartSøknadsfristDokumentHolder")
@Table(name = "SF_AVKLART_DOKUMENTER")
@Immutable
public class KravDokumentHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SF_AVKLART_DOKUMENTER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "dokumenter_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<AvklartKravDokument> dokumenter;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    KravDokumentHolder() {
        // hibernate
    }

    public KravDokumentHolder(Set<AvklartKravDokument> dokumenter) {
        this.dokumenter = dokumenter;
    }

    public Set<AvklartKravDokument> getDokumenter() {
        return dokumenter;
    }
}
