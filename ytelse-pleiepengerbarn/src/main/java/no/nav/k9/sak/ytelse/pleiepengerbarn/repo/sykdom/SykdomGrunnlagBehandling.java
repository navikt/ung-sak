package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.typer.Saksnummer;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "SykdomGrunnlagBehandling")
@Table(name = "SYKDOM_GRUNNLAG_BEHANDLING")
public class SykdomGrunnlagBehandling {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_GRUNNLAG_BEHANDLING")
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "SYKDOM_GRUNNLAG_ID", nullable = false)
    private SykdomGrunnlag grunnlag;

    @ManyToOne
    @JoinColumn(name = "SOEKER_PERSON_ID", nullable = false)
    private SykdomPerson søker;
    
    @ManyToOne
    @JoinColumn(name = "PLEIETRENGENDE_PERSON_ID", nullable = false)
    private SykdomPerson pleietrengende;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "saksnummer", column = @Column(name = "SAKSNUMMER", nullable = false)))
    private Saksnummer saksnummer;

    @Column(name = "BEHANDLING_UUID", nullable = false)
    private UUID behandlingUuid;

    @Column(name = "BEHANDLINGSNUMMER", nullable = false)
    private Long behandlingsnummer;

    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    
    SykdomGrunnlagBehandling() {
        
    }
    
    public SykdomGrunnlagBehandling(SykdomGrunnlag grunnlag, SykdomPerson søker, SykdomPerson pleietrengende, Saksnummer saksnummer,
            UUID behandlingUuid, Long behandlingsnummer, Long versjon, String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.grunnlag = grunnlag;
        this.søker = søker;
        this.pleietrengende = pleietrengende;
        this.saksnummer = saksnummer;
        this.behandlingUuid = behandlingUuid;
        this.behandlingsnummer = behandlingsnummer;
        this.versjon = versjon;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }
    

    public SykdomGrunnlag getGrunnlag() {
        return grunnlag;
    }
    
    public Long getBehandlingsnummer() {
        return behandlingsnummer;
    }
    
    public Long getVersjon() {
        return versjon;
    }
    
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }
    
    public boolean isFørsteGrunnlagPåBehandling() {
        return versjon != 0;
    }
}
