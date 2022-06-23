package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.typer.Saksnummer;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "MedisinskGrunnlag")
@Table(name = "GR_MEDISINSK")
public class MedisinskGrunnlag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_MEDISINSK")
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "MEDISINSK_GRUNNLAGSDATA_ID", nullable = false)
    private MedisinskGrunnlagsdata grunnlagsdata;

    @ManyToOne
    @JoinColumn(name = "SOEKER_PERSON_ID", nullable = false)
    private Person søker;

    @ManyToOne
    @JoinColumn(name = "PLEIETRENGENDE_PERSON_ID", nullable = false)
    private Person pleietrengende;

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


    MedisinskGrunnlag() {

    }

    public MedisinskGrunnlag(MedisinskGrunnlagsdata grunnlagsdata, Person søker, Person pleietrengende, Saksnummer saksnummer,
                             UUID behandlingUuid, Long behandlingsnummer, Long versjon, String opprettetAv,
                             LocalDateTime opprettetTidspunkt) {
        this.grunnlagsdata = grunnlagsdata;
        this.søker = søker;
        this.pleietrengende = pleietrengende;
        this.saksnummer = saksnummer;
        this.behandlingUuid = behandlingUuid;
        this.behandlingsnummer = behandlingsnummer;
        this.versjon = versjon;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }


    public MedisinskGrunnlagsdata getGrunnlagsdata() {
        return grunnlagsdata;
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

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public boolean isFørsteGrunnlagPåBehandling() {
        return versjon == 0;
    }
}
