package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.Resultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingType;


@ApplicationScoped
public class SykdomVurderingMapper {

    public SykdomVurderingDto map(Behandling behandling, String vurderingId) {
        final var pleietrengende = behandling.getFagsak().getPleietrengendeAktørId();
        
        // TODO: MÅ SJEKKE OM VURDERING LIGGER I BEHANDLING.
        return new SykdomVurderingDto("lalaa", SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, 
                Arrays.asList(
                    new SykdomVurderingVersjonDto("2",
                      "Fordi forda.", Resultat.OPPFYLT,
                      Arrays.asList(new Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5))),
                      Arrays.asList(
                              new SykdomDokumentDto("nay1", SykdomDokumentType.LEGEERKLÆRING, true, false, LocalDate.now().minusDays(5), true),
                              new SykdomDokumentDto("lala", SykdomDokumentType.MEDISINSKE_OPPLYSNINGER, false, true, LocalDate.now().minusDays(8),false)
                      ),
                      "SaraSydokke", LocalDateTime.now()
                    ),
                    
                    new SykdomVurderingVersjonDto("1",
                            "Fordi forda.", Resultat.OPPFYLT,
                            Arrays.asList(new Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(8))),
                            Arrays.asList(new SykdomDokumentDto("nay1", SykdomDokumentType.LEGEERKLÆRING, true, false, LocalDate.now().minusDays(5), true)),
                            "SaraSydokke", LocalDateTime.now()
                          )
                    
                ),
                new SykdomVurderingAnnenInformasjon(
                    Arrays.asList(new Periode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(3))),
                    Arrays.asList(new Periode(LocalDate.now().minusDays(10), LocalDate.now()))
                )
            );
    }

}
