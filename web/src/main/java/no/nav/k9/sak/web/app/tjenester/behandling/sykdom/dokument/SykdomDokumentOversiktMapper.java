package no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;


@ApplicationScoped
public class SykdomDokumentOversiktMapper {
    
    public SykdomDokumentOversikt map(String behandlingUuid, Collection<SykdomDokument> dokumenter) {        
        final List<SykdomDokumentOversiktElement> elementer = hentDokumenter()
            .stream()
            .map(d -> {
                return new SykdomDokumentOversiktElement(
                        "" + d.getId(),
                        "1", // TODO: Sett riktig verdi.
                        d.getType(),
                        false,  // TODO: Sette riktig verdi.
                        LocalDate.now(), // TODO: Sette riktig verdi.
                        LocalDate.now(), // TODO: Sette riktig verdi.
                        LocalDateTime.now(), // TODO: Sette riktig verdi.
                        false,  // TODO: Sette riktig verdi.
                        Collections.emptyList()
                    ); 
            })
            .collect(Collectors.toList())
            ;

        return new SykdomDokumentOversikt(
                elementer,
                Arrays.asList()
                );
    }
       
    private List<SykdomDokument> hentDokumenter() {
        return Collections.emptyList();
    }

}
