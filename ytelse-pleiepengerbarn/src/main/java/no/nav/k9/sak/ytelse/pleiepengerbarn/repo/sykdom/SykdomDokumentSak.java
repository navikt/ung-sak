package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.typer.Saksnummer;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "SykdomDokumentSak")
@Table(name = "SYKDOM_DOKUMENT_SAK")
public class SykdomDokumentSak {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_DOKUMENT_SAK")
    private Long id;

    @Column(name = "SAKSNUMMER")
    private Saksnummer saksnummer;

    @ManyToOne
    @JoinColumn(name = "SAK_PERSON_ID", nullable = false)
    private SykdomPerson sakPerson;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR
}
