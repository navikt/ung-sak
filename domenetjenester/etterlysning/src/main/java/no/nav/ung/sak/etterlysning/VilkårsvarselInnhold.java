package no.nav.ung.sak.etterlysning;

import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

/**
 * Innhold for en vilkårsavklaring. Brukes i {@link VilkårsavklaringEtterlysningTjeneste},
 * men vilkåret som implementerer interfacet eier likhetssjekken!
 */
public interface VilkårsvarselInnhold {

    Periode periode();
    IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak();
    boolean skalSendeVarsel();

    default DatoIntervallEntitet hentPeriodeSomDatoIntervallEntitet() {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode().getFom(), periode().getTom());
    }
}
