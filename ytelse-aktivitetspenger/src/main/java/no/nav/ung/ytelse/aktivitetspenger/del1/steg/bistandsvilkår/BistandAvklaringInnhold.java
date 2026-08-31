package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

// OBS: Det utføres set-operasjoner vha denne recorden.
// Feltene brukes for å avgjøre om en avklaring har endret innhold og må varsles på nytt.
// Feltet skalVarsle regnes som en del av innholdet, da en endring av dette feltet gjør at etterlysningene må oppdateres.
// Feltene Referanse, vurdertAv og tidspunkt er utelatt.
public record BistandAvklaringInnhold(
    Periode periode,
    BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    String begrunnelse,
    boolean skalSendeVarsel,
    String fritekstTilVarsel,
    String begrunnelseIkkeVarsel,
    Avklaringtype avklaringtype
) {

    public DatoIntervallEntitet hentPeriodeSomDatoIntervallEntitet() {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }
}
