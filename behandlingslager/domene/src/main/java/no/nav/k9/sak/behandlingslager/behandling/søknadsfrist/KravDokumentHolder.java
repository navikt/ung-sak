package no.nav.k9.sak.behandlingslager.behandling.søknadsfrist;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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
