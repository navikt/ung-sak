package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.Presedens;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.FørstegangsInnvilgelseInnholdBygger;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class FørstegangsInnvilgelseStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final FørstegangsInnvilgelseInnholdBygger førstegangsInnvilgelseInnholdBygger;

    @Inject
    public FørstegangsInnvilgelseStrategy(FørstegangsInnvilgelseInnholdBygger førstegangsInnvilgelseInnholdBygger) {
        this.førstegangsInnvilgelseInnholdBygger = førstegangsInnvilgelseInnholdBygger;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        var detaljertResultat = resultatTidslinje.tilVurdering();
        boolean manueltOpprettet = behandling.erManueltOpprettet();
        boolean minstEnPeriodeInnvilget = detaljertResultat.stream()
            .anyMatch(it -> erInnvilgelse(it.getValue(), manueltOpprettet));
        if (minstEnPeriodeInnvilget) {
            return List.of(VedtaksbrevStrategyResultat.medUredigerbarBrev(DokumentMalType.INNVILGELSE_DOK, førstegangsInnvilgelseInnholdBygger, "Automatisk brev ved ny innvilgelse. "));
        }
        return List.of();
    }

    private static boolean erInnvilgelse(DetaljertResultat r, boolean manueltOpprettet) {
        boolean nyPeriode = r.harÅrsak(BehandlingÅrsakType.NY_SØKT_PERIODE)
            || (manueltOpprettet && r.harÅrsak(BehandlingÅrsakType.RE_SATS_ENDRING));
        return nyPeriode && r.avslåtteVilkår().isEmpty() && r.ikkeVurderteVilkår().isEmpty();
    }

    @Override
    public Presedens presedens() {
        return Presedens.OVERSTYRENDE_ENKELTBREV;
    }
}
