package repo.sykdom;

import static repo.sykdom.SykdomFelles.BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity(name = "SykdomVurderinger")
@Table(name = "SYKDOM_VURDERINGER")
public class SykdomVurderinger {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_VURDERINGER")
    private Long id;

    @OneToOne
    @JoinColumn(name = "SYK_PERSON_ID", nullable = false)
    private SykdomPerson person;

    @OneToMany(mappedBy = "sykdomVurderinger", cascade = {CascadeType.PERSIST, CascadeType.REFRESH}) //TODO: cascades?
    private List<SykdomVurdering> vurderinger = new ArrayList<>();

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    SykdomVurderinger() {
        // hibernate
    }

    public SykdomVurderinger(
            SykdomPerson person,
            List<SykdomVurdering> vurderinger,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.person = person;
        this.vurderinger = vurderinger.stream()
            .peek(it -> it.setSykdomVurderinger(this))
            .collect(Collectors.toList());
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    @PrePersist
    protected void onCreate() {
        this.opprettetAv = opprettetAv != null ? opprettetAv : finnBrukernavn();
        this.opprettetTidspunkt = opprettetTidspunkt != null ? opprettetTidspunkt : LocalDateTime.now();
    }

    private static String finnBrukernavn() {
        String brukerident = SubjectHandler.getSubjectHandler().getUid();
        return brukerident != null ? brukerident : BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES;
    }

    public Long getId() {
        return id;
    }

    public SykdomPerson getPerson() {
        return person;
    }
}
