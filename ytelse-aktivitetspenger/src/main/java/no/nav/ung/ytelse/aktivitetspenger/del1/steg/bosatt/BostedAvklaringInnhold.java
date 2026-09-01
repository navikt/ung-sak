package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

// OBS: Det utføres set-operasjoner vha denne recorden.
// Feltene brukes for å avgjøre om en avklaring har endret innhold og må varsles på nytt.
// Feltet skalVarsle regnes som en del av innholdet, da en endring av dette feltet gjør at etterlysningene må oppdateres.
// Kilde regnes også som en del av innholdet, siden den skal vises for bruker i varselet.
// Feltene Referanse, vurdertAv og tidspunkt er utelatt.
public record BostedAvklaringInnhold(
    Periode periode,
    BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    String begrunnelse,
    boolean skalSendeVarsel,
    String fritekstTilVarsel,
    String begrunnelseIkkeVarsel,
    BostedsavklaringKildeType kilde,
    String kildeFritekst,
    Avklaringtype avklaringtype
) {

    public DatoIntervallEntitet hentPeriodeSomDatoIntervallEntitet() {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }
}
