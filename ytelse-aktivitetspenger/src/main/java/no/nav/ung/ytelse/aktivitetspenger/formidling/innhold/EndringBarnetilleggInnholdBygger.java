package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.BarnetilleggEndringsdatoUtleder;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.EndringBarnetilleggDto;

import java.time.LocalDate;

@Dependent
public class EndringBarnetilleggInnholdBygger implements VedtaksbrevInnholdBygger {

    private final AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository;
    private final boolean fødselFørStartdatoEnabled;

    @Inject
    public EndringBarnetilleggInnholdBygger(
        AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository,
        @KonfigVerdi(value = BarnetilleggEndringsdatoUtleder.TOGGLE_FØDSEL_FØR_STARTDATO, defaultVerdi = "false") boolean fødselFørStartdatoEnabled) {
        this.aktivitetspengerGrunnlagRepository = aktivitetspengerGrunnlagRepository;
        this.fødselFørStartdatoEnabled = fødselFørStartdatoEnabled;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, DetaljertResultatTidslinje tidslinje) {
        var fødselTidslinje = tidslinje.filtrerPåÅrsak(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);

        var grunnlag = aktivitetspengerGrunnlagRepository.hentGrunnlag(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Mangler grunnlag for aktivitetspenger"));

        var satsTidslinje = grunnlag.hentAktivitetspengerSatsTidslinje();
        LocalDate satsendringsdato = fødselFørStartdatoEnabled
            ? BarnetilleggEndringsdatoUtleder.finnFørsteDatoMedSats(fødselTidslinje, satsTidslinje)
            : fødselTidslinje.getMinLocalDate();

        var nyeSatser = satsTidslinje.getSegment(new LocalDateInterval(satsendringsdato, satsendringsdato)).getValue();

        if (nyeSatser.satsGrunnlag().antallBarn() == 0) {
            throw new IllegalStateException("Ingen barn på fom=" + satsendringsdato);
        }

        var sats = Satsberegner.beregnBarnetilleggSats(nyeSatser.satsGrunnlag());

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_ENDRING_BARNETILLEGG,
            new EndringBarnetilleggDto(satsendringsdato, nyeSatser.hentBeregnetSats().dagsatsBarnetillegg(), sats));
    }
}

