package repo.sykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.typer.Saksnummer;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "SykdomVurderingVersjonBesluttet")
@Table(name = "SYKDOM_VURDERING_VERSJON_BESLUTTET")
public class SykdomVurderingVersjonBesluttet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_VURDERING_VERSJON")
    private Long id;

    SykdomVurderingVersjonBesluttet() {
        // hibernate
    }

}
