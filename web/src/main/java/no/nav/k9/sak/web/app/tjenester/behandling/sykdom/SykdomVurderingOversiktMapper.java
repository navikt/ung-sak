package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.time.LocalDate;
import java.util.Arrays;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.typer.Periode;
import repo.sykdom.Resultat;
import repo.sykdom.SykdomVurderingType;

@ApplicationScoped
public class SykdomVurderingOversiktMapper {

    public SykdomVurderingOversikt map(Behandling behandling, SykdomVurderingType type) {
        return new SykdomVurderingOversikt(
            Arrays.asList(
                new SykdomVurderingOversiktElement("124d15", Resultat.OPPFYLT, new Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5)), true, false)
            ), 
            Arrays.asList(new Periode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(3))),
            Arrays.asList(new Periode(LocalDate.now().minusDays(8), LocalDate.now())),
            Arrays.asList(new Periode(LocalDate.now().minusDays(10), LocalDate.now()))
        );
    }

}
